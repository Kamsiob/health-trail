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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Chevron
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.Hero
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.GroupedRows
import com.kamsiob.healthtrail.ui.components.FoldRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedVisibility
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.TabChip
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
    const val SEARCH = "today_search"
    const val ALSO_WAITING = "today_also_waiting"
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
    /**
     * When the person was last here, or null on a first run.
     *
     * **The heading says "since you were last here" and never said when.**
     * After four months away, "47 new" without a date is the one piece of
     * context that matters, and journey seven in Part Three is precisely
     * somebody coming back after four months.
     */
    lastVisitMillis: Long? = null,
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
    openIncidents: Int = 0,
    openQuestions: Int = 0,
    waitingOnSomebody: Int = 0,
    unfiled: Int = 0,
    nextAppointment: Repository.Appointment? = null,
    onOpenIncidents: () -> Unit = {},
    onOpenQuestions: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenUnfiled: () -> Unit = {},
    onOpenAppointments: () -> Unit = {},
    onOpenEmergencyCard: () -> Unit = {},
    /**
     * Opens search.
     *
     * **`MASTER_SPEC.md` 4.8 puts universal search at the top of Today**, and
     * it was two taps into More. Walking the "find something from three months
     * ago" journey is what surfaced it: the search itself takes one word, and
     * getting to it took longer than using it.
     */
    onSearch: () -> Unit = {},
) {
    val strings = LocalStrings.current
    var alsoOpen by rememberSaveable { mutableStateOf(false) }
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
            Spacer(Modifier.height(Space.sm))

            // **The tab and the way to search, on one row**, per grid screen 01.
            // Today belongs to no section, so it takes gold and the base ladder
            // rather than a section hue, per `DESIGN.md` 4.3.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabChip(hue = wholeAppHue(), labelKey = "today.tab")
                QuietButton(
                    label = strings["today.search"],
                    onClick = onSearch,
                    modifier = Modifier.testTag(TodayTags.SEARCH),
                )
            }
            Spacer(Modifier.height(Space.cardGap))

            // **At the top, under the title**, where 4.8 puts it.
            //
            // **The full width search row is gone.** Grid screen 01 puts the
            // way to search in the header beside the tab, as a small outlined
            // action, and a second search affordance under the title would be
            // two doors to one place on the screen law 1 says must have one
            // dominant thing. What is dominant here is what changed, not the
            // way to go looking for something else.

            // **Only when there is something to report.** A first launch and a
            // quiet week both land here and both correctly show nothing.
            if (hasAnything) {
                // **The hero, per 11.5.** One line at display size saying what
                // changed, which is the single thing this screen exists to
                // answer, and it used to be a Display S heading over a stack of
                // identical cards. Rule 15: the biggest thing on the screen
                // carries the most information.
                //
                // **"Nothing new" is a hero too, and that is deliberate.** It
                // is the answer to the question the person opened the app to
                // ask, and burying it in body text would make the calm case
                // read as the failure case.
                Hero(eyebrowKey = "today.digest.heading") {
                    Text(
                        text = if (digest.isEmpty) {
                            strings["today.digest.empty"]
                        } else {
                            strings("today.digest.total", "count" to digest.newThings)
                        },
                        // **Hero scale, per law 1**, which is the answer to
                        // why the person opened this screen and is meant to be
                        // felt at arm's length. `DESIGN.md` 5.1 puts the hero at
                        // 21 to 24sp against supporting content at 13sp, and the
                        // jump is the hierarchy.
                        style = HealthTrail.type.hero,
                        color = colors.ink,
                        modifier = Modifier.testTag(TodayTags.DIGEST),
                    )

                    // **Only when it is genuinely a gap.** Below the threshold
                    // this is a line telling somebody who opens the app every
                    // morning that they opened it yesterday.
                    lastVisitMillis
                        ?.takeIf { millis ->
                            java.time.Duration.between(
                                java.time.Instant.ofEpochMilli(millis),
                                java.time.Instant.now(),
                            ).toDays() >= AWAY_DAYS
                        }
                        ?.let { millis ->
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                text = strings(
                                    "today.digest.lastvisit",
                                    "date" to EventDateText.render(
                                        strings,
                                        java.time.Instant.ofEpochMilli(millis)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate()
                                            .toString(),
                                    ),
                                ),
                                style = HealthTrail.type.bodyM,
                                color = colors.ink2,
                            )
                        }
                }

                // **The breakdown as dense rows, not cards**, per 11.3. Each is
                // a name, a count and a way in, which is exactly the shape 11.3
                // is for, and four of them as cards was four things all
                // shouting the same volume as the thing that mattered.
                if (!digest.isEmpty) {
                    // **In a group, like every other run of rows in the app.**
                    // Bare rows with chevrons floating on `paper` are a legal
                    // costume but a different shape from the same rows two
                    // inches below them, and a screen read at a glance cannot
                    // afford two treatments of one thing.
                    GroupedRows(items = digest.added) { added, isLast ->
                        DenseRow(
                            title = strings[labelKey(added.section)],
                            trailing = strings("today.digest.new", "count" to added.count),
                            chevron = true,
                            divider = !isLast,
                            onClick = { onOpenSection(added.section) },
                            modifier = Modifier.testTag(TodayTags.digestRow(added.section)),
                        )
                    }

                    val asides = listOfNotNull(
                        digest.corrected.takeIf { it > 0 }
                            ?.let { strings("today.digest.corrected", "count" to it) },
                        digest.removed.takeIf { it > 0 }
                            ?.let { strings("today.digest.removed", "count" to it) },
                    )
                    // One line each, as before. They are not counts of new
                    // things and must not sit among the rows that are.
                    asides.forEach { aside ->
                        Spacer(Modifier.height(Space.s))
                        Text(
                            text = aside,
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                    }
                }
            }

            if (coaching.isNotEmpty()) {
                if (hasAnything) Spacer(Modifier.height(Space.sectionGap))
                CoachedStart(steps = coaching, nothingWrittenYet = !hasAnything)
            }

            // **What is still open, and it appears only when something is.**
            // An empty "nothing waiting" block on a quiet day would be a
            // heading over nothing, and this screen is read at a glance.
            val open = listOfNotNull(
                // **Incidents come first**, because an incident nobody has
                // answered is the thing the person is carrying around. Never a
                // judgment about how long, per rule 2.
                openIncidents.takeIf { it > 0 }?.let {
                    OpenItem(strings("today.open.incidents", "count" to it), onOpenIncidents)
                },
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

            // **The one that needs the person leads; the rest fold.**
            //
            // This was four dense rows and an appointment, all at one weight,
            // which is the shape law 1 exists to end: a person in a hallway had
            // to read five lines to find the one that was theirs to chase.
            //
            // Grid screen 01 draws it as one row and a fold called "Also
            // waiting" carrying its count, and screen 02 shows that fold opened
            // in place. **Nothing is hidden**: the fold names what it holds and
            // how much, and one tap has all of it.
            //
            // **The order is already the priority order.** Incidents first,
            // because an incident nobody has answered is the thing the person
            // is carrying around, then questions, then what somebody else owes,
            // then filing. So the lead is simply the first of them, with no
            // second ranking rule to disagree with the first.
            val everything = open + listOfNotNull(
                nextAppointment?.let { appointment ->
                    OpenItem(
                        label = Bidi.isolate(appointment.title),
                        onOpen = onOpenAppointments,
                        testTag = TodayTags.NEXT_APPOINTMENT,
                        subtitle = EventDateText.render(strings, appointment.scheduledEdtf),
                    )
                },
            )

            if (everything.isNotEmpty()) {
                Spacer(Modifier.height(Space.sectionGap))

                val lead = everything.first()
                val rest = everything.drop(1)

                GroupedSurface {
                    DenseRow(
                        title = Bidi.isolate(lead.label),
                        subtitle = lead.subtitle,
                        chevron = true,
                        divider = false,
                        onClick = lead.onOpen,
                        modifier = lead.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                    )
                }

                if (rest.isNotEmpty()) {
                    Spacer(Modifier.height(Space.cardGap))
                    FoldRow(
                        labelKey = "today.also_waiting",
                        expanded = alsoOpen,
                        onToggle = { alsoOpen = !alsoOpen },
                        count = rest.size.toString(),
                        modifier = Modifier.testTag(TodayTags.ALSO_WAITING),
                    )
                    AnimatedVisibility(visible = alsoOpen) {
                        Column {
                            Spacer(Modifier.height(Space.cardGap))
                            GroupedRows(items = rest) { item, isLast ->
                                DenseRow(
                                    title = Bidi.isolate(item.label),
                                    subtitle = item.subtitle,
                                    chevron = true,
                                    divider = !isLast,
                                    onClick = item.onOpen,
                                    modifier = item.testTag?.let { Modifier.testTag(it) }
                                        ?: Modifier,
                                )
                            }
                        }
                    }
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
                Spacer(Modifier.height(Space.xl))
                // **Sized to its label.** D137 gives the full width outlined
                // bar to the way back, and a bar that wide across the foot of
                // Today reads as a way out of Today rather than a door into
                // the card. Still one tap, which is what rule 18 asks for.
                // #371 item 5.
                QuietButton(
                    label = strings["notebook.section.emergency_card"],
                    onClick = onOpenEmergencyCard,
                    modifier = Modifier.testTag(TodayTags.EMERGENCY),
                )
            }

            // Clearance for the capture button, which overlaps the navigation
            // bar and would otherwise sit on the last line.
            Spacer(Modifier.height(Space.fabScrollClearance))
        }
    }
}

