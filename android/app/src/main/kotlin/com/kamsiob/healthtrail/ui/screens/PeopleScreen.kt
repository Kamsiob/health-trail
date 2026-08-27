package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton

object PeopleTags {
    const val NAME = "people"
    const val ADD = "people_add"
    fun row(id: String) = "people_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_people"
}

/**
 * Profiles: who this notebook is about, and switching between them. #379,
 * renamed by the owner in #468. Rewritten onto `ui/v4`, #386.
 *
 * **"If my mom and my dad are both in a nursing home."** One family, one phone,
 * two people whose care is entirely separate: two care teams, two medication
 * lists, two sets of paperwork that must never be confused with each other.
 *
 * **The data layer anticipated this and the surface never reached it.** Every
 * table has carried `subject_id` since Phase 0 and every query already filters
 * on it, so the separation is by construction rather than by discipline.
 *
 * **Exactly one person is showing at a time, and the switch says so plainly.**
 * A split view, or a filter that could be left half applied, is how two people's
 * records get mixed by somebody tired at two in the morning.
 */
@Composable
fun PeopleScreen(
    subjects: List<Repository.Subject>,
    activeId: String?,
    onSwitch: (Repository.Subject) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.more",
) {
    val strings = LocalStrings.current
    val showing = subjects.filter { it.id == activeId }
    val others = subjects.filterNot { it.id == activeId }

    Page(
        section = Repository.Section.CARE_TEAM,
        // **Its own tip, not the care team's.** #464: the section is right,
        // because this screen is about people and wears their ink, and the tip
        // that came with it opened "Who you call" on a screen about which
        // notebook is showing.
        tipKey = "profiles",
        eyebrow = strings["nav.more"],
        title = strings["people.title"],
        subtitle = strings["people.lead"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(PeopleTags.ROOT),
        // **The way in floats over the list rather than sitting under it.**
        // D200: it was the last `item` in the `LazyColumn`, and a section
        // screen has no capture button in that corner to compete with.
        fab = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = {
                    Icon(painter = painterResource(Symbols.addPerson), contentDescription = null)
                },
                text = { Text(text = strings["people.add"]) },
                // The sentence sits on the button's own node, `docs/TRAPS.md`.
                modifier = Modifier
                    .testTag(PeopleTags.ADD)
                    .semantics { contentDescription = strings["people.add"] },
            )
        },
    ) {
        labeledBlock(
            leading = true,
            label = strings["people.showing"],
            rows = showing.map { subject ->
                { PersonRow(subject = subject, isShowing = true, onClick = null) }
            },
        )

        labeledBlock(
            label = strings["people.others"],
            rows = others.map { subject ->
                {
                    PersonRow(
                        subject = subject,
                        isShowing = false,
                        onClick = { onSwitch(subject) },
                    )
                }
            },
        )

    }
}

@Composable
private fun PersonRow(
    subject: Repository.Subject,
    isShowing: Boolean,
    onClick: (() -> Unit)?,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.CARE_TEAM)

    ListRow(
        title = Bidi.isolate(subject.displayName),
        support = subject.relationship?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) },
        leading = { Avatar(name = subject.displayName, hue = hue) },
        // **The one showing carries a mark rather than a control.** It is
        // already what you are looking at, so a button that switches to it
        // would do nothing and read as broken. Rule 16.
        trailing = if (isShowing) {
            {
                Box(
                    modifier = Modifier
                        .clip(Radius.pill)
                        .background(colors.leafWash)
                        .padding(horizontal = Space.s, vertical = Space.xs),
                ) {
                    // bidi-ok: the app's own word for the state.
                    Eyebrow(text = strings["people.showing"], color = colors.leafInk)
                }
            }
        } else {
            null
        },
        isDoor = onClick != null,
        onClick = onClick,
        clickLabel = strings["people.switch"],
        modifier = Modifier.testTag(PeopleTags.row(subject.id)),
    )
}
