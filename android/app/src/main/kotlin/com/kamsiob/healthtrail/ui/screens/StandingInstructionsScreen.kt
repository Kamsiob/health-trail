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
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor

object InstructionTags {
    fun violation(id: String) = "instruction_violation_$id"
    fun violationRow(id: String) = "instruction_violation_row_$id"
    const val NAME = "standing_instructions"
    const val ADD = "standing_instructions_add"
    const val MEANING = "standing_instructions_meaning"
    fun row(id: String) = "standing_instruction_$id"
    fun tag(id: String) = "standing_instruction_tag_$id"
}

/**
 * Standing instructions: what this family has asked for, and what backs it.
 *
 * **The tag is the load bearing part of this screen and it is never optional.**
 * Every instruction says either that federal nursing home rules require it or
 * that it is a request nobody has to agree to. An instruction rendered without
 * its tag would let the app imply a right that may not exist, which is the
 * sharpest way this project could break rule 2. `run_all.py` carries a check
 * for exactly this, listed as waiting on this screen.
 *
 * **The tag's explainer is shown, not just its label.** "Backed by federal
 * rules for nursing homes" is useless to somebody whose mother is in assisted
 * living, and the catalog's explainer says plainly that other kinds of places
 * are not covered by those rules. Showing the label alone would be the app
 * being accurate and misleading at the same time.
 *
 * **The app records that something was asked. It never says what follows from
 * that.** No advice about escalating, no assessment of whether a violation
 * occurred, no count presented as a conclusion. `MASTER_SPEC.md` section 4.3
 * and the schema comment on `instruction_violation` both say the app counts and
 * concludes nothing.
 */
