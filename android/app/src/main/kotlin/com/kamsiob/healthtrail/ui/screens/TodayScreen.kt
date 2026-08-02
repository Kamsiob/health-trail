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
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Chevron
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object TodayTags {
    const val ROOT = "today_root"
    const val EMPTY = "today_empty"
    const val DIGEST = "today_digest"
    const val EMERGENCY = "today_emergency"
    const val NEXT_APPOINTMENT = "today_next_appointment"
    fun step(number: Int) = "today_step_$number"
    fun digestRow(section: Repository.Section) = "today_digest_${section.name.lowercase()}"
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
 * **The digest is real now and says only what the change log says.** It counts
 * what was written since the last visit, groups it by where it lives, and
 * stops. It never remarks on a quiet week or a busy one. The screen carried an
 * apology for the unbuilt engine as its most prominent element for as long as
 * the engine did not exist, which meant the app's front door led with what it
 * could not do.
 *
 * **The steps are controls, and only the ones still worth taking appear.** They
 * were three fixed sentences that led nowhere, so a notebook with a full care
 * team and four logged calls still advised adding people and logging a call,
 * and telling somebody to fill in the emergency card left them to go and find
 * it. Each step now opens the thing it names and disappears once it is done.
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
    /**
     * What changed since the last visit.
     *
     * Defaults to nothing, so a caller that has not worked it out yet shows no
     * digest rather than a wrong one.
     */
    digest: Digest.Summary = Digest.nothing,
    /** Where each digest row goes. Rule 18: a count that leads nowhere is a dead end. */
    onOpenSection: (Repository.Section) -> Unit = {},
    /**
     * The steps still worth suggesting, already filtered by the caller.
     *
     * **What is already done is not suggested.** The list used to be three
     * fixed sentences, so a notebook with a full care team and four logged
     * calls still advised adding people and logging a call. Advice that ignores
     * what is on the screen behind it reads as an app that is not paying
     * attention, on the one screen whose job is to be paying attention.
     */
    coaching: List<CoachStep> = emptyList(),
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

            // **Only when there is something to report.** A heading over
            // "nothing changed" is a heading over nothing, and this screen is
            // read at a glance. A first launch and a quiet week both land here
            // and both correctly show nothing.
            if (hasAnything && !digest.isEmpty) {
                DigestSection(digest, onOpenSection)
                if (coaching.isNotEmpty()) Spacer(Modifier.height(Space.sectionGap))
            }
            if (coaching.isNotEmpty()) {
                CoachedStart(steps = coaching, nothingWrittenYet = !hasAnything)
            }

            // **What is still open, and it appears only when something is.**
            // An empty "nothing waiting" block on a quiet day would be a
            // heading over nothing, and this screen is read at a glance.
            val open = listOfNotNull(
                openQuestions.takeIf { it > 0 }?.let {
                    OpenItem(strings("today.open.questions", "count" to it), onOpenQuestions)
                },
                waitingOnSomebody.takeIf { it > 0 }?.let {
                    OpenItem(strings("today.open.waiting", "count" to it), onOpenProjects)
                },
                unfiled.takeIf { it > 0 }?.let {
                    OpenItem(strings("unfiled.waiting", "count" to it), onOpenUnfiled)
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
            //
            // **Not while the coaching is still asking for it.** When the card
            // is empty the first coached step already offers it, in more words
            // and with the reason attached, and two controls to the same empty
            // screen is the same offer twice.
            if (coaching.none { it.section == Repository.Section.EMERGENCY_CARD }) {
                Spacer(Modifier.height(Space.sectionGap))
                QuietButton(
                    label = strings["notebook.section.emergency_card"],
                    onClick = onOpenEmergencyCard,
                    modifier = Modifier.fillMaxWidth().testTag(TodayTags.EMERGENCY),
                )
            }

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
private fun CoachedStart(steps: List<CoachStep>, nothingWrittenYet: Boolean) {
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

        steps.forEachIndexed { index, step ->
            Step(number = index + 1, text = strings[step.labelKey], onOpen = step.onOpen)
            if (index < steps.lastIndex) Spacer(Modifier.height(Space.cardGap))
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

@Composable
private fun Step(number: Int, text: String, onOpen: () -> Unit) {
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
            // **The advice goes where the advice points.** These were three
            // plain cards for as long as the screens they named did not exist.
            // They exist, so telling somebody to fill in the emergency card and
            // leaving them to find it is a dead end wearing a suggestion.
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            )
            .testTag(TodayTags.step(number))
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
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
        Spacer(Modifier.width(Space.sm))
        Chevron()
    }
}

/**
 * What changed since the last visit.
 *
 * **Each section that gained something is a row that opens it**, drawn like the
 * notebook's own rows, because it is the same idea: a place and how much is in
 * it. Somebody who reads "the trail, 2 new" is already asking which two, and
 * the answer is one tap away rather than a hunt through a table of contents.
 *
 * **Corrections and removals are stated quietly and without a destination.**
 * They are usually the person tidying up after themselves, and the app has
 * nothing useful to show if they tapped: the corrected row is wherever it
 * always was, and the removed one is gone. Giving them the same weight as new
 * records would make the screen a report card on how tidy somebody is being.
 */
@Composable
private fun DigestSection(
    summary: Digest.Summary,
    onOpenSection: (Repository.Section) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = Modifier.fillMaxWidth().testTag(TodayTags.DIGEST)) {
        Text(
            text = strings["today.digest.heading"],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.m))

        summary.added.forEach { added ->
            OpenRow(
                OpenItem(
                    label = strings[labelKey(added.section)],
                    detail = strings("today.digest.new", "count" to added.count),
                    onOpen = { onOpenSection(added.section) },
                    testTag = TodayTags.digestRow(added.section),
                ),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        val asides = listOfNotNull(
            summary.corrected.takeIf { it > 0 }
                ?.let { strings("today.digest.corrected", "count" to it) },
            summary.removed.takeIf { it > 0 }
                ?.let { strings("today.digest.removed", "count" to it) },
        )
        if (asides.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            asides.forEach { aside ->
                Text(
                    text = aside,
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.xs))
            }
        }
    }
}

/**
 * One row that says a thing and goes where the thing is.
 *
 * [detail] is the quieter second line, present when the label alone does not
 * say how much. It is a separate line rather than a longer sentence so the
 * count reads at a glance, which is how this screen is used.
 */
private data class OpenItem(
    val label: String,
    val onOpen: () -> Unit,
    val detail: String? = null,
    val testTag: String? = null,
)

/**
 * One open count, as a row that goes where the thing actually is.
 *
 * Rule 18: a count that cannot be reached is a dead end wearing a number.
 */
@Composable
private fun OpenRow(item: OpenItem) {
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
            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )
            if (item.detail != null) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = item.detail,
                    style = HealthTrail.type.mono,
                    color = colors.ink3Text,
                )
            }
        }
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
