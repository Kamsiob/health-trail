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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
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
    const val UNFILED_NOTE = "capture_form_unfiled_note"
    const val THREAD_UNSURE = "capture_thread_unsure"
    fun whenChip(rough: RoughWhen) = "capture_when_${rough.name.lowercase()}"
    fun threadChip(id: String) = "capture_thread_$id"
}

/**
 * How roughly a person remembers when something happened.
 *
 * **Rough is the default, not the fallback.** Someone writing at 11pm about a
 * call three days ago does not know the time, and a date picker gets either a
 * guess recorded as fact or nothing recorded at all. Four answers, and one of
 * them is not knowing.
 */
enum class RoughWhen { TODAY, YESTERDAY, THIS_WEEK, NOT_SURE }

/** What a captured entry carries. Every part of it can be empty. */
data class CaptureDraft(
    val kind: CaptureKind,
    val who: String,
    val note: String,
    val rough: RoughWhen,
    /** Null means the person did not say, which sends the entry to the Unfiled tray. */
    val threadId: String?,
)

/**
 * One form for four of the six capture inputs: a call, a visit, an incident, and
 * a question.
 *
 * **One form rather than four, deliberately.** All four record who, when, what
 * it is part of, and what happened. Building them separately would put the same
 * pattern on screen in four slightly different shapes, which `DESIGN.md` section
 * 10.2 calls a defect. The shape is defined once and the words come from the
 * catalog per kind, so a person who has logged a call has learned how to log a
 * visit.
 *
 * Measurement and document are genuinely different shapes, a value with a unit
 * and a photograph, and they get their own screens rather than being forced
 * through this one.
 *
 * **This is screen 26 of the reference file, "capture that forgives".** An
 * earlier version was two single line text fields. It worked, and it was not
 * what was designed: rough date chips, thread chips, an open note area, and a
 * save action that accepts whatever is there. Recorded as D30.
 *
 * **Every field is optional and saving with all of them untouched is allowed.**
 * A person who hangs up and taps this has already done the useful thing, which
 * is recording that something happened and roughly when. Partial is a finished
 * state.
 *
 * **The thread question defaults to not knowing**, which sends the entry to the
 * Unfiled tray. That is the honest default for someone who just tapped save, and
 * the note underneath says where it is going while they can still change it.
 *
 * Composed from Display L, Body M, Body S, the text field from section 5.9, the
 * choice chip from section 5.11, one filled button, and one text action.
 */
@Composable
fun CaptureFormScreen(
    kind: CaptureKind,
    threads: List<Repository.CareThread>,
    onSave: (CaptureDraft) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var who by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rough by remember { mutableStateOf(RoughWhen.TODAY) }
    // Null means "not sure yet", which is a real answer rather than a blank.
    var threadId by remember { mutableStateOf<String?>(null) }

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
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(Modifier.height(Space.s))

                // Says once, in a full sentence, what the old screen said with
                // the word Optional in a mono eyebrow. Section 5.9 asks for it
                // once per screen rather than beside every field, and a sentence
                // does more of the work than a label does.
                Text(
                    text = strings["capture.sub"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings[key(kind, "who")],
                    value = who,
                    onValueChange = { who = it },
                    hint = strings[key(kind, "who.hint")],
                    fieldTestTag = CaptureFormTags.WHO,
                )

                Spacer(Modifier.height(Space.sectionGap))

                ChoiceChipGroup(
                    label = strings["capture.when"],
                    aside = strings["capture.when.hint"],
                ) {
                    RoughWhen.entries.forEach { option ->
                        ChoiceChip(
                            label = strings[option.labelKey],
                            selected = rough == option,
                            onClick = { rough = option },
                            modifier = Modifier.testTag(CaptureFormTags.whenChip(option)),
                        )
                    }
                }

                // Only asked where there is something to answer with. A notebook
                // with no situation template has no threads, and a question with
                // one possible answer is not a question.
                if (threads.isNotEmpty()) {
                    Spacer(Modifier.height(Space.sectionGap))

                    ChoiceChipGroup(
                        label = strings["capture.thread"],
                        aside = strings["capture.thread.hint"],
                    ) {
                        threads.forEach { thread ->
                            ChoiceChip(
                                label = thread.label,
                                selected = threadId == thread.id,
                                onClick = { threadId = thread.id },
                                dotColor = colors.threadRoutes[
                                    thread.colorIndex.mod(colors.threadRoutes.size),
                                ],
                                modifier = Modifier
                                    .testTag(CaptureFormTags.threadChip(thread.id)),
                            )
                        }
                        ChoiceChip(
                            label = strings["capture.thread.not_sure"],
                            selected = threadId == null,
                            onClick = { threadId = null },
                            modifier = Modifier.testTag(CaptureFormTags.THREAD_UNSURE),
                        )
                    }
                }

                Spacer(Modifier.height(Space.sectionGap))

                HealthTrailTextField(
                    label = strings[key(kind, "note")],
                    value = note,
                    onValueChange = { note = it },
                    hint = strings[key(kind, "note.hint")],
                    fieldTestTag = CaptureFormTags.NOTE,
                    // Grows with what is typed rather than sitting at a fixed
                    // height, because a fixed height silently teaches people to
                    // write less.
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )

                // Said on the screen rather than discovered afterward. An entry
                // that quietly went somewhere the person did not choose is the
                // thing this app promises never to do, so the screen says where
                // it is going while they can still change it.
                if (threads.isNotEmpty() && threadId == null) {
                    Spacer(Modifier.height(Space.m))
                    Text(
                        text = strings["capture.unfiled.note"],
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                        modifier = Modifier.testTag(CaptureFormTags.UNFILED_NOTE),
                    )
                }

                Spacer(Modifier.height(Space.l))
            }

            FilledButton(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        CaptureDraft(
                            kind = kind,
                            who = who.trim(),
                            note = note.trim(),
                            rough = rough,
                            threadId = threadId,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.CANCEL),
            )
        }
    }
}

/** The catalog key for a rough date answer. */
private val RoughWhen.labelKey: String
    get() = when (this) {
        RoughWhen.TODAY -> "capture.when.today"
        RoughWhen.YESTERDAY -> "capture.when.yesterday"
        RoughWhen.THIS_WEEK -> "capture.when.this_week"
        RoughWhen.NOT_SURE -> "capture.when.not_sure"
    }

/**
 * A rough answer turned into the date the schema actually stores.
 *
 * **The chip says exactly as much as the person did, and no more.** "Today" is
 * a day, so it stores a day and not the minute they happened to tap the button.
 * "Sometime this week" is a week, so it stores the week as the interval a week
 * is. **"Not sure" stores unknown**, rather than today's date with a shrug
 * attached, and every screen downstream renders it as not known because that is
 * what it says.
 *
 * Contract section 3.1 and `Edtf`.
 */
fun RoughWhen.edtf(today: LocalDate): Edtf.Date = when (this) {
    RoughWhen.TODAY -> Edtf.day(today)
    RoughWhen.YESTERDAY -> Edtf.day(today.minusDays(1))
    RoughWhen.THIS_WEEK -> Edtf.week(today)
    RoughWhen.NOT_SURE -> Edtf.unknown()
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
 * a document carries a photograph, and neither fits a form made of free text and
 * chips.
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
 * any locale is missing one, and `CaptureTest` fails if a kind the form serves
 * has no words, which is what makes building a key safe.
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
