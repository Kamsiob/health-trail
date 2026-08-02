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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * section 5.4, and never as a resting state on a screen. That rule is also why
 * removal is reached by a long press rather than by a Remove button sitting on
 * every card: a visible destructive affordance on every row in eight sections
 * would be exactly the resting state 5.4 forbids, multiplied.
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
            // Shown back so somebody who long pressed the wrong card sees it
            // before the tap that matters, rather than after.
            Text(text = what, style = HealthTrail.type.bodyL, color = colors.ink)

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

/**
 * Makes a card removable by a long press.
 *
 * **A long press rather than a visible button, and the reasoning is section
 * 5.4's.** A Remove control resting on every row of every list is a destructive
 * affordance sitting on the screen, which that section rules out. The long
 * press is also what Android already means by "more to do with this row", and
 * a screen reader announces it as an available action rather than hiding it.
 *
 * **The card still does nothing on a short press**, because it did nothing
 * before, and inventing a tap target that opens something unrelated would be
 * worse than the honest absence.
 */
@Composable
fun Modifier.removableByLongPress(
    label: String,
    onLongPress: () -> Unit,
): Modifier = this
    .pointerInput(onLongPress) {
        detectTapGestures(onLongPress = { onLongPress() })
    }
    // **The gesture and the reader action are declared separately on purpose.**
    // `combinedClickable` would give both at once and would also make the card
    // respond to a short press with nothing, which rule 16 calls broken. A
    // gesture detector adds no click semantics, and the explicit long click
    // action is what puts this in a reader user's list of things they can do
    // rather than leaving it as a gesture they cannot discover.
    .semantics { onLongClick(label = label) { onLongPress(); true } }
