package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.Road
import com.kamsiob.healthtrail.ui.v4.Stop

object MilestoneTags {
    const val NAME = "milestones"
    const val ADD = "milestones_add"
    fun row(id: String) = "milestone_$id"
    fun chapter(id: String) = "milestone_chapter_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_milestones"
}

/**
 * The path so far, on one continuous road. Rewritten onto `ui/v4`, #386.
 *
 * **Every one of these was put here by hand.** Nothing on this screen is
 * derived: not from a count of entries, not from a measurement crossing a
 * number, not from a chapter starting or ending. Rule 2, and it is the reason
 * `milestone` is a table of its own. The app holds what somebody decided was
 * worth marking and never decides one for them.
 *
 * **Oldest first, which is the opposite of the trail.** The trail answers "what
 * happened lately" and is read backward. This answers "how did we get here" and
 * is read forward, which is what makes it an arc rather than a list.
 *
 * **Every stop on this road has happened**, which is what a milestone is: it is
 * marked after the fact, so there is no "now" and nothing ahead. The road is
 * solid the whole way down, and that is the screen saying what kind of screen it
 * is rather than distinguishing between its rows. `docs/V4.md` 2.1, and the
 * measurements are `m3v4-2`'s.
 *
 * **A date nobody remembers goes at the end.** It still happened, and putting it
 * at the head of the arc would claim it happened before everything else. At the
 * end it reads as "and also this", which is what it is.
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
    val hue = hueFor(Repository.Section.CHAPTERS)

    Page(
        eyebrow = strings["notebook.section.chapters"],
        eyebrowColor = hue.ink,
        title = strings["milestones.heading"],
        subtitle = strings["milestones.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(MilestoneTags.ROOT),
    ) {
        if (milestones.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about an arc with nothing
                    // marked on it yet.
                    Body(
                        text = strings["milestones.empty"],
                        color = colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        // **The whole road is one item, and it has to be.** A page puts air
        // between its items, and air between two stops on a road is a road with
        // gaps in it: the line ended, restarted twelve points lower, and read as
        // four roads rather than one. Seen on the phone, rule 21. A section's
        // arc is a handful of stops, so the cost of composing them together is
        // nothing.
        item {
            Column {
                milestones.forEachIndexed { index, milestone ->
                    Road(
                        stop = Stop.Done,
                        continuesAbove = index > 0,
                        continuesBelow = index < milestones.lastIndex,
                    ) {
                        Column(
                            modifier = Modifier.padding(bottom = Space.sm),
                            verticalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            Block(
                                modifier = Modifier
                                    .semantics(mergeDescendants = true) { }
                                    // **The label says what the tap does.** It said
                                    // "Open this milestone" and opened the sheet
                                    // headed "Change what you marked", so a reader
                                    // was told a screen would open and got a form.
                                    // A milestone is a date, a line and a note, and
                                    // this row is already all three, so correcting
                                    // it is the only thing a tap can mean.
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = strings["milestones.open"],
                                        onClick = { onOpen(milestone) },
                                    )
                                    .testTag(MilestoneTags.row(milestone.id)),
                            ) {
                                Eyebrow(
                                    text = EventDateText.render(strings, milestone.occurredEdtf),
                                    fixed = false,
                                )
                                Body(
                                    text = Bidi.isolate(milestone.label),
                                    color = colors.ink,
                                    style = HealthTrail.type.displayS,
                                )
                                milestone.note?.takeIf { it.isNotBlank() }?.let {
                                    Body(text = Bidi.isolate(it))
                                }
                            }

                            // **Where it happened, as a door, per rule 18.** The
                            // chapter lists its milestones and this is the other
                            // direction. It sits outside the block rather than
                            // inside it, because the block opens the milestone and
                            // a second target inside a tappable block is two things
                            // fighting for one finger.
                            milestone.chapterId?.let { chapterId ->
                                val name = milestone.chapterName
                                    ?.takeIf { it.isNotBlank() } ?: return@let
                                Block(padding = Space.none) {
                                    ListRow(
                                        title = Bidi.isolate(name),
                                        support = strings["milestones.where.open"],
                                        mark = Symbols.chapters,
                                        markTint = hue.ink,
                                        markWash = hue.wash,
                                        isDoor = true,
                                        onClick = { onOpenChapter(chapterId) },
                                        clickLabel = strings["open.action"],
                                        modifier = Modifier.testTag(
                                            MilestoneTags.chapter(milestone.id),
                                        ),
                                    )
                                }
                            }
                            }
                        }
                    }
                }
        }

        item {
            Spacer(Modifier.height(Space.s))
            // **The one filled action, and it is the only way one gets made.**
            // Nothing else in the app creates a milestone, which is deliberate:
            // marking one is a decision, and a decision belongs to a moment the
            // person chose rather than to a checkbox on some other form.
            Action(
                label = strings["milestones.add"],
                onClick = onAdd,
                emphasis = ActionEmphasis.Main,
                mark = Symbols.add,
                modifier = Modifier.testTag(MilestoneTags.ADD),
            )
        }
    }
}
