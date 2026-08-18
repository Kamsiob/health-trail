package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object StepEditTags {
    const val SHEET = "step-edit-sheet"
    const val TEXT = "step-edit-text"
    const val NOTE = "step-edit-note"
    const val SAVE = "step-edit-save"
    const val EARLIER = "step-edit-earlier"
    const val LATER = "step-edit-later"
    const val DONE = "step-edit-done"
    const val REMOVE = "step-edit-remove"
}

/**
 * Everything that can be done to one step. `DESIGN.md` 20.5, screen 18.
 *
 * **One sheet rather than three controls on every row.** A row carrying move
 * up, move down and remove is three small targets repeated down the screen,
 * which fails section 9 at font scale 1.0 and falls apart at 2.0. The row is
 * the whole target and this is what it opens, per law 3.
 *
 * **The note is a field, not an afterthought.** `project_step.note` is where
 * "the woman on the phone said to call back after the 15th" goes, and that
 * sentence is the whole reason these processes are survivable.
 *
 * **Removing is offered plainly and is not dressed as a warning.** Rule 13:
 * taking a step off a list somebody was handed is an ordinary decision about
 * how to run their own process, and the template it came from is untouched. It
 * is a tombstone in the record either way, per the data contract, so nothing
 * is actually destroyed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditSheet(
    step: Repository.ProjectStep,
    /** False for the first step in the list, so the control is not offered. */
    canMoveEarlier: Boolean,
    /** False for the last one. */
    canMoveLater: Boolean,
    onSave: (text: String, note: String?) -> Unit,
    onMove: (earlier: Boolean) -> Unit,
    /**
     * Whether the step has been taken.
     *
     * **The schema has recorded this since the first migration and nothing in
     * the app could set it.** `project_step.is_done` was written by the
     * repository, read by nobody, and the one callback that reached it was
     * handed to a screen that never called it. #390.
     *
     * **Here rather than as a tick on every row**, per this sheet's own note
     * and law 3: the row is the whole target and this is what it opens.
     *
     * **It is the person's own record, never a score.** Rule 13: nothing counts
     * these, nothing shows a proportion, and an untaken step is "not yet"
     * rather than a failing.
     */
    onSetDone: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()

    var text by remember(step.id) { mutableStateOf(step.text) }
    var note by remember(step.id) { mutableStateOf(step.note.orEmpty()) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(StepEditTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // **Every sheet that carries a form scrolls**, or its own
                // action is unreachable on a short screen. The standing sheet
                // proved it on 2026-08-12: its Save sat below the fold with no
                // way to reach it, and the old test device was tall enough to
                // hide that for the life of the screen. #129.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.step.edit"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["project.step.text"],
                value = text,
                onValueChange = { text = it },
                support = strings["project.step.text.hint"],
                singleLine = false,
                imeAction = ImeAction.Next,
                fieldTestTag = StepEditTags.TEXT,
            )

            Spacer(Modifier.height(Space.m))

            DictatableField(
                label = strings["project.step.note"],
                value = note,
                onValueChange = { note = it },
                support = strings["project.step.note.hint"],
                singleLine = false,
                imeAction = ImeAction.Done,
                fieldTestTag = StepEditTags.NOTE,
            )

            // **Order is a control, not a drag.** A drag handle on a list this
            // short is a gesture somebody has to discover, and 20.5 puts
            // reordering here rather than on the row. Neither is offered where
            // it would do nothing.
            if (canMoveEarlier || canMoveLater) {
                Spacer(Modifier.height(Space.l))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    if (canMoveEarlier) {
                        Action(
                            label = strings["project.step.earlier"],
                            onClick = { onMove(true) },
                            modifier = Modifier.weight(1f).testTag(StepEditTags.EARLIER),
                        )
                    }
                    if (canMoveLater) {
                        Action(
                            label = strings["project.step.later"],
                            onClick = { onMove(false) },
                            modifier = Modifier.weight(1f).testTag(StepEditTags.LATER),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.l))

            // **Material's own switch**, D196, and the row is one node so a
            // reader hears the words and the state together rather than a
            // sentence and then an unlabeled control.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(StepEditTags.DONE),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                // bidi-ok: the app's own word for the state, from the catalog.
                Text(
                    text = strings[if (step.isDone) "project.step.done" else "project.step.not_done"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = step.isDone,
                    onCheckedChange = { onSetDone(it) },
                )
            }

            Spacer(Modifier.height(Space.l))

            Action(
                label = strings["common.save"],
                // The one thing a step cannot be is nothing. Everything else
                // about it, including the note, can wait or never happen.
                enabled = text.isNotBlank(),
                onClick = { onSave(text.trim(), note.trim().takeIf { it.isNotBlank() }) },
                modifier = Modifier.fillMaxWidth().testTag(StepEditTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["project.step.remove"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(StepEditTags.REMOVE),
            )

            Spacer(Modifier.height(Space.xs))

            Action(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
