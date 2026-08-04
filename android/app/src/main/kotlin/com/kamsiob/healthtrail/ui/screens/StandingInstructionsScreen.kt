package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
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
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object InstructionTags {
    fun violation(id: String) = "instruction_violation_$id"
    const val NAME = "standing_instructions"
    const val ADD = "standing_instructions_add"
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
    onRemove: (Repository.StandingInstruction) -> Unit,
    onAcknowledge: (Repository.StandingInstruction) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * How many times each instruction was not followed.
     *
     * **A count and never a judgment**, per rule 2 and `MASTER_SPEC.md` 4.11.
     * Nothing here says a facility is bad, nothing is colored by how many, and
     * no threshold turns a number into an opinion. What it means is the
     * person's to judge, and the row says so in those words.
     */
    violations: Map<String, Int> = emptyMap(),
    onRecordViolation: (Repository.StandingInstruction) -> Unit = {},
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = InstructionTags.NAME,
        title = strings["notebook.section.standing_instructions"],
        subtitle = strings["instructions.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.STANDING_INSTRUCTIONS,
    ) {
        if (instructions.isEmpty()) {
            item {
                SectionEmpty(name = InstructionTags.NAME, text = strings["instructions.empty"], section = Repository.Section.STANDING_INSTRUCTIONS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        for (instruction in instructions) {
            item(key = instruction.id) {
                InstructionRow(
                    violationCount = violations[instruction.id] ?: 0,
                    onRecordViolation = { onRecordViolation(instruction) },
                    instruction = instruction,
                    tag = tags[instruction.tag],
                    onRemove = { onRemove(instruction) },
                    onAcknowledge = { onAcknowledge(instruction) },
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["instructions.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(InstructionTags.ADD),
            )
        }
    }
}

@Composable
private fun InstructionRow(
    violationCount: Int,
    onRecordViolation: () -> Unit,
    instruction: Repository.StandingInstruction,
    tag: TemplateCatalog.InstructionTag?,
    onRemove: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            // A tap records how they answered, which is the half that gets
            // pointed back to later.
            .removableByLongPress(strings["edit.hint"], onRemove, onAcknowledge)
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
            text = instruction.name,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        Spacer(Modifier.height(Space.xs))
        Text(
            text = instruction.wording,
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
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
                    text = tag.label,
                    style = HealthTrail.type.mono,
                    color = if (instruction.tag == "federal") {
                        colors.leafInk
                    } else {
                        colors.ink2
                    },
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = tag.explainer,
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
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
                ?: strings["instructions.ack.none"],
            style = HealthTrail.type.bodyM,
            color = if (instruction.acknowledgedHow.isNullOrBlank()) {
                colors.ink2
            } else {
                colors.ink2
            },
        )

        instruction.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.sm))
            Text(text = notes, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        // **The count, and immediately after it the sentence that says what it
        // is not.** `MASTER_SPEC.md` 4.11 requires that line every time a count
        // like this is shown, and the two are one thought: a bare number here
        // would be the app implying a conclusion it is not entitled to.
        //
        // Zero says nothing at all rather than "0 times", because a count of
        // nothing is not a finding and printing it would turn every instruction
        // into a scoreboard with most of the scores at zero.
        if (violationCount > 0) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = strings("instruction.violations.count", "count" to violationCount),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings["instruction.violations.meaning"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }

        Spacer(Modifier.height(Space.sm))
        TextAction(
            label = strings["instruction.violations.add"],
            onClick = onRecordViolation,
            modifier = Modifier.testTag(InstructionTags.violation(instruction.id)),
        )
    }
}
