package com.kamsiob.healthtrail.ui.v4

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols

/**
 * Speaking instead of typing. #387, D196.
 *
 * **Written fresh in `ui/v4` and the old file deleted.** Two things went with
 * it and nothing else changed:
 *
 * - **The microphone was drawn on a `Canvas`**, three stroked sub-paths on a 24
 *   grid this repository maintained by hand: a capsule, a cradle and a stem,
 *   with their own stroke width, cap and join. It is `Symbols.dictate`, which
 *   is Google's own Material Symbol, fetched rather than authored.
 * - **The in-field control was a `Box`** with a fixed size, a `graphicsLayer`
 *   scale animation, a clip and an `indication = null` `clickable`. It is
 *   Material's `IconButton`, which owns the 48dp target, the state layer, the
 *   shape and the button role.
 *
 * **Law 3: voice is the biggest control on the note stage.** Somebody standing
 * in a corridor with a phone in one hand types badly and speaks fine, and a
 * text link beside a keyboard is not an offer they will take. Everywhere else
 * it stays a quiet action beside a field.
 *
 * **The glyph is never announced.** Where it sits beside the words "Speak it"
 * those carry the meaning, and where it is alone inside a field the button
 * around it carries the name.
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
     */
    prominent: Boolean = false,
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

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
                // What the recognizer shows on its own screen while listening.
                // Written for this app rather than left to the system's
                // default, which says "Speak now" with no idea what is being
                // spoken into.
                putExtra(RecognizerIntent.EXTRA_PROMPT, strings["dictate.prompt"])
            },
        )
    }

    if (inField) {
        IconButton(
            onClick = speak,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            // **The label a reader hears, on the node that takes the tap.**
            // It is the control's whole name: there are no words beside it.
            modifier = modifier.semantics { contentDescription = strings["dictate.speak"] },
        ) {
            Icon(painter = painterResource(Symbols.dictate), contentDescription = null)
        }
        return
    }

    // **A quiet button rather than a filled one, deliberately.** There is
    // already one filled action on the capture screen and it is Save, and law 2
    // allows exactly one. What "biggest" buys here is width and a real border
    // at rest: it reads as a control somebody would press rather than as a link
    // beside a field.
    Action(
        label = strings["dictate.speak"],
        onClick = speak,
        enabled = enabled,
        mark = Symbols.dictate,
        modifier = if (prominent) modifier.fillMaxWidth() else modifier,
    )
}
