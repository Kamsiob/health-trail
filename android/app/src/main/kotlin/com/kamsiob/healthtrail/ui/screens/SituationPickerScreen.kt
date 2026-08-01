package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object SituationPickerTags {
    const val ROOT = "situation_picker_root"
    const val LIST = "situation_picker_list"
    const val SKIP = "situation_picker_skip"
    fun row(id: String) = "situation_row_$id"
}

/**
 * Choosing the care setting, which is what configures the notebook.
 *
 * A situation template sets which notebook sections sit expanded, which care
 * threads are offered, the first days checklist, and the document slots. All of
 * it is editable and deletable afterward, so this is a starting point rather
 * than a commitment.
 *
 * **Choosing is not applying, and nothing is applied by looking.** Tapping a row
 * selects that setting and moves on. There is no preview state here yet, and
 * when the template library gains one it uses the same presentation as this,
 * per `MASTER_SPEC.md` section 4.10.
 *
 * **The posture strings are shown verbatim** at the top, from the catalog, per
 * `templates/SCHEMA.md`. They are what keep this content reading as structure
 * rather than as advice, and paraphrasing them in the interface is forbidden.
 *
 * **Skipping is a real path.** A notebook with no situation template still
 * works: every section exists, none is folded, and nothing is missing. The
 * person can pick one later.
 *
 * Composed from Display L, Body M, the list row idiom already used elsewhere,
 * and one text action. Nothing new was introduced.
 */
@Composable
fun SituationPickerScreen(
    situations: TemplateCatalog.Situations,
    onChoose: (TemplateCatalog.Situation) -> Unit,
    onSkip: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SituationPickerTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Text(
                text = strings["situation.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))

            Text(
                text = strings["situation.subtitle"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            if (situations.posture.generalGuide.isNotBlank()) {
                Spacer(Modifier.height(Space.sm))
                // Verbatim from the catalog. Not paraphrased, not shortened.
                Text(
                    text = situations.posture.generalGuide,
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }

            Spacer(Modifier.height(Space.l))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(SituationPickerTags.LIST),
            ) {
                items(situations.all, key = { it.id }) { situation ->
                    SituationRow(situation = situation, onClick = { onChoose(situation) })
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            Spacer(Modifier.height(Space.sm))

            TextAction(
                label = strings["situation.skip"],
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SituationPickerTags.SKIP),
            )
        }
    }
}

/**
 * One setting, as a card.
 *
 * Carries the name, the subtitle that tells two similar settings apart, and the
 * burden line, which is one sentence naming what is hard about this setting. The
 * burden is there so the person feels understood rather than processed, which is
 * what `templates/SCHEMA.md` says it is for.
 *
 * No chevron. A chevron implies going somewhere to look at something, and
 * tapping here chooses.
 */
@Composable
private fun SituationRow(
    situation: TemplateCatalog.Situation,
    onClick: () -> Unit,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                // The row's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(SituationPickerTags.row(situation.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = situation.name,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        if (situation.subtitle.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = situation.subtitle,
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }
        if (situation.burden.isNotBlank()) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = situation.burden,
                style = HealthTrail.type.bodyS,
                color = colors.ink3Text,
            )
        }
    }
}
