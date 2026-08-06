package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object StageTags {
    const val SHEET = "stage-sheet"
    const val WHEN = "stage-when"
    fun stage(id: String) = "stage-$id"
}

/**
 * Moving a project along its road. `DESIGN.md` 20.5, screen 12.
 *
 * **The road could be drawn and never advanced.** `moveProjectToStage` has
 * existed since the schema landed and nothing called it, so a project sat on
 * whatever stage the fixture put it on forever.
 *
 * **A stage already reached keeps its original date.** Roads turn back: an
 * application returns to In review after somebody asks for more. Overwriting
 * the first arrival would erase that it had ever been there, and the trail
 * carries the sequence.
 *
 * **Every stage is offered, not only the next one.** These processes skip
 * stages and go backward, and a control that only moves forward one step is a
 * control that is wrong the first time something unusual happens, which on
 * these processes is most of the time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageSheet(
    stages: List<Repository.ProjectStage>,
    currentStageId: String?,
    onPick: (Repository.ProjectStage, Edtf.Date) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var on by remember { mutableStateOf(Edtf.day(LocalDate.now())) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = null,
        modifier = Modifier.testTag(StageTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.stage.move"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.stage.move.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            // **The sheet's own copy promised this and nothing offered it.**
            // "The date is today unless you change it" was true of the writer
            // and false of the screen: tapping a stage recorded today and there
            // was no way to say otherwise. Somebody writing down on Thursday
            // that the letter arrived on Monday needs this, and rule 17 says a
            // date is never falsely precise and always the person's.
            //
            // **Today by default, so the common case stays one tap.**
            QuietButton(
                label = EventDateText.render(strings, on),
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth().testTag(StageTags.WHEN),
            )

            Spacer(Modifier.height(Space.m))

            GroupedSurface {
                stages.forEachIndexed { index, stage ->
                    DenseRow(
                        title = Bidi.isolate(stage.name),
                        subtitle = stage.enteredEdtf
                            ?.let {
                                strings(
                                    "project.stage.reached",
                                    "date" to EventDateText.render(strings, it),
                                )
                            }
                            ?: strings["project.stage.not_reached"],
                        selected = stage.id == currentStageId,
                        onClick = { onPick(stage, on) },
                        divider = index < stages.lastIndex,
                        modifier = Modifier.testTag(StageTags.stage(stage.id)),
                    )
                }
            }

            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = on,
            onPick = { on = it; picking = false },
            onDismiss = { picking = false },
            // A stage is reached in the past or today, never ahead: the road
            // records where the project got to, not where it is going.
            titleKey = "date.pick.title",
        )
    }
}
