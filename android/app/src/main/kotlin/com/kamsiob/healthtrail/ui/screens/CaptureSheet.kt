package com.kamsiob.healthtrail.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.Tile
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

/**
 * The capture sheet, which is the only way data enters this app.
 *
 * **FROZEN on 2026-08-13**, `docs/REMOVAL-LEDGER.md`. It is superseded by the
 * capture bloom, which is what grid screen 04 always drew: six labeled choices
 * rising from the button itself over a dimmed screen. **Never called, never
 * extended, never fixed and never translated.** What it says below about the
 * expressive spring and about the person choosing what happened rather than
 * where it goes is still true of the live path, which is why it is kept rather
 * than deleted.
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
    val sheetState = rememberSheet()

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
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
 * **The shared tile from 11.2**, not a second one. This screen had its own copy
 * for a few hours, written before the notebook needed the same shape, and two
 * copies of one component is what section 10.2 calls a defect outright: the fix
 * is to correct the earlier one rather than leave both standing.
 *
 * **`sand` rather than `card`**, which is the one departure 11.2 names. The
 * sheet itself is `card`, so a card tile on it would be a shape with no edges.
 * The recessed surface is what 2.1 sets aside for exactly this.
 *
 * **The drawing carries no fill of its own**, per 5.12's standing weight,
 * because a `sand` icon tile inside a `sand` tile is a shape nobody can see. It
 * is `ink` rather than `ink2`: on a tile the drawing is the content rather than
 * a marker beside a row.
 *
 * **There is no count.** 11.2 puts one under the name, and a capture kind has
 * nothing to count: it is a thing to do, not a place with things in it. An
 * empty count line would be an empty area, per rule 11.
 */
@Composable
private fun CaptureTile(
    kind: CaptureKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    Tile(
        label = label,
        onClick = onClick,
        modifier = modifier,
        container = colors.sand,
        icon = { tileSize, drawingSize ->
            IconTile(
                kind = kind,
                tint = colors.ink,
                background = Color.Transparent,
                tileSize = tileSize,
                iconSize = drawingSize,
            )
        },
    )
}

private fun labelKey(kind: CaptureKind): String = when (kind) {
    CaptureKind.CALL -> "capture.call"
    CaptureKind.VISIT -> "capture.visit"
    CaptureKind.INCIDENT -> "capture.incident"
    CaptureKind.MEASUREMENT -> "capture.measurement"
    CaptureKind.QUESTION -> "capture.question"
    CaptureKind.DOCUMENT -> "capture.document"
}

