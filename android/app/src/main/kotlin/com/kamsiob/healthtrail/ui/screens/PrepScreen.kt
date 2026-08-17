package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.CalendarHandoff
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
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

object PrepTags {
    const val NAME = "prep"
    const val WHEN = "prep_when"
    const val WITH = "prep_with"
    const val SHARE = "prep_share"
    const val CALENDAR = "prep_calendar"
    const val WRITE_UP = "prep_write_up"
    const val CORRECT = "prep_correct"
    const val REMOVE = "prep_remove"
    const val CHANGES_FOLD = "prep_changes_fold"
    const val ASKED_FOLD = "prep_asked_fold"
    const val ASK = "prep_ask"
    fun asked(id: String) = "prep_asked_$id"
    fun question(id: String) = "prep_question_$id"
    fun roleFold(role: String) = "prep_role_$role"
    fun change(id: String) = "prep_change_$id"
}

/**
 * One appointment: when it is, and what to walk in carrying.
 *
 * `MASTER_SPEC.md` 4.5: the questions waiting for that person plus a change
 * summary composed from real entries, every line tapping through to its source.
 *
 * **This is the app's most useful two minutes, and it is entirely
 * composition.** Nothing on this screen is generated, inferred, or summarized.
 * The questions are the ones somebody wrote down and never asked. The changes
 * are the entries themselves, not a description of them. 4.11 requires that,
 * and rule 2 is why.
 *
 * **When it is, is the one thing.** Law 1, and it is what somebody opening an
 * appointment is checking: not what it is called, which they already knew when
 * they tapped it, but which day, and whether they have anything to ask. So the
 * date is at hero weight and the questions are the next thing under it.
 *
 * **Since the last appointment, not since some window.** Somebody walking into
 * a care plan meeting wants what has happened since the last time they sat in
 * that room. A fixed thirty days would either repeat what was already covered
 * or silently drop what was not, and the screen says which date it is counting
 * from rather than leaving it to be guessed. **It folds**, because in year
 * three that list is longer than the screen and the questions are the job.
 *
 * **Every line opens its entry**, which is the requirement in the spec's own
 * words and the reason journey five needed the entry screen first.
 *
 * **The calendar hand-off is offered and never assumed.** `DESIGN.md` 9.1: one
 * event, user initiated, one way, nothing read back, and no action at all when
 * either the phone has no calendar app or the date is coarser than a day.
 * `CalendarHandoff` carries the reasoning.
 */
