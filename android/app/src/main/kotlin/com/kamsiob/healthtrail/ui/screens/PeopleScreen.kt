package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Avatar
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object PeopleTags {
    const val NAME = "people"
    const val ADD = "people_add"
    fun row(id: String) = "people_$id"
}

/**
 * Who this notebook is about, and switching between them. #379.
 *
 * **"If my mom and my dad are both in a nursing home."** One family, one
 * phone, two people whose care is entirely separate: two care teams, two
 * medication lists, two sets of paperwork that must never be confused with
 * each other.
 *
 * **The data layer anticipated this and the surface never reached it.** Every
 * table has carried `subject_id` since Phase 0 and every query already filters
 * on it, so the separation is by construction rather than by discipline: there
 * is no code path that could show one person's medication on the other's
 * screen. What was missing was a way to make a second subject and a way to
 * choose.
 *
 * **Exactly one person is showing at a time, and the switch says so plainly.**
 * A split view, or a filter that could be left half applied, is how two
 * people's records get mixed by somebody tired at two in the morning. One at a
 * time is the safe shape and it is also the simple one.
 */
@Composable
fun PeopleScreen(
    subjects: List<Repository.Subject>,
    activeId: String?,
    onSwitch: (Repository.Subject) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val showing = subjects.filter { it.id == activeId }
    val others = subjects.filterNot { it.id == activeId }

    SectionScaffold(
        name = PeopleTags.NAME,
        title = strings["nav.more"],
        heading = strings["people.title"],
        subtitle = strings["people.lead"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (showing.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "people.showing")
                Spacer(Modifier.height(Space.headerGap))
                GroupedSurface {
                    showing.forEach { subject ->
                        PersonRow(subject = subject, isShowing = true, onClick = null)
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        if (others.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "people.others")
                Spacer(Modifier.height(Space.headerGap))
                GroupedSurface {
                    others.forEachIndexed { index, subject ->
                        PersonRow(
                            subject = subject,
                            isShowing = false,
                            onClick = { onSwitch(subject) },
                            divider = index < others.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        item {
            QuietButton(
                label = strings["people.add"],
                onClick = onAdd,
                modifier = Modifier.testTag(PeopleTags.ADD),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

@Composable
private fun PersonRow(
    subject: Repository.Subject,
    isShowing: Boolean,
    onClick: (() -> Unit)?,
    divider: Boolean = false,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    DenseRow(
        title = Bidi.isolate(subject.displayName),
        subtitle = subject.relationship?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) },
        leading = {
            Avatar(
                name = subject.displayName,
                hue = hueFor(Repository.Section.CARE_TEAM),
            )
        },
        // **The one showing carries a mark rather than a control.** It is
        // already what you are looking at, so a button that switches to it
        // would do nothing and read as broken. Rule 16.
        trailingContent = if (isShowing) {
            {
                Box(
                    modifier = Modifier
                        .clip(Radius.pill)
                        .background(colors.leafWash)
                        .padding(horizontal = Space.s, vertical = Space.xs),
                ) {
                    Text(
                        text = strings["people.showing"],
                        style = HealthTrail.type.eyebrow,
                        color = colors.leafInk,
                    )
                }
            }
        } else {
            null
        },
        chevron = onClick != null,
        divider = divider,
        onClick = onClick,
        clickLabel = strings["people.switch"],
        modifier = Modifier.testTag(PeopleTags.row(subject.id)),
    )
}
