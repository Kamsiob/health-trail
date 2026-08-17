package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.components.fabScrollClearance
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Face
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.InsetDoor
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.NextBlock
import com.kamsiob.healthtrail.ui.v4.RowDivider
import com.kamsiob.healthtrail.ui.v4.StatBlock
import com.kamsiob.healthtrail.ui.v4.hueForPerson
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TodayTags {
    const val ROOT = "today_root"
    const val EMPTY = "today_empty"
    const val DIGEST = "today_digest"
    const val SEARCH = "today_search"
    const val EMERGENCY = "today_emergency"
    const val NEXT_APPOINTMENT = "today_next_appointment"
    const val NEXT_QUESTIONS = "today_next_questions"
    const val PEOPLE = "today_people"
    const val TRACK = "today_track"
    const val TRACK_OPEN = "today_track_open"
    const val OPEN_GROUP = "today_open_group"
    fun step(number: Int) = "today_step_$number"
    fun digestRow(section: Repository.Section) = "today_digest_${section.name.lowercase()}"
}

/**
 * Today: the front door, rewritten from scratch onto `ui/v4`. #386.
 *
 * **This is `m3v4-0`, measured.** The owner's ruling on the method is not
 * negotiable: "no old design language at all. get rid of it so it doesn't
 * influence." Nothing here descends from the screen this replaces. What the
 * drawing sets, in order: a gold date over whose day it is at display weight, a
 * saturated blue block holding what is coming with a white card inside it for
 * the one thing worth doing first, then what the person tracks on a white card
 * with its value, its count and its line.
 *
 * **The appointment leads, and that is a change of subject.** The screen used to
 * lead with what had changed since the last visit, which is the past. A person
 * opening this at seven in the morning is asking what today asks of them, and
 * the drawing answers that: the next appointment, where it is, and the questions
 * already saved up for it. Law 1, one lead.
 *
 * **What needs somebody comes above what is merely interesting.** The drawing is
 * of a quiet notebook and shows two blocks, so on a quiet notebook this screen
 * is the drawing exactly. When an incident is open or somebody else is sitting
 * on something, that group sits directly under the lead and above the tracked
 * measure, because an open incident is the thing the person is carrying around.
 * D191.
 *
 * **The fold is gone.** D185: nothing behind a fold that a label and a scroll can
 * carry. What was "Also waiting" with a count is now simply the rest of the
 * rows, under the label that names them.
 *
 * **Nothing here concludes anything.** Every number is a count of things waiting
 * on somebody, never a score, and rule 13 forbids measuring the person's own
 * diligence: a measure with no readings keeps its card and says so plainly, and
 * a step already taken stops being suggested rather than being ticked off.
 */
