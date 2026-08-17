package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Hero
import com.kamsiob.healthtrail.ui.components.HeroLine
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider
import java.time.ZoneId

object ReviewTags {
    const val NAME = "review"
    const val SHARE = "review_share"
    const val ENTRIES_FOLD = "review_entries_fold"
    fun milestone(id: String) = "review_milestone_$id"
    fun chapter(id: String) = "review_chapter_$id"
    fun appointment(id: String) = "review_appointment_$id"
    fun incident(id: String) = "review_incident_$id"
    fun document(id: String) = "review_document_$id"
    fun entry(id: String) = "review_entry_$id"
}

/**
 * One month of the record, gathered.
 *
 * `MASTER_SPEC.md` 4.5 and `DESIGN.md` section 14: a period of the trail,
 * composed, **every line tapping through to its source entry.** It follows
 * screen 08 and is the second half of #200.
 *
 * **Nothing on this screen is a new fact.** Every row is a row that already
 * exists somewhere else in the notebook, gathered by the one thing they have in
 * common, which is when they happened. That is what lets each line open its own
 * source, and it is `MASTER_SPEC.md` section 5's whole claim: the output is
 * verifiable because there is nothing to verify beyond a date comparison. No
 * model, no inference, no sentence the app made up about somebody's month.
 *
 * **The hero is the milestones, and only the milestones.** Law 1 wants one
 * dominant element answering why the person opened the screen, and the honest
 * candidate list here is short: anything the app chose to lead with, whether the
 * worst incident or the busiest week, would be the app deciding what mattered
 * about somebody's month, which rule 2 forbids in every form. **A milestone is
 * the one thing in this app the person themselves marked as worth remembering**,
 * so leading with it repeats their decision rather than making one.
 *
 * **A month with no milestone has no hero, and that is correct.** Law 1 says no
 * hero at all is a valid screen. The alternative is inventing one to fill the
 * slot, which section 1 calls a decorative banner, and here it would be worse
 * than decorative: it would be a claim.
 *
 * **The order is the notebook's, never by how much happened.** Ranking the
 * groups by count would move them around from month to month and put whichever
 * part of a month happened most at the top. `Digest` refuses the same thing for
 * the same reason: the places never move.
 *
 * **What was written down folds, because in year three it is forty rows.** It is
 * the largest group and the least surprising, and the fold names it and counts
 * it, so nothing is hidden. Law 1's hallway test: three things without opening
 * anything, everything with ten minutes.
 *
 * **A row belongs to this month only if its whole date fits inside it**, which
 * `Repository.monthReview` decides and this screen only renders. An entry dated
 * to a year overlaps twelve months and is in none of their reviews, because the
 * record never said which. Rule 17.
 */
