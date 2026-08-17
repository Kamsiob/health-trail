package com.kamsiob.healthtrail.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.BlockAction
import com.kamsiob.healthtrail.ui.v4.BlockIconAction
import com.kamsiob.healthtrail.ui.v4.Face
import com.kamsiob.healthtrail.ui.v4.InsetDoor
import com.kamsiob.healthtrail.ui.v4.NextBlock
import com.kamsiob.healthtrail.ui.v4.hueForPerson
import java.time.LocalDate

/**
 * What is coming, permanently, at the top of Today. #386.
 *
 * **Owner ruling, 2026-08-17: this section is a fixture of the screen and is
 * independent of the card field under it.** It is not a card, it is not in the
 * layout, it cannot be moved, resized or taken off, and arranging Today does not
 * touch it. Everything a person arranges is below it, and the two never mix.
 *
 * The reason is the screen's job. Today answers "what does today ask of me",
 * and the honest answer is either an appointment or nothing yet. A person who
 * had taken the appointment card off their Today, or never had one, opened the
 * app on the morning of a meeting and the screen did not mention it. **A front
 * door whose most important line is optional is a front door that is sometimes
 * wrong.**
 *
 * **Four states, and all four are finished screens.** Rule 11 and rule 13:
 *
 * | Ahead | Saved to ask | What the foot offers |
 * |---|---|---|
 * | An appointment | Some | The count as a door, and a mark to save another |
 * | An appointment | None | Save a question |
 * | Nothing | Some | The count as a door, and marks for both ways in |
 * | Nothing | None | Put it on the calendar, and a mark to save a question |
 *
 * **The empty state is an invitation, never an absence**, rule 13. "Nothing on
 * the calendar yet" with two ways to change that is a finished state; a blank
 * area or a scolding line is what that rule exists to stop. Nothing here counts
 * how much the person has filled in.
 *
 * **Shared by both Today screens on purpose.** `TodayFieldScreen` draws the
 * arranged Today and `TodayScreen` is the fallback for a notebook with no
 * layout, D191, and this being permanent means it must say the same thing on
 * both. One composable, so the two cannot drift.
 */
@Composable
fun TodayHero(
    /** The soonest appointment still ahead, or null when there is none. */
    appointment: Repository.Appointment?,
    /**
     * How many saved questions would come to it.
     *
     * **A question waiting on nobody in particular comes to every appointment**,
     * which is the commonest case, and one waiting on the wound nurse comes only
     * to hers. The caller counts, because the rule is about whose question it is.
     */
    questionsReady: Int,
    onOpenAppointment: () -> Unit,
    onOpenQuestions: () -> Unit,
    onAddAppointment: () -> Unit,
    onAddQuestion: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whose day it is, so their own mark sits on the block beside whoever it is with. */
    subjectName: String? = null,
    /** What day it is, passed rather than read, so "tomorrow" is testable. */
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current
    val hues = HealthTrail.colors.tabHues

    // **"Tomorrow 10:15" where the date allows it and the full date where it
    // does not.** A month is not a day, so it can be neither today nor
    // tomorrow, and saying otherwise would be inventing a precision the person
    // never gave. Rule 17.
    val whenLabel = appointment?.let {
        EventDateText.nearby(strings, it.scheduledEdtf, today)
            ?: EventDateText.render(strings, it.scheduledEdtf)
    }
    val title = appointment?.let { Bidi.isolate(it.title) } ?: strings["today.next.none"]
    val where = appointment?.locationNote
        ?.takeIf { it.isNotBlank() }
        ?.let { Bidi.isolate(it) }

    val faces = if (appointment == null) {
        emptyList()
    } else {
        listOfNotNull(
            subjectName?.takeIf { it.isNotBlank() }?.let { Face(it, hueForPerson(it, hues)) },
            appointment.personName?.takeIf { it.isNotBlank() }?.let {
                Face(it, hueForPerson(appointment.personId ?: it, hues))
            },
        )
    }

    NextBlock(
        title = title,
        description = when {
            appointment == null -> strings["today.next.none.description"]
            where == null -> strings(
                "today.next.description",
                "title" to title,
                "when" to whenLabel.orEmpty(),
            )
            else -> strings(
                "today.next.description.where",
                "title" to title,
                "when" to whenLabel.orEmpty(),
                "where" to where,
            )
        },
        whenLabel = whenLabel,
        where = where,
        faces = faces,
        // **Nothing ahead means nothing to open.** A press that does nothing
        // reads as broken, rule 16, so the empty block offers its actions and
        // never itself.
        onOpen = appointment?.let { { onOpenAppointment() } },
        modifier = modifier.testTag(TodayHeroTags.ROOT),
        // **One row: one wide thing, and up to two marks beside it.** Owner
        // ruling, 2026-08-17. Which is which follows from what the block
        // already knows, and the rule is one sentence: **the wide slot is the
        // most useful thing that is not already on the screen above it.**
        //
        // | Ahead | Saved to ask | Wide | Marks |
        // |---|---|---|---|
        // | An appointment | Some | The count, as a door | Save a question |
        // | An appointment | None | Save a question | none |
        // | Nothing | Some | The count, as a door | Put it on the calendar, save a question |
        // | Nothing | None | Put it on the calendar | Save a question |
        //
        // **A count always takes the wide slot when there is one**, because it
        // is the only thing here carrying a number somebody needs before they
        // walk into a room. **Adding is never offered twice**: whatever takes
        // the wide slot loses its mark.
        footer = {
            val counted = questionsReady > 0
            if (counted) {
                InsetDoor(
                    count = questionsReady.toString(),
                    label = strings("today.next.questions.short", "count" to questionsReady),
                    description = strings("today.next.questions", "count" to questionsReady),
                    hue = hueFor(Repository.Section.ASK_NEXT_TIME),
                    onOpen = onOpenQuestions,
                    modifier = Modifier.weight(1f).testTag(TodayHeroTags.QUESTIONS),
                )
            } else if (appointment == null) {
                BlockAction(
                    mark = Symbols.addToCalendar,
                    label = strings["appts.add"],
                    onClick = onAddAppointment,
                    modifier = Modifier.weight(1f).testTag(TodayHeroTags.ADD_APPOINTMENT),
                )
            } else {
                BlockAction(
                    mark = Symbols.addQuestion,
                    label = strings["questions.add"],
                    onClick = onAddQuestion,
                    modifier = Modifier.weight(1f).testTag(TodayHeroTags.ADD_QUESTION),
                )
            }

            if (appointment == null && counted) {
                BlockIconAction(
                    mark = Symbols.addToCalendar,
                    label = strings["appts.add"],
                    onClick = onAddAppointment,
                    modifier = Modifier.testTag(TodayHeroTags.ADD_APPOINTMENT),
                )
            }
            if (counted || appointment == null) {
                BlockIconAction(
                    mark = Symbols.addQuestion,
                    label = strings["questions.add"],
                    onClick = onAddQuestion,
                    modifier = Modifier.testTag(TodayHeroTags.ADD_QUESTION),
                )
            }
        },
    )
}

object TodayHeroTags {
    const val ROOT = "today_hero"
    const val QUESTIONS = "today_hero_questions"
    const val ADD_APPOINTMENT = "today_hero_add_appointment"
    const val ADD_QUESTION = "today_hero_add_question"
}