@Composable
fun TodayScreen(
    /** Whether there is anything at all yet, which decides if a digest is owed. */
    hasAnything: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Whose day it is, for the masthead. Null before setup names anybody.
     *
     * **The screen says the person's name**, D170. Today opened with the word
     * "Today" over a navigation tab an inch below saying the same word.
     */
    subjectName: String? = null,
    /**
     * What day it is, passed rather than read here.
     *
     * A composable that read the clock itself would not recompose when the day
     * turned, and a test could not put the screen on a chosen morning.
     */
    today: LocalDate = LocalDate.now(),
    /** What changed since the last visit. Nothing by default, so a caller that has not worked it out shows no digest. */
    digest: Digest.Summary = Digest.nothing,
    /**
     * When the person was last here, or null on a first run.
     *
     * **After four months away, "47 new" without a date is the one piece of
     * context that matters.**
     */
    lastVisitMillis: Long? = null,
    /** Where each digest row goes. Rule 18: a count that leads nowhere is a dead end. */
    onOpenSection: (Repository.Section) -> Unit = {},
    /**
     * The steps still worth suggesting, already filtered by the caller.
     *
     * **What is already done is not suggested**, so a notebook with a full care
     * team is never advised to add people.
     */
    coaching: List<CoachStep> = emptyList(),
    openIncidents: Int = 0,
    openQuestions: Int = 0,
    waitingOnSomebody: Int = 0,
    unfiled: Int = 0,
    /** The soonest appointment still ahead, which is what the screen leads with. */
    nextAppointment: Repository.Appointment? = null,
    /**
     * How many saved questions would come to that appointment.
     *
     * **Counted by the caller, because the rule is about whose question it is**:
     * a question waiting on nobody in particular comes to every appointment, and
     * one waiting on the wound nurse comes only to hers.
     */
    questionsReady: Int = 0,
    /** The one measurement the front door shows. Null where nothing is tracked. */
    tracked: TrackedMeasure? = null,
    onOpenIncidents: () -> Unit = {},
    onOpenQuestions: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenUnfiled: () -> Unit = {},
    onOpenAppointments: () -> Unit = {},
    onOpenEmergencyCard: () -> Unit = {},
    /** Opens the Progress section, which the tracked card is a door into. */
    onOpenProgress: () -> Unit = {},
    /** Opens who this notebook is about, which is what the mark in the corner is. */
    onPeople: () -> Unit = {},
    /** Opens search. `MASTER_SPEC.md` 4.8 puts universal search on this screen. */
    onSearch: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = modifier.fillMaxSize().testTag(TodayTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal),
            // **Every item here is a group**, so the air between them is the
            // between-groups gap and there are three gaps in the whole app.
            // D188.
            verticalArrangement = Arrangement.spacedBy(Space.betweenGroups),
        ) {
            Spacer(Modifier.height(Space.sm))

            Masthead(
                subjectName = subjectName,
                today = today,
                onSearch = onSearch,
                onPeople = onPeople,
            )

            // **The lead: what is coming.** The one saturated block on the
            // screen, and absent when there is no appointment ahead, because a
            // hero announcing that nothing is scheduled would be an empty frame
            // at the loudest weight on the screen. Rule 11.5.
            nextAppointment?.let { appointment ->
                NextAppointment(
                    appointment = appointment,
                    subjectName = subjectName,
                    questionsReady = questionsReady,
                    today = today,
                    onOpen = onOpenAppointments,
                    onOpenQuestions = onOpenQuestions,
                )
            }

            // **What is still open, above what is merely tracked.** D191. Each
                // row goes where the thing actually is, rule 18, and the order is
            // the priority order: an incident nobody has answered first, then
            // questions, then what somebody else owes, then filing.
            val open = listOfNotNull(
                openIncidents.takeIf { it > 0 }
                    ?.let { OpenItem(strings("today.open.incidents", "count" to it), Symbols.incidents, onOpenIncidents) },
                openQuestions.takeIf { it > 0 }
                    ?.let { OpenItem(strings("today.open.questions", "count" to it), Symbols.askNextTime, onOpenQuestions) },
                waitingOnSomebody.takeIf { it > 0 }
                    ?.let { OpenItem(strings("today.open.waiting", "count" to it), Symbols.projects, onOpenProjects) },
                unfiled.takeIf { it > 0 }
                    ?.let { OpenItem(strings("unfiled.waiting", "count" to it), Symbols.noteStack, onOpenUnfiled) },
            )
            if (open.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
                    Eyebrow(
                        text = strings["today.open.group"],
                        modifier = Modifier.testTag(TodayTags.OPEN_GROUP),
                    )
                    Block(padding = Space.none) {
                        open.forEachIndexed { index, item ->
                            ListRow(
                                // bidi-ok: every one of these is a count the app
                                // composed from a catalog template, never
                                // anything somebody typed.
                                title = item.label,
                                mark = item.mark,
                                markTint = colors.ink2,
                                markWash = colors.card,
                                isDoor = true,
                                onClick = item.onOpen,
                            )
                            if (index != open.lastIndex) RowDivider()
                        }
                    }
                }
            }

            // **What you track**, the second half of the drawing.
            tracked?.let { measure ->
                WhatYouTrack(tracked = measure, onOpenProgress = onOpenProgress)
            }

            // **What changed since the last visit**, and only when there is a
            // notebook to have changed. A first launch and a quiet week both
            // land here and both correctly say what is true of them.
            if (hasAnything) {
                SinceLastHere(
                    digest = digest,
                    lastVisitMillis = lastVisitMillis,
                    onOpenSection = onOpenSection,
                )
            }

            if (coaching.isNotEmpty()) {
                CoachedStart(steps = coaching, nothingWrittenYet = !hasAnything)
            }

            // **The emergency card is one tap from here, always**, per
            // `MASTER_SPEC.md` 4.1: it is the one thing in the app that is
            // useful to somebody else in a hurry, and in a hurry is exactly when
            // nobody wants to go looking through a table of contents.
            //
            // **Not while the coaching is still asking for it**, which would be
            // the same offer twice.
            if (coaching.none { it.section == Repository.Section.EMERGENCY_CARD }) {
                Action(
                    label = strings["notebook.section.emergency_card"],
                    onClick = onOpenEmergencyCard,
                    emphasis = ActionEmphasis.Quiet,
                    modifier = Modifier.testTag(TodayTags.EMERGENCY),
                )
            }

            // Clearance for the capture button, which overlaps the navigation
            // bar and would otherwise sit on the last line.
            Spacer(Modifier.height(fabScrollClearance))
        }
    }
}

