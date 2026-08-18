package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/** What a test reaches the confirmation by. */
object ConfirmTags {
    const val SHEET = "confirm_sheet"
    const val CONFIRM = "confirm_confirm"
    const val KEEP = "confirm_keep"
}

/**
 * Asking before removing something. #387, D196.
 *
 * **Written fresh in `ui/v4` and the old file deleted.** The sheet and the
 * keeping action were already Material's; the destructive button was a `Box`
 * with a clip, a background, a transparent border kept around to be a focus
 * ring, an `indication = null` `clickable` and a hand animated pressed surface.
 * It is Material's `Button` now, in the scheme's own error role.
 *
 * **The color did not change and the second name for it went.** The scheme sets
 * `error` to this app's alert and `onError` to paper, so asking Material for
 * the error role paints exactly what `alertFill` painted, and the button stops
 * being the one control in the app that reaches past `colorScheme` for a fill.
 *
 * **Destructive actions live only inside a confirmation flow** and never as a
 * resting state on a screen. That is why removal is reached from the thing's
 * own screen rather than from a row in a list: an outlined pill saying "Remove
 * this" opens this sheet, so nothing is destroyed by the control itself and
 * there is no destructive affordance sitting on every row of eight sections.
 *
 * **It used to be reached by a long press for the same reason**, which solved
 * the resting state and created a worse problem: a sighted person who did not
 * already know to press and hold could not remove anything at all, while a
 * reader user was handed the action in their list. #218 and law 2.
 *
 * **The wording says what actually happens, not "are you sure".** It stops
 * appearing in the notebook, and nothing else the person wrote is touched.
 * Both halves matter: somebody removing one row needs to know the rest is safe.
 *
 * **Keeping is the calm option and it is listed second**, so the destructive
 * one is not the one a thumb lands on by reflex, while the person who came here
 * deliberately still finds it first by reading order.
 *
 * The row itself is never told the record is a tombstone rather than a
 * deletion. That is the schema's business, per rule 20.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmRemoveSheet(
    /** What is being removed, in the person's own words, shown back to them. */
    what: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberSheet()

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(ConfirmTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["remove.title"],
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
            )

            Spacer(Modifier.height(Space.s))
            // Shown back so somebody who opened the wrong thing sees it before
            // the tap that matters, rather than after.
            Text(
                text = Bidi.isolate(what),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["remove.body"],
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.l))

            DestructiveButton(
                label = strings["remove.confirm"],
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().testTag(ConfirmTags.CONFIRM),
            )

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["remove.keep"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(ConfirmTags.KEEP),
            )
        }
    }
}

/**
 * The destructive button: the scheme's error role, and only ever inside a
 * confirmation flow.
 *
 * **It stays private to this file** rather than joining `Actions.kt`, so that
 * it cannot be reached without also reaching the confirmation it belongs to.
 * That was the old file's reasoning and it survives the rewrite unchanged.
 */
@Composable
private fun DestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = Space.touchTarget),
        shape = Radius.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            // The button's own minimum would clip a label at font scale 2.0;
            // the 48dp target around it is what keeps the finger honest.
            modifier = Modifier.sizeIn(minHeight = Space.none),
        )
    }
}
