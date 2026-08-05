package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.RoadSize
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.RoadStrip
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object StartPreviewTags {
    const val SHEET = "start-preview-sheet"
    const val NAME = "start-preview-name"
    const val CREATE = "start-preview-create"
    const val ROAD = "start-preview-road"
    const val STEPS = "start-preview-steps"
    const val PAPERS = "start-preview-papers"
    const val KINDS = "start-preview-kinds"
}

/**
 * What a template sets up, shown before anything is created.
 *
 * `DESIGN.md` 20.5, screen 04. **This is the template system made visible.** A
 * template was a word somebody agreed to without being told what it meant: the
 * old flow created the project, its road, its steps, its papers and its date
 * chips the instant a row was tapped, and the first time the person saw any of
 * it was on a screen that already existed. **Nothing is applied until Create.**
 *
 * **Each line says what will be created and that it can be changed**, which is
 * law 5 stated where somebody is actually deciding rather than in a document.
 * The road is drawn rather than described, because the road strip is what they
 * will see at the top of the project for the next eleven months.
 *
 * **Nothing here is an instruction.** 20.4: the starting steps are things
 * families in this process usually gather, not a list the app expects to see
 * completed, and the copy says so in those terms. Nothing counts them.
 *
 * **The name is here and pre-filled**, so the common case is one tap and the
 * uncommon case is one edit. A stage of its own that most people would tap
 * through is a stage that costs everybody a tap to serve a few, which is the
 * arithmetic rule 18 asks for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartProjectPreviewSheet(
    template: TemplateCatalog.ProjectTemplate,
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(template.id) { mutableStateOf(template.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = null,
        modifier = Modifier.testTag(StartPreviewTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // **Scrolls, because five defaults and a name do not fit at
                // font scale 2.0.** A sheet that clips its own Create button is
                // a sheet nobody can finish.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.preview.stage"],
                style = type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings["project.preview.title"],
                style = type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.l))

            // **The road, drawn rather than listed.** It is the thing they will
            // look at first on this project for months, and a row saying "3
            // stages" teaches nothing about what it will look like.
            if (template.stages.size >= 2) {
                RoadStrip(
                    stages = template.stages.map { RoadStage(it, reached = false) },
                    description = strings(
                        "project.preview.road.spoken",
                        "count" to template.stages.size,
                        "names" to template.stages.joinToString(", "),
                    ),
                    size = RoadSize.FULL,
                    modifier = Modifier.testTag(StartPreviewTags.ROAD),
                )
                Spacer(Modifier.height(Space.l))
            }

            GroupedSurface {
                DenseRow(
                    title = strings("project.preview.steps", "count" to template.steps.size),
                    subtitle = strings["project.preview.steps.aside"],
                    subtitleMaxLines = 3,
                    modifier = Modifier.testTag(StartPreviewTags.STEPS),
                )
                DenseRow(
                    title = strings("project.preview.papers", "count" to template.papers.size),
                    subtitle = strings["project.preview.papers.aside"],
                    subtitleMaxLines = 3,
                    modifier = Modifier.testTag(StartPreviewTags.PAPERS),
                )
                DenseRow(
                    title = strings["project.preview.kinds"],
                    subtitle = strings["project.preview.kinds.aside"],
                    subtitleMaxLines = 3,
                    divider = false,
                    modifier = Modifier.testTag(StartPreviewTags.KINDS),
                )
            }

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.preview.not_instructions"],
                style = type.bodyS,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.s))
            // **Which of the three answers it will open with, in words.** The
            // shape is the one template default nothing else on this sheet
            // shows, and it decides what the person sees first every time they
            // open the project. It is named here with where to change it, per
            // law 5.
            Text(
                text = strings(
                    "project.preview.lead",
                    "answer" to strings["today.card.project_${template.lead}.long"],
                ),
                style = type.bodyS,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["projects.name"],
                value = name,
                onValueChange = { name = it },
                hint = strings["projects.name.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = StartPreviewTags.NAME,
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["project.preview.create"],
                // A project has to be called something, and the template
                // already called it something, so this is only ever disabled by
                // somebody deliberately emptying the field.
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(StartPreviewTags.CREATE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                // **Back, not cancel.** Dismissing this returns to the picker
                // with nothing created, which is a step backward through a flow
                // rather than abandoning one, and saying cancel would suggest
                // something had already been started.
                label = strings["project.preview.back"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