/**
 * The date, whose day it is, and the two marks in the corner.
 *
 * **A gold overline carrying the date and the name at display weight under it**,
 * D170, measured off `m3v4-0`: 12sp tracked capitals in `goldInk`, 16dp of air,
 * then 40sp. Four points made them one block, which is most of why the old
 * masthead read as cramped.
 *
 * **Capitals are for the eye and not for the reader.** Compose has no text
 * transform, so uppercasing changes the string the semantics tree carries too,
 * and a reader announcing "THURSDAY, AUGUST 16" is being handed a shape rather
 * than a date. D183.
 *
 * **Two marks, where the drawing has one.** The drawing puts the person's own
 * circle in the corner. Search is beside it because `MASTER_SPEC.md` 4.8 puts
 * universal search on this screen and a seeded notebook, which is every real
 * one, would otherwise have no way to search from the front door. D191.
 */
@Composable
private fun Masthead(
    subjectName: String?,
    today: LocalDate,
    onSearch: () -> Unit,
    onPeople: () -> Unit,
) {
    val strings = LocalStrings.current
    val locale = LocalConfiguration.current.locales[0]
    val date = EventDateText.masthead(strings, today)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.uppercase(locale),
                style = HealthTrail.type.eyebrow,
                color = HealthTrail.colors.goldInk,
                modifier = Modifier.semantics { contentDescription = date },
            )
            Spacer(Modifier.height(Space.m))
            Text(
                text = subjectName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { strings("today.masthead", "name" to Bidi.isolate(it)) }
                    ?: strings["today.masthead.noname"],
                style = HealthTrail.type.displayL,
                color = HealthTrail.colors.ink,
            )
        }
        IconAction(
            symbol = Symbols.search,
            label = strings["today.search"],
            onClick = onSearch,
            modifier = Modifier.testTag(TodayTags.SEARCH),
            tint = HealthTrail.colors.ink2,
        )
        // **The person's own mark, and it is a door into who this notebook is
        // about.** A mark that did nothing on press would read as broken, rule
        // 16, and rule 18 wants it to lead where the person it names lives.
        subjectName?.takeIf { it.isNotBlank() }?.let { name ->
            Avatar(
                name = name,
                hue = hueForPerson(name, HealthTrail.colors.tabHues),
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = strings["people.open"],
                        onClick = onPeople,
                    )
                    .testTag(TodayTags.PEOPLE)
                    .semantics { contentDescription = strings["people.open"] },
            )
        }
    }
}

/**
 * The next appointment, as the one saturated block on the screen.
 *
 * **The faces are whose appointment it is**, the person the notebook is about
 * and whoever it is with, which is what `m3v4-0` draws: two overlapped circles
 * opposite the pill saying when.
 *
 * **The questions card only when there are questions.** A white card inside the
 * block saying nothing is waiting would be a frame around an absence, and rule
 * 11.5 is explicit that announcing the absence of a problem is not information.
 */
