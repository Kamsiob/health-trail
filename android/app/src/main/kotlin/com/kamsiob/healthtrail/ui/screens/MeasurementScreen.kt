package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.labeledBlock
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.EdtfSaver
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.FieldBlock
import java.time.LocalDate
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object MeasurementTags {
    const val REMOVE = "measurement_remove"
    const val PICK = "measurement_pick"
    const val FORM = "measurement_form"
    const val VALUE = "measurement_value"
    const val SAVE = "measurement_save"
    const val CANCEL = "measurement_cancel"
    const val RECORD_ONLY = "measurement_record_only"
    fun preset(id: String) = "measurement_preset_$id"
    fun measure(id: String) = "measurement_measure_$id"
    /** The door in the picker. */
    const val OWN = "measurement_own"
    /** The stage it opens, which is a different node and needs its own name. */
    const val OWN_STAGE = "measurement_own_stage"
    const val OWN_NAME = "measurement_own_name"
    const val OWN_UNIT = "measurement_own_unit"
    const val OWN_NUMBER = "measurement_own_number"
    const val OWN_TEXT = "measurement_own_text"
    const val OWN_START = "measurement_own_start"
}

/** What a recorded measurement carries. Every part of it except the measure can be empty. */
/** Something the person named, which no preset covers. #203. */
data class OwnMeasure(val name: String, val unit: String?, val isText: Boolean)

data class MeasurementDraft(
    /** Set when the person picked something this notebook already tracks. */
    val measureId: String?,
    /** Set when they started tracking something new from the catalog. */
    val preset: TemplateCatalog.Preset?,
    /**
     * Set when they named it themselves.
     *
     * **Sixteen presets is a good starting set and it is not the world.** Until
     * #203 they were the only way in, which made the sixteen read as the only
     * sixteen things worth writing down.
     */
    val own: OwnMeasure? = null,
    val unit: String?,
    val number: Double?,
    val text: String,
    val occurred: Edtf.Date,
    val note: String,
)

/**
 * Adding a measurement, which is the fifth of the six capture inputs.
 *
 * **It does not fit the shared form, which is why it has its own screen.** The
 * other four record that something happened and what was said. This one records
 * a value, and a value needs to know what is being measured before anything
 * else on the screen means anything.
 *
 * **Two steps, and the first one is a real question.** Choosing what you are
 * tracking is not a wizard step in front of the real form, it is the first
 * thing anyone recording a measurement has already decided. A notebook that
 * already tracks something skips almost all of it: those come first, as chips,
 * so the common case is one tap.
 *
 * **Nothing here is judged.** No range, no threshold, no normal value, no color
 * by value, no arrow, no comparison to last time. `CLAUDE.md` rule 2 and
 * `DESIGN.md` section 5.8. The screen says plainly that the app writes the
 * number down and does not tell anyone what it means, which is the honest limit
 * stated where the person is rather than buried in a settings page.
 *
 * **A number and words are different things.** Weight is a number. "Ate about
 * half her lunch" is not, and forcing it into one would either lose it or
 * invent a figure nobody gave. The preset says which, and the field follows.
 *
 * Composed from Display L, Body M, Body S, the group header 5.13, the text
 * field 5.9, choice chips 5.11, the pinned action footer 5.15, one filled
 * button, and one text action. Nothing new was introduced.
 */
