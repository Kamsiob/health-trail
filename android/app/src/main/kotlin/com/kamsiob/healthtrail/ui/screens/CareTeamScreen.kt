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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.Avatar
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
    onOpen: (Repository.Person) -> Unit,
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
        section = Repository.Section.CARE_TEAM,
        headingKey = "careteam.heading",
    ) {
        if (people.isEmpty()) {
            item { SectionEmpty(name = CareTeamTags.NAME, text = strings["careteam.empty"], section = Repository.Section.CARE_TEAM, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION)) }
            item { Spacer(Modifier.height(Space.l)) }
        }

        // **One group, not one card each.** The grid draws the people you call
        // as a single grouped surface with hairlines between them.
        if (people.isNotEmpty()) {
            item {
                GroupedSurface {
                    people.forEachIndexed { index, person ->
                        PersonRow(
                            person = person,
                            onCall = { onCall(person) },
                            onOpen = { onOpen(person) },
                            isLast = index == people.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **The way in sits under the list rather than over it.** A floating
        // button would be the second thing hovering above the content on a
        // screen that already carries the capture button, and section 5.5 gives
        // that position to capture alone.
        //
        // **Sized to its label**, D118 and #340, and every section list is the
        // same. Drawn full width it sat directly above the scaffold's way back
        // wearing the identical costume, so the foot of eight section screens
        // was two identical bars of which only the second leaves.
        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["careteam.add"],
                onClick = onAdd,
                modifier = Modifier.testTag(CareTeamTags.ADD),
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
    onOpen: () -> Unit,
    isLast: Boolean,
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

    // **A dense row with an avatar, not a card.** Grid screen 11, and
    // `DESIGN.md` 7: a card is for something with three or more lines a person
    // reads, and this is a name, a role and a number somebody scans for. Twelve
    // cards is a wall; twelve rows with initial marks is scannable in one pass.
    //
    // **The number is an outlined action on the row itself**, which is what the
    // grid draws and what law 2 gives to a dialable thing. It is the one action
    // worth taking about a person from a list, and it saves opening them first.
    DenseRow(
        title = heading.orEmpty(),
        subtitle = listOfNotNull(
            role.takeIf { it != heading },
            person.notes?.takeIf { it.isNotBlank() },
        ).let { Bidi.join(it) }.takeIf { it.isNotBlank() },
        leading = {
            // The subject's own avatar carries blue, per `DESIGN.md` 7, and
            // there is exactly one of them. Everyone else is the section's rose.
            Avatar(name = heading.orEmpty(), hue = hueFor(Repository.Section.CARE_TEAM))
        },
        trailingContent = if (phone != null) {
            {
                QuietButton(
                    label = if (heading == phone) {
                        strings["careteam.call"]
                    } else {
                        strings("careteam.call.number", "number" to phone)
                    },
                    onClick = onCall,
                    modifier = Modifier.testTag(CareTeamTags.call(person.id)),
                )
            }
        } else {
            null
        },
        divider = !isLast,
        onClick = onOpen,
        clickLabel = strings["open.action"],
        modifier = Modifier.testTag(CareTeamTags.person(person.id)),
    )
}
