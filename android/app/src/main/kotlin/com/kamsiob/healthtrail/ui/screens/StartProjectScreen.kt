package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object StartProjectTags {
    const val ROOT = "start_project_root"
    const val CANCEL = "start_project_cancel"
    fun template(id: String) = "start_project_$id"
}

/**
 * Choosing a process to take on.
 *
 * **Each card says how many steps it carries before it is chosen.** These are
 * months of work, and somebody deciding whether to start a Medicaid application
 * tonight deserves to know it is eleven steps rather than three before they
 * commit, not after.
 *
 * **A process whose rules vary by state says so, plainly.** The catalog marks
 * those, and the app says the steps stay general rather than inventing a rule
 * for a state it knows nothing about. That is rule 2 at its most load bearing:
 * a confidently wrong step in a Medicaid application costs somebody real money.
 */
@Composable
fun StartProjectScreen(
    templates: List<TemplateCatalog.ProjectTemplate>,
    onChoose: (TemplateCatalog.ProjectTemplate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(StartProjectTags.ROOT)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item {
                    Spacer(Modifier.height(Space.l))
                    Text(
                        text = strings["projects.start.title"],
                        style = HealthTrail.type.displayL,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = strings["projects.start.lead"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.l))
                }

                for (template in templates) {
                    item(key = template.id) {
                        TemplateCard(template = template, onChoose = { onChoose(template) })
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }

                item { Spacer(Modifier.height(Space.l)) }
            }

            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(StartProjectTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateCatalog.ProjectTemplate,
    onChoose: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onChoose,
            )
            .testTag(StartProjectTags.template(template.id))
            .padding(Space.cardPadding),
    ) {
        Text(text = template.name, style = HealthTrail.type.displayS, color = colors.ink)

        template.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
            Spacer(Modifier.height(Space.xs))
            Text(text = subtitle, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        if (template.steps.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Text(
                // A count of what the process involves, not a progress meter
                // reading zero. Nothing has been started yet, and "0 of 14"
                // would put a scoreboard in front of somebody deciding whether
                // they have the strength to begin.
                text = strings("projects.step_count", "count" to template.steps.size),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
        }

        if (template.stateVariance) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings["projects.state_varies"],
                style = HealthTrail.type.bodyS,
                color = colors.ink3Text,
            )
        }
    }
}
