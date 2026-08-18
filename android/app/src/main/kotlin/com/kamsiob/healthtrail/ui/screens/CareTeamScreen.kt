package com.kamsiob.healthtrail.ui.screens

import com.kamsiob.healthtrail.ui.v4.listGroupShape
import com.kamsiob.healthtrail.ui.v4.RowDivider
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockIconAction
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.PersonHero
import com.kamsiob.healthtrail.ui.v4.PersonRow
import com.kamsiob.healthtrail.ui.v4.Segments
import com.kamsiob.healthtrail.ui.v4.hueForPerson
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource

object CareTeamTags {
    const val NAME = "care_team"
    fun person(id: String) = "care_team_person_$id"
    const val ADD = "care_team_add"
    fun call(id: String) = "care_team_call_$id"

    /** The lead person's own block, which is not a row. */
    const val LEAD = "care_team_lead"
    const val LEAD_CALL = "care_team_lead_call"
    const val LEAD_EMAIL = "care_team_lead_email"

    /** One side of the toggle: 0 is where they are, 1 is everyone else. */
    fun segment(index: Int) = "care_team_segment_$index"

    /**
     * The screen itself.
     *
     * **The tag the old scaffold produced**, so a journey that waits for this
     * screen still finds it after the rewrite. #386.
     */
    const val ROOT = "section_root_care_team"
}

/**
 * The care team: everyone involved, and how to reach them. Rewritten onto
 * `ui/v4` from `m3v4-3`, #386.
 *
 * **The whole point of this screen is the number.** Somebody standing in a
 * corridor needs to call the case manager, and the value of writing her down at
 * all is that doing so is one tap later rather than a search through a phone, an
 * email and a discharge folder. So the number is not metadata here. It is the
 * action, and it sits on the row.
 *
 * **The drawing raises one person and lists the rest**, which is rule 15 on the
 * screen that most needs it: the one you call is a block in the section's own
 * wash with calling and writing to them inside it, and everyone else is a row
 * with the one action. Fifteen names at one weight is the uniform weight rule 15
 * names, and it is what this screen used to be.
 *
 * **The accordions are gone and nothing is hidden.** They were the thing the
 * owner named: "just look at the accordions on the care team page, that's copy
 * and paste from the old design." The drawing answers with a toggle at the top,
 * so where somebody is and everybody outside it are two views of one list rather
 * than two doors, and the second is one tap away whether or not it is showing.
 * D185, D186.
 *
 * **Somebody with no number is a complete row, not a broken one.** Rule 13:
 * partial is a finished state. The row says there is no number yet in the same
 * quiet register as everything else, never as a field they failed to fill in.
 *
 * Setup writes the first person here if the person gave one, so this screen is
 * very often not empty on the first visit.
 */
