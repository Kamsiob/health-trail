package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.data.ExportContainer
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.ChoiceRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object RestoreTags {
    const val NAME = "restore"
    const val HOW_REPLACE = "restore_how_replace"
    const val HOW_MERGE = "restore_how_merge"
    const val CHOOSE = "restore_choose"
    const val PASSPHRASE = "restore_passphrase"
    const val UNLOCK = "restore_unlock"
    const val CONFIRM = "restore_confirm"
    const val PROBLEM = "restore_problem"
    const val STATUS = "restore_status"
    const val MERGE_COUNTS = "restore_merge_counts"
}

/**
 * What the person chose to happen to the notebook that is already here.
 *
 * **A choice rather than a default, and 8.3 says so**: merge or replace is
 * explicit, described in plain words, never a guess. The two are genuinely
 * different promises. Replace means the file wins and anything written since it
 * was made is gone; keeping both means nothing here is removed and the later
 * version of anything written twice is the one kept.
 *
 * **Neither is preselected.** A default here is the app guessing which of those
 * two sentences the person meant, and one of them loses work.
 */
enum class RestoreHow { REPLACE, MERGE }

/** Where the restore has got to. */
sealed interface RestoreState {
    /** Nothing chosen yet. */
    data object Empty : RestoreState

    /**
     * Encrypted, and waiting for the passphrase. Not a failure, a question.
     *
     * `problem` carries the message from a previous attempt, so a mistyped
     * passphrase leaves the person on this screen able to try again rather
     * than sending them back to choose the file a second time. Retyping a
     * passphrase is the expected case; re-picking the file is not.
     */
    data class NeedsPassphrase(val problem: String? = null) : RestoreState

    /** Opened and read, nothing applied. The person decides from here. */
    data class Ready(val manifest: ExportContainer.Manifest) : RestoreState

    /** The file could not be read, and this says exactly why. */
    data class Problem(val message: String) : RestoreState

    data object Working : RestoreState

    /** Replaced. The notebook is what was in the file. */
    data object Done : RestoreState

    /**
     * Merged. Both notebooks are here.
     *
     * **A different sentence rather than the same one**, because the two
     * promises are different and telling somebody their notebook "is what was
     * in the file" after a merge would be false.
     */
    /**
     * What the merge actually did. #454.
     *
     * **It used to be thrown away.** `RestoreFlow` mapped the report to 0 and
     * the screen said only "Done. Both notebooks are here now." After two weeks
     * of a sibling's work, silence is the wrong answer, and contract 8.3
     * requires unknown content to be named rather than dropped, which the layer
     * below honored and this layer defeated.
     *
     * **Plain numbers rather than `MergeApply.Report`**, because that type is
     * `internal` and this one is not. Carrying it here would mean widening the
     * visibility of the merge engine to satisfy a screen, which is the wrong
     * direction: the screen needs five integers, not the engine.
     */
    data class Merged(
        val added: Int = 0,
        val updated: Int = 0,
        val unchanged: Int = 0,
        val conflicts: Int = 0,
        val attachments: Int = 0,
        val skipped: Int = 0,
        /** False when the counts are not known, so nothing is claimed. */
        val counted: Boolean = false,
    ) : RestoreState
}

/**
 * Restoring a notebook from an export.
 *
 * **Nothing is applied until the person has seen what is in the file.** Reading
 * and importing are separate in `ExportContainer` for exactly this reason, and
 * the screen follows it: choose a file, unlock it if it is locked, read what it
 * contains, and only then decide. Somebody about to replace their notebook
 * deserves to know they picked the right file first.
 *
 * **The warning says what actually happens.** Restoring replaces everything,
 * and anything written since the file was made is gone. That is the truth and
 * it is said before the button, not in a toast afterward.
 *
 * **A problem names itself.** The container's checks each say what was wrong
 * rather than reporting a generic failure, and this shows that sentence
 * unchanged. The one it cannot resolve is a wrong passphrase against a tampered
 * file, because GCM cannot tell them apart, so that message says both and
 * claims neither.
 */
