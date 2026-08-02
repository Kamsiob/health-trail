package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Chevron
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.ShellTags
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object TodayTags {
    const val ROOT = "today_root"
    const val EMPTY = "today_empty"
    const val INTERIM = "today_interim"
    const val EMERGENCY = "today_emergency"
    const val NEXT_APPOINTMENT = "today_next_appointment"
    fun step(number: Int) = "today_step_$number"
}

/**
 * Today.
 *
 * **Built because persona P1 requires it and was not getting it.** P1 is the
 * person standing in a corridor on the day of an admission, and one of the five
 * things that must be true for them is that the empty Today coaches rather than
 * sitting blank, with the Emergency Card first. It was the not-built screen,
 * which is exactly the wrong thing to hand someone at the moment they are most
 * likely to put the phone away.
 *
 * **The empty state is the finished screen, and the rest is honest about
 * itself.** The digest, the open counts, and the next appointment all need the
 * deterministic engine and screens that do not exist. A notebook with something
 * in it therefore says plainly that the summary is still being built, and says
 * in the same breath that nothing the person writes is waiting on it. That is
 * D44: an interface may offer something it has not built, and it may not go
 * quiet about it.
 *
 * **The three steps are guidance, not controls.** Two of the three lead to
 * screens that do not exist yet, and offering them as buttons would be exactly
 * the dead end D44 removed from the capture sheet. The one thing a person can
 * act on right now, capture, is already the gold button on every screen, so the
 * list reads as what to do next rather than as three disabled offers.
 *
 * **The numbering is allowed here** because this is genuinely a sequence, which
 * is the condition section 1's ban on numbered markers attaches to. Filling in
 * the emergency card first is the whole point of the list.
 *
 * Composed from Display L, Display S, Body L, Body M, the Mono style, and
 * cards 5.3. Nothing new was introduced.
 */