@Composable
fun StandingInstructionsScreen(
    instructions: List<Repository.StandingInstruction>,
    tags: Map<String, TemplateCatalog.InstructionTag>,
    /**
     * Opens the request: how they answered is recorded there, and so is taking
     * it off the list.
     *
     * **It was two handlers**, a tap that recorded the answer and a long press
     * that removed the request, and the long press was the only path to
     * removal. #218 and law 2.
     */
    onOpen: (Repository.StandingInstruction) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Every time each instruction was not followed, most recent first.
     *
     * **A count and never a judgment**, per rule 2 and `MASTER_SPEC.md` 4.11.
     * Nothing here says a facility is bad, nothing is colored by how many, and
     * no threshold turns a number into an opinion. What it means is the
     * person's to judge, and the row says so in those words.
     *
     * **It used to be the count alone**, which is what #371 found: somebody
     * typed "the night nurse gave it at 9 instead of 6" and the app showed them
     * a 3. The words are the record. The count is only how many of them there
     * are, and it is now the list's own size rather than a second query.
     */
    violations: Map<String, List<Repository.Violation>> = emptyMap(),
    onRecordViolation: (Repository.StandingInstruction) -> Unit = {},
    /**
     * Opens one recorded time, where it can be corrected or taken off.
     *
     * **It was the only record in the app that could never be fixed**, and
     * three of the five panels named that first: somebody interrupted mid
     * sentence taps Save, and the half typed word is permanent and permanently
     * counted.
     */
    onOpenViolation: (Repository.StandingInstruction, Repository.Violation) -> Unit =
        { _, _ -> },
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = InstructionTags.NAME,
        title = strings["notebook.section.standing_instructions"],
        subtitle = strings["instructions.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.STANDING_INSTRUCTIONS,
        headingKey = "instructions.heading",
    ) {
        if (instructions.isEmpty()) {
            item {
                SectionEmpty(name = InstructionTags.NAME, text = strings["instructions.empty"], section = Repository.Section.STANDING_INSTRUCTIONS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        // **The one with something wrong leads.** Law 1: what a person opens
        // this screen for is whether what they asked for is being done, and the
        // instruction that has not been is the answer to that. Where nothing
        // has been broken, the one still waiting for an answer leads instead,
        // and where everything is settled the most recent does.
        val lead = instructions.maxWithOrNull(
            compareBy(
                { violations[it.id]?.size ?: 0 },
                { if (it.isAcknowledged) 0 else 1 },
            ),
        )

        // **Each tag explains itself once, on the first instruction carrying
        // it.** Explaining only on the leading instruction was the first fix and
        // it was wrong: a notebook whose lead is an unbacked request would never
        // show what "backed by federal rules" means anywhere at all. Seen on the
        // phone, where the federal instruction sat with its label and no
        // explanation on the screen.
        val explained = mutableSetOf<String>()
        val ordered = instructions.sortedByDescending { it.id == lead?.id }

        ordered.forEach { instruction ->
            val explainHere = explained.add(instruction.tag)
            val times = violations[instruction.id].orEmpty()

            item(key = instruction.id) {
                InstructionRow(
                    onRecordViolation = { onRecordViolation(instruction) },
                    instruction = instruction,
                    tag = tags[instruction.tag],
                    lead = explainHere,
                    onOpen = { onOpen(instruction) },
                )
                Spacer(
                    Modifier.height(if (times.isEmpty()) Space.cardGap else Space.sectionGap),
                )
            }

            // **What was asked, then every time it was not followed, on a
            // spine.** `DESIGN.md` section 14 wrote that line for the
            // instruction's own screen, which cannot be built while
            // `NotebookShell` sits at the JVM's method limit, and the shape it
            // asks for is right here either way: these are dated events in
            // order, which is what the inventory reserves a spine for.
            //
            // **Outside the card rather than inside it**, and that is the
            // structural half. The card is one `openableByTap`, which merges
            // its descendants, so five times written inside it became one
            // uninterrupted reader announcement labeled "Open" with no way to
            // land on a single one, and a person tapping their own sentence got
            // the acknowledgment sheet. Out here each time is its own node.
            if (times.isNotEmpty()) {
                item(key = "${instruction.id}_times") {
                    GroupHeaderText(
                        label = strings["instruction.violations.group"],
                        count = times.size.toString(),
                        // **The reader hears the sentence, not the digit.**
                        // "Times it was not followed 5" is not what the count
                        // means, and section 9 says what is read aloud says
                        // the same thing the screen says.
                        countDescription = strings(
                            "instruction.violations.count",
                            "count" to times.size,
                        ),
                    )
                    Spacer(Modifier.height(Space.headerGap))
                }
                times.forEachIndexed { index, violation ->
                    item(key = violation.id) {
                        ViolationRow(
                            violation = violation,
                            continuesAbove = index > 0,
                            continuesBelow = index < times.lastIndex,
                            onOpen = { onOpenViolation(instruction, violation) },
                        )
                    }
                }
                item(key = "${instruction.id}_times_end") {
                    Spacer(Modifier.height(Space.sectionGap))
                }
            }
        }

        // **Said once for the screen rather than once per instruction.** Both
        // of these paragraphs were repeated verbatim on every card: three
        // instructions meant the same two hundred words three times, which is
        // section 1's "same label in two slots" at paragraph length, and it
        // buried what each instruction actually said.
        //
        // They are still said. The content rules require the app to state that
        // it counts and does not conclude, and that a request is not a rule.
        // What changes is that a person reads each of them once.
        if (instructions.isNotEmpty()) {
            item(key = "footnote") {
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["instruction.violations.meaning"],
                    style = HealthTrail.type.bodyS,
                    color = HealthTrail.colors.ink2,
                    modifier = Modifier.testTag(InstructionTags.MEANING),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["instructions.add"],
                onClick = onAdd,
                modifier = Modifier.testTag(InstructionTags.ADD),
            )
        }
    }
}

/**
 * One time an instruction was not followed, in the person's own words.
 *
 * **A spine, because these are dated events in order**, which is what rule 22
 * gives a spine to and what `DESIGN.md` section 14 already specifies for this
 * content. Drawn flat they were five paragraphs indistinguishable from the
 * instruction's own copy above them, with a four month gap between two of them
 * looking exactly like a same day one.
 *
 * **The line is continuous rather than dashed**, per the component's own rule:
 * a dash means a filter over somebody's entries, and these are not a view of
 * anything. They are the times themselves.
 *
 * **A tap opens it**, where the words, the date and the link can be corrected
 * and where it can be taken off the record. Until then this was the one thing a
 * family typed that they had to live with exactly as typed.
 *
 * **What it broke against is said plainly and is not yet a door.** Rule 18 wants
 * it opening the incident or the bill, and the reverse line on those two screens
 * with it. That is the next commit rather than this one, and a line that looks
 * tappable and is not would be worse than a line that does not.
 */
@Composable
private fun ViolationRow(
    violation: Repository.Violation,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.STANDING_INSTRUCTIONS)

    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = hue.ink,
        routeColor = hue.ink,
        dash = null,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // One announcement per time, so a reader can move through
                    // them one at a time. Inside the card they were part of a
                    // single utterance that began with the instruction's name.
                    .semantics(mergeDescendants = true) { }
                    .clip(Radius.card)
                    .openableByTap(
                        label = strings["open.action"],
                        onTap = onOpen,
                        resting = colors.card,
                    )
                    .testTag(InstructionTags.violationRow(violation.id))
                    .padding(Space.cardPadding),
            ) {
                // **Not mono.** `DESIGN.md` 5.1: "Mono never touches a date, a
                // location, a role, or anything with a verb. A date is
                // something a person reads, so it is Atkinson." The table row
                // two lines below that saying "dates as data" contradicts it,
                // and #371 item 3 is the work of moving 21 date sites off mono.
                // D149.
                Text(
                    text = EventDateText.render(strings, violation.occurredEdtf),
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
                violation.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = Bidi.isolate(note),
                        style = HealthTrail.type.bodyM,
                        color = colors.ink,
                    )
                }
                // **What it broke against, where the person said so.** Both of
                // these were columns the schema carried, the reader joined, and
                // no form ever wrote, so this line could not appear at all
                // until the form began to ask. #371.
                violation.incidentTitle?.takeIf { it.isNotBlank() }?.let { title ->
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = strings(
                            "instruction.violations.about.incident",
                            "what" to Bidi.isolate(title),
                        ),
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }
                violation.billDescription?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = strings(
                            "instruction.violations.about.bill",
                            "what" to Bidi.isolate(description),
                        ),
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }
            }
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun InstructionRow(
    /**
     * True on the first instruction carrying this tag, which is the one that
     * spells the tag out. Every other instruction with the same tag shows the
     * label alone, because the label is what differs between them.
     */
    lead: Boolean = false,
    onRecordViolation: () -> Unit,
    instruction: Repository.StandingInstruction,
    tag: TemplateCatalog.InstructionTag?,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            // **A tap opens the request**, where how they answered is recorded
            // and where it can be taken off the list. It used to also carry
            // removal on a long press, which was the only path to it. #218.
            .openableByTap(label = strings["open.action"], onTap = onOpen)
            .testTag(InstructionTags.row(instruction.id))
            .padding(Space.cardPadding),
    ) {
        instruction.givenEdtf?.takeIf { it.isNotBlank() }?.let { given ->
            Text(
                text = strings(
                    "instructions.asked_on",
                    "date" to EventDateText.render(strings, given),
                ),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = Bidi.isolate(instruction.name),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        // **The card's subject, so it carries the card's ink.** It was `ink2`
        // while the times underneath it were `ink`, so the heaviest body text
        // on a request was three repetitions of "Happened again" and the thing
        // actually asked for receded behind them. Rule 15, with the polarity
        // reversed: the inversion was found by two panels reading the same
        // screenshot.
        Spacer(Modifier.height(Space.xs))
        Text(
            text = Bidi.isolate(instruction.wording),
            style = HealthTrail.type.bodyM,
            color = colors.ink,
        )

        // **Always present.** A federal tag is `leaf` toned because it is
        // something the family can stand on; a request is `ink3` because it is
        // not. Neither is red: an unbacked request is not a warning, it is
        // simply a different kind of thing, and coloring it as a problem would
        // be the app editorializing.
        if (tag != null) {
            Spacer(Modifier.height(Space.sm))
            Column(modifier = Modifier.testTag(InstructionTags.tag(instruction.id))) {
                Text(
                    // bidi-ok: a catalog label, in the app's own words rather than the person's.
                    text = tag.label,
                    style = HealthTrail.type.mono,
                    color = if (instruction.tag == "federal") {
                        colors.leafInk
                    } else {
                        colors.ink2
                    },
                )
                // **The explainer only on the one that leads.** It is the same
                // paragraph for every instruction carrying the same tag, so
                // three requests meant the same seventy words three times, and
                // what each instruction actually said was buried between them.
                // The label stays on every row, because that is the part that
                // differs and the part somebody scans for.
                if (lead) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = tag.explainer,
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }
            }
        }

        // **How they answered, always said one way or the other.** A blank
        // here would read as though nobody responded, and being told nothing
        // and not having written it down yet are different things.
        Spacer(Modifier.height(Space.sm))
        if (instruction.isAcknowledged) {
            Text(
                text = strings(
                    "instructions.ack.on",
                    "date" to EventDateText.render(strings, instruction.acknowledgedEdtf),
                ),
                style = HealthTrail.type.mono,
                color = colors.leafInk,
            )
            Spacer(Modifier.height(Space.xs))
        }
        Text(
            text = instruction.acknowledgedHow?.takeIf { it.isNotBlank() }
                ?.let { Bidi.isolate(it) }
                ?: strings["instructions.ack.none"],
            style = HealthTrail.type.bodyM,
            // **A recorded answer is ink and the absence of one is ink2**, and
            // until now both branches of this returned `ink2`, so a conditional
            // that had been written to make exactly that distinction rendered
            // nothing at all. Found by a panel reading the file rather than the
            // screen, which is the only way a dead branch is ever found.
            color = if (instruction.acknowledgedHow.isNullOrBlank()) {
                colors.ink2
            } else {
                colors.ink
            },
        )

        instruction.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.sm))
            Text(text = Bidi.isolate(notes), style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        // **The way to add to the record stays with the request**, and the
        // record itself is below the card. A person with a dozen times written
        // down should not scroll past all of them to reach the control that
        // writes the next one.
        Spacer(Modifier.height(Space.sectionGap))
        TextAction(
            label = strings["instruction.violations.add"],
            onClick = onRecordViolation,
            modifier = Modifier.testTag(InstructionTags.violation(instruction.id)),
        )
    }
}
