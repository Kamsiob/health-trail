package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Page

object AddInstructionTags {
    const val ROOT = "add_instruction_root"
    fun starter(id: String) = "add_instruction_$id"
}

/**
 * Choosing something to ask for.
 *
 * **Every starter shows its tag before it is chosen, not after.** Whether
 * federal rules back a request changes whether somebody asks for it at all, and
 * how hard they push. Hiding that until after the choice would make the app a
 * menu of equally weighted options, which is exactly the misleading thing.
 *
 * **The wording is shown in full.** These are sentences a person is going to
 * say out loud to a nurse or a social worker, and the value of the catalog is
 * the wording rather than the topic. A list of titles would make somebody
 * choose blind and then improvise the sentence themselves, which is the part
 * that is hard.
 *
 * **"How to ask" is practical and never legal advice.** The catalog's own
 * `ask_for` field, which `check_templates.py` already holds to the content
 * rules: ask for it in writing, ask who makes the call at night. Nothing about
 * rights beyond what the tag says.
 */
@Composable
fun AddInstructionScreen(
    catalog: TemplateCatalog.Instructions,
    onChoose: (TemplateCatalog.Instruction) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **Federal first**, because it is the group somebody can stand on and the
    // one most worth reading before the other. Ordered by an explicit rank
    // rather than alphabetically, which put "request" first and buried the
    // stronger group under the weaker one.
    val grouped = remember(catalog) {
        catalog.starters
            .groupBy { it.tag }
            .toList()
            .sortedBy { (tag, _) -> if (tag == "federal") 0 else 1 }
    }

    Page(
        title = strings["instructions.add"],
        onBack = onCancel,
        backLabel = strings["common.cancel"],
        modifier = modifier,
        eyebrow = strings[labelKey(Repository.Section.STANDING_INSTRUCTIONS)],
        section = Repository.Section.STANDING_INSTRUCTIONS,
        // **The form's own gaps, not the page's.** A form is one
        // question after another rather than a column of groups, and
        // it spaces itself inside its single item.
        itemSpacing = Space.none,
        band = {
        },
    ) {
        item {
                Spacer(Modifier.height(Space.m))
                FactBlock(
                    label = null,
                    text = strings["instructions.add.lead"],
                    tone = BlockTone.Section,
                    mark = Symbols.of(Repository.Section.STANDING_INSTRUCTIONS),
                    hue = hueFor(Repository.Section.STANDING_INSTRUCTIONS),
                )
            Spacer(Modifier.height(Space.l))
        }

        for ((tagKey, starters) in grouped) {
            val tag = catalog.tags[tagKey]
            item(key = "tag_$tagKey") {
                // bidi-ok: a catalog label, in the app's own words rather than the person's.
                Eyebrow(text = tag?.label ?: tagKey, fixed = false)
                Spacer(Modifier.height(Space.xs))
                if (tag != null) {
                    Text(
                        text = tag.explainer,
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }
                Spacer(Modifier.height(Space.headerGap))
            }
            for (starter in starters) {
                item(key = starter.id) {
                    StarterCard(starter = starter, onChoose = { onChoose(starter) })
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item { Spacer(Modifier.height(Space.l)) }
    }
}

@Composable
private fun StarterCard(
    starter: TemplateCatalog.Instruction,
    onChoose: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **Material's surface owns the container and the press.** #392, and the
    // tonal color replaces `card`. docs/V4.md 2.1. This one had no focus ring
    // at all, which Material's own state layer now gives it for free.
    Surface(
        onClick = onChoose,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddInstructionTags.starter(starter.id)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Space.cardPadding),
    ) {
        // bidi-ok: a catalog label, in the app's own words rather than the person's.
        Text(text = starter.name, style = HealthTrail.type.displayS, color = colors.ink)

        Spacer(Modifier.height(Space.sm))
        Eyebrow(text = strings["instructions.what_to_say"])
        Spacer(Modifier.height(Space.xs))
        // bidi-ok: a catalog label, in the app's own words rather than the person's.
        Text(text = starter.wording, style = HealthTrail.type.bodyL, color = colors.ink)

        starter.basis.takeIf { it.isNotBlank() }?.let { basis ->
            Spacer(Modifier.height(Space.sm))
            // bidi-ok: a catalog label, in the app's own words rather than the person's.
            Text(text = basis, style = HealthTrail.type.bodyS, color = colors.ink2)
        }

        starter.askFor.takeIf { it.isNotBlank() }?.let { askFor ->
            Spacer(Modifier.height(Space.sm))
            Eyebrow(text = strings["instructions.how_to_ask"])
            Spacer(Modifier.height(Space.xs))
            // bidi-ok: a catalog label, in the app's own words rather than the person's.
            Text(text = askFor, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        Spacer(Modifier.height(Space.sm))
        Text(
            text = strings["instructions.record"],
            style = HealthTrail.type.label,
            color = colors.blue,
        )
    }
    }
}