@Composable
fun PrepScreen(
    prep: Repository.Prep,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onShare: () -> Unit,
    onWriteUp: () -> Unit,
    /**
     * Taking the appointment out of the notebook, per #218. The list row used
     * to do this on a long press, which a sighted person could not find.
     */
    /**
     * Corrects the appointment itself: its title, its day, where it is.
     *
     * **`updateAppointment` was written and no caller could reach it**, so an
     * appointment that moved could only be removed and retyped, and rule 17's
     * promise that a date is editable forever from the entry itself was not
     * kept here. #360.
     */
    /**
     * Opens the question, where it can be marked asked or answered. #360.
     *
     * **The sheet stays open underneath**, so somebody who ticks one off is
     * still standing on the list they came in with.
     */
    onOpenQuestion: (Repository.Question) -> Unit,
    onCorrect: () -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Writing down something else to ask, from the sheet itself.
     *
     * **#46: a question written during an appointment belongs to it.** Until
     * now the only way to add one was to leave this sheet, open Ask next time,
     * and write it there with nobody attached, which is how a question for the
     * charge nurse ends up on the billing meeting's sheet. The form opens
     * knowing who this appointment is with, and that is a prefill the person
     * can change rather than a decision made for them.
     */
    onAsk: () -> Unit = {},

    backLabelKey: String = "section.back.appointments",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val context = LocalContext.current
    val appointment = prep.appointment
    // Which folded roles are open, kept by label so it survives rotation and
    // the list being scrolled away and back.

    // **Resolved once and used to decide whether the action exists at all.**
    // A phone with no calendar app would otherwise get a button that throws,
    // which rule 11 calls unfinished. The manifest declares the query that
    // makes this answerable at all on Android 11 and later.
    val calendarIntent = remember(appointment) {
        CalendarHandoff.intent(
            title = appointment.title.ifBlank { strings["prep.untitled"] },
            scheduledEdtf = appointment.scheduledEdtf,
            scheduledStart = appointment.scheduledStart,
            locationNote = appointment.locationNote,
        )?.takeIf { context.packageManager.resolveActivity(it, 0) != null }
    }

    SectionScaffold(
        name = PrepTags.NAME,
        title = strings["notebook.section.appointments"],
        heading = Bidi.isolate(appointment.title.ifBlank { strings["prep.untitled"] }),
        section = Repository.Section.APPOINTMENTS,
        // **The other half of the one thing.** Law 1 for this screen is when it
        // is and whether the prep is ready, so the count of what there is to
        // ask sits with the name and the date carries the weight. It is a count
        // of what is written down and never of how far along the person is,
        // which rule 13 rules out, and at zero it says nothing is saved yet
        // rather than treating an empty list as a failing.
        subtitle = strings("prep.ready", "count" to prep.questions.size),
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            // **The date at hero weight**, at exactly the precision somebody
            // gave it. "Sometime in November" stays a month and never becomes
            // the first of it, per rule 17, and a date nobody knows yet says so
            // rather than showing a blank, per rule 13.
            Text(
                text = appointment.scheduledEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                    ?: strings["date.unknown"],
                style = HealthTrail.type.hero,
                color = colors.ink,
                modifier = Modifier.testTag(PrepTags.WHEN),
            )

            // **Who it is with, and it is not decoration here.** The questions
            // below are filtered to this person plus the ones waiting on nobody
            // in particular, so a sheet that filtered silently would be the app
            // deciding what to show and not saying on what basis. Rule 20: the
            // complexity lives in the code, and what the person sees is a
            // name they chose.
            appointment.personName?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings("appts.with", "what" to Bidi.isolate(it)),
                    style = HealthTrail.type.bodyL,
                    color = colors.ink,
                    modifier = Modifier.testTag(PrepTags.WITH),
                )
            }

            // Where it is, at reading size, because it is the second thing
            // somebody checks and the one they will be reading off the phone in
            // a car park. The notes recede under it.
            appointment.locationNote?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.s))
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
            }

            appointment.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.xs))
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyM, color = colors.ink2)
            }

            Spacer(Modifier.height(Space.sectionGap))
        }

        // **The questions come first and do not fold.** They are the reason to
        // prepare at all, and the thing most easily forgotten in the room.
        //
        // **"Ask about" appears only when there is nothing under it.** With
        // questions present the first role heading follows immediately, and two
        // mono rules stacked with nothing between them is a header heading
        // another header. The line above already says how many there are to ask
        // about, so the roles can carry it from there.
        if (prep.questions.isEmpty()) {
            item {
                Eyebrow(text = strings["prep.questions"])
                Spacer(Modifier.height(Space.headerGap))
                Text(
                    text = strings["prep.questions.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else {
            // **Grouped by who answers it, which is how the room works.** These
            // were eight cards on a spine, each repeating its own role label in
            // mono, so "Charge nurse" appeared three times in a column and a
            // spine implied an order the questions do not have. Rule 22: a
            // question is one sentence, which is a row, and a card for it is
            // what makes eight of them a wall nothing stands out of.
            //
            // **This is screen 21's composition, not a new one.** The questions
            // section already solved a wall of questions exactly this way, and
            // the prep sheet is the same content in a smaller room, so it reads
            // as its sibling rather than as a second answer to one problem.
            //
            // **The largest group leads open and the rest fold.** Somebody
            // standing in front of the charge nurse wants the charge nurse's
            // questions, and the biggest group is the likeliest reason they
            // opened this at all.
            val byRole = prep.questions
                .groupBy { it.roleLabel?.takeIf { role -> role.isNotBlank() } }
                // Size then name, so the order is stable rather than depending
                // on which question happened to be written first.
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String?, List<Repository.Question>>> {
                        it.second.size
                    }.thenBy { it.first ?: "" },
                )

            byRole.forEachIndexed { index, (role, inRole) ->
                val label = role ?: strings["questions.group.anyone"]
                val leads = index == 0

                if (!leads) {
                    item(key = "prep_fold_${role ?: "anyone"}") {
                        Eyebrow(text = Bidi.join(Bidi.isolate(label), inRole.size.toString()), modifier = Modifier.testTag(PrepTags.roleFold(label)), fixed = false)
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }

                item(key = "prep_group_${role ?: "anyone"}") {
                    if (leads) {
                        Eyebrow(text = Bidi.isolate(label), fixed = false)
                        Spacer(Modifier.height(Space.s))
                    }
                    Block(padding = Space.none) {
                        inRole.forEachIndexed { row, question ->
                            ListRow(
                                // The question itself carries the row,
                                // because it is the thing being read out
                                // loud. Its role is the heading above it
                                // and is not repeated here, per 17.
                                title = Bidi.isolate(question.text),
                                // **It opens, because this is the screen
                                // somebody is holding in the room.** The
                                // rows were the only ones on this sheet
                                // that did not: the changes below them
                                // opened all along, so half the sheet
                                // answered a tap and half of it did not,
                                // and ticking off the question just asked
                                // meant leaving the prep sheet and finding
                                // it again in Ask next time. #360.
                                onClick = { onOpenQuestion(question) },
                                clickLabel = strings["open.action"],
                                modifier = Modifier
                                    .testTag(PrepTags.question(question.id)),
                            )
                            if (row < inRole.lastIndex) RowDivider(inset = false)
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            // Air before the record, so the four sand folds do not read as one
            // run of four equivalent things. Three of them are people to ask
            // and the fourth is what has already happened.
            item { Spacer(Modifier.height(Space.sectionGap)) }
        }

        // **What was asked here, folded and counted.** The other half of the
        // link, per rule 18: `asked_at_appointment_id` shipped in Phase 0, the
        // archive renders it, all four catalogs name it, and until #371 the
        // only thing that had ever written it was the fixture. So a question
        // could claim an appointment that said nothing about it.
        //
        // **Folded rather than led with**, because somebody opening this in a
        // car park is here for what to ask rather than for what they asked last
        // time, and **it is not empty state**: a sheet with nothing asked yet
        // draws nothing at all rather than a heading over a sentence saying so.
        if (prep.asked.isNotEmpty()) {
            item(key = "asked_here") {
                Eyebrow(text = Bidi.join(strings["prep.asked.here"], prep.asked.size.toString()), modifier = Modifier.testTag(PrepTags.ASKED_FOLD))
                Spacer(Modifier.height(Space.cardGap))
            }
            item(key = "asked_here_rows") {
                Block(padding = Space.none) {
                    prep.asked.forEachIndexed { row, question ->
                        ListRow(
                            title = Bidi.isolate(question.text),
                            // What came back, where somebody wrote it down.
                            // Absent rather than empty when they did not:
                            // being told nothing and not having written it
                            // down are different things.
                            support = question.answerText
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Bidi.isolate(it) },
                            onClick = { onOpenQuestion(question) },
                            clickLabel = strings["open.action"],
                            modifier = Modifier.testTag(PrepTags.asked(question.id)),
                        )
                        if (row < prep.asked.lastIndex) RowDivider(inset = false)
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **The way to add one sits under the questions**, where somebody who
        // has just read them is looking, rather than at the foot of a sheet
        // whose middle is forty entries. #46.
        item {
            Action(
                label = strings["questions.add"],
                onClick = onAsk,
                modifier = Modifier.testTag(PrepTags.ASK),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **What has happened folds and is counted.** In year three this is
        // forty entries and the questions are still the job, so it arrives
        // closed with its number on it rather than pushing everything else off
        // the screen. One tap, and the count says whether the tap is worth it.
        if (prep.changes.isEmpty()) {
            item {
                Eyebrow(text = strings["prep.changes"])
                Spacer(Modifier.height(Space.headerGap))
                Text(
                    text = strings["prep.changes.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
            }
        } else {
            item {
                Eyebrow(text = Bidi.join(strings["prep.changes"], prep.changes.size.toString()), modifier = Modifier.testTag(PrepTags.CHANGES_FOLD))
                Spacer(Modifier.height(Space.cardGap))
            }

            item {
                // **Says what window it is showing**, rather than leaving
                // somebody to work out whether something is missing.
                Text(
                    text = prep.sinceEdtf?.takeIf { it.isNotBlank() }
                        ?.let {
                            strings(
                                "prep.changes.since",
                                "date" to EventDateText.render(strings, it),
                            )
                        }
                        ?: strings["prep.changes.all"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.m))
            }

            prep.changes.forEachIndexed { index, entry ->
                item(key = "c_${entry.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < prep.changes.lastIndex,
                        node = colors.gold,
                        routeColor = colors.gold,
                        dash = RouteDash.TRAIL,
                    ) {
                        Column {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) { }
                                    .clip(Radius.cardLarge)
                                    // **It opens the entry and it says so.**
                                    // This carried the removal treatment
                                    // before, which told a reader user that
                                    // tapping would remove the entry and
                                    // offered them a long press labeled
                                    // "remove" that did nothing at all.
                                    // Nothing is removed from a prep sheet:
                                    // it is a view of the trail.
                                    .openableByTap(
                                        label = strings["prep.change.open"],
                                        onTap = { onOpenEntry(entry) },
                                    )
                                    .testTag(PrepTags.change(entry.id))
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
                                    text = entry.title?.takeIf { it.isNotBlank() }
                                        ?.let { Bidi.isolate(it) }
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
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // **One filled action, and it is the one that leaves the app.** The
            // prep sheet exists to be carried into a room, and handing it to
            // whatever the person already messages people with is the act the
            // rest of the screen is for. Everything else here is a supporting
            // move and wears the quiet costume.
            Action(
                label = strings["prep.share"],
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().testTag(PrepTags.SHARE), emphasis = ActionEmphasis.Main,
            )

            calendarIntent?.let { intent ->
                Spacer(Modifier.height(Space.cardGap))
                Action(
                    label = strings["prep.calendar"],
                    onClick = {
                        // The calendar app opens its own new event screen with
                        // this filled in and unsaved. Nothing exists until the
                        // person saves it there.
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.testTag(PrepTags.CALENDAR),
                )
            }

            Spacer(Modifier.height(Space.cardGap))
            // **Writing it up afterward is the other half of the journey**, and
            // it opens the ordinary capture form rather than a special one, so
            // what comes out is an ordinary entry on the trail.
            //
            // **The two quiet ones are sized to their label**, D118. The share
            // stays full width because it is filled and is the one act this
            // screen exists for; a full width outlined pill is the way back's
            // costume and an in-content action must not wear it.
            Action(
                label = strings["prep.writeup"],
                onClick = onWriteUp,
                modifier = Modifier.testTag(PrepTags.WRITE_UP),
            )

            // **Correcting sits above removing**, per rule 15 and the person's
            // screen: an appointment moves far more often than it is taken off
            // the record.
            Spacer(Modifier.height(Space.cardGap))
            Action(
                label = strings["appts.correct"],
                onClick = onCorrect,
                modifier = Modifier.testTag(PrepTags.CORRECT),
            )

            // **The appointment itself, removed from the appointment's own
            // screen**, per #218. It opens the confirmation and removes
            // nothing on its own.
            Spacer(Modifier.height(Space.sectionGap))
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(PrepTags.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
