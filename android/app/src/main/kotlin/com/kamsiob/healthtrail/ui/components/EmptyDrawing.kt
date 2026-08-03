package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail

/**
 * The drawing on an empty screen. `DESIGN.md` section 5.17.
 *
 * **Empty states were the biggest character opportunity in this app and the
 * thinnest thing in it.** One line of gray text, on the screen a new person
 * sees most, with nothing competing for their attention. Section 1 bans every
 * cheap way to make a screen interesting, which is right, and left nothing in
 * their place, so this is what the app's own vocabulary puts there instead.
 *
 * **Two halves, and only one of them varies.**
 *
 * The **ground** is the sibling half and is identical on every screen: two
 * contour lines with a dashed route running between them and one waypoint on
 * it. That is the trail map, drawn small and quiet.
 *
 * The **mark** is the identifying half, and it is the section's own icon from
 * the table of contents, scaled up rather than redrawn. **Nothing here was
 * invented.** Thirteen freshly drawn illustrations would drift in weight and
 * character however carefully they were made, and this app already owns
 * thirteen drawings that do not drift because they came off one grid. Reusing
 * them also means an empty screen is teaching the icon the person will navigate
 * by for the next two years, at the moment they have nothing else to look at.
 *
 * **Decorative, in the sense section 2.3 defines.** Remove every one of these
 * and nothing becomes unreadable, because the words carry the screen alone. So
 * they are exempt from the 3:1 ratio, they carry no content description, and
 * they are cleared from the semantics tree entirely: a reader announcing "line
 * drawing of a path" on every empty screen is noise rather than access.
 *
 * Banned here specifically, restating section 1 because this is where
 * illustration usually goes wrong: no 3D, no blobs, no plastic, no stock, no
 * mascot, no character, no scene with a person in it, and no color.
 */
@Composable
fun EmptyDrawing(
    section: Repository.Section?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val colors = HealthTrail.colors

    val ground = remember {
        Path().apply {
            GROUND.forEach { addPath(PathParser().parsePathString(it).toPath()) }
        }
    }
    val mark = remember(section) {
        section?.let {
            Path().apply {
                SectionIconPaths.of(it).forEach { data ->
                    addPath(PathParser().parsePathString(data).toPath())
                }
            }
        }
    }

    Box(
        modifier = modifier.size(size).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        val stroke = Stroke(width = STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)

        Canvas(modifier = Modifier.size(size)) {
            val factor = this.size.minDimension / VIEWPORT
            scale(factor, pivot = Offset.Zero) {
                drawPath(ground, colors.ink.copy(alpha = GROUND_ALPHA), style = stroke)
            }
        }

        // **A separate canvas rather than a nested transform.** The ground and
        // the icon are authored on different grids, and composing the two
        // scales by hand is the kind of arithmetic that is wrong once and then
        // wrong forever. Compose already centers and offsets for free.
        if (mark != null) {
            Canvas(
                modifier = Modifier
                    .size(size * MARK_SCALE)
                    // Lifted, so the ground reads as something the mark stands
                    // on rather than something crossing it.
                    .offset(y = -size * MARK_LIFT),
            ) {
                val factor = this.size.minDimension / ICON_VIEWPORT
                scale(factor, pivot = Offset.Zero) {
                    drawPath(mark, colors.ink.copy(alpha = MARK_ALPHA), style = stroke)
                }
            }
        }
    }
}

/** The same 24 unit grid and 1.7 stroke every drawing in this app uses, per 5.12. */
private const val ICON_VIEWPORT = 24f
private const val STROKE = 1.7f

/** The ground is drawn on a wider grid, because a landscape is wider than an icon. */
private const val VIEWPORT = 48f

/** The mark takes a little under half the drawing, so the ground is not decoration around a logo. */
private const val MARK_SCALE = 0.46f

/** How far above center the mark sits, as a fraction of the drawing. */
private const val MARK_LIFT = 0.14f

/**
 * Both halves are quiet, and the mark is the louder of the two.
 *
 * 12% for the ground is the figure section 5.17 sets. The mark is lifted to 18%
 * so the section is identifiable at a glance without the drawing becoming
 * content, which is what a heavier one would be.
 */
private const val GROUND_ALPHA = 0.12f
private const val MARK_ALPHA = 0.18f

/**
 * The trail map ground: two contours, a dashed route between them, one waypoint.
 *
 * On a 48 unit grid. The route is drawn as separate segments rather than with a
 * dash effect, because a path effect would also dash the contours, and because
 * the segments are then part of the drawing rather than a property of how it
 * happens to be stroked.
 */
private val GROUND = listOf(
    // The far contour, a low ridge.
    "M2 30c6-5 10-5 15-1 5 4 9 4 14 0 4-3 8-3 15 2",
    // The near contour.
    "M2 39c7-4 11-4 16 0 5 3 9 3 13-1 5-4 9-4 17 3",
    // The route, running across them, in six dashes.
    "M6 44l3-2", "M13 40l4-2", "M21 37l4-1", "M29 35l4 1", "M37 37l3 2",
    // One waypoint on the route, hollow, because an empty section is a place
    // nothing has happened yet.
    "M25.2 36a1.8 1.8 0 1 0 3.6 0 1.8 1.8 0 1 0-3.6 0",
)