@Composable
fun MeasurementScreen(
    measures: List<Repository.Measure>,
    presets: List<TemplateCatalog.Preset>,
    onSave: (MeasurementDraft) -> Unit,
    onCancel: () -> Unit,
) {
    var chosenMeasure by remember { mutableStateOf<Repository.Measure?>(null) }
    var chosenPreset by remember { mutableStateOf<TemplateCatalog.Preset?>(null) }
    // **Naming it yourself is a third answer to the first question**, not a
    // second screen: law 3 asks one question at a time and "what are you
    // tracking" is still the question. #203.
    var naming by remember { mutableStateOf(false) }
    var chosenOwn by remember { mutableStateOf<OwnMeasure?>(null) }

    val measure = chosenMeasure
    val preset = chosenPreset
    val own = chosenOwn

    if (naming) {
        NameSomethingElse(
            onStart = { chosenOwn = it; naming = false },
            // Back to the question rather than out of the flow, which is the
            // same choice the value stage already makes.
            onCancel = { naming = false },
        )
    } else if (measure == null && preset == null && own == null) {
        PickWhatToTrack(
            measures = measures,
            presets = presets,
            onPickMeasure = { chosenMeasure = it },
            onPickPreset = { chosenPreset = it },
            onNameYourOwn = { naming = true },
            onCancel = onCancel,
        )
    } else {
        RecordValue(
            name = measure?.name ?: preset?.name ?: own!!.name,
            units = when {
                measure != null -> listOfNotNull(measure.unit)
                preset != null -> preset.unitOptions
                else -> listOfNotNull(own!!.unit)
            },
            isText = measure?.isText ?: preset?.isText ?: own!!.isText,
            onSave = { unit, number, text, occurred, note ->
                onSave(
                    MeasurementDraft(
                        measureId = measure?.id,
                        preset = preset,
                        own = own,
                        unit = unit,
                        number = number,
                        text = text,
                        occurred = occurred,
                        note = note,
                    )
                )
            },
            // Back to the question rather than out of the flow, since getting
            // here means the person already decided to record something. The
            // question's own cancel is the way out, one tap further.
            onCancel = { chosenMeasure = null; chosenPreset = null; chosenOwn = null },
        )
    }
}

/**
 * The first question: what are you tracking.
 *
 * Things this notebook already tracks come first and as chips, because after
 * the first week that is the whole answer and it should cost one tap. The
 * sixteen presets sit below under their own heading, as short rows.
 */
/**
 * What to start tracking. Rewritten onto `Page`, #388 and #392.
 *
 * **It was the one form in the app with no top bar.** A full screen `Surface`
 * around a `Column` around a `LazyColumn`, so it had no back arrow, no section
 * eyebrow, no band, and its only way out was a Cancel below sixteen presets.
 * Every other form in the app is a `Page` and this one looked like a different
 * app. Seen by walking every form rather than by reading the file.
 *
 * **The presets are rows that say they are doors.** They were bare tonal cards
 * with a name and a cadence and nothing else: tappable, and nothing on them
 * said so. `ListRow` is the row the whole app already uses, so they carry the
 * section's mark in the section's hue and end in the mark law 2 gives anything
 * that opens something. `docs/V4.md` 6.1 item 11: a screen that could belong to
 * any app has failed.
 */
@Composable
private fun PickWhatToTrack(
    measures: List<Repository.Measure>,
    presets: List<TemplateCatalog.Preset>,
    onPickMeasure: (Repository.Measure) -> Unit,
    onPickPreset: (TemplateCatalog.Preset) -> Unit,
    onNameYourOwn: () -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    // A preset already being tracked is not offered twice.
    val tracked = measures.mapNotNull { it.presetId }.toSet()
    val hue = hueFor(Repository.Section.PROGRESS)

    Page(
        title = strings["measurement.pick.title"],
        subtitle = strings["measurement.pick.lead"],
        eyebrow = strings["notebook.section.progress"],
        section = Repository.Section.PROGRESS,
        onBack = onCancel,
        backLabel = strings["common.cancel"],
        modifier = Modifier.testTag(MeasurementTags.PICK),
        // **The way out is where every other form puts it.** It was a tonal
        // pill at the foot of the list, which meant scrolling past sixteen
        // presets to leave.
        band = {
            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(MeasurementTags.CANCEL),
            )
        },
    ) {
        if (measures.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
                    Eyebrow(text = strings["measurement.tracked"])
                    ChoiceChipGroup(label = "") {
                        measures.forEach { measure ->
                            ChoiceChip(
                                label = Bidi.isolate(measure.name),
                                selected = false,
                                onClick = { onPickMeasure(measure) },
                                modifier = Modifier.testTag(
                                    MeasurementTags.measure(measure.id)
                                ),
                            )
                        }
                    }
                }
            }
        }

        val offered = presets.filter { it.id !in tracked }
        labeledBlock(
            label = strings["measurement.presets"],
            rows = offered.map { preset ->
                {
                    ListRow(
                        // bidi-ok: a catalog label, in the app's own words
                        // rather than the person's.
                        title = preset.name,
                        support = preset.cadence.takeIf { it.isNotBlank() },
                        mark = Symbols.of(Repository.Section.PROGRESS),
                        markHue = hue,
                        isDoor = true,
                        onClick = { onPickPreset(preset) },
                        clickLabel = strings["open.action"],
                        modifier = Modifier.testTag(MeasurementTags.preset(preset.id)),
                    )
                }
            },
        )

        // **Under the catalog rather than beside it**, because after the first
        // week the answer is usually one of the chips above and this is the
        // rarer errand. It is not hidden: 13.5 calls a capability only its
        // author can find unfinished, and sixteen presets with no way past them
        // was exactly that.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
                Eyebrow(text = strings["measurement.own"])
                Action(
                    label = strings["measurement.own.action"],
                    onClick = onNameYourOwn,
                    modifier = Modifier.testTag(MeasurementTags.OWN),
                )
            }
        }
    }
}

