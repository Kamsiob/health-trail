package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import java.time.LocalDate
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MeasurementTags {
    const val PICK = "measurement_pick"
    const val FORM = "measurement_form"
    const val VALUE = "measurement_value"
    const val SAVE = "measurement_save"
    const val CANCEL = "measurement_cancel"
    const val RECORD_ONLY = "measurement_record_only"
    fun preset(id: String) = "measurement_preset_$id"
    fun measure(id: String) = "measurement_measure_$id"
}

/** What a recorded measurement carries. Every part of it except the measure can be empty. */
data class MeasurementDraft(
    /** Set when the person picked something this notebook already tracks. */
    val measureId: String?,
    /** Set when they started tracking something new. */
    val preset: TemplateCatalog.Preset?,
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

    val measure = chosenMeasure
    val preset = chosenPreset

    if (measure == null && preset == null) {
        PickWhatToTrack(
            measures = measures,
            presets = presets,
            onPickMeasure = { chosenMeasure = it },
            onPickPreset = { chosenPreset = it },
            onCancel = onCancel,
        )
    } else {
        RecordValue(
            name = measure?.name ?: preset!!.name,
            units = when {
                measure != null -> listOfNotNull(measure.unit)
                else -> preset!!.unitOptions
            },
            isText = measure?.isText ?: preset!!.isText,
            onSave = { unit, number, text, occurred, note ->
                onSave(
                    MeasurementDraft(
                        measureId = measure?.id,
                        preset = preset,
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
            onCancel = { chosenMeasure = null; chosenPreset = null },
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
@Composable
private fun PickWhatToTrack(
    measures: List<Repository.Measure>,
    presets: List<TemplateCatalog.Preset>,
    onPickMeasure: (Repository.Measure) -> Unit,
    onPickPreset: (TemplateCatalog.Preset) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    // A preset already being tracked is not offered twice.
    val tracked = measures.mapNotNull { it.presetId }.toSet()

    Surface(modifier = Modifier.fillMaxSize(), color = colors.paper) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(MeasurementTags.PICK)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item {
                    Spacer(Modifier.height(Space.l))
                    Text(
                        text = strings["measurement.pick.title"],
                        style = HealthTrail.type.displayL,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = strings["measurement.pick.lead"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                }

                if (measures.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(Space.sectionGap))
                        GroupHeader("measurement.tracked")
                        Spacer(Modifier.height(Space.headerGap))
                        ChoiceChipGroup(label = "") {
                            measures.forEach { measure ->
                                ChoiceChip(
                                    label = measure.name,
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

                item {
                    Spacer(Modifier.height(Space.sectionGap))
                    GroupHeader("measurement.presets")
                    Spacer(Modifier.height(Space.headerGap))
                }

                presets.filter { it.id !in tracked }.forEach { preset ->
                    item(key = preset.id) {
                        PresetRow(preset = preset, onClick = { onPickPreset(preset) })
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }

                item { Spacer(Modifier.height(Space.s)) }
            }

            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(MeasurementTags.CANCEL),
            )

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
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
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
                onClick = onClick,
            )
            .testTag(MeasurementTags.preset(preset.id))
            .padding(Space.cardPadding),
    ) {
        Text(text = preset.name, style = HealthTrail.type.displayS, color = colors.ink)
        if (preset.cadence.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            Text(text = preset.cadence, style = HealthTrail.type.bodyS, color = colors.ink2)
        }
    }
}

/** The second step: the value, when, and anything worth remembering. */
@Composable
private fun RecordValue(
    name: String,
    units: List<String>,
    isText: Boolean,
    onSave: (unit: String?, number: Double?, text: String, occurred: Edtf.Date, note: String) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var raw by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(units.firstOrNull()) }
    var rough by remember { mutableStateOf<RoughWhen?>(RoughWhen.TODAY) }
    var picked by remember { mutableStateOf<Edtf.Date?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.paper) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag(MeasurementTags.FORM)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(text = name, style = HealthTrail.type.displayL, color = colors.ink)
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["capture.sub"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings[
                        if (isText) "measurement.value.text" else "measurement.value.number"
                    ],
                    hint = strings[
                        if (isText) "measurement.value.text.hint"
                        else "measurement.value.number.hint"
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
                    hint = strings["measurement.note.hint"],
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

            FilledButton(
                label = strings["capture.save"],
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
                    .testTag(MeasurementTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.back"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal),
            )

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
