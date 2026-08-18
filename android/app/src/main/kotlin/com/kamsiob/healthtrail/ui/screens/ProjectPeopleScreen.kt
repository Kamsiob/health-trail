package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.WaypointDot
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page

object ProjectPeopleTags {
    const val NAME = "project-people"
    const val CARE_TEAM = "project-people-care-team"
    fun person(id: String) = "project-people-$id"
    fun alsoIn(id: String) = "project-people-also-$id"
    fun call(id: String) = "project-people-call-$id"
}

/**
 * The people one project has actually involved. `DESIGN.md` 20.5, screen 14.
 *
 * **The project's own contacts, and not the care team.** The two overlap and
 * they are not the same list: the care team is everybody looking after the
 * person, and this is whoever has turned up in this process. Showing the whole
 * team under a Medicaid application would bury the two caseworkers who matter
 * in a list of nurses, so the team is a door at the bottom rather than the
 * contents.
 *
 * **Derived, and nothing here writes anything.** Somebody is on this project
 * because they are named on an entry linked to it, which the record already
 * says. There is no project-to-person table and this screen does not want one.
 *
 * **The "also in" row is the cross-project door**, which screen 14 calls the one
 * new navigation idea on this surface. A caseworker running two of somebody's
 * processes is the case where a person is holding two threads at once, and this
 * is the app saying so rather than leaving them to remember.
 *
 * **The count is how many times somebody turned up, never a score.** Rule 13:
 * nothing here says how responsive anybody has been, and nothing is colored by
 * how long it has been.
 */
@Composable
fun ProjectPeopleScreen(
    projectName: String,
    people: List<Repository.ProjectPerson>,
    careTeamSize: Int,
    onOpenPerson: (Repository.Person) -> Unit,
    onOpenProject: (Repository.EntryProject) -> Unit,
    onCall: (String) -> Unit,
    onOpenCareTeam: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Page(
        title = strings["project.people"],
        onBack = onBack,
        backLabel = strings["section.back.project"],
        modifier = modifier.testTag(SectionTags.root(ProjectPeopleTags.NAME)),
        eyebrow = strings["notebook.section.projects"],
        subtitle = Bidi.isolate(projectName),
    ) {
        if (people.isEmpty()) {
            item {
                // **Not an error and not a prompt.** Nobody has been named on
                // this project yet, which is the ordinary state of a process
                // somebody has not started ringing about. Rule 13.
                SectionEmpty(
                    name = ProjectPeopleTags.NAME,
                    lead = strings["project.people.empty.lead"],
                    text = strings["project.people.none"],
                    section = Repository.Section.CARE_TEAM,
                )
            }
        }

        items(people, key = { it.person.id }) { entry ->
            val person = entry.person
            Column {
                Block(padding = Space.none) {
                    ListRow(
                        title = Bidi.isolate(person.displayName),
                        // **Their role, then how often and when.** The role is
                        // what a person is looking for months later; the count
                        // is context and never a judgment on anybody.
                        support = Bidi.join(
                            person.roleLabel?.takeIf { it.isNotBlank() },
                            strings("project.people.mentions", "count" to entry.mentions),
                            entry.lastEdtf?.let {
                                strings(
                                    "project.people.last",
                                    "date" to EventDateText.render(strings, it),
                                )
                            },
                        ),
                        leading = {
                            Avatar(
                                name = person.displayName,
                                hue = hueFor(Repository.Section.CARE_TEAM),
                            )
                        },
                        isDoor = true,
                        onClick = { onOpenPerson(person) },
                        modifier = Modifier.testTag(ProjectPeopleTags.person(person.id)),
                    )
                }

                // **Dialable, per screen 14.** Only where there is a number:
                // a call control with nothing to ring is a control that does
                // nothing, D42.
                person.phone?.takeIf { it.isNotBlank() }?.let { number ->
                    Spacer(Modifier.height(Space.s))
                    Action(
                        label = strings("person.call", "number" to number),
                        onClick = { onCall(number) },
                        modifier = Modifier.testTag(ProjectPeopleTags.call(person.id)),
                    )
                }

                // **The cross-project door.** One row per other process they
                // are in, each a door, because a person who is in two is the
                // whole reason this row exists.
                entry.alsoIn.forEach { other ->
                    Spacer(Modifier.height(Space.s))
                    Block(padding = Space.none) {
                        ListRow(
                            title = strings(
                                "project.people.also_in",
                                "name" to Bidi.isolate(person.displayName),
                            ),
                            support = Bidi.isolate(other.name),
                            // **Marked as a project.** Without it this card is
                            // the same white row as the person above it and
                            // reads as another person rather than as a door
                            // into another process. The gold waypoint is what
                            // the entry screen already uses to mean a project.
                            leading = { WaypointDot(color = colors.gold) },
                            isDoor = true,
                            onClick = { onOpenProject(other) },
                            modifier = Modifier.testTag(
                                ProjectPeopleTags.alsoIn(other.id),
                            ),
                        )
                    }
                }
            }
        }

        // **The care team is a door, not the contents.** It is the other list,
        // and naming it here is how somebody gets to the people who are not on
        // this process. Drawn whatever the count, because a door that appears
        // only once there is something behind it is one nobody learns about.
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings["project.people.care_team.note"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(bottom = Space.s),
                )
            }
            Block(padding = Space.none) {
                ListRow(
                    title = strings["notebook.section.care_team"],
                    support = strings("project.people.care_team.count", "count" to careTeamSize),
                    isDoor = true,
                    onClick = onOpenCareTeam,
                    modifier = Modifier.testTag(ProjectPeopleTags.CARE_TEAM),
                )
            }
        }
    }
}
