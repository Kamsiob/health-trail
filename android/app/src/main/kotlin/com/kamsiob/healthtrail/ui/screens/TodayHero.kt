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
 * | Ahead | Saved to ask | What it draws |
 * |---|---|---|
 * | An appointment | Some | When, who, what, where, the count as a door, and a way to add another |
 * | An appointment | None | The same, without the count, and a way to save the first |
 * | Nothing | Some | The invitation, the count as a door, and both ways in |
 * | Nothing | None | The invitation and both ways in |
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
        door = questionsReady.takeIf { it > 0 }?.let { count ->
            {
                InsetDoor(
                    count = count.toString(),
                    label = strings("today.next.questions.short", "count" to count),
                    description = strings("today.next.questions", "count" to count),
                    hue = hueFor(Repository.Section.ASK_NEXT_TIME),
                    onOpen = onOpenQuestions,
                    modifier = Modifier.testTag(TodayHeroTags.QUESTIONS),
                )
            }
        },
        actions = {
            // **The calendar first, because it is the thing that is missing.**
            // Once something is on it, saving a question is the only quick add
            // the block still owes: what to ask at the appointment it is showing.
            if (appointment == null) {
                BlockAction(
                    mark = Symbols.addToCalendar,
                    label = strings["appts.add"],
                    onClick = onAddAppointment,
                    modifier = Modifier.testTag(TodayHeroTags.ADD_APPOINTMENT),
                )
            }
            BlockAction(
                mark = Symbols.addQuestion,
                label = strings["questions.add"],
                onClick = onAddQuestion,
                modifier = Modifier.testTag(TodayHeroTags.ADD_QUESTION),
            )
        },
    )
}

object TodayHeroTags {
    const val ROOT = "today_hero"
    const val QUESTIONS = "today_hero_questions"
    const val ADD_APPOINTMENT = "today_hero_add_appointment"
    const val ADD_QUESTION = "today_hero_add_question"
}
