package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object ProjectPapersTags {
    const val NAME = "project-papers"
    const val ADD_FIELD = "project-papers-add-field"
    const val ADD = "project-papers-add"
    fun paper(id: String) = "project-papers-$id"
}

/**
 * The named places this project keeps its paper. `DESIGN.md` 20.5 screen 18.
 *
 * **A placeholder is a place, not a paper.** "The decision letter" exists
 * before the decision letter does, and 20.4 is explicit that an empty one reads
 * as "not yet" and never as a gap somebody has failed to fill. Nothing here
 * counts how many are filled and nothing chases the empty ones.
 *
 * **Every list this screen edits leaves the person's own documents alone.**
 * Taking a paper out of its place says it was not the thing this place was
 * waiting for; it does not delete the photograph. Removing the place does not
 * either. Somebody who photographed the wrong letter needs both of those and
 * should not have to lose the photograph to get them.
 */
@Composable
fun ProjectPapersScreen(
    projectName: String,
    papers: List<Repository.ProjectPaper>,
    onAdd: (String) -> Unit,
    onOpen: (Repository.ProjectPaper) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    var pending by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ProjectPapersTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.papers.title",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        backLabelKey = "section.back.setup",
        modifier = modifier,
    ) {
        item {
            Text(
                text = strings["project.papers.lead"],
                style = type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        if (papers.isEmpty()) {
            item {
                Text(
                    text = strings["project.papers.none"],
                    style = type.bodyL,
                    color = colors.ink2,
                )
            }
        } else {
            item {
                GroupedSurface {
                    papers.forEachIndexed { index, paper ->
                        DenseRow(
                            title = Bidi.isolate(paper.name),
                            // **"Waiting" and never "missing".** An empty place
                            // is the ordinary state of a paper nobody has been
                            // sent yet, rule 13, and the word chooses between
                            // those two readings for somebody scanning six of
                            // them at eleven at night.
                            subtitle = strings[
                                if (paper.isFilled) {
                                    "project.papers.filed"
                                } else {
                                    "project.papers.waiting"
                                },
                            ],
                            chevron = true,
                            onClick = { onOpen(paper) },
                            divider = index != papers.lastIndex,
                            modifier = Modifier.testTag(ProjectPapersTags.paper(paper.id)),
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            DictatableField(
                label = strings["project.papers.new"],
                value = pending,
                onValueChange = { pending = it },
                hint = strings["project.papers.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectPapersTags.ADD_FIELD,
            )
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["project.papers.add"],
                enabled = pending.isNotBlank(),
                onClick = {
                    onAdd(pending.trim())
                    pending = ""
                },
                modifier = Modifier.testTag(ProjectPapersTags.ADD),
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

object PaperEditTags {
    const val SHEET = "paper-edit-sheet"
    const val NAME = "paper-edit-name"
    const val SAVE = "paper-edit-save"
    const val EMPTY = "paper-edit-empty"
    const val REMOVE = "paper-edit-remove"
}

/**
 * Renaming, emptying or removing one paper placeholder.
 *
 * **Emptying and removing are different things and both are offered.** Taking
 * the wrong document out of the right place is the common mistake and it should
 * not require destroying the place; taking the place away is a decision about
 * how this project is organized. Neither touches the document.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperEditSheet(
    paper: Repository.ProjectPaper,
    onSave: (String) -> Unit,
    onEmpty: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()

    var name by remember(paper.id) { mutableStateOf(paper.name) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(PaperEditTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // **It scrolls, like every other sheet carrying a form.** #129
                // gave seven sheets this and these three were missed: each
                // stacks a title, a note, a field and three full width actions,
                // and on a Pixel 8 at font scale 2.0 Save, Remove and Cancel
                // are all below the sheet's own bottom edge with no way to
                // reach them. A click at a node outside the viewport does
                // nothing at all, which reads as a save that did not fire.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.papers.one"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.papers.keeps_document"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["project.papers.name"],
                value = name,
                onValueChange = { name = it },
                hint = strings["project.papers.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = PaperEditTags.NAME,
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["common.save"],
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(PaperEditTags.SAVE),
            )

            // Offered only where there is something to take out, so the sheet
            // never draws a control that would do nothing.
            if (paper.isFilled) {
                Spacer(Modifier.height(Space.s))
                TextAction(
                    label = strings["project.papers.empty"],
                    onClick = onEmpty,
                    modifier = Modifier.fillMaxWidth().testTag(PaperEditTags.EMPTY),
                )
            }

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["project.papers.remove"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(PaperEditTags.REMOVE),
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