@Composable
fun RestoreScreen(
    state: RestoreState,
    onChoose: () -> Unit,
    onUnlock: (String) -> Unit,
    onRestore: (RestoreHow) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var passphrase by remember(state) { mutableStateOf("") }
    // Reset whenever the state changes, so a second file never inherits the
    // answer given about the first one.
    var how by remember(state) { mutableStateOf<RestoreHow?>(null) }
    val busy = state is RestoreState.Working

    Page(
        title = strings["restore.title"],
        onBack = onBack,
        backLabel = strings["section.back.more"],
        modifier = modifier.testTag(SectionTags.root(RestoreTags.NAME)),
        eyebrow = strings["nav.more"],
        subtitle = strings["restore.lead"],
    ) {
        item {
            // **Filled, because it is the one thing this screen is for.**
            // Everything else here depends on having chosen a file, and law 2
            // gives the filled costume to the one primary action. It was a
            // full width outlined pill, which is the treatment the scaffold
            // uses at the foot to mean the way back, so the screen opened and
            // closed with the same bar twice. #340 and D118.
            Action(
                label = strings["restore.choose"],
                onClick = onChoose,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(RestoreTags.CHOOSE), emphasis = ActionEmphasis.Main,
            )

            when (state) {
                is RestoreState.Empty -> Unit

                is RestoreState.NeedsPassphrase -> {
                    Spacer(Modifier.height(Space.sectionGap))
                    Text(
                        text = strings["restore.passphrase"],
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                    state.problem?.let { problem ->
                        Spacer(Modifier.height(Space.s))
                        Text(
                            text = problem,
                            style = HealthTrail.type.bodyM,
                            color = colors.alertInk,
                            modifier = Modifier.testTag(RestoreTags.PROBLEM),
                        )
                    }
                    Spacer(Modifier.height(Space.m))
                    Field(
                        label = strings["export.passphrase"],
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        keyboardType = KeyboardType.Password,
                        masked = true,
                        imeAction = ImeAction.Done,
                        fieldTestTag = RestoreTags.PASSPHRASE,
                        support = strings["restore.passphrase.hint"],
                    )
                    Spacer(Modifier.height(Space.m))
                    Action(
                        label = strings["restore.unlock"],
                        onClick = { onUnlock(passphrase) },
                        enabled = passphrase.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().testTag(RestoreTags.UNLOCK), emphasis = ActionEmphasis.Main,
                    )
                }

                is RestoreState.Problem -> {
                    Spacer(Modifier.height(Space.sectionGap))
                    Text(
                        // The container's own sentence, unchanged. It names
                        // what was wrong, and rewording it here would lose
                        // that.
                        text = state.message,
                        style = HealthTrail.type.bodyL,
                        color = colors.alertInk,
                        modifier = Modifier.testTag(RestoreTags.PROBLEM),
                    )
                }

                is RestoreState.Ready -> {
                    Spacer(Modifier.height(Space.sectionGap))
                    Eyebrow(text = strings["restore.found"])
                    Spacer(Modifier.height(Space.headerGap))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.cardLarge)
                            .background(colors.card)
                            .padding(Space.cardPadding),
                    ) {
                        Text(
                            text = strings(
                                "restore.made",
                                "date" to madeOn(state.manifest.exportedAt),
                            ),
                            style = HealthTrail.type.bodyS,
                            color = colors.ink2,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = strings(
                                "restore.rows",
                                "count" to state.manifest.rowCounts.values.sum(),
                            ),
                            style = HealthTrail.type.displayS,
                            color = colors.ink,
                        )
                    }

                    Spacer(Modifier.height(Space.sectionGap))
                    Eyebrow(text = strings["restore.how.group"])
                    Spacer(Modifier.height(Space.m))

                    // One grouped surface rather than two cards: it is one
                    // question with two answers, each needing a sentence, which
                    // is a radio group. `ChoiceRow` is the same shape the
                    // appearance question uses.
                    Block(padding = Space.none) {
                        ChoiceRow(
                            label = strings["restore.how.replace.label"],
                            selected = how == RestoreHow.REPLACE,
                            onClick = { how = RestoreHow.REPLACE },
                            detail = strings["restore.how.replace.detail"],
                            modifier = Modifier.testTag(RestoreTags.HOW_REPLACE),
                        )
                        RowDivider(inset = false)
                        ChoiceRow(
                            label = strings["restore.how.merge.label"],
                            selected = how == RestoreHow.MERGE,
                            onClick = { how = RestoreHow.MERGE },
                            detail = strings["restore.how.merge.detail"],
                            modifier = Modifier.testTag(RestoreTags.HOW_MERGE),
                        )
                    }

                    // **The warning follows the choice**, because the two are
                    // different truths and a single sentence covering both
                    // would be true of neither. It is silent until the person
                    // has chosen, rather than warning about something they have
                    // not asked for.
                    how?.let { chosen ->
                        Spacer(Modifier.height(Space.l))
                        Text(
                            text = when (chosen) {
                                RestoreHow.REPLACE -> strings["restore.warning"]
                                RestoreHow.MERGE -> strings["restore.warning.merge"]
                            },
                            style = HealthTrail.type.bodyM,
                            // **Only one of these is a warning.** Replace loses
                            // work and earns the alert ink. Keeping both loses
                            // nothing, and its sentence begins "Nothing here is
                            // removed": setting that in alarm red was visible
                            // the moment it was looked at on the phone, and an
                            // app that shouts reassurance teaches people to
                            // ignore the color when it means something.
                            color = when (chosen) {
                                RestoreHow.REPLACE -> colors.alertInk
                                RestoreHow.MERGE -> colors.ink2
                            },
                        )
                    }

                    Spacer(Modifier.height(Space.l))
                    Action(
                        // The button says which of the two it will do, so the
                        // last thing read before the irreversible tap is the
                        // thing that is about to happen.
                        //
                        // **And it says neither until one is chosen.** Looking
                        // at the screen on the phone showed a disabled button
                        // reading "Replace everything with this" under two
                        // unselected options, which is the app asserting a
                        // choice nobody made and reads as replace being the
                        // default. The whole point of this control is that
                        // there is no default.
                        label = when (how) {
                            RestoreHow.MERGE -> strings["restore.confirm.merge"]
                            RestoreHow.REPLACE -> strings["restore.confirm"]
                            null -> strings["restore.confirm.unchosen"]
                        },
                        onClick = { how?.let(onRestore) },
                        enabled = !busy && how != null,
                        modifier = Modifier.fillMaxWidth().testTag(RestoreTags.CONFIRM), emphasis = ActionEmphasis.Main,
                    )
                }

                is RestoreState.Working, is RestoreState.Done, is RestoreState.Merged -> {
                    Spacer(Modifier.height(Space.sectionGap))
                    Text(
                        text = when (state) {
                            is RestoreState.Working -> strings["restore.working"]
                            is RestoreState.Merged -> strings["restore.done.merge"]
                            else -> strings["restore.done"]
                        },
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                        modifier = Modifier.testTag(RestoreTags.STATUS),
                    )

                    // **What the merge did, in counts.** #454. Plain numbers,
                    // no judgment and nothing concluded from them, rule 2.
                    val report = (state as? RestoreState.Merged)?.takeIf { it.counted }
                    if (report != null) {
                        Spacer(Modifier.height(Space.s))
                        Text(
                            text = strings(
                                "restore.merged.counts",
                                "added" to report.added,
                                "updated" to report.updated,
                                "unchanged" to report.unchanged,
                            ),
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                            modifier = Modifier.testTag(RestoreTags.MERGE_COUNTS),
                        )
                        Text(
                            text = strings(
                                "restore.merged.attachments",
                                "count" to report.attachments,
                            ),
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                        // **Only when there were any.** A zero here is not news,
                        // and rule 13 says an empty slot reads as "not yet"
                        // rather than as a finding.
                        if (report.conflicts > 0) {
                            Text(
                                text = strings(
                                    "restore.merged.conflicts",
                                    "count" to report.conflicts,
                                ),
                                style = HealthTrail.type.bodyM,
                                color = colors.ink2,
                            )
                        }
                        if (report.skipped > 0) {
                            Text(
                                text = strings(
                                    "restore.merged.skipped",
                                    "count" to report.skipped,
                                ),
                                style = HealthTrail.type.bodyM,
                                color = colors.ink2,
                            )
                        }
                    }
                }
            }

        }
    }
}

/** The day the file was made, in the reader's own locale. */
private fun madeOn(exportedAt: Long): String =
    Instant.ofEpochMilli(exportedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