/**
 * Naming something the catalog never heard of. #203.
 *
 * **One question at a time**, per law 3: what it is called, whether it is a
 * number or words, and a unit only when a number could have one. Nothing here
 * is required except the name, because without a name there is nothing to put
 * the value against.
 *
 * **A number and words are different things and both are kept.** That is the
 * schema's own distinction, not a flag on one column, and this is where the
 * person decides which they are writing down. "Ate about half her lunch" is
 * not a number and storing it as one would either lose it or invent a figure
 * nobody gave.
 *
 * **The app makes no claim about anything named here.** `advice_risk` is low
 * and the style is the plain one, which is what lets the rendering layer hold
 * the content rules: no range, no threshold, no color by value, ever.
 */
/**
 * Correcting what a measure is called and the unit it is kept in. #374, the
 * last of the six.
 *
 * **A measure's name is on every reading of it**, on its card on Today, and on
 * the chart's own heading, so a name typed wrong at setup is typed wrong in
 * four places forever. Its unit is worse: "lb" where the scale says "kg" makes
 * every number under it mean the wrong thing.
 *
 * **The same form that named it**, for the reason `CorrectReadingScreen` uses
 * the form that recorded the reading: two forms for one record is how two forms
 * drift apart.
 *
 * **The kind is shown and cannot be changed.** A continuous measure with
 * readings in it does not become an observational one because somebody tapped
 * a chip: every number already written down would have nowhere to live. Rule 3
 * puts that beyond this screen, and offering the choice would be the app
 * pretending it is reversible.
 */
@Composable
fun CorrectMeasureScreen(
    measure: Repository.Measure,
    onSave: (OwnMeasure) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** Anything added can be removed, 2026-08-16. Its readings go with it. */
    onRemove: (() -> Unit)? = null,
) {
    NameSomethingElse(
        onStart = onSave,
        onCancel = onCancel,
        onRemove = onRemove,
        modifier = modifier,
        startName = measure.name,
        startUnit = measure.unit.orEmpty(),
        startIsText = measure.isText,
        kindIsFixed = true,
        titleKey = "measurement.own.correct",
        saveKey = "measurement.own.correct.save",
    )
}

