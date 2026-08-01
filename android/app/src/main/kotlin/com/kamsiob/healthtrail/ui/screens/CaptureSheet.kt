package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object CaptureTags {
    const val SHEET = "capture_sheet"
    fun option(kind: CaptureKind) = "capture_option_${kind.name.lowercase()}"
}

/**
 * The six things a person can write down.
 *
 * The order is the order in `MASTER_SPEC.md` section 4.2 and it does not change,
 * because someone reaching for this while a nurse is still on the phone is
 * reaching by position rather than by reading.
 */
enum class CaptureKind { CALL, VISIT, INCIDENT, MEASUREMENT, QUESTION, DOCUMENT }

/**
 * The capture sheet, which is the only way data enters this app.
 *
 * **The person chooses what happened, never where it goes.** Each of the six
 * files itself into the right section. Anything that cannot be categorized lands
 * in the Unfiled tray, where the app suggests a home by plain word matching and
 * the person confirms with one tap. The app never files anything on its own.
 *
 * A real bottom sheet rather than the dimmed composite the mockups show, per
 * `DESIGN.md` section 3 item 7: a static image cannot show a transition, so the
 * mockup drew it that way and the built thing is a sheet.
 *
 * It opens with the expressive spring, which is one of only three moments in the
 * whole app allowed to overshoot. The other two are a milestone joining the arc
 * and an incident being marked resolved. Three, because each is a small piece of
 * relief in an app used during hard times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    onChoose: (CaptureKind) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        // **A scrim that actually dims.** Material's default is a light veil,
        // and against this app's dark surfaces the notebook behind the sheet
        // stayed almost as bright as the sheet itself and went on competing for
        // the eye. Looked at on the phone, it did not read as a sheet over a
        // screen so much as two screens at once.
        scrimColor = Color.Black.copy(alpha = SCRIM_ALPHA),
        shape = Radius.bottomSheet,
        // **No drag handle**, and it is removed rather than labeled.
        //
        // `ScreenReaderTest` found it carrying a click action and announcing
        // nothing, so a reader user met an unlabeled button on the one screen
        // every piece of data enters through. With `skipPartiallyExpanded` the
        // handle has no state to toggle, so it was a control that did nothing
        // and said nothing.
        //
        // Labeling it was tried twice, wrapping it and passing the modifier
        // down, and Material applies its own semantics outside both. Removing
        // it costs nothing the reference asked for: section 3 item 7 records
        // that the mockups draw this as a dimmed composite with no handle. The
        // sheet still dismisses by tapping outside and by the back gesture.
        dragHandle = null,
        modifier = Modifier.testTag(CaptureTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Text(
                text = strings["capture.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.l))

            CaptureKind.entries.forEach { kind ->
                CaptureOption(
                    label = strings[labelKey(kind)],
                    onClick = { onChoose(kind) },
                    modifier = Modifier.testTag(CaptureTags.option(kind)),
                )
                Spacer(Modifier.height(Space.s))
            }
        }
    }
}

/**
 * One choice.
 *
 * A `sand` inset row rather than a card, because these are actions inside a
 * sheet rather than records. No icons: six icons would be six small pictures of
 * what a call or an incident looks like, and the words are already the shortest
 * unambiguous form.
 */
@Composable
private fun CaptureOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
            .clickable(
                interactionSource = interaction,
                // The row's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
        )
    }
}

private fun labelKey(kind: CaptureKind): String = when (kind) {
    CaptureKind.CALL -> "capture.call"
    CaptureKind.VISIT -> "capture.visit"
    CaptureKind.INCIDENT -> "capture.incident"
    CaptureKind.MEASUREMENT -> "capture.measurement"
    CaptureKind.QUESTION -> "capture.question"
    CaptureKind.DOCUMENT -> "capture.document"
}

/**
 * How far the scrim dims what is behind the sheet.
 *
 * Enough that the notebook reads as behind rather than beside, and not so much
 * that the person loses where they were. Judged on the device in dark theme,
 * which is the harder of the two: on warm paper a lighter scrim would do, and
 * one value that works in both is worth more than two that each work in one.
 */
private const val SCRIM_ALPHA = 0.62f
