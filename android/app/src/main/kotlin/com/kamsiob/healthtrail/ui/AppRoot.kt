package com.kamsiob.healthtrail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.BuildConfig
import com.kamsiob.healthtrail.data.DatabaseKey
import com.kamsiob.healthtrail.data.DatabaseKeyLost
import com.kamsiob.healthtrail.data.DatabaseUnreadable
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.WelcomeSeen
import androidx.compose.foundation.layout.fillMaxWidth
import com.kamsiob.healthtrail.ui.screens.RestoreFlow
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
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Body

object AppRootTags {
    const val LOADING = "app_loading"
    const val UNRECOVERABLE = "app_unrecoverable"
    const val UNRECOVERABLE_RESTORE = "app_unrecoverable_restore"
    const val STUCK = "app_stuck"
    const val STUCK_RETRY = "app_stuck_retry"
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
    // **Bumped after a restore into a phone that had nothing readable**, so the
    // same decision runs again and finds the notebook that has just arrived.
    // #343.
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        state = try {
            val repository = Repository.open(context)
            // **Either the notebook or this phone remembers.** Restore replaces
            // the notebook and the acceptance goes with it, so somebody putting
            // their own backup on a new phone met "Before you start" the next
            // time the process started, which on this app is as ordinary as
            // changing the font scale. #307, D146.
            val accepted = repository.settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED) != null ||
                WelcomeSeen(context).seen()
            when {
                !accepted -> RootState.Gate(repository)
                repository.activeSubject() == null -> RootState.Setup(repository)
                else -> RootState.Ready(repository)
            }
        } catch (_: DatabaseKeyLost) {
            RootState.Unrecoverable
        } catch (_: DatabaseUnreadable) {
            // **The same dead end, and not the same sentence.** #407. The key
            // being gone and the file being torn both end here, and the screen
            // says which, because "the key that unlocked it is gone" is a false
            // account of a damaged file and the person acts on what it says.
            RootState.Damaged
        } catch (problem: Throwable) {
            // **Everything else used to escape and crash, on every launch,
            // forever.** #410. Only DatabaseKeyLost was caught, so
            // Migrations.Failed, Migrations.FromTheFuture, which is an archive
            // from a newer build or a sideloaded older one, and a plain
            // SQLiteException from a full or failing disk all came straight out
            // of this LaunchedEffect uncaught.
            //
            // **The record is intact on disk and completely unreachable**, and
            // the remedy a person reaches for is to uninstall and reinstall,
            // which is the one action that destroys it. A migration failure
            // must never end at "reinstall".
            //
            // The two named catches stay above this one because they are the
            // cases the app can say something specific and true about.
            RootState.Stuck(problem::class.java.simpleName + ": " + (problem.message ?: ""))
        }
    }

    // **Keyed on the language rather than on the context**, #326. Compose does
    // not replace the context object when the configuration changes, it updates
    // the configuration behind the same object, so `remember(context)` never
    // invalidated: setting the app's language flipped the layout to right to
    // left immediately and left every word in English until the process was
    // restarted. That reads exactly like a missing translation and is not one.
    val languageTag = LocalConfiguration.current.locales.get(0)?.toLanguageTag().orEmpty()
    val strings = remember(context, languageTag) { Strings.load(context) }

    CompositionLocalProvider(LocalStrings provides strings) {
        when (val current = state) {
            RootState.Opening -> OpeningScreen()

            is RootState.Stuck -> StuckScreen(
                problem = current.problem,
                onRetry = {
                    state = RootState.Opening
                    attempt += 1
                },
            )

            RootState.Damaged -> UnrecoverableScreen(
                reason = UnreadableReason.DAMAGED,
                onRestore = { state = RootState.Replacing(UnreadableReason.DAMAGED) },
            )

            RootState.Unrecoverable -> UnrecoverableScreen(
                // **The app does it rather than telling somebody to.** #343 and
                // rule 20: this screen said "install the app again and restore
                // from it", which is honest and is the app declining to absorb
                // its own complexity. The flow it opens is the same one More
                // uses, which is why that flow is a composable of its own.
                onRestore = { state = RootState.Replacing(UnreadableReason.KEY_LOST) },
            )

            is RootState.Replacing -> {
                RestoreFlow(
                    // **Back to the sentence they were reading**, not to the
                    // other one. #407 gave this screen two accounts of why the
                    // notebook will not open, and returning from the restore
                    // flow used to land on whichever one was written first.
                    onBack = {
                        state = when (current.from) {
                            UnreadableReason.KEY_LOST -> RootState.Unrecoverable
                            UnreadableReason.DAMAGED -> RootState.Damaged
                        }
                    },
                    onApplied = {
                        // The database the archive was restored into is this
                        // phone's now, so the decision at the top runs again
                        // and finds it the way any other launch would.
                        state = RootState.Opening
                        attempt += 1
                    },
                )
                // **The unreadable key and the file it cannot open go first.**
                // `Backup.restore` opens the database, and here that is exactly
                // what throws. Nothing is lost that was not already lost: the
                // person has been told this notebook cannot be opened, and
                // chose to replace it.
                LaunchedEffect(Unit) { DatabaseKey(context).discardUnreadable() }
            }

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
                    // The phone remembers too, so a restore cannot take this
                    // away from the person who did it. #307, D146.
                    WelcomeSeen(context).remember()
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
                        //
                        // **And it still gets a Today.** 21.5: nobody ever sees
                        // a blank one, and somebody who declined to name their
                        // setting has declined to name their setting, not asked
                        // for an empty screen.
                        onSkip = {
                            state = RootState.SkippingSituation(
                                current.repository, current.subjectId,
                            )
                        },
                    )
                }
            }

            is RootState.SkippingSituation -> {
                OpeningScreen()
                LaunchedEffect(Unit) {
                    current.repository.applyDefaultStartingHand(current.subjectId)
                    state = RootState.Ready(current.repository)
                }
            }

            is RootState.ApplyingSituation -> {
                OpeningScreen()
                LaunchedEffect(Unit) {
                    current.repository.applySituation(
                        subjectId = current.subjectId,
                        templateId = current.situation.id,
                        threads = current.situation.threads.map { it.id to it.label },
                        // **The Today this setting ships.** DESIGN.md 21.5:
                        // nobody ever sees a blank Today, and a person facing a
                        // blank dashboard and forty options builds nothing. It
                        // is a starting hand, editable from the first minute.
                        startingHand = current.situation.startingHand,
                        // **The two halves that sat in the data reading
                        // nothing**, #135. The first days list and the papers
                        // this setting produces become a project, and its name
                        // is composed here rather than in the repository for
                        // the same reason a thread's label is.
                        checklist = current.situation.checklist,
                        documents = current.situation.documents,
                        firstDaysName = strings["situation.firstdays"],
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

internal sealed interface RootState {
    data object Opening : RootState
    data object Unrecoverable : RootState

    /**
     * The file is on disk and cannot be read as a database. #407.
     *
     * **Its own state rather than a flag**, because the difference between this
     * and [Unrecoverable] is what the person is told and therefore what they do
     * next. Both offer the same one action.
     */
    data object Damaged : RootState

    /**
     * The notebook did not open and the app cannot say why. #410.
     *
     * **Not a dead end, which is the difference from [Unrecoverable].** The
     * record is on the disk and readable; something between it and the screen
     * failed. A migration that will not run, a disk with no room left, a
     * database written by a newer build than this one. Every one of those used
     * to crash at launch, on every launch, and the remedy a person reaches for
     * is the one that destroys the record.
     */
    data class Stuck(val problem: String) : RootState

    /**
     * The person chose to replace a notebook this phone cannot open. #343.
     *
     * **A state rather than a flag on the screen**, because what happens here
     * is a sequence: the unreadable key and the file it cannot open are thrown
     * away, then an archive is chosen and restored into a database this phone
     * can read. `RootStatesTest` walks it, which is the only way this screen
     * gets looked at: reaching it for real means a factory reset.
     */
    data class Replacing(val from: UnreadableReason) : RootState
    data class Gate(val repository: Repository) : RootState
    data class Setup(val repository: Repository) : RootState
    data class Saving(val repository: Repository, val answers: SetupAnswers) : RootState
    data class Situation(val repository: Repository, val subjectId: String) : RootState
    data class ApplyingSituation(
        val repository: Repository,
        val subjectId: String,
        val situation: TemplateCatalog.Situation,
    ) : RootState

    /**
     * The person declined to name a setting, and still gets a Today.
     *
     * A state of its own rather than a call from the button, because applying a
     * layout is a suspend write and a callback is not a coroutine scope.
     */
    data class SkippingSituation(
        val repository: Repository,
        val subjectId: String,
    ) : RootState
    data class Accepting(val repository: Repository) : RootState
    data class Ready(val repository: Repository) : RootState
}

/**
 * Shown while the database opens. Deliberately quiet: one word on warm paper,
 * no branding moment, and **no spinner and no progress bar**, because neither
 * knows how long a database read takes and both would be the app performing
 * effort at somebody who is waiting.
 *
 * **The KDoc here used to describe a spinner that was never in the code**, and
 * a comment claiming a component that does not exist is worse than no comment:
 * the next person changes the code to match the comment. Corrected 2026-08-11.
 *
 * The word itself is [Waiting], which is the app's one loading treatment.
 */
@Composable
internal fun OpeningScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AppRootTags.LOADING),
        color = HealthTrail.colors.paper,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(Space.screenHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            // bidi-ok: the app's own word for waiting.
            Body(text = LocalStrings.current["common.loading"])
        }
    }
}