@Composable
private fun NameSomethingElse(
    onStart: (OwnMeasure) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the fields start with, for a correction. Empty when naming. */
    startName: String = "",
    startUnit: String = "",
    startIsText: Boolean = false,
    /**
     * Whether the kind is shown as a fact rather than offered as a choice.
     *
     * **True when correcting**, because a measure with readings in it cannot
     * change kind without every number already written down having nowhere to
     * live.
     */
    kindIsFixed: Boolean = false,
    titleKey: String? = null,
    saveKey: String? = null,
    /** Null on a pure add; a correction passes the way out. 2026-08-16. */
    onRemove: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by rememberSaveable(startName) { mutableStateOf(startName) }
    var unit by rememberSaveable(startUnit) { mutableStateOf(startUnit) }
    var isText by rememberSaveable(startIsText) { mutableStateOf(startIsText) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(MeasurementTags.OWN_STAGE),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["measurement.own.name"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["measurement.own.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))
                // **The field's label is not the heading again.** The heading
                // asks what you are tracking; the field says what to type. The
                // same words in both slots is the defect #341 spent a night
                // taking out of four other screens, and it went in here while
                // that was being written.
                // **One container rather than four loose controls.** D174.
                // A name, a kind, a unit and their gaps read as four
                // unrelated things stacked on paper; they are one
                // decision about one measure and now they look like it.
                FieldBlock(label = strings["measurement.own.group"]) {
                    DictatableField(
                        label = strings["measurement.own.name.field"],
                        value = name,
                        onValueChange = { name = it },
                        fieldTestTag = MeasurementTags.OWN_NAME,
                        support = strings["measurement.own.name.hint"],
                    )

                    Spacer(Modifier.height(Space.m))
                    if (kindIsFixed) {
                        // **Said, not offered.** A measure with readings in it
                        // cannot change kind: every number already written down
                        // would have nowhere to live. A chip that looks tappable
                        // and is not would be worse than a sentence, per rule 16,
                        // so this is a sentence.
                        Text(
                            text = strings["measurement.own.kind"],
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = strings[
                                if (isText) {
                                    "measurement.own.kind.text"
                                } else {
                                    "measurement.own.kind.number"
                                },
                            ],
                            style = HealthTrail.type.bodyM,
                            color = colors.ink,
                        )
                    } else {
                        ChoiceChipGroup(label = strings["measurement.own.kind"]) {
                            ChoiceChip(
                                label = strings["measurement.own.kind.number"],
                                selected = !isText,
                                onClick = { isText = false },
                                modifier = Modifier.testTag(MeasurementTags.OWN_NUMBER),
                            )
                            ChoiceChip(
                                label = strings["measurement.own.kind.text"],
                                selected = isText,
                                onClick = { isText = true },
                                modifier = Modifier.testTag(MeasurementTags.OWN_TEXT),
                            )
                        }
                    }

                    // **The unit only where a unit could mean anything.** Words
                    // have no units, and a field asking for one under "how the
                    // wound looks" is the app not listening to the answer it just
                    // got.
                    if (!isText) {
                        Spacer(Modifier.height(Space.m))
                        DictatableField(
                            label = strings["measurement.unit"],
                            value = unit,
                            onValueChange = { unit = it },
                            fieldTestTag = MeasurementTags.OWN_UNIT,
                            support = strings["measurement.own.unit.hint"],
                        )
                    }

                }
                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            Action(
                label = strings["measurement.own.start"],
                onClick = {
                    onStart(
                        OwnMeasure(
                            name = name.trim(),
                            unit = unit.trim().takeIf { it.isNotBlank() && !isText },
                            isText = isText,
                        ),
                    )
                },
                // **A name is the one thing this cannot do without**, because
                // there is nothing to put the value against otherwise. Rule 13
                // holds everywhere else on this screen: the unit is optional
                // and nothing else is asked at all.
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(MeasurementTags.OWN_START), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.sm))

            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal),
            )
            onRemove?.let { takeOut ->
                Spacer(Modifier.height(Space.cardGap))
                Action(
                    label = strings["remove.action"],
                    onClick = takeOut,
                    modifier = Modifier.testTag(MeasurementTags.REMOVE),
                )
            }

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * One thing that can be tracked.
 *
 * Carries the name and the cadence line, which says how often families
 * typically record it. **It is guidance about the shape of the thing, never a
 * schedule the app holds anyone to**, and nothing on this screen ever reminds
 * or alerts.
 */
@Composable
private fun PresetRow(preset: TemplateCatalog.Preset, onClick: () -> Unit) {
    val colors = HealthTrail.colors

    // **Material's surface owns the container, the press and the focus ring.**
    // #392, and the tonal color replaces `card`. docs/V4.md 2.1.
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .testTag(MeasurementTags.preset(preset.id)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Space.cardPadding),
    ) {
        // bidi-ok: a catalog label, in the app's own words rather than the person's.
        Text(text = preset.name, style = HealthTrail.type.displayS, color = colors.ink)
        if (preset.cadence.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            // bidi-ok: a catalog label, in the app's own words rather than the person's.
            Text(text = preset.cadence, style = HealthTrail.type.bodyS, color = colors.ink2)
        }
    }
    }
}

