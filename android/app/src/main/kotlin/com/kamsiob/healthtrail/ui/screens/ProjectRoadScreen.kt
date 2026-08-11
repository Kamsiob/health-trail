package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RoadSize
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.RoadStrip
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectRoadTags {
    const val NAME = "project-road"
    const val ADD_FIELD = "project-road-add-field"
    const val ADD = "project-road-add"
    const val STRIP = "project-road-strip"
    fun stage(id: String) = "project-road-$id"
}

/**
 * The road, changed without penalty. `DESIGN.md` 20.5 screen 18, and law 5.
 *
 * **The road it draws is the road it edits.** The same `RoadStrip` sits at the
 * top, so what somebody is changing is the thing they see on the project rather
 * than an abstract list that turns into it. A stage list edited in the blind is
 * how a road ends up with two stages called nearly the same thing.
 *
 * **A stage the project has reached says so and is not protected.** Roads get
 * named wrong and processes turn out to have a step nobody knew about, and a
 * screen that refused to touch reached stages would be the app deciding it knew
 * the process better than the person running it. Removing one moves the project
 * back to where it actually got to, in the same transaction.
 */
@Composable
fun ProjectRoadScreen(
    projectName: String,
    stages: List<Repository.ProjectStage>,
    onAdd: (String) -> Unit,
    onOpen: (Repository.ProjectStage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    var pending by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ProjectRoadTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.road.title",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        backLabelKey = "section.back.setup",
        modifier = modifier,
    ) {
        item {
            Text(
                text = strings["project.road.lead"],
                style = type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.l))
        }

        // **Two stages is the least a road can be**, which is what `RoadStrip`
        // requires: one waypoint on a dashed line says nothing. A project down
        // to one stage is drawn as the list alone until it has a road again.
        if (stages.size >= 2) {
            item {
                RoadStrip(
                    stages = stages.map { RoadStage(it.name, it.isReached) },
                    description = strings(
                        "project.road.spoken",
                        "count" to stages.size,
                        "names" to stages.joinToString(", ") { it.name },
                    ),
                    size = RoadSize.FULL,
                    modifier = Modifier.testTag(ProjectRoadTags.STRIP),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        item {
            GroupedSurface {
                stages.forEachIndexed { index, stage ->
                    DenseRow(
                        title = Bidi.isolate(stage.name),
                        subtitle = if (stage.isReached) {
                            strings["project.road.reached"]
                        } else {
                            null
                        },
                        chevron = true,
                        onClick = { onOpen(stage) },
                        divider = index != stages.lastIndex,
                        modifier = Modifier.testTag(ProjectRoadTags.stage(stage.id)),
                    )
                }
            }
        }

        if (stages.isEmpty()) {
            item {
                Text(
                    text = strings["project.road.none"],
                    style = type.bodyL,
                    color = colors.ink2,
                )
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // The field names what you are typing; the button is the verb.
            DictatableField(
                label = strings["project.road.new"],
                value = pending,
                onValueChange = { pending = it },
                hint = strings["project.road.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectRoadTags.ADD_FIELD,
            )
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["project.road.add"],
                enabled = pending.isNotBlank(),
                onClick = {
                    onAdd(pending.trim())
                    pending = ""
                },
                modifier = Modifier.testTag(ProjectRoadTags.ADD),
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

object StageEditTags {
    const val SHEET = "stage-edit-sheet"
    const val NAME = "stage-edit-name"
    const val SAVE = "stage-edit-save"
    const val EARLIER = "stage-edit-earlier"
    const val LATER = "stage-edit-later"
    const val REMOVE = "stage-edit-remove"
}

/**
 * Everything that can be done to one stage.
 *
 * **The same shape as a step's sheet**, because they are the same job: rename,
 * reorder, remove. Two different answers to one problem is the drift section
 * 10.2 names, and somebody who has learned one of these has learned both.
 *
 * **Renaming keeps the arrival.** Deciding a stage is really called something
 * else does not change when the project got there, and the sheet says so where
 * that is true rather than leaving somebody to wonder what they are about to
 * lose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageEditSheet(
    stage: Repository.ProjectStage,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onSave: (String) -> Unit,
    onMove: (earlier: Boolean) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(stage.id) { mutableStateOf(stage.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = null,
        modifier = Modifier.testTag(StageEditTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.road.stage"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            if (stage.isReached) {
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["project.road.keeps_arrival"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["project.road.name"],
                value = name,
                onValueChange = { name = it },
                hint = strings["project.road.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = StageEditTags.NAME,
            )

            if (canMoveEarlier || canMoveLater) {
                Spacer(Modifier.height(Space.l))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    if (canMoveEarlier) {
                        QuietButton(
                            label = strings["project.step.earlier"],
                            onClick = { onMove(true) },
                            modifier = Modifier.weight(1f).testTag(StageEditTags.EARLIER),
                        )
                    }
                    if (canMoveLater) {
                        QuietButton(
                            label = strings["project.step.later"],
                            onClick = { onMove(false) },
                            modifier = Modifier.weight(1f).testTag(StageEditTags.LATER),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["common.save"],
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(StageEditTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["project.road.remove"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(StageEditTags.REMOVE),
            )

            Spacer(Modifier.height(Space.xs))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
