package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object CareTeamTags {
    const val NAME = "care_team"
    fun person(id: String) = "care_team_person_$id"
    const val ADD = "care_team_add"
    fun call(id: String) = "care_team_call_$id"
}

/**
 * The care team: everyone involved, and how to reach them.
 *
 * **The whole point of this screen is the number.** A caregiver standing in a
 * corridor needs to call the case manager, and the value of writing her down at
 * all is that doing so is one tap later rather than a search through a phone,
 * an email, and a discharge folder. So the number is not metadata on this
 * screen. It is the action, and it sits on the row.
 *
 * **Somebody with no number is a complete row, not a broken one.** Rule 13:
 * partial is a finished state. The row says there is no number yet, in the same
 * quiet register as everything else, and never as a field the person failed to
 * fill in.
 *
 * Setup writes the first person here, if the person gave one, which means this
 * screen is very often not empty on the first visit. That is deliberate: the
 * one question setup asks about reaching somebody turns into a row that is
 * already useful.
 */
@Composable
fun CareTeamScreen(
    people: List<Repository.Person>,
    onCall: (Repository.Person) -> Unit,
    onRemove: (Repository.Person) -> Unit,
    onEdit: (Repository.Person) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = CareTeamTags.NAME,
        title = strings["notebook.section.care_team"],
        subtitle = strings["careteam.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (people.isEmpty()) {
            item { SectionEmpty(name = CareTeamTags.NAME, text = strings["careteam.empty"]) }
            item { Spacer(Modifier.height(Space.l)) }
        }

        for (person in people) {
            item(key = person.id) {
                PersonRow(
                    person = person,
                    onCall = { onCall(person) },
                    onRemove = { onRemove(person) },
                    onEdit = { onEdit(person) },
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **The way in sits under the list rather than over it.** A floating
        // button would be the second thing hovering above the content on a
        // screen that already carries the capture button, and section 5.5 gives
        // that position to capture alone.
        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["careteam.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(CareTeamTags.ADD),
            )
        }
    }
}

/**
 * One person, with the one thing worth doing about them.
 *
 * Rule 15's hierarchy: the name carries the weight, the role is the quiet
 * eyebrow above it that says why this person is in the notebook, and the
 * number is an action rather than a line of text to read out and dial by hand.
 */
@Composable
private fun PersonRow(
    person: Repository.Person,
    onCall: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val phone = person.phone?.takeIf { it.isNotBlank() }
    val role = person.roleLabel?.takeIf { it.isNotBlank() }
    val name = person.displayName.takeIf { it.isNotBlank() }

    // **A row always has a heading, and it is whatever the person actually
    // gave.** Every field is optional, so a name cannot be assumed. Somebody
    // recorded as a number and nothing else reads as that number, which is what
    // they wrote down and what they will recognize. The fallback never invents
    // a placeholder like "Unnamed", which rule 11 forbids and which would also
    // be the app characterizing somebody it knows nothing about.
    val heading = name ?: phone ?: role

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .removableByLongPress(strings["edit.hint"], onRemove, onEdit)
            .testTag(CareTeamTags.person(person.id))
            .padding(Space.cardPadding),
    ) {
        // The role is the eyebrow only when it is not already carrying the
        // heading, so it never appears twice on the same card.
        if (role != null && heading != role) {
            Text(text = role, style = HealthTrail.type.mono, color = colors.ink3Text)
            Spacer(Modifier.height(Space.xs))
        }

        if (heading != null) {
            Text(
                text = heading,
                style = HealthTrail.type.displayS,
                color = colors.ink,
            )
        }

        person.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = notes, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        Spacer(Modifier.height(Space.xs))

        if (phone != null) {
            // A text action rather than a filled button. There is one per card,
            // and a column of filled buttons turns a quiet list into a wall of
            // blue, which is section 2.2's accent spent on repetition. The
            // Unfiled tray made the same call for the same reason.
            //
            // When the number is already the heading, the action says "Call"
            // rather than repeating the number two lines under itself.
            TextAction(
                label = if (heading == phone) {
                    strings["careteam.call"]
                } else {
                    strings("careteam.call.number", "number" to phone)
                },
                onClick = onCall,
                modifier = Modifier.testTag(CareTeamTags.call(person.id)),
            )
        } else {
            Text(
                text = strings["careteam.no_phone"],
                style = HealthTrail.type.bodyS,
                color = colors.ink3Text,
            )
        }
    }
}