/**
 * The key that unlocks the notebook is gone.
 *
 * This is the honest form of a bad outcome. It does not offer a retry, because
 * retrying cannot work, and offering one would waste the person's time at the
 * worst possible moment. See `DECISIONS.md` D24.
 *
 * **It used to say "That did not work. Nothing was changed."** That is the
 * copy for an action that failed, and it is the wrong sentence in the one
 * place in the app where it matters most: it says nothing about what happened,
 * it implies there is something else to try, and "nothing was changed" is true
 * and beside the point when the notebook cannot be opened at all. Found on
 * 2026-08-11 by reading D24 against the screen it decided.
 *
 * **What it says now is what D24 decided years of design ago**: the key is
 * gone, a factory reset or some device transfers take it, what the person
 * wrote is still on the phone and nothing can read it without that key, and
 * the one real answer is the file they exported. D24 is also why backup is
 * load bearing rather than optional: it is the only recovery path this app
 * has for its own encryption.
 *
 * **What it still does not do is offer restore from here**, which is #343.
 * Telling somebody to install the app again is honest and it is not finished.
 */
/**
 * Which of the two dead ends this is. #407.
 *
 * The title and the one action are the same either way. The account of what
 * happened is not, and it is the part the person acts on.
 */
internal enum class UnreadableReason { KEY_LOST, DAMAGED }