/** The second step: the value, when, and anything worth remembering. */
/**
 * Correcting a reading that is already written down. #374.
 *
 * **A reading is typed one handed while holding something else**, which is how
 * 138.8 becomes 1388, and until now it could not be fixed. Rule 17 says a date
 * is editable forever from the entry itself, and a reading's date is one of
 * them.
 *
 * **The same form that recorded it**, rather than a second one that asks the
 * same four questions in a slightly different order. Two forms for one record
 * is how two forms drift apart, which is the argument `SectionScaffold` and
 * `AddThreadScreen` both already make.
 */
@Composable
fun CorrectReadingScreen(
    name: String,
    units: List<String>,
    isText: Boolean,
    reading: Repository.Reading,
    onSave: (unit: String?, number: Double?, text: String, occurred: Edtf.Date, note: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** Anything added can be removed, 2026-08-16: a reading typed twice. */
    onRemove: (() -> Unit)? = null,
) {
    RecordValue(
        onRemove = onRemove,
        name = name,
        units = units,
        isText = isText,
        onSave = onSave,
        onCancel = onCancel,
        modifier = modifier,
        // **Started from what is there**, per D147: somebody who opened this to
        // fix a digit should not retype the reading, the unit, the date and the
        // note.
        startValue = reading.number?.let { number ->
            if (number == number.toLong().toDouble()) {
                number.toLong().toString()
            } else {
                number.toString()
            }
        } ?: reading.text.orEmpty(),
        startUnit = reading.unit,
        startOccurred = reading.occurredEdtf?.let { Edtf.parse(it) },
        startNote = reading.note.orEmpty(),
        saveKey = "measurement.correct.save",
        leadKey = "measurement.correct.lead",
    )
}

@Composable
private fun RecordValue(
    name: String,
    units: List<String>,
    isText: Boolean,
    onSave: (unit: String?, number: Double?, text: String, occurred: Edtf.Date, note: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the field starts with, for a correction. Empty when recording. */
    startValue: String = "",
    startUnit: String? = null,
    startOccurred: Edtf.Date? = null,
    startNote: String = "",
    /** The save button's words, so a correction does not say "record". */
    saveKey: String? = null,
    /** The lead line, so a correction does not invite somebody to skip. */
    leadKey: String? = null,
    /** Null on a pure add; a correction passes the way out. 2026-08-16. */
    onRemove: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **A reading taken at a bedside survives the process.** #371 item 7: a
    // number read off a cuff, a unit, a rough when and a note all lived in a
    // plain remember, so an interruption emptied the form and the reading was
    // gone with it.
    var raw by rememberSaveable(startValue) { mutableStateOf(startValue) }
    var unit by rememberSaveable(startUnit) {
        mutableStateOf(startUnit ?: units.firstOrNull())
    }
    // **No rough when on a correction.** "Today" is the right default for a
    // reading being taken now and the wrong one for a reading from March: the
    // date it already has is the answer, and offering a rough one would invite
    // somebody to overwrite a fact with an approximation.
    var rough by rememberSaveable(startOccurred) {
        mutableStateOf(if (startOccurred == null) RoughWhen.TODAY else null)
    }
    var picked by rememberSaveable(startOccurred, stateSaver = EdtfSaver) {
        mutableStateOf(startOccurred)
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var note by rememberSaveable(startNote) { mutableStateOf(startNote) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag(MeasurementTags.FORM)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(text = Bidi.isolate(name), style = HealthTrail.type.displayL, color = colors.ink)
                Spacer(Modifier.height(Space.s))
                Text(
                    // **A correction is not being invited to skip anything.**
                    // "Answer what you can. Skip the rest" is right for
                    // somebody recording at a bedside and wrong for somebody
                    // who opened this to move a decimal point: they are fixing
                    // one thing, not deciding how much to write.
                    text = strings[leadKey ?: "capture.sub"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                DictatableField(
                    label = strings[
                        if (isText) "measurement.value.text" else "measurement.value.number"
                    ],
                    value = raw,
                    onValueChange = { raw = it },
                    // A number pad for a number and a keyboard for words. The
                    // field still accepts whatever arrives: there is no error
                    // state here, per 5.9, and a value the app cannot read as a
                    // number is kept as what the person typed.
                    keyboardType = if (isText) KeyboardType.Text else KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    fieldTestTag = MeasurementTags.VALUE,
                    support = strings[
                        if (isText) "measurement.value.text.hint"
                        else "measurement.value.number.hint"
                    ],
                )

                // One unit is not a choice, so it is not offered as one.
                if (units.size > 1) {
                    Spacer(Modifier.height(Space.sectionGap))
                    ChoiceChipGroup(label = strings["measurement.unit"]) {
                        units.forEach { option ->
                            ChoiceChip(
                                label = option,
                                selected = unit == option,
                                onClick = { unit = option },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.sectionGap))

                ChoiceChipGroup(
                    label = strings["capture.when"],
                    aside = strings["capture.when.hint"],
                ) {
                    RoughWhen.entries.forEach { option ->
                        ChoiceChip(
                            label = strings[option.labelKey],
                            selected = picked == null && rough == option,
                            onClick = { rough = option; picked = null },
                        )
                    }
                    // The same peer the capture form offers, in the same words
                    // and the same place. A reading remembered from last month
                    // is as normal here as a call was there, and two screens
                    // asking the same question two ways is the defect section
                    // 10.2 names.
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = picked != null,
                        onClick = { pickerOpen = true },
                    )
                }

                picked?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }

                Spacer(Modifier.height(Space.sectionGap))

                DictatableField(
                    label = strings["measurement.note"],
                    support = strings["measurement.note.hint"],
                    value = note,
                    onValueChange = { note = it },
                    imeAction = ImeAction.Done,
                    singleLine = false,
                )

                Spacer(Modifier.height(Space.m))

                // The honest limit, said where the person is. The medications
                // screen carries the same sentence for the same reason.
                Text(
                    text = strings["measurement.record_only"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.testTag(MeasurementTags.RECORD_ONLY),
                )

                Spacer(Modifier.height(Space.l))
            }

            Spacer(Modifier.height(Space.m))

            Action(
                label = strings[saveKey ?: "capture.save"],
                onClick = {
                    onSave(
                        unit,
                        if (isText) null else raw.trim().replace(',', '.').toDoubleOrNull(),
                        if (isText) raw.trim() else "",
                        picked ?: rough?.edtf(LocalDate.now()) ?: Edtf.unknown(),
                        note.trim(),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(MeasurementTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            // **Sized to its label, not the width of the screen.** D137: a
            // full width outlined bar is the way back and nothing else, and
            // under a full width filled action it is a second bar of which
            // only one leaves. #371 item 5, and it is retroactive per rule 14.
            Action(
                label = strings["common.back"],
                onClick = onCancel,
                modifier = Modifier.padding(horizontal = Space.screenHorizontal),
            )
            onRemove?.let { takeOut ->
                Spacer(Modifier.height(Space.cardGap))
                Action(
                    label = strings["remove.action"],
                    onClick = takeOut,
                    modifier = Modifier.testTag(MeasurementTags.REMOVE),
                )
            }

            Spacer(Modifier.height(Space.l))
        }
    }

    if (pickerOpen) {
        DatePickerSheet(
            initial = picked ?: rough?.edtf(LocalDate.now()),
            onPick = { chosen ->
                pickerOpen = false
                if (chosen.precision == Edtf.Precision.UNKNOWN) {
                    picked = null
                    rough = RoughWhen.NOT_SURE
                } else {
                    picked = chosen
                    rough = null
                }
            },
            onDismiss = { pickerOpen = false },
        )
    }
}
