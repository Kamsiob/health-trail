package com.kamsiob.healthtrail.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Speaking into a field instead of typing into it.
 *
 * **For somebody in a hallway who wants to record what the nurse just said,
 * this is the difference between a note and no note.** They are holding a phone
 * in one hand, they are tired, and the thing they need to keep is a paragraph.
 * Android provides speech input free and this app was not offering it.
 *
 * **A first class control, not a hidden one.** The soft keyboard usually has a
 * microphone key, and relying on it means relying on a key that some keyboards
 * hide, some move, and nobody discovers under stress. This one is the app's own
 * and is the same on every field in it.
 *
 * **It lives inside the field, at the end edge**, since 2026-08-12 and #361.
 * It was an outlined pill reading "Speak it" under every text area, which is a
 * second row of furniture per field and four of them on a form with four
 * fields. The owner's words were that a microphone belongs in the text entry
 * field: it is where every other application on the phone puts it and where
 * somebody looks without being told. **The words did not go with it**, they
 * became the control's label for a reader, so nothing was lost but the row.
 *
 * **It appends rather than replaces.** Somebody who typed half a sentence and
 * then decided to say the rest keeps both. What comes back is ordinary text in
 * an ordinary field, so it can be corrected, which matters because recognition
 * gets names and drug names wrong and this app is full of both.
 *
 * **It disappears when there is nothing behind it.** A device with no
 * recognition service shows no control rather than a control that opens
 * nothing, per rule 11 and D44: an interface may offer something it has not
 * built, but it may not go quiet when somebody takes it up.
 */
@Composable
fun DictateAction(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** True where this is drawn inside a field rather than beside one. */
    inField: Boolean = false,
    /**
     * True where speaking is the point of the screen rather than an alternative
     * on it.
     *
     * **Law 3: voice is the biggest control on the note stage.** Somebody
     * standing in a corridor with a phone in one hand types badly and speaks
     * fine, and a text link beside a keyboard is not an offer they will take.
     * Everywhere else it stays a quiet action beside a field.
     */
    prominent: Boolean = false,
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // Asked of the package manager rather than assumed. Most phones have it,
    // and "most" is not a thing to put a control behind.
    val available = remember(context) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .resolveActivity(context.packageManager) != null ||
            context.packageManager.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                PackageManager.MATCH_DEFAULT_ONLY,
            ).isNotEmpty()
    }
    if (!available) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return@rememberLauncherForActivityResult
        onText(spoken)
    }

    // **The language the app is running in, not the phone's.** Somebody who set
    // this app to Spanish and left the phone in English is speaking Spanish,
    // and handing the recognizer the wrong language produces confident nonsense
    // rather than an error. D52 is the same mistake in the other direction.
    val languageTag = strings.locale.toLanguageTag()

    val speak: () -> Unit = {
            launcher.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    // What the recognizer shows on its own screen while
                    // listening. Written for this app rather than left to the
                    // system's default, which says "Speak now" with no idea
                    // what is being spoken into.
                    putExtra(RecognizerIntent.EXTRA_PROMPT, strings["dictate.prompt"])
                },
            )
    }

    if (inField) {
        // **A 48dp target on an 18dp glyph**, per `DESIGN.md` 12, which is why
        // the icon is centered in a box rather than clickable itself. The field
        // is 48dp tall, so this fills its end edge exactly and nothing grows.
        Box(
            modifier = modifier
                .size(Space.touchTarget)
                .clip(Radius.tile)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = strings["dictate.speak"],
                    onClick = speak,
                )
                // **The label a reader hears**, since the words that used to sit
                // beside it are gone. It is the control's whole name.
                .semantics { contentDescription = strings["dictate.speak"] },
            // **Top, not center**, because the field grows downward as somebody
            // types. Centered, the glyph drifted to the middle of a two line
            // note and read as floating beside nothing; on the first line it
            // sits level with the words, which is where every other application
            // puts it. The target stays the full 48dp underneath it.
            contentAlignment = Alignment.TopCenter,
        ) {
            MicrophoneGlyph(tint = if (enabled) colors.blue else colors.ink2)
        }
        return
    }

    if (prominent) {
        // **A quiet button rather than a filled one, deliberately.** There is
        // already one filled action on the capture screen and it is Save, and
        // law 2 allows exactly one. What "biggest" buys here is width and a
        // real border at rest: it reads as a control somebody would press
        // rather than as a link beside a field.
        QuietButton(
            label = strings["dictate.speak"],
            onClick = speak,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        TextAction(
            label = strings["dictate.speak"],
            onClick = speak,
            enabled = enabled,
            leading = { MicrophoneGlyph(tint = if (enabled) colors.blue else colors.ink2) },
            modifier = modifier,
        )
    }
}

/**
 * The microphone, drawn on the same grid as every other icon in the app.
 *
 * Decorative in every case: where it sits beside the words "Speak it" those
 * carry the meaning, and where it is alone inside a field the control around it
 * carries the name. Either way the glyph itself is cleared from the semantics
 * tree rather than announced twice. Section 5.12.
 */
@Composable
private fun MicrophoneGlyph(tint: androidx.compose.ui.graphics.Color) {
    val path = remember {
        Path().apply {
            listOf(
                // The capsule.
                "M12 4a3 3 0 013 3v5a3 3 0 01-6 0V7a3 3 0 013-3z",
                // The cradle under it.
                "M6 11v1a6 6 0 0012 0v-1",
                // The stem.
                "M12 18v3",
            ).forEach { addPath(PathParser().parsePathString(it).toPath()) }
        }
    }

    Canvas(modifier = Modifier.size(18.dp).clearAndSetSemantics { }) {
        val factor = size.minDimension / 24f
        scale(factor, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * A field and its dictation control, which is the pairing every text area uses.
 *
 * Exists so that "every text area offers dictation" is one call rather than a
 * habit twelve screens have to remember, and so the spacing between the two is
 * decided once.
 */
@Composable
fun DictatableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    note: String? = null,
    fieldTestTag: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    imeAction: androidx.compose.ui.text.input.ImeAction =
        androidx.compose.ui.text.input.ImeAction.Default,
    /** Passed through to [DictateAction]. Law 3, on the note stage of capture. */
    prominentVoice: Boolean = false,
) {
    // Appended, with a space, so half typed and half spoken is a sentence
    // rather than two words jammed together.
    val append: (String) -> Unit = { spoken ->
        onValueChange(if (value.isBlank()) spoken else "${value.trimEnd()} $spoken")
    }

    Column(modifier = modifier) {
        HealthTrailTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            hint = hint,
            note = note,
            fieldTestTag = fieldTestTag,
            enabled = enabled,
            singleLine = singleLine,
            imeAction = imeAction,
            // **Inside the field**, unless this is the one screen where
            // speaking is the point rather than an alternative.
            trailing = if (prominentVoice) {
                null
            } else {
                { DictateAction(inField = true, enabled = enabled, onText = append) }
            },
        )

        if (prominentVoice) {
            Spacer(Modifier.height(Space.s))
            DictateAction(prominent = true, enabled = enabled, onText = append)
        }
    }
}