@Composable
private fun NextAppointment(
    appointment: Repository.Appointment,
    subjectName: String?,
    questionsReady: Int,
    today: LocalDate,
    onOpen: () -> Unit,
    onOpenQuestions: () -> Unit,
) {
    val strings = LocalStrings.current
    val hues = HealthTrail.colors.tabHues

    // **"Tomorrow 10:15" where the date allows it and the full date where it
    // does not.** A month is not a day, so it can be neither today nor
    // tomorrow, and saying otherwise would be inventing a precision the person
    // never gave. Rule 17.
    val whenLabel = EventDateText.nearby(strings, appointment.scheduledEdtf, today)
        ?: EventDateText.render(strings, appointment.scheduledEdtf)

    val title = Bidi.isolate(appointment.title)
    val where = appointment.locationNote?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }

    val faces = listOfNotNull(
        subjectName?.takeIf { it.isNotBlank() }?.let { Face(it, hueForPerson(it, hues)) },
        appointment.personName?.takeIf { it.isNotBlank() }?.let {
            Face(it, hueForPerson(appointment.personId ?: it, hues))
        },
    )

    NextBlock(
        whenLabel = whenLabel,
        title = title,
        where = where,
        faces = faces,
        description = if (where == null) {
            strings("today.next.description", "title" to title, "when" to whenLabel)
        } else {
            strings(
                "today.next.description.where",
                "title" to title,
                "when" to whenLabel,
                "where" to where,
            )
        },
        onOpen = onOpen,
        modifier = Modifier.testTag(TodayTags.NEXT_APPOINTMENT),
        door = questionsReady.takeIf { it > 0 }?.let {
            {
                InsetDoor(
                    count = it.toString(),
                    label = strings("today.next.questions.short", "count" to it),
                    description = strings("today.next.questions", "count" to it),
                    hue = hueFor(Repository.Section.ASK_NEXT_TIME),
                    onOpen = onOpenQuestions,
                    modifier = Modifier.testTag(TodayTags.NEXT_QUESTIONS),
                )
            }
        },
    )
}

/**
 * One tracked measurement, under the heading the Progress section already uses.
 *
 * **The heading is a block's own title and the way in is beside it**, which is
 * what `m3v4-0` draws: "What you track" at 22sp against "Progress" in `goldInk`
 * at the trailing edge. Both were measured off the drawing.
 *
 * **The card wears the Progress section's own hue**, `hueFor`, rather than the
 * green the drawing happens to be painted in. A section hue is identity and the
 * mapping is the owner's and lives in one place: a person who learned which
 * color Progress is from the notebook must find the same color here. D190.
 */
@Composable
private fun WhatYouTrack(
    tracked: TrackedMeasure,
    onOpenProgress: () -> Unit,
) {
    val strings = LocalStrings.current
    val hue = hueFor(Repository.Section.PROGRESS)
    val name = Bidi.isolate(tracked.name)
    val value = tracked.value
    val count = strings("progress.readings", "count" to tracked.readings.size)

    Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings["progress.heading"],
                style = HealthTrail.type.displayS,
                color = HealthTrail.colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = strings["notebook.section.progress"],
                style = HealthTrail.type.rowTitle,
                color = HealthTrail.colors.goldInk,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onOpenProgress)
                    .testTag(TodayTags.TRACK_OPEN),
            )
        }
        StatBlock(
            name = name,
            value = value,
            unit = tracked.unit,
            count = count.takeIf { tracked.readings.isNotEmpty() },
            readings = tracked.readings,
            footnote = tracked.footnote(strings),
            empty = strings["progress.empty"],
            hue = hue,
            description = if (value == null) {
                strings("today.track.description.empty", "name" to name)
            } else {
                strings(
                    "today.track.description",
                    "name" to name,
                    // Isolated for the reason a figure always is: a Latin number
                    // and its unit keep their own direction rather than joining
                    // the sentence around them.
                    "value" to Bidi.isolate(
                        tracked.unit
                            ?.takeIf { it.isNotBlank() }
                            ?.let { strings("today.track.value", "value" to value, "unit" to it) }
                            ?: value,
                    ),
                    "readings" to count,
                )
            },
            onOpen = onOpenProgress,
            modifier = Modifier.testTag(TodayTags.TRACK),
        )
    }
}

/**
 * What changed since the last visit, as a line and the rows behind it.
 *
 * **"Nothing new" is said in the same place and the same words as "3 new
 * things", and that is deliberate.** It is the answer to a question the person
 * asked, and putting the calm case in smaller type would make it read as the
 * failure case.
 */
