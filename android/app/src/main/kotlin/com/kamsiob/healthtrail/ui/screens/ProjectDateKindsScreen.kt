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
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object ProjectKindsTags {
    const val NAME = "project-kinds"
    const val ADD_FIELD = "project-kinds-add-field"
    const val ADD = "project-kinds-add"
    fun kind(id: String) = "project-kinds-$id"
}

/**
 * The kinds of date this project tends to have. `DESIGN.md` 20.5 screen 18.
 *
 * **These are the chips somebody taps when they write a date down**, and until
 * now they could only be added to: a template that offered "Renewal" to a
 * process that never renews left a chip in the way forever.
 *
 * **This list is what is offered next time and never a key into the record.**
 * A date already written down keeps the words the person used at the time, so
 * renaming a kind here does not reach back and rewrite what they recorded, and
 * removing one does not take the date with it. The screen says so, because
 * otherwise the only way to find out is to try it.
 */
@Composable
fun ProjectDateKindsScreen(
    projectName: String,
    kinds: List<Repository.ProjectDateKind>,
    onAdd: (String) -> Unit,
    onOpen: (Repository.ProjectDateKind) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    var pending by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ProjectKindsTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.kinds.title",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        backLabelKey = "section.back.setup",
        modifier = modifier,
    ) {
        item {
            Text(
                text = strings["project.kinds.lead"],
                style = type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        if (kinds.isEmpty()) {
            item {
                Text(
                    text = strings["project.kinds.none"],
                    style = type.bodyL,
                    color = colors.ink2,
                )
            }
        } else {
            item {
                Block(padding = Space.none) {
                    kinds.forEachIndexed { index, kind ->
                        ListRow(
                            title = Bidi.isolate(kind.label),
                            isDoor = true,
                            onClick = { onOpen(kind) },
                            modifier = Modifier.testTag(ProjectKindsTags.kind(kind.id)),
                        )
                        if (index != kinds.lastIndex) RowDivider(inset = false)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            DictatableField(
                label = strings["project.kinds.new"],
                value = pending,
                onValueChange = { pending = it },
                support = strings["project.kinds.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectKindsTags.ADD_FIELD,
            )
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["project.kinds.add"],
                enabled = pending.isNotBlank(),
                onClick = {
                    onAdd(pending.trim())
                    pending = ""
                },
                modifier = Modifier.testTag(ProjectKindsTags.ADD),
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

object KindEditTags {
    const val SHEET = "kind-edit-sheet"
    const val NAME = "kind-edit-name"
    const val SAVE = "kind-edit-save"
    const val REMOVE = "kind-edit-remove"
}

/**
 * Renaming or removing one date kind.
 *
 * **No reordering here.** The chips on the date sheet are capped and offered by
 * what the project has, and a handful of labels does not have an order somebody
 * reads down the way the road and the steps do. A control that exists because
 * the neighboring screen has one is decoration, section 10.8.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateKindEditSheet(
    kind: Repository.ProjectDateKind,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()

    var label by remember(kind.id) { mutableStateOf(kind.label) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(KindEditTags.SHEET),
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
                text = strings["project.kinds.one"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.kinds.keeps_dates"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["project.kinds.name"],
                value = label,
                onValueChange = { label = it },
                support = strings["project.kinds.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = KindEditTags.NAME,
            )

            Spacer(Modifier.height(Space.l))

            Action(
                label = strings["common.save"],
                enabled = label.isNotBlank(),
                onClick = { onSave(label.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(KindEditTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["project.kinds.remove"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(KindEditTags.REMOVE),
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
