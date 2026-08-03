package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.IconTile
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                // **The top padding is the defect this line fixes.** The sheet
                // has 28dp rounded top corners, per 4.2, and the title was
                // placed at the content's first pixel, so the corners cut into
                // the word and the ascenders ran into the sheet's edge. It read
                // as a sheet that had failed to finish opening. Nothing about
                // it was visible in the code, and it is the first thing anyone
                // sees when they tap the one control the whole app is for.
                .padding(top = Space.l, bottom = Space.l),
        ) {
            Text(
                text = strings["capture.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.l))

            // **A two by three grid rather than six rows**, per `DESIGN.md`
            // 11.2. Six choices in a column is six things to read; six tiles is
            // one glance and one tap, which is what somebody standing in a
            // corridor with a nurse still talking actually has.
            //
            // **One column above font scale 1.3**, per 11.2's table, because a
            // 170dp tile cannot hold "Report an incident" at that size without
            // either wrapping to five lines or dropping below the 13sp floor,
            // and the grid gives way rather than the type.
            val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2

            CaptureKind.entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The tiles in a row are one height, so a two line
                        // label never leaves its neighbor short.
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    row.forEach { kind ->
                        CaptureTile(
                            kind = kind,
                            label = strings[labelKey(kind)],
                            onClick = { onChoose(kind) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag(CaptureTags.option(kind)),
                        )
                    }
                    // A row that is not full keeps its columns rather than
                    // letting the last tile stretch to the width of two, which
                    // would read as one choice being the important one.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(Space.sm))
            }
        }
    }
}

/**
 * One choice, as a tile.
 *
 * **`sand` rather than `card`**, which is the one departure from 11.2's "a tile
 * is a card surface". The sheet itself is `card`, so a card tile on it would be
 * a shape with no edges. The recessed surface is what 2.1 sets aside for
 * exactly this, and it is what the six rows already used before they became
 * tiles.
 *
 * **The icon carries no fill of its own**, per 5.12's standing weight, because a
 * `sand` icon tile inside a `sand` tile is a shape nobody can see. The drawing
 * is `ink` rather than `ink2`: here the icon is the content rather than a marker
 * beside a row, which is the difference 11.2 draws between a tile and a row.
 *
 * **There is no count slot.** 11.2 puts the count under the name, and a capture
 * kind has nothing to count: it is a thing to do, not a place with things in it.
 * An empty count line would be an empty area, per rule 11.
 */
@Composable
private fun CaptureTile(
    kind: CaptureKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = modifier
            // One stop for the reader, asked for rather than relied on, which
            // is D54's finding. A tile is a drawing and a label and it is one
            // thing: "Log a call, button", not a silent node followed by words.
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = TILE_MIN_HEIGHT)
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                // The tile's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(Space.cardPadding),
    ) {
        IconTile(
            kind = kind,
            tint = colors.ink,
            background = Color.Transparent,
            tileSize = ICON_TILE,
            iconSize = ICON_DRAWING,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = label,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
    }
}

/** The standard tile from `DESIGN.md` 11.2. */
private val TILE_MIN_HEIGHT = 132.dp
private val ICON_TILE = 40.dp
private val ICON_DRAWING = 24.dp

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
