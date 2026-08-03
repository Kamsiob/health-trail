package com.kamsiob.healthtrail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.DatabaseKeyLost
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import com.kamsiob.healthtrail.ui.screens.DisclaimerScreen
import com.kamsiob.healthtrail.ui.screens.SetupAnswers
import com.kamsiob.healthtrail.ui.screens.SetupScreen
import com.kamsiob.healthtrail.ui.screens.SituationPickerScreen
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object AppRootTags {
    const val LOADING = "app_loading"
    const val UNRECOVERABLE = "app_unrecoverable"
}

/**
 * What the person sees, and in what order.
 *
 * Three states, and all three are real screens rather than one screen with
 * things missing:
 *
 * **Opening.** The database has to be unlocked before anything can be decided,
 * and that is Keystore work plus, on first run, executing the whole schema. It
 * is fast, and it is not instantaneous, so it says something is happening from
 * the moment it starts rather than showing a blank rectangle.
 *
 * **The gate.** Until the disclaimer is accepted, nothing else exists.
 *
 * **The notebook.** After acceptance.
 *
 * A fourth state exists that is not a screen so much as an honest dead end: if
 * the key that unlocks the database is gone, the notebook cannot be decrypted
 * and no amount of retrying changes that. It says so plainly rather than
 * pretending, and it does not blame the person.
 */
@Composable
fun AppRoot(
    themeChoice: ThemeChoice = ThemeChoice.DEFAULT,
    onThemeChoice: (ThemeChoice) -> Unit = {},
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<RootState>(RootState.Opening) }

    LaunchedEffect(Unit) {
        state = try {
            val repository = Repository.open(context)
            val accepted = repository.settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED)
            when {
                accepted == null -> RootState.Gate(repository)
                repository.activeSubject() == null -> RootState.Setup(repository)
                else -> RootState.Ready(repository)
            }
        } catch (_: DatabaseKeyLost) {
            RootState.Unrecoverable
        }
    }

    val strings = remember(context) { Strings.load(context) }

    CompositionLocalProvider(LocalStrings provides strings) {
        when (val current = state) {
            RootState.Opening -> OpeningScreen()

            RootState.Unrecoverable -> UnrecoverableScreen()

            is RootState.Gate -> DisclaimerScreen(
                onAccept = {
                    // Recorded with a timestamp so it is never shown twice.
                    state = RootState.Accepting(current.repository)
                },
            )

            is RootState.Accepting -> {
                OpeningScreen()
                LaunchedEffect(Unit) {
                    current.repository.putSettingTimestamp(
                        Repository.KEY_DISCLAIMER_ACCEPTED,
                        System.currentTimeMillis(),
                    )
                    // **Asked again rather than assumed.** This used to go
                    // straight to setup, which is right on a fresh install and
                    // wrong after a restore: restoring replaces everything
                    // including the acceptance, so somebody who has just put
                    // six months of their own notebook back on a new phone was
                    // shown "Who are you looking after" and, if they answered,
                    // would have created a second subject alongside the one
                    // they had just recovered.
                    //
                    // Found walking journey six, a notebook exported, moved to
                    // another device, and restored intact, with a fixture
                    // packed by `tools/fixtures/pack.py`.
                    state = if (current.repository.activeSubject() == null) {
                        RootState.Setup(current.repository)
                    } else {
                        RootState.Ready(current.repository)
                    }
                }
            }

            is RootState.Setup -> SetupScreen(
                onContinue = { answers -> state = RootState.Saving(current.repository, answers) },
                // Skipping still creates the notebook, with nothing in it. The
                // alternative is an app with nowhere to write, which turns a
                // skipped question into a dead end.
                onSkip = { state = RootState.Saving(current.repository, SetupAnswers("", "", "", "", "")) },
            )

            is RootState.Saving -> {
                OpeningScreen()
                LaunchedEffect(Unit) {
                    val repository = current.repository
                    val answers = current.answers
                    val subjectId = repository.createSubject(
                        displayName = answers.name,
                        relationship = answers.relationship,
                    )
                    if (answers.where.isNotBlank()) {
                        repository.createChapter(subjectId, answers.where)
                    }
                    if (answers.phoneName.isNotBlank() || answers.phoneNumber.isNotBlank()) {
                        repository.createPerson(
                            subjectId = subjectId,
                            displayName = answers.phoneName,
                            phone = answers.phoneNumber,
                        )
                    }
                    state = RootState.Situation(repository, subjectId)
                }
            }

            is RootState.Situation -> {
                var catalog by remember { mutableStateOf<TemplateCatalog.Situations?>(null) }
                LaunchedEffect(Unit) { catalog = TemplateCatalog.situations(context) }

                val loaded = catalog
                if (loaded == null) {
                    OpeningScreen()
                } else {
                    SituationPickerScreen(
                        situations = loaded,
                        onChoose = { situation ->
                            state = RootState.ApplyingSituation(
                                current.repository, current.subjectId, situation,
                            )
                        },
                        // A notebook with no situation template still works.
                        // Every section exists and nothing is missing, so this
                        // is a real answer rather than a postponement.
                        onSkip = { state = RootState.Ready(current.repository) },
                    )
                }
            }

            is RootState.ApplyingSituation -> {
                OpeningScreen()
                LaunchedEffect(Unit) {
                    current.repository.applySituation(
                        subjectId = current.subjectId,
                        templateId = current.situation.id,
                        threads = current.situation.threads.map { it.id to it.label },
                    )
                    state = RootState.Ready(current.repository)
                }
            }

            is RootState.Ready -> NotebookShell(
                repository = current.repository,
                themeChoice = themeChoice,
                onThemeChoice = onThemeChoice,
            )
        }
    }
}

private sealed interface RootState {
    data object Opening : RootState
    data object Unrecoverable : RootState
    data class Gate(val repository: Repository) : RootState
    data class Setup(val repository: Repository) : RootState
    data class Saving(val repository: Repository, val answers: SetupAnswers) : RootState
    data class Situation(val repository: Repository, val subjectId: String) : RootState
    data class ApplyingSituation(
        val repository: Repository,
        val subjectId: String,
        val situation: TemplateCatalog.Situation,
    ) : RootState
    data class Accepting(val repository: Repository) : RootState
    data class Ready(val repository: Repository) : RootState
}

/**
 * Shown while the database opens. Deliberately quiet: a spinner on a warm paper
 * background, no branding moment, no progress bar pretending to know how long
 * something takes.
 */
@Composable
private fun OpeningScreen() {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AppRootTags.LOADING),
        color = HealthTrail.colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(Space.screenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings["common.loading"],
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink2,
            )
        }
    }
}

/**
 * The key that unlocks the notebook is gone.
 *
 * This is the honest form of a bad outcome. It does not offer a retry, because
 * retrying cannot work, and offering one would waste the person's time at the
 * worst possible moment. It will offer import once the export container exists,
 * which is issue #9, and that is why backup is load bearing rather than
 * optional. See DECISIONS.md D24.
 */
@Composable
private fun UnrecoverableScreen() {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AppRootTags.UNRECOVERABLE),
        color = HealthTrail.colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(Space.screenHorizontal),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = strings["common.error.generic"],
                style = HealthTrail.type.bodyL,
                color = HealthTrail.colors.ink,
            )
        }
    }
}