@Composable
private fun SinceLastHere(
    digest: Digest.Summary,
    lastVisitMillis: Long?,
    onOpenSection: (Repository.Section) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
        Eyebrow(text = strings["today.digest.heading"])
        Text(
            text = if (digest.isEmpty) {
                strings["today.digest.empty"]
            } else {
                strings("today.digest.total", "count" to digest.newThings)
            },
            style = HealthTrail.type.displayS,
            color = colors.ink,
            modifier = Modifier.testTag(TodayTags.DIGEST),
        )

        // **Only when it is genuinely a gap.** Below the threshold this is a
        // line telling somebody who opens the app every morning that they
        // opened it yesterday.
        lastVisitMillis
            ?.takeIf { millis ->
                java.time.Duration.between(Instant.ofEpochMilli(millis), Instant.now())
                    .toDays() >= AWAY_DAYS
            }
            ?.let { millis ->
                Body(
                    text = strings(
                        "today.digest.lastvisit",
                        "date" to EventDateText.render(
                            strings,
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                        ),
                    ),
                )
            }

        if (!digest.isEmpty) {
            Block(padding = Space.none) {
                digest.added.forEachIndexed { index, added ->
                    ListRow(
                        title = strings[labelKey(added.section)],
                        value = strings("today.digest.new", "count" to added.count),
                        isDoor = true,
                        onClick = { onOpenSection(added.section) },
                        modifier = Modifier.testTag(TodayTags.digestRow(added.section)),
                    )
                    if (index != digest.added.lastIndex) RowDivider(inset = false)
                }
            }

            // One line each. They are not counts of new things and must not sit
            // among the rows that are.
            listOfNotNull(
                digest.corrected.takeIf { it > 0 }
                    ?.let { strings("today.digest.corrected", "count" to it) },
                digest.removed.takeIf { it > 0 }
                    ?.let { strings("today.digest.removed", "count" to it) },
            ).forEach { aside -> Body(text = aside) }
        }
    }
}

/**
 * What to do first, written as an invitation rather than an absence.
 *
 * **The first item is always the Emergency Card**, because it is the highest
 * value two minutes a new person can spend, and because it is the one thing in
 * this app that is useful to somebody else in a hurry.
 *
 * **The numbering is allowed here** because this is genuinely a sequence, which
 * is the condition the ban on numbered markers attaches to.
 */
@Composable
private fun CoachedStart(steps: List<CoachStep>, nothingWrittenYet: Boolean) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxWidth().testTag(TodayTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Text(
            // "Nothing written down yet" stops being true the moment something
            // is, and a heading that contradicts the screen under it reads as a
            // bug. The list is the same either way.
            text = strings[
                if (nothingWrittenYet) "today.empty.title" else "today.coach.heading"
            ],
            style = HealthTrail.type.displayS,
            color = HealthTrail.colors.ink,
        )
        Block(padding = Space.none) {
            steps.forEachIndexed { index, step ->
                ListRow(
                    title = strings[step.labelKey],
                    isDoor = true,
                    onClick = step.onOpen,
                    modifier = Modifier.testTag(TodayTags.step(index + 1)),
                )
                if (index != steps.lastIndex) RowDivider(inset = false)
            }
        }
    }
}

/**
 * One thing worth doing, and where it happens.
 *
 * **[section] is what the step is about**, so the screen can tell that the
 * emergency card is already being offered and not offer it twice.
 */
data class CoachStep(
    val labelKey: String,
    val section: Repository.Section?,
    val onOpen: () -> Unit,
)

/**
 * The one measurement the front door shows, with everything it needs to draw it.
 *
 * **The screen is handed the measure and its readings rather than a formatted
 * card**, because the value's formatting is a rule about numbers the person
 * gave: no rounding, no padding, no trailing zero, because "128.0" is a claim of
 * precision nobody made.
 */
data class TrackedMeasure(
    /** The measure's own name, in the case somebody typed it. */
    val name: String,
    /** Its unit, where it has one. */
    val unit: String?,
    /** Every reading, newest first as the repository returns them. */
    val readings: List<Repository.Reading>,
) {
    /** The latest reading, formatted as it was written. Null where there are none. */
    val value: String?
        get() = readings
            .maxByOrNull { it.occurredStart ?: Long.MIN_VALUE }
            ?.let { readingValue(it) }

    /**
     * The month the line starts in, at the foot of the card.
     *
     * **A date, never a scale.** It says when the readings begin, which is a
     * fact about the record, and says nothing about the values, which would be
     * the judgment rule 2 forbids. Absent where no reading carries a date,
     * because a chart of undated readings has no month to name.
     */
    fun footnote(strings: com.kamsiob.healthtrail.i18n.Strings): String? =
        readings
            .mapNotNull { it.occurredStart }
            .minOrNull()
            ?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern(strings["date.format.month_short"]))
            }
}

/** One open count, and the door it is. */
private data class OpenItem(
    val label: String,
    val mark: Int,
    val onOpen: () -> Unit,
)

/**
 * How long away has to be before the app says when you were last here.
 *
 * Three days. Somebody who opens this every morning does not need telling that
 * they opened it yesterday, and somebody back after a hospital week or four
 * months does.
 */
private const val AWAY_DAYS = 3L
