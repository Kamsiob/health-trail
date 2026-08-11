package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ConfirmTags {
    const val SHEET = "confirm_sheet"
    const val CONFIRM = "confirm_confirm"
    const val KEEP = "confirm_keep"
}

/**
 * Asking before removing something.
 *
 * **Destructive actions live only inside a confirmation flow**, per `DESIGN.md`
 * section 5.4, and never as a resting state on a screen. **That is why removal
 * is reached from the thing's own screen rather than from a row in a list**: an
 * outlined pill saying "Remove this" opens this sheet, so nothing is destroyed
 * by the control itself and there is no destructive affordance sitting on every
 * row of eight sections.
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
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        // Removed for the reason D42 gives: with the sheet fully expanded the
        // handle is a control that does nothing and announces nothing.
        dragHandle = null,
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
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))
            // Shown back so somebody who opened the wrong thing sees it before
            // the tap that matters, rather than after.
            Text(text = Bidi.isolate(what), style = HealthTrail.type.bodyL, color = colors.ink)

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["remove.body"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DestructiveButton(
                label = strings["remove.confirm"],
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().testTag(ConfirmTags.CONFIRM),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["remove.keep"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(ConfirmTags.KEEP),
            )
        }
    }
}

/**
 * The destructive button, per section 5.4: `alert` fill, white label, and only
 * ever inside a confirmation flow.
 *
 * It exists here rather than in `Buttons.kt` so that it cannot be reached
 * without also reaching the confirmation it belongs to.
 */
@Composable
private fun DestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.alertFill)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.ink.copy(alpha = ring), Radius.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.l, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.label,
            color = colors.onAlertFill,
            textAlign = TextAlign.Center,
        )
    }
}
