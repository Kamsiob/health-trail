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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.hueFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.FoldRowText
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
    const val REST_FOLD = "care_team_rest"
    fun placeFold(name: String) = "care_team_place_" + name.lowercase().replace(' ', '_')
    fun call(id: String) = "care_team_call_$id"
}

/**
 * How many people lead the screen, per grid screen 11, which draws three.
 *
 * Three is what fits above the fold beside a heading and a subtitle, and it is
 * short enough that the eye takes it as a set rather than as the top of a list.
 */
private const val LEAD_COUNT = 3

/**
 * How many have to be left over before the rest are worth folding at all.
 *
 * Folding one person behind a tap is furniture, and a roster of four fits on
 * the screen without any of this.
 */
private const val MIN_FOLDED = 2

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
    /**
     * The same people, most recently used first. #351.
     *
     * **The screen had every name at one weight**, where grid screen 11 draws
     * a short group of the ones somebody actually calls and folds the rest.
     * Fifteen rows and fifteen identical call pills is the uniform weight rule
     * 15 names, on the screen a person reaches for when they need a number in
     * the next ten seconds.
     *
     * `Repository.peopleByRecentUse` already answered this and this screen was
     * the only list not asking it. Empty falls back to the order in [people],
     * which is what a notebook with no history has to offer and is honest.
     */
    byRecentUse: List<Repository.Person> = emptyList(),
) {
    val strings = LocalStrings.current
    // **One open set rather than one flag**, because there is a fold per place
    // now and a boolean can only describe one of them. #353.
    var openPlaces by rememberSaveable { mutableStateOf(emptySet<String>()) }

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

        // **The ones you actually call lead, and the rest fold**, per grid
        // screen 11 and #351. Ordering by when somebody was last called or
        // written about is the same question the capture form and the unfiled
        // tray already ask, and this list was the only one not asking it.
        val ordered = byRecentUse.takeIf { it.isNotEmpty() } ?: people
        // **Anybody pinned leads, and recent use fills the rest.** #361, on the
        // owner's ruling: recent use is a good default and it was the only
        // answer, so somebody who wanted the night charge nurse at the top
        // could not say so. A pin is the person overriding the guess, so it
        // wins outright, and the count only grows if they pin more than three.
        // Most recently pinned first, the same ordering the trail's pinned run
        // uses.
        val pinned = people.filter { it.pinnedAt != null }
            .sortedByDescending { it.pinnedAt }
        val lead = pinned + ordered
            .filterNot { person -> pinned.any { it.id == person.id } }
            .take((LEAD_COUNT - pinned.size).coerceAtLeast(0))
        // **In the order they were added**, which is what somebody scanning a
        // roster expects, exactly as the thread list keeps `sort_index` for the
        // same reason. Two orders, because they answer two questions.
        val rest = people.filterNot { person -> lead.any { it.id == person.id } }

        // **Only split where splitting says something.** Folding one or two
        // people behind a tap to make a short list look organized is furniture,
        // and on a notebook with four names the whole roster fits anyway.
        val folds = rest.size >= MIN_FOLDED

        // **One group, not one card each.** The grid draws the people you call
        // as a single grouped surface with hairlines between them.
        if (people.isNotEmpty()) {
            val top = if (folds) lead else people
            item {
                GroupedSurface {
                    top.forEachIndexed { index, person ->
                        PersonRow(
                            person = person,
                            onCall = { onCall(person) },
                            onOpen = { onOpen(person) },
                            isLast = index == top.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **Folded by where they work, which is what grid screen 11 draws**:
        // "At Maplewood, 4 more" and "Outside, billing, ombudsman, 2". #353.
        //
        // **It could not be built until 2026-08-13**, because
        // `person.organization_id` shipped in Phase 0 with its own index and
        // nothing ever wrote it, so grouping by it produced one fold holding
        // everybody labeled with nothing. The person form asks now.
        //
        // **A notebook where nobody has a place looks exactly as it did**: one
        // group, one fold, the same words. That is the ordinary case and it
        // must not change shape because a feature exists.
        //
        // **People with no place are their own fold and it goes last**, because
        // "everyone else" is what it has always been and it is not a place.
        if (folds) {
            val byPlace = rest
                .groupBy { it.organizationName?.takeIf { name -> name.isNotBlank() } }
                .toList()
                .sortedWith(
                    compareBy(
                        { it.first == null },
                        { -it.second.size },
                        { it.first.orEmpty() },
                    ),
                )

            byPlace.forEach { (place, group) ->
                val key = place ?: NO_PLACE
                item(key = "fold_$key") {
                    if (place == null) {
                        FoldRow(
                            labelKey = "careteam.everyone",
                            expanded = openPlaces.contains(key),
                            onToggle = { openPlaces = openPlaces.toggle(key) },
                            count = group.size.toString(),
                            modifier = Modifier.testTag(CareTeamTags.REST_FOLD),
                        )
                    } else {
                        FoldRowText(
                            // A place is a name somebody typed, so it is
                            // isolated exactly as every other one is.
                            label = Bidi.isolate(place),
                            expanded = openPlaces.contains(key),
                            onToggle = { openPlaces = openPlaces.toggle(key) },
                            count = group.size.toString(),
                            modifier = Modifier.testTag(CareTeamTags.placeFold(place)),
                        )
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
                if (openPlaces.contains(key)) {
                    item(key = "group_$key") {
                        GroupedSurface {
                            group.forEachIndexed { index, person ->
                                PersonRow(
                                    person = person,
                                    onCall = { onCall(person) },
                                    onOpen = { onOpen(person) },
                                    isLast = index == group.lastIndex,
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
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
        // **The number is on the row's own line, not inside the button.** A
        // real United States number is "(555) 123-4567", fourteen characters
        // before the word Call, and a button carrying it took the row from the
        // name beside it however it was weighted. The fixture's "555 0121" is
        // seven and hid that for the life of this screen. #361, owner's words:
        // it needs to fit. On the line it wraps like any other detail, and the
        // button says the verb, which is what law 2 gives an action.
        subtitle = listOfNotNull(
            role.takeIf { it != heading },
            phone?.takeIf { it != heading },
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
                    label = strings["careteam.call"],
                    onClick = onCall,
                    // **A reader still hears whose number it is.** The word
                    // alone on fifteen rows would be fifteen controls called
                    // "Call", which is the ambiguity 5.12 exists to prevent.
                    modifier = Modifier
                        .semantics {
                            contentDescription =
                                strings("careteam.call.number", "number" to phone)
                        }
                        .testTag(CareTeamTags.call(person.id)),
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


/** The name a fold with no place goes by, which is never a real place name. */
private const val NO_PLACE = "care_team_no_place"

/** Open it if it is closed, close it if it is open. */
private fun Set<String>.toggle(key: String): Set<String> =
    if (contains(key)) this - key else this + key