@Composable
fun MonthReviewScreen(
    review: Repository.MonthReview,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onOpenMilestones: () -> Unit,
    onOpenChapter: (Repository.Chapter) -> Unit,
    onOpenAppointment: (Repository.Appointment) -> Unit,
    onOpenIncident: (Repository.Incident) -> Unit,
    onOpenDocument: (Repository.Document) -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val month = EventDateText.monthHeading(strings, review.monthStart, zone)

    SectionScaffold(
        name = ReviewTags.NAME,
        title = strings["notebook.section.trail"],
        // **The month is the heading, in the locale's own words.** It is
        // composed from the month's own first instant rather than passed in as
        // a string, so Arabic gets Arabic and nothing is a rendered English
        // month wearing another script.
        heading = month,
        subtitle = strings["review.subtitle"],
        // The trail belongs to no section and wears gold with the base ladder,
        // per 4.3's rule for whole-app surfaces.
        section = null,
        onBack = onBack,
        backLabelKey = "section.back.trail",
        modifier = modifier,
    ) {
        if (review.isEmpty) {
            item {
                SectionEmpty(
                    name = ReviewTags.NAME,
                    text = strings["review.empty"],
                    section = null,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
            }
            return@SectionScaffold
        }

        // **The one thing, when the person marked one.** Two at most, per the
        // hero's own rule: two things worth remembering in a month is a fact
        // about a notebook, and three at display size is a screen shouting.
        // Any beyond the second are still on the arc, which the line opens.
        if (review.milestones.isNotEmpty()) {
            item(key = "hero") {
                Hero(eyebrowKey = "review.marked") {
                    review.milestones.take(MAX_HERO_LINES).forEach { milestone ->
                        HeroLine(
                            text = Bidi.isolate(milestone.label),
                            onClick = onOpenMilestones,
                            modifier = Modifier.testTag(ReviewTags.milestone(milestone.id)),
                        )
                    }
                }
            }
        }

        // **There is no total on this screen, and that is a decision.** It
        // carried a gold band reading "Written down this month, 42" above the
        // groups. Two problems, either of which is enough. Law 1: a number at
        // that weight competes with the hero for the top, and when two things
        // compete for the top the screen is wrong. Rule 2: a single number over
        // a month is an invitation to compare it with last month's, which is a
        // judgment about somebody's care that the app must not offer even
        // implicitly. Each group counts itself where a count answers something,
        // and the fold below carries the only count anybody needs.

        // **Where they were comes first of the groups, because a month somebody
        // moved in is a month defined by the move.** It is also the rarest, so
        // most months skip it entirely rather than carrying an empty heading.
        //
        // **One row per place, however many of its dates fall in the month.** An
        // overnight stay begins and ends inside one month, and it listed the
        // same name twice under one heading, which reads as a rendering fault
        // rather than as two facts. The subtitle carries which of the two it
        // was, or that it was both.
        val places = (review.began + review.ended).distinctBy { it.id }
        if (places.isNotEmpty()) {
            val began = review.began.map { it.id }.toSet()
            val ended = review.ended.map { it.id }.toSet()
            group(key = "where", headingKey = "review.where") {
                Block(padding = Space.none) {
                    places.forEachIndexed { index, chapter ->
                        ListRow(
                            title = Bidi.isolate(chapter.name),
                            support = strings[
                                when {
                                    chapter.id in began && chapter.id in ended ->
                                        "review.bothends"
                                    chapter.id in began -> "review.began"
                                    else -> "review.ended"
                                },
                            ],
                            isDoor = true,
                            onClick = { onOpenChapter(chapter) },
                            modifier = Modifier.testTag(ReviewTags.chapter(chapter.id)),
                        )
                        if (index < places.lastIndex) RowDivider(inset = false)
                    }
                }
            }
        }

        if (review.appointments.isNotEmpty()) {
            group(key = "appointments", headingKey = "review.appointments") {
                Block(padding = Space.none) {
                    review.appointments.forEachIndexed { index, appointment ->
                        ListRow(
                            title = Bidi.isolate(
                                appointment.title.ifBlank { strings["prep.untitled"] },
                            ),
                            // The date at exactly the precision somebody gave
                            // it, in mono because a date beside a name is data.
                            value = appointment.scheduledEdtf
                                ?.takeIf { it.isNotBlank() }
                                ?.let { EventDateText.render(strings, it) },
                            isDoor = true,
                            onClick = { onOpenAppointment(appointment) },
                            modifier = Modifier.testTag(
                                ReviewTags.appointment(appointment.id),
                            ),
                        )
                        if (index < review.appointments.lastIndex) RowDivider(inset = false)
                    }
                }
            }
        }

        // **Reported and answered are two groups, and nothing is in both.** An
        // incident reported in March and answered in June belongs in June's
        // review, and saying so under its own heading is the only way a person
        // reading June learns it. One reported and answered inside the same
        // month appears under "what went wrong" alone, carrying its state in a
        // word: it listed twice before, and the second row taught nothing the
        // first could not say.
        if (review.reported.isNotEmpty()) {
            group(key = "reported", headingKey = "review.reported") {
                IncidentRows(
                    incidents = review.reported,
                    onOpen = onOpenIncident,
                    // The state in the app's own two words, so a row under
                    // "what went wrong" never leaves somebody wondering whether
                    // anybody ever came back to it.
                    stateOf = { if (it.isOpen) "incidents.open" else "incidents.settled" },
                )
            }
        }

        if (review.answered.isNotEmpty()) {
            group(key = "answered", headingKey = "review.answered") {
                IncidentRows(
                    incidents = review.answered,
                    onOpen = onOpenIncident,
                    // The heading already says these were answered, so the row
                    // says when the thing itself happened instead. Section 15:
                    // what is the same for every row is said once.
                    stateOf = { null },
                )
            }
        }

        if (review.documents.isNotEmpty()) {
            group(key = "documents", headingKey = "review.documents") {
                Block(padding = Space.none) {
                    review.documents.forEachIndexed { index, document ->
                        ListRow(
                            title = Bidi.isolate(document.title),
                            support = document.category?.takeIf { it.isNotBlank() }
                                ?.let { Bidi.isolate(it) },
                            isDoor = true,
                            onClick = { onOpenDocument(document) },
                            modifier = Modifier.testTag(ReviewTags.document(document.id)),
                        )
                        if (index < review.documents.lastIndex) RowDivider(inset = false)
                    }
                }
            }
        }

        // **The record itself, folded and counted.** It is the largest group in
        // every month past the first and the least surprising, so it arrives
        // closed with its number on it. The count says whether the tap is worth
        // it, which is what a fold is for.
        if (review.entries.isNotEmpty()) {
            item(key = "entries_fold") {
                Eyebrow(text = Bidi.join(strings["review.entries"], review.entries.size.toString()), modifier = Modifier.testTag(ReviewTags.ENTRIES_FOLD))
                Spacer(Modifier.height(Space.cardGap))
            }

            // On the trail's own spine, so a month opened from the trail
            // still looks like the trail rather than like a second answer
            // to the same shape. `DESIGN.md` section 7's "a spine for
            // anything sequential."
            review.entries.forEachIndexed { index, entry ->
                item(key = "e_${entry.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < review.entries.lastIndex,
                        node = colors.gold,
                        routeColor = colors.gold,
                        dash = RouteDash.TRAIL,
                    ) {
                        Column {
                            EntryLine(entry = entry, onOpen = { onOpenEntry(entry) })
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
        }

        item(key = "share") {
            Spacer(Modifier.height(Space.sectionGap))
            // **One filled action, and it is the one that leaves the app.** A
            // month review exists to be told to somebody, per `MASTER_SPEC.md`
            // 4.2, and handing it to whatever the person already messages
            // people with is the act the rest of the screen is for.
            Action(
                label = strings["review.share"],
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().testTag(ReviewTags.SHARE), emphasis = ActionEmphasis.Main,
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * A heading and its rows, with the air around them.
 *
 * Here rather than repeated seven times, because seven copies of the same three
 * lines is where the spacing between two groups quietly stops matching.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.group(
    key: String,
    headingKey: String,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Eyebrow(text = LocalStrings.current[headingKey])
        Spacer(Modifier.height(Space.headerGap))
        content()
        Spacer(Modifier.height(Space.sectionGap))
    }
}

/**
 * Incidents as rows, which is what they are here.
 *
 * **A word rather than a pill.** The state is a subtitle in the app's own two
 * words, because a colored pill on a row inside a review would be the only
 * colored thing on the screen and would carry more weight than the incident.
 * [stateOf] returns null where the heading already says it, per section 15.
 *
 * **The date is when it was reported**, in every group, because that is when
 * the thing happened and it is what somebody needs to place it.
 */
@Composable
private fun IncidentRows(
    incidents: List<Repository.Incident>,
    onOpen: (Repository.Incident) -> Unit,
    stateOf: (Repository.Incident) -> String?,
) {
    val strings = LocalStrings.current
    Block(padding = Space.none) {
        incidents.forEachIndexed { index, incident ->
            ListRow(
                title = Bidi.isolate(incident.title),
                support = stateOf(incident)?.let { strings[it] },
                value = incident.reportedEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) },
                isDoor = true,
                onClick = { onOpen(incident) },
                modifier = Modifier.testTag(ReviewTags.incident(incident.id)),
            )
            if (index < incidents.lastIndex) RowDivider(inset = false)
        }
    }
}

/**
 * One entry inside the fold, exactly as the prep sheet renders one.
 *
 * The same composition rather than a second one, per 13.2: two screens showing a
 * run of entries that already happened is one shape, and building it twice is
 * how the two drift.
 */
@Composable
private fun EntryLine(entry: Repository.TrailEntry, onOpen: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .clip(Radius.cardLarge)
            .openableByTap(label = strings["review.entry.open"], onTap = onOpen)
            .testTag(ReviewTags.entry(entry.id))
            .padding(Space.cardPadding),
    ) {
        entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = EventDateText.render(strings, it),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
        }
        Text(
            text = entry.title?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
                ?: strings[kindNameKey(entry.kind)],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        entry.body?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Bidi.isolate(it),
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * How many milestones the hero carries before the rest stay on the arc.
 *
 * Two, per the hero's own rule in `Hero.kt`: two is a fact about a notebook and
 * three at display size is a list shouting.
 */
private const val MAX_HERO_LINES = 2