@Composable
fun TodayScreen(
    /**
     * Whether the coached start still applies.
     *
     * **Not "is the notebook empty".** It was, and that was wrong: logging one
     * call made the emergency card suggestion disappear before the person had
     * done it. The coaching is about what has been set up, not about whether
     * anything has been written, so it stays until the emergency card exists.
     */
    showCoaching: Boolean,
    /** Whether there is anything at all yet, which decides if a digest is owed. */
    hasAnything: Boolean,
    /**
     * What is still open, and what is coming.
     *
     * **These are counts of things waiting on somebody, never a score.** Rule
     * 13 forbids measuring the person's diligence, and the difference is who
     * the number is about: a question waiting on the wound nurse and a project
     * waiting on a caseworker are other people's inaction, which is precisely
     * what a caregiver loses track of and precisely what this screen is for.
     */
    modifier: Modifier = Modifier,
    openQuestions: Int = 0,
    waitingOnSomebody: Int = 0,
    unfiled: Int = 0,
    nextAppointment: Repository.Appointment? = null,
    onOpenQuestions: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenUnfiled: () -> Unit = {},
    onOpenAppointments: () -> Unit = {},
    onOpenEmergencyCard: () -> Unit = {},
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
        ) {
            Spacer(Modifier.height(Space.l))
            Text(
                text = strings["today.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.l))

            // **Both, when both apply.** Somebody who has logged a call and
            // still has no emergency card needs the digest note and the
            // coaching, and showing one instead of the other took the most
            // useful two minutes in the app off the screen the moment they
            // wrote anything down.
            if (hasAnything) {
                InterimDigest()
                if (showCoaching) Spacer(Modifier.height(Space.sectionGap))
            }
            if (showCoaching) {
                CoachedStart(nothingWrittenYet = !hasAnything)
            }

            // **What is still open, and it appears only when something is.**
            // An empty "nothing waiting" block on a quiet day would be a
            // heading over nothing, and this screen is read at a glance.
            val open = listOfNotNull(
                openQuestions.takeIf { it > 0 }?.let {
                    OpenItem("today.open.questions", it, onOpenQuestions)
                },
                waitingOnSomebody.takeIf { it > 0 }?.let {
                    OpenItem("today.open.waiting", it, onOpenProjects)
                },
                unfiled.takeIf { it > 0 }?.let {
                    OpenItem("unfiled.waiting", it, onOpenUnfiled)
                },
            )

            if (open.isNotEmpty() || nextAppointment != null) {
                Spacer(Modifier.height(Space.sectionGap))
                GroupHeader(labelKey = "today.open.group")
                Spacer(Modifier.height(Space.headerGap))

                open.forEach { item ->
                    OpenRow(item)
                    Spacer(Modifier.height(Space.cardGap))
                }

                nextAppointment?.let { appointment ->
                    NextAppointment(appointment, onOpenAppointments)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            // **The emergency card is one tap from here, always.**
            // MASTER_SPEC section 4.1 puts it on this screen for the reason the
            // coaching gives: it is the one thing in the app that is useful to
            // somebody else in a hurry, and in a hurry is exactly when nobody
            // wants to go looking through a table of contents for it.
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["notebook.section.emergency_card"],
                onClick = onOpenEmergencyCard,
                modifier = Modifier.fillMaxWidth().testTag(TodayTags.EMERGENCY),
            )

            // Clearance for the capture button, which overlaps the navigation
            // bar and would otherwise sit on the last line.
            Spacer(Modifier.height(Space.xxl + Space.l))
        }
    }
}

/**
 * What to do first, written as an invitation rather than an absence.
 *
 * Section 5.10: every list has an empty state and it is written as an
 * invitation. **The first item is always the Emergency Card**, because it is
 * the highest value two minutes a new person can spend, and because it is the
 * one thing in this app that is useful to somebody else in a hurry.
 */
@Composable
private fun CoachedStart(nothingWrittenYet: Boolean) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = Modifier.fillMaxWidth().testTag(TodayTags.EMPTY)) {
        Text(
            // "Nothing written down yet" stops being true the moment something
            // is, and a heading that contradicts the screen under it reads as
            // a bug. The list is the same list either way: what changes is
            // whether it is describing an empty notebook or suggesting a next
            // step in one that has started.
            text = strings[
                if (nothingWrittenYet) "today.empty.title" else "today.coach.heading"
            ],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.m))

        listOf(
            strings["today.empty.step.1"],
            strings["today.empty.step.2"],
            strings["today.empty.step.3"],
        ).forEachIndexed { index, step ->
            Step(number = index + 1, text = step)
            if (index < 2) Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = HealthTrail.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(TodayTags.step(number))
            .padding(Space.cardPadding),
    ) {
        // A numbered disc rather than a bullet, because the order is the
        // advice: the emergency card first is the point of the list.
        Row(
            modifier = Modifier
                .size(Space.l)
                .clip(CircleShape)
                .background(colors.sand),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            text = text,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A notebook with something in it, before the digest engine exists.
 *
 * **Says what is missing and says nothing is waiting on it.** The second half
 * matters more than the first: a person who reads that a summary is being built
 * needs to know immediately that their records are being kept regardless, or
 * the sentence reads as a reason to stop writing things down.
 *
 * Carries `ShellTags.NOT_BUILT` so it stays greppable and cannot survive to
 * release.
 */
@Composable
private fun InterimDigest() {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TodayTags.INTERIM)
            .testTag(ShellTags.NOT_BUILT),
    ) {
        Text(
            text = strings["today.digest.heading"],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["today.digest.not_built"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}

/** One thing still open, with the words that say how many and where it lives. */
private data class OpenItem(val key: String, val count: Int, val onOpen: () -> Unit)

/**
 * One open count, as a row that goes where the thing actually is.
 *
 * Rule 18: a count that cannot be reached is a dead end wearing a number.
 */
@Composable
private fun OpenRow(item: OpenItem) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = item.onOpen,
            )
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings(item.key, "count" to item.count),
            style = HealthTrail.type.bodyL,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Space.sm))
        Chevron()
    }
}

/**
 * The next appointment, which is the one piece of the future this screen holds.
 *
 * Its prep sheet is Phase 2 and is not built, so this says when and what and
 * goes to the list rather than promising more than it has.
 */
@Composable
private fun NextAppointment(
    appointment: Repository.Appointment,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            )
            .testTag(TodayTags.NEXT_APPOINTMENT)
            .padding(Space.cardPadding),
    ) {
        Text(
            text = EventDateText.render(strings, appointment.scheduledEdtf),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = appointment.title,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
    }
}