@Composable
fun CareTeamScreen(
    people: List<Repository.Person>,
    onCall: (Repository.Person) -> Unit,
    onOpen: (Repository.Person) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The same people, most recently used first. #351.
     *
     * `Repository.peopleByRecentUse` already answered this. Empty falls back to
     * the order in [people], which is what a notebook with no history has to
     * offer and is honest.
     */
    byRecentUse: List<Repository.Person> = emptyList(),
    /**
     * Where the person being cared for actually is, so that facility's staff
     * lead the list. #379, the owner: the care team at the facility and the
     * medical staff outside it are two different groups.
     *
     * Null on a notebook with no current place, and then there is no toggle:
     * one list, everybody in it.
     */
    currentPlace: String? = null,
    /**
     * Writes to somebody, where they gave an address. `m3v4-3` draws it beside
     * calling on the person the screen leads with.
     *
     * Null leaves the block with one action, which is the ordinary case: most
     * people on a care team are a phone number and nothing else.
     */
    onEmail: ((Repository.Person) -> Unit)? = null,
    /**
     * What the way back says, which is where the person is actually going.
     *
     * **The shell already knows**, and provides it per destination, so the care
     * team reached from Today says Today. It defaulted to a key no catalog
     * defines, and the app crashed the moment the screen opened: `Strings`
     * refuses an unknown key rather than showing one. Seen on the phone, rule
     * 21, and invisible to the string check because a default is a plain
     * literal rather than a lookup.
     */
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val hue = hueFor(Repository.Section.CARE_TEAM)
    val hues = HealthTrail.colors.tabHues

    // **The ones somebody actually calls lead**, #351, and a pin beats recent
    // use outright because it is the person overriding the guess. #361.
    val ordered = byRecentUse.takeIf { it.isNotEmpty() } ?: people
    val lead = people.filter { it.pinnedAt != null }.maxByOrNull { it.pinnedAt ?: 0L }
        ?: ordered.firstOrNull()

    // **Where they are, against everywhere else.** The toggle only appears when
    // both sides have somebody in them: a control that filters a list into
    // itself and an empty one is furniture. #379.
    val here = people.filter {
        currentPlace != null && it.organizationName.equals(currentPlace, ignoreCase = true)
    }
    val elsewhere = people.filterNot { person -> here.any { it.id == person.id } }
    val split = here.isNotEmpty() && elsewhere.isNotEmpty()
    var side by rememberSaveable(split) { mutableStateOf(if (here.isEmpty()) 1 else 0) }

    val shown = when {
        !split -> people
        side == 0 -> here
        else -> elsewhere
    }.filterNot { it.id == lead?.id }

    Page(
        eyebrow = strings["notebook.section.care_team"],
        eyebrowColor = hue.ink,
        title = strings["careteam.heading"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(CareTeamTags.ROOT),
        // **The way in floats over the list rather than sitting under it.**
        // It was the last `item` in the `LazyColumn`, so adding somebody meant
        // scrolling past everybody already on the team, and the fixture has
        // fifteen. `docs/TRAPS.md`: a floating action button is on the
        // scaffold, not in the list.
        //
        // **The reason it was in the list did not survive being looked at.**
        // The comment said a floating button would be the second thing hovering
        // on a screen the capture button already hovers over. The capture
        // button is on the four destinations; a section screen is pushed over
        // them and has no button of its own in that corner, checked on the
        // phone rather than reasoned about. D200.
        fab = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = {
                    Icon(
                        painter = painterResource(Symbols.addPerson),
                        contentDescription = null,
                    )
                },
                text = { Text(text = strings["careteam.add"]) },
                // The sentence sits on the button's own node: the words are in
                // a `Text` two levels down inside the row Material builds, so
                // the node that takes the tap would otherwise have nothing to
                // say. `docs/TRAPS.md`, and rule 19 is a gate.
                modifier = Modifier
                    .testTag(CareTeamTags.ADD)
                    .semantics { contentDescription = strings["careteam.add"] },
            )
        },
    ) {
        if (people.isEmpty()) {
            item {
                Block {
                    Body(
                        // bidi-ok: the app's own sentence about an empty care
                        // team, never anything somebody typed.
                        text = strings["careteam.empty"],
                        color = HealthTrail.colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        if (split) {
            item {
                Segments(
                    options = listOf(
                        strings("careteam.at_place", "place" to currentPlace.orEmpty()),
                        strings["careteam.everyone"],
                    ),
                    selected = side,
                    onSelect = { side = it },
                    tagFor = { CareTeamTags.segment(it) },
                )
            }
        }

        // **The one you call, raised**, and only on the side they are on, so the
        // toggle moves the whole screen rather than leaving one person pinned
        // above a list they are not part of.
        lead?.takeIf { !split || (side == 0) == here.any { p -> p.id == it.id } }?.let { person ->
            item {
                val phone = person.phone?.takeIf { it.isNotBlank() }
                val email = person.email?.takeIf { it.isNotBlank() }
                PersonHero(
                    name = heading(person),
                    hue = hue,
                    role = person.roleLabel?.takeIf { it.isNotBlank() },
                    support = person.organizationName?.takeIf { it.isNotBlank() },
                    noNumber = strings["careteam.no_phone"],
                    call = phone?.let {
                        {
                            Action(
                                label = strings["careteam.call"],
                                onClick = { onCall(person) },
                                emphasis = ActionEmphasis.Main,
                                mark = Symbols.call,
                                // It opens the dialer with the number in it. The
                                // person presses the green button, so nothing is
                                // committed here and nothing ticks.
                                confirms = false,
                                modifier = Modifier
                                    .semantics {
                                        contentDescription =
                                            strings("careteam.call.number", "number" to it)
                                    }
                                    .testTag(CareTeamTags.LEAD_CALL),
                            )
                        }
                    },
                    email = email?.takeIf { onEmail != null }?.let {
                        {
                            Action(
                                label = strings["careteam.email"],
                                onClick = { onEmail?.invoke(person) },
                                mark = Symbols.mail,
                                modifier = Modifier
                                    .semantics {
                                        contentDescription =
                                            strings("careteam.email.address", "address" to it)
                                    }
                                    .testTag(CareTeamTags.LEAD_EMAIL),
                            )
                        }
                    },
                    onOpen = { onOpen(person) },
                    openLabel = strings["open.action"],
                    modifier = Modifier.testTag(CareTeamTags.LEAD),
                )
            }
        }

        // **The label names the side, and only where there are two.** With one
        // list there is nothing to tell apart, and "Outside the facility" over
        // everybody, including the staff at it, is the app saying something
        // untrue about the person's own notebook. Seen on the phone, rule 21.
        if (shown.isNotEmpty() && split) {
            item {
                Eyebrow(
                    text = if (side == 0) {
                        strings("careteam.at_place", "place" to currentPlace.orEmpty())
                    } else {
                        strings["careteam.everyone"]
                    },
                    // A place is a name somebody typed, so it keeps its own case
                    // and loses the tracking. `docs/V4.md` 2.1.
                    fixed = side != 0,
                )
            }
        }

        // **One list, not fifteen cards.** Rule 22: a card is for three or more
        // lines actually read, and a care team row is two lines glanced at
        // while somebody is looking for a phone number. Fifteen separate cards
        // with the between-groups gap between each is the shape that makes a
        // scanned list slow, and it was the loudest thing on this screen.
        //
        // **One lazy item, because a care team is not a long list.** The trail
        // is six hundred rows and pays for laziness; this is fifteen people and
        // grouping them costs nothing.
        item {
            Block(padding = Space.none, shape = listGroupShape) {
                shown.forEachIndexed { index, person ->
                    val phone = person.phone?.takeIf { it.isNotBlank() }
                    PersonRow(
                        name = heading(person),
                        hue = hueForPerson(person.id, hues),
                        support = support(person, showPlace = !split || side == 1),
                        onOpen = { onOpen(person) },
                        openLabel = strings["open.action"],
                        action = phone?.let {
                            {
                                // **A gold circular button carrying the phone**, which
                                // `m3v4-3` draws on every row of the unit list. A reader
                                // still hears whose number it is: fifteen controls all
                                // called "Call" is the ambiguity 5.12 exists to prevent.
                                BlockIconAction(
                                    mark = Symbols.call,
                                    // Whose number it is, as the label: fifteen
                                    // controls all called "Call" is the ambiguity 5.12
                                    // exists to prevent.
                                    label = strings("careteam.call.number", "number" to it),
                                    onClick = { onCall(person) },
                                    modifier = Modifier.testTag(CareTeamTags.call(person.id)),
                                    // Gold, wash and ink, which is what the drawing
                                    // puts on every row of the unit.
                                    container = HealthTrail.colors.goldWash,
                                    tint = HealthTrail.colors.goldInk,
                                )
                            }
                        },
                        modifier = Modifier.testTag(CareTeamTags.person(person.id)),
                        // The block carries the surface, so a row inside it painting
                        // its own would be a second container on top of the first.
                        container = Color.Transparent,
                        shape = RectangleShape,
                    )
                    if (index != shown.lastIndex) RowDivider()
                }
            }
        }

    }
}

/**
 * What a row is headed by, which is whatever the person actually gave.
 *
 * Every field is optional, so a name cannot be assumed. Somebody recorded as a
 * number and nothing else reads as that number, which is what they wrote down
 * and what they will recognize. **It never invents a placeholder**, which rule
 * 11 forbids and which would also be the app characterizing somebody it knows
 * nothing about.
 */
private fun heading(person: Repository.Person): String =
    person.displayName.takeIf { it.isNotBlank() }
        ?: person.phone?.takeIf { it.isNotBlank() }
        ?: person.roleLabel?.takeIf { it.isNotBlank() }
        ?: ""

/**
 * The quiet line under a name: what they do, and where, when that is not
 * already the answer to which side of the toggle you are on.
 *
 * **The number is not on it.** `m3v4-3` puts the role there and the number in
 * the button beside it, and the number as text was the line that pushed a real
 * United States number, fourteen characters of it, into the name's space. #361.
 */
private fun support(person: Repository.Person, showPlace: Boolean): String? {
    val role = person.roleLabel?.takeIf { it.isNotBlank() && it != heading(person) }
    val place = person.organizationName?.takeIf { showPlace && it.isNotBlank() }
    return listOfNotNull(role, place).joinToString(", ") { Bidi.isolate(it) }.takeIf { it.isNotBlank() }
}
