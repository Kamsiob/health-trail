package com.kamsiob.healthtrail.ui.screens

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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MilestoneTags {
    const val NAME = "milestones"
    const val ADD = "milestones_add"
    fun row(id: String) = "milestone_$id"
    fun chapter(id: String) = "milestone_chapter_$id"
}

/**
 * The path so far, on one continuous trail.
 *
 * **Every one of these was put here by hand.** Nothing on this screen is
 * derived: not from a count of entries, not from a measurement crossing a
 * number, not from a chapter starting or ending. Rule 2, and it is the reason
 * `milestone` is a table of its own. The app holds what somebody decided was
 * worth marking and never decides one for them.
 *
 * **The table shipped in the schema and nothing read it or wrote it until
 * 2026-08-04.** The fixture wrote milestones, the export listed them under
 * "Milestones", and a person could neither see one nor make one. #234.
 *
 * **Oldest first, which is the opposite of the trail.** The trail answers "what
 * happened lately" and is read backward. This answers "how did we get here" and
 * is read forward, which is what makes it an arc rather than a list.
 *
 * **Every node is ringed, and that is the one place in the app where that is
 * true.** Section 5.2.1 keeps the milestone ring rare precisely so it means
 * something, and here every row is one by definition. There is nothing to
 * distinguish, so the ring is not carrying a distinction: it is saying what
 * kind of screen this is.
 *
 * **A date nobody remembers goes at the end.** It still happened, and putting
 * it at the head of the arc would claim it happened before everything else.
 * At the end it reads as "and also this", which is what it is.
 */
@Composable
fun MilestonesScreen(
    milestones: List<Repository.Milestone>,
    onOpen: (Repository.Milestone) -> Unit,
    onOpenChapter: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.chapters",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = MilestoneTags.NAME,
        title = strings["notebook.section.chapters"],
        headingKey = "milestones.heading",
        subtitle = strings["milestones.subtitle"],
        section = Repository.Section.CHAPTERS,
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        if (milestones.isEmpty()) {
            item {
                SectionEmpty(
                    name = MilestoneTags.NAME,
                    text = strings["milestones.empty"],
                    section = Repository.Section.CHAPTERS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
                Spacer(Modifier.height(Space.l))
            }
        }

        milestones.forEachIndexed { index, milestone ->
            item(key = milestone.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < milestones.lastIndex,
                    node = colors.gold,
                    state = Waypoint.MILESTONE,
                    routeColor = colors.gold,
                    dash = RouteDash.TRAIL,
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clip(Radius.cardLarge)
                                // **The label says what the tap does.** It said
                                // "Open this milestone" and opened the sheet
                                // headed "Change what you marked", so a reader
                                // user was told a screen would open and got a
                                // form. There is nothing further to open: a
                                // milestone is a date, a line and a note, and
                                // this row is already all three, so correcting
                                // it is the only thing left for a tap to mean.
                                .openableByTap(
                                    label = strings["milestones.open"],
                                    onTap = { onOpen(milestone) },
                                )
                                .testTag(MilestoneTags.row(milestone.id))
                                .padding(Space.cardPadding),
                        ) {
                            Text(
                                text = EventDateText.render(
                                    strings,
                                    milestone.occurredEdtf,
                                ),
                                style = HealthTrail.type.bodyS,
                                color = colors.ink2,
                            )
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                text = Bidi.isolate(milestone.label),
                                style = HealthTrail.type.displayS,
                                color = colors.ink,
                            )
                            milestone.note?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = Bidi.isolate(it),
                                    style = HealthTrail.type.bodyM,
                                    color = colors.ink2,
                                )
                            }
                        }

                        // **Where it happened, as a door, per rule 18.** The
                        // chapter lists its milestones and this is the other
                        // direction. It sits outside the card rather than
                        // inside it, because the card opens the milestone and a
                        // second tap target inside a tappable card is two
                        // things fighting for one finger.
                        //
                        // **It wears the navigation costume rather than the
                        // action one.** It was an outlined pill carrying the
                        // chapter's name, and law 2 gives that costume to "a
                        // smaller action, always a verb or a dialable number"
                        // while it gives navigation the row ending in a
                        // chevron. A place name is a noun and this opens a
                        // screen, so it was wearing the wrong one on both
                        // counts. Same shape one incident already uses to point
                        // at a person: the name on the first line, what it is
                        // to this record on the second.
                        //
                        // **The second line rather than a mono header**, which
                        // would be one header per milestone saying the same
                        // three words down the whole arc. Section 15 says a
                        // line that is the same for every row is said once, and
                        // there is no once here, because only some milestones
                        // name a place.
                        milestone.chapterId?.let { chapterId ->
                            val name = milestone.chapterName
                                ?.takeIf { it.isNotBlank() } ?: return@let
                            Spacer(Modifier.height(Space.xs))
                            GroupedSurface {
                                DenseRow(
                                    title = Bidi.isolate(name),
                                    subtitle = strings["milestones.where.open"],
                                    chevron = true,
                                    divider = false,
                                    onClick = { onOpenChapter(chapterId) },
                                    modifier = Modifier.testTag(
                                        MilestoneTags.chapter(milestone.id),
                                    ),
                                )
                            }
                        }

                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // **The one filled action, and it is the only way one gets made.**
            // Nothing else in the app creates a milestone, which is deliberate:
            // marking one is a decision, and a decision belongs to a moment the
            // person chose rather than to a checkbox on some other form.
            FilledButton(
                label = strings["milestones.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(MilestoneTags.ADD),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