/**
 * The way into search, as a recessed row rather than a card.
 *
 * It was a `QuietButton`, which is a full width white bar with a shadow, and
 * 11.4 is blunt about that shape: a card whose whole content is one line is a
 * dense row that was given a shadow. On the screen whose job is to say what
 * changed, it was taking a card's weight to say "search".
 *
 * **`sand` at the field radius, which is what 5.9 sets aside for a way into
 * typing**, and that is honest here because what it opens is a field. The label
 * is `ink2` rather than a hint color, because it is a label rather than a
 * placeholder standing in for one.
 */
@Composable
private fun SearchRow(onSearch: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onSearch,
            )
            .testTag(TodayTags.SEARCH)
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings["today.search"],
            style = HealthTrail.type.bodyL,
            color = colors.ink2,
        )
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
            .clip(Radius.cardLarge)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.cardLarge)
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
    /** A second line, which only the appointment has: it is the one row here with a date to say. */
    val subtitle: String? = null,
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
            .clip(Radius.cardLarge)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.cardLarge)
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
                text = Bidi.isolate(item.label),
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )
            if (item.detail != null) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Bidi.isolate(item.detail),
                    style = HealthTrail.type.mono,
                    color = colors.ink2,
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
            .clip(Radius.cardLarge)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.cardLarge)
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
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = Bidi.isolate(appointment.title),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
    }
}

/**
 * How long away has to be before the app says when you were last here.
 *
 * Three days. Somebody who opens this every morning does not need telling that
 * they opened it yesterday, and somebody back after a hospital week or four
 * months does.
 */
private const val AWAY_DAYS = 3L
