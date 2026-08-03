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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object StartProjectTags {
    const val OWN = "start_project_own"
    fun ownTemplate(id: String) = "start_project_template_$id"
    const val OWN_NAME = "start_project_own_name"
    const val OWN_START = "start_project_own_start"
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
    /** Starts a project with no template behind it, given what to call it. */
    onStartOwn: (String) -> Unit = {},
    /**
     * The person's own templates, offered above the sixteen.
     *
     * **A saved template nothing can start from is a saved template that does
     * not exist.** The library showed them and the start screen did not offer
     * them for the first hour, which is a feature with a hole through the
     * middle of it.
     */
    own: List<Repository.OwnTemplate> = emptyList(),
    onChooseOwn: (Repository.OwnTemplate) -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        // **`imePadding` because this screen grew a text field.** It had none
        // until the blank project option landed, and without it the keyboard
        // covered the very control the person had just typed a name for: type,
        // then dismiss the keyboard, then scroll, then tap. That is D38 for the
        // third time and it is invisible at rest, which is why 10.6 requires
        // looking at every screen with a field with the keyboard actually up.
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
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

                // **Theirs first.** Somebody who has made one has learned
                // something the catalog did not know, and putting the sixteen
                // shipped ones above it would say the opposite.
                if (own.isNotEmpty()) {
                    item {
                        GroupHeader(labelKey = "library.own")
                        Spacer(Modifier.height(Space.headerGap))
                    }
                    for (template in own) {
                        item(key = template.id) {
                            OwnTemplateCard(
                                template = template,
                                onChoose = { onChooseOwn(template) },
                            )
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                    item {
                        Spacer(Modifier.height(Space.s))
                        GroupHeader(labelKey = "library.shipped")
                        Spacer(Modifier.height(Space.headerGap))
                    }
                }

                for (template in templates) {
                    item(key = template.id) {
                        TemplateCard(template = template, onChoose = { onChoose(template) })
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }

                // **Something the catalog never heard of**, which
                // `MASTER_SPEC.md` 4.10 has always required and which nothing
                // offered. Sixteen processes is a good starting set and it is
                // not the world, and offering only sixteen made them read as
                // the only sixteen things that count.
                //
                // **Last, not first.** For most people one of the sixteen is
                // the answer, and putting the blank one at the top would ask
                // everybody to write from nothing before showing them they did
                // not have to.
                item {
                    Spacer(Modifier.height(Space.m))
                    OwnProject(onStart = onStartOwn)
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

/**
 * Starting something the catalog does not cover.
 *
 * **The name is the only thing asked for**, because it is the only thing the
 * app needs and everything else is the person's to add as they learn it. A
 * blank project with a name is a working project, which is rule 13's "partial
 * is a finished state" applied to a whole record rather than to a field.
 *
 * The action appears only once there is a name, because saving one with no name
 * would put an untitled row in a list whose whole job is to be scanned.
 */
@Composable
private fun OwnProject(onStart: (String) -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(StartProjectTags.OWN)
            .padding(Space.cardPadding),
    ) {
        Text(
            text = strings["projects.blank"],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = strings["projects.blank.aside"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.m))
        HealthTrailTextField(
            label = strings["projects.name"],
            value = name,
            onValueChange = { name = it },
            hint = strings["projects.name.hint"],
            fieldTestTag = StartProjectTags.OWN_NAME,
        )
        if (name.isNotBlank()) {
            Spacer(Modifier.height(Space.m))
            FilledButton(
                label = strings["projects.start"],
                onClick = { onStart(name.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(StartProjectTags.OWN_START),
            )
        }
    }
}

/**
 * One of the person's own templates, on the start screen.
 *
 * The same card shape as a shipped one, because it is the same choice. What
 * differs is the line under the name, which says where it came from rather than
 * what the process is: they wrote it, so they already know what it is for.
 */
@Composable
private fun OwnTemplateCard(
    template: Repository.OwnTemplate,
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
            .testTag(StartProjectTags.ownTemplate(template.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = strings("projects.step_count", "count" to template.steps.size),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.height(Space.xs))
        Text(text = template.name, style = HealthTrail.type.displayS, color = colors.ink)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = if (template.derivedFromId != null) {
                strings["library.derived"]
            } else {
                strings["library.scratch"]
            },
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}
