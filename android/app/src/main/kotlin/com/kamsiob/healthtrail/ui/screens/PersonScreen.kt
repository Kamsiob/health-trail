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
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.Avatar
import com.kamsiob.healthtrail.ui.components.AvatarSize
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object PersonTags {
    const val NAME = "person"
    const val CALL = "person_call"
    const val EDIT = "person_edit"
    fun entry(id: String) = "person_entry_$id"
}

/**
 * One person, and every call and visit that involved them.
 *
 * **`MASTER_SPEC.md` section 3 promises exactly this**, in one line: a person
 * knows every call and visit involving them. **It had no data behind it.**
 * `entry_person` has been in the schema since Phase 0 with nothing writing to
 * it, because capture kept who was spoken to as the entry's title, which is a
 * string. A person's page could not have listed their own calls if it had
 * existed, and it did not exist.
 *
 * Both halves landed together on 2026-08-03: the capture form offers the care
 * team as chips, choosing one writes the link, and this reads it back.
 *
 * **Drawn on the spine, dashed**, per `DESIGN.md` 5.2.3, because this is a
 * filter over the record rather than the person's own path. Same reasoning as
 * a search result and deliberately not the continuous line an incident thread
 * gets.
 *
 * **Their number is one tap away**, which is the whole argument for the care
 * team section: somebody standing in a corridor who needs the charge nurse
 * needs the number, not a form.
 */
@Composable
fun PersonScreen(
    person: Repository.Person,
    entries: List<Repository.TrailEntry>,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = PersonTags.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["notebook.section.care_team"],
        heading = Bidi.isolate(person.displayName.ifBlank { strings["person.unnamed"] }),
        section = Repository.Section.CARE_TEAM,
        subtitle = person.roleLabel?.takeIf { it.isNotBlank() }
            ?: strings["person.norole"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        // **Identity first, at 56dp.** A person's screen opens with the person,
        // and initials in their section's own wash are what makes a list of
        // nine names a list of nine people. It sits under the name rather than
        // beside it, because the scaffold's heading is where the name belongs
        // and putting a second copy of it next to the mark would be the same
        // words twice again.
        item {
            Avatar(
                name = person.displayName,
                hue = hueFor(Repository.Section.CARE_TEAM),
                size = AvatarSize.header,
            )
            Spacer(Modifier.height(Space.m))
        }

        // **What you need to know before you dial, above the thing that
        // dials.** "Days, 7 to 3, ask for her by name" and "the one who
        // actually calls back" are the reason somebody opened this screen at
        // all, and they were sitting underneath both buttons where the eye
        // reaches them after the decision has been made.
        person.notes?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.l))
            }
        }

        item {
            // **The number carries the weight, because it is the whole point.**
            // Somebody standing in a corridor who needs the charge nurse needs
            // the number, not a form, and this screen gave the two the same
            // treatment: one quiet button above another quiet button, which
            // makes the reader sort them. Rule 15. Correcting a spelling is the
            // rare errand and stays quiet.
            person.phone?.takeIf { it.isNotBlank() }?.let { number ->
                FilledButton(
                    label = strings("person.call", "number" to number),
                    onClick = { onCall(number) },
                    modifier = Modifier.fillMaxWidth().testTag(PersonTags.CALL),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            QuietButton(
                label = strings["person.edit"],
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().testTag(PersonTags.EDIT),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        item {
            GroupHeader(labelKey = "person.entries")
            Spacer(Modifier.height(Space.headerGap))
        }

        // **Nothing yet is the ordinary state and says so plainly.** A person
        // added before they were spoken to has no entries, which is not a gap
        // in the record, and rule 13 forbids reading an unfilled thing as a
        // deficiency.
        if (entries.isEmpty()) {
            item {
                Text(
                    text = strings("person.entries.empty", "name" to person.displayName),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
            }
            return@SectionScaffold
        }

        entries.forEachIndexed { index, entry ->
            item(key = entry.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < entries.lastIndex,
                    node = colors.blue,
                    routeColor = colors.blue,
                    dash = RouteDash.TRAIL,
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clip(Radius.card)
                                // **`openableByTap`, not the removal
                                // modifier with its gesture switched off.**
                                // That declared a tap action called "remove" on
                                // a card that opens an entry, and a long press
                                // called "remove" that ran an empty function.
                                // The gesture went quiet and the words did not.
                                // #231.
                                .openableByTap(
                                    label = strings["prep.change.open"],
                                    onTap = { onOpenEntry(entry) },
                                    resting = colors.card,
                                )
                                .testTag(PersonTags.entry(entry.id))
                                .padding(Space.cardPadding),
                        ) {
                            entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = EventDateText.render(strings, it),
                                    style = HealthTrail.type.mono,
                                    color = colors.ink2,
                                )
                                Spacer(Modifier.height(Space.xs))
                            }
                            Text(
                                text = entry.title?.takeIf { it.isNotBlank() }
                                    ?: strings[kindNameKey(entry.kind)],
                                style = HealthTrail.type.displayS,
                                color = colors.ink,
                            )
                            entry.body?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = it,
                                    style = HealthTrail.type.bodyM,
                                    color = colors.ink2,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }
    }
}