@Composable
internal fun UnrecoverableScreen(
    reason: UnreadableReason = UnreadableReason.KEY_LOST,
    onRestore: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val bodyKey = when (reason) {
        UnreadableReason.KEY_LOST -> "unrecoverable.body"
        UnreadableReason.DAMAGED -> "unrecoverable.damaged.body"
    }
    val nextKey = when (reason) {
        UnreadableReason.KEY_LOST -> "unrecoverable.next"
        UnreadableReason.DAMAGED -> "unrecoverable.damaged.next"
    }
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
                text = strings["unrecoverable.title"],
                style = HealthTrail.type.displayM,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings[bodyKey],
                style = HealthTrail.type.bodyL,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.m))
            // **The answer, not an apology.** D24 says the honest one is the
            // export, so it is the last thing read and it is the only thing
            // here that tells somebody what to do next.
            Text(
                text = strings[nextKey],
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink2,
            )
            // **The one action, and it is the answer rather than an
            // instruction.** #343: this screen used to end by telling somebody
            // to install the app again and restore from their file, which is
            // work the app can do for them. Rule 20.
            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["unrecoverable.restore"],
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth().testTag(AppRootTags.UNRECOVERABLE_RESTORE), emphasis = ActionEmphasis.Main,
            )
        }
    }
}


/**
 * The notebook did not open, and this screen exists so that the person does not
 * uninstall the app. #410.
 *
 * **What it must do, in order.** Say plainly that the record is still there.
 * Say what went wrong, in the app's own words plus the raw problem, because the
 * one person who can act on the raw text is whoever they show the phone to. Name
 * the version, because "a database from a newer build" is only diagnosable
 * against a version number. Then ask them not to uninstall, which is the
 * sentence the whole screen is for.
 *
 * **One action, and it is Try again rather than restore.** Restore replaces the
 * notebook, and this is the state where the notebook is intact. A disk that was
 * full a moment ago is not full now, and a retry costs nothing. Offering to
 * replace an intact record as the remedy for a transient error would be the
 * screen doing the damage it exists to prevent.
 */
@Composable
internal fun StuckScreen(problem: String, onRetry: () -> Unit = {}) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AppRootTags.STUCK),
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
                text = strings["stuck.title"],
                style = HealthTrail.type.displayM,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["stuck.body"],
                style = HealthTrail.type.bodyL,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.m))
            // **The one sentence this screen exists for.** Rule 13: it is not a
            // scolding and not a warning color, it is a fact about what
            // uninstalling does.
            Text(
                text = strings["stuck.keep"],
                style = HealthTrail.type.bodyL,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.m))
            Text(
                text = strings("stuck.problem", "problem" to problem),
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink2,
            )
            Text(
                text = strings("about.version", "version" to BuildConfig.VERSION_NAME),
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink2,
            )
            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["stuck.retry"],
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().testTag(AppRootTags.STUCK_RETRY),
                emphasis = ActionEmphasis.Main,
            )
        }
    }
}
