package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object CaptureFormTags {
    const val ROOT = "capture_form_root"
    const val WHO = "capture_form_who"
    const val NOTE = "capture_form_note"
    const val SAVE = "capture_form_save"
    const val CANCEL = "capture_form_cancel"
}

/** What a captured entry carries. Both fields can be blank. */
data class CaptureDraft(val kind: CaptureKind, val who: String, val note: String)

/**
 * One form for four of the six capture inputs: a call, a visit, an incident, and
 * a question.
 *
 * **One form rather than four, deliberately.** All four record who and what, and
 * building them separately would put the same pattern on screen in four slightly
 * different shapes. `DESIGN.md` section 10 calls that a defect and says to fix
 * the earlier one rather than leave both, so the shape is defined once and the
 * words come from the catalog per kind. A person who has logged a call has
 * learned how to log a visit.
 *
 * Measurement and document are genuinely different shapes, a value with a unit
 * and a photograph, and they get their own screens rather than being forced
 * through this one.
 *
 * **Every field is optional and saving with both blank is allowed.** A person
 * who hangs up and taps this has already done the useful thing, which is
 * recording that something happened and when. What was said can be added later
 * or never. Partial is a finished state.
 *
 * The note grows with what is typed rather than sitting at a fixed height,
 * because a fixed height silently teaches people to write less.
 *
 * Composed from Display L, the text field from section 5.9, one filled button,
 * and one text action. Nothing new was introduced.
 */
@Composable
fun CaptureFormScreen(
    kind: CaptureKind,
    onSave: (CaptureDraft) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var who by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(CaptureFormTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = strings[key(kind, "title")],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["entry.optional"],
                    style = HealthTrail.type.mono,
                    color = colors.ink3Text,
                )

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings[key(kind, "who")],
                    hint = strings[key(kind, "who.hint")],
                    value = who,
                    onValueChange = { who = it },
                    fieldTestTag = CaptureFormTags.WHO,
                )

                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings[key(kind, "note")],
                    hint = strings[key(kind, "note.hint")],
                    value = note,
                    onValueChange = { note = it },
                    singleLine = false,
                    imeAction = ImeAction.Default,
                    fieldTestTag = CaptureFormTags.NOTE,
                )

                Spacer(Modifier.height(Space.l))
            }

            FilledButton(
                label = strings["common.save"],
                onClick = { onSave(CaptureDraft(kind, who.trim(), note.trim())) },
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.SAVE),
            )

            Spacer(Modifier.height(Space.sm))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.CANCEL),
            )
        }
    }
}

/**
 * Whether this kind is one of the four the shared form serves.
 *
 * **Declared in one place, and exhaustively.** Adding a seventh capture kind
 * will not compile until it answers this question, which is the whole point: a
 * kind that quietly defaulted to using the form would render raw catalog keys
 * on screen, and a kind that quietly defaulted to not using it would open
 * nothing at all when tapped. Both are silent, so neither is left to a default.
 *
 * Measurement and document say no. A measurement carries a value and a unit and
 * a document carries a photograph, and neither fits a form made of two free text
 * fields.
 */
val CaptureKind.usesTheSharedForm: Boolean
    get() = when (this) {
        CaptureKind.CALL, CaptureKind.VISIT, CaptureKind.INCIDENT, CaptureKind.QUESTION -> true
        CaptureKind.MEASUREMENT, CaptureKind.DOCUMENT -> false
    }

/**
 * The catalog key for one kind and one slot.
 *
 * Built rather than looked up in a table, so adding a kind means adding its
 * strings to the catalog and nothing else. `check_i18n.py` fails the build if
 * any locale is missing one, which is what makes building a key safe.
 */
private fun key(kind: CaptureKind, slot: String): String =
    "capture.${kind.name.lowercase()}.$slot"

/**
 * The row kind stored on the entry, which is what the trail and every filter
 * read. Kept next to the form so a new capture kind cannot be added without
 * deciding what it is called in the schema.
 */
fun CaptureKind.entryKind(): String = when (this) {
    CaptureKind.CALL -> "call"
    CaptureKind.VISIT -> "visit"
    CaptureKind.INCIDENT -> "incident"
    CaptureKind.MEASUREMENT -> "measurement"
    CaptureKind.QUESTION -> "question"
    CaptureKind.DOCUMENT -> "document"
}
