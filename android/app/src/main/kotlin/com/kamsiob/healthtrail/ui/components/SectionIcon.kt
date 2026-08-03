package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.Radius

/**
 * The icon tile, per `DESIGN.md` section 5.12.
 *
 * A rounded tile carrying one line drawing, which is how the reference file
 * draws every row of the table of contents. The radius comes from section 4.2,
 * which already named the icon tile before anything was built against it.
 *
 * **The drawings are paths rather than an icon font.** The same reason the
 * chevron is a path: a font can fall back to a box glyph in a language nobody
 * tested, and this app ships in four scripts. A path draws the same everywhere.
 *
 * **Icons never mirror here.** Section 4.4 mirrors directional icons, and none
 * of these is directional: a clipboard points nowhere. The trail's own icon
 * describes a branching route rather than a direction of travel, so it stays as
 * drawn in every locale, and the chevron beside it is the thing that flips.
 *
 * The stroke is held to a 3:1 non-text ratio rather than a text ratio, which is
 * what section 2.3 sets `ink3NonText` aside for, and no icon here is ever the
 * only thing naming its row: every row carries the section's name in words.
 */
@Composable
fun IconTile(
    section: Repository.Section,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    tileSize: Dp = 36.dp,
    iconSize: Dp = 20.dp,
) {
    val drawing = remember(section) {
        Path().apply {
            SectionIconPaths.of(section).forEach { data ->
                addPath(PathParser().parsePathString(data).toPath())
            }
        }
    }

    Box(
        modifier = modifier
            .size(tileSize)
            .clip(Radius.tile)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            // The paths are authored on the reference file's 24 unit grid, so
            // they scale to whatever the tile is rather than being redrawn per
            // size. The stroke is specified on that same grid, which is why it
            // is not converted to pixels here: the scale carries it.
            val factor = size.minDimension / VIEWPORT
            scale(factor, pivot = Offset.Zero) {
                drawPath(
                    path = drawing,
                    color = tint,
                    style = Stroke(
                        width = STROKE,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

/**
 * One icon on its own, with no tile behind it.
 *
 * The navigation bar has no room for a tile and does not want one: a tile
 * behind four glyphs at the bottom of every screen is four more boxes competing
 * with the content above them. Same paths, same grid, same stroke.
 */
@Composable
fun NavIcon(
    destination: Destination,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
) {
    val drawing = remember(destination) {
        Path().apply {
            SectionIconPaths.of(destination).forEach { data ->
                addPath(PathParser().parsePathString(data).toPath())
            }
        }
    }

    Canvas(modifier = modifier.size(iconSize)) {
        val factor = size.minDimension / VIEWPORT
        scale(factor, pivot = Offset.Zero) {
            drawPath(
                path = drawing,
                color = tint,
                style = Stroke(width = STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private const val VIEWPORT = 24f
private const val STROKE = 1.7f

/**
 * One line drawing per section, on the 24 unit grid the reference file uses.
 *
 * **Eight of these are the reference file's own paths, copied exactly.** Care
 * team, medications, appointments, the trail, progress, documents, ask next
 * time, and emergency card are all drawn in screen 04 and are reproduced here
 * character for character rather than redrawn from memory.
 *
 * **Four had to be composed:** chapters, care threads, money, and standing
 * instructions have no drawing anywhere in the reference. Each is built to the
 * same rules as the eight that do: one 24 unit grid, a 1.7 stroke, round caps
 * and joins, no fill, and no more than three strokes of detail, so a person
 * cannot tell which four were added. That is composing inside an existing
 * idiom rather than inventing one, per `DESIGN.md` section 10.2.
 */
internal object SectionIconPaths {

    /**
     * The drawing for a section, as path data.
     *
     * Internal rather than private because the empty state drawings compose
     * their mark from exactly these paths, per `DESIGN.md` 5.17. **One drawing
     * per section, in one place**, so the icon a person learns in the table of
     * contents is the same drawing they met on the empty screen.
     */
    fun of(section: Repository.Section): List<String> = when (section) {
        // People, with a second figure behind the first.
        Repository.Section.CARE_TEAM -> listOf(
            "M17 21v-2a4 4 0 00-4-4H7a4 4 0 00-4 4v2",
            circle(10f, 7f, 4f),
            "M21 21v-2a4 4 0 00-3-3.9",
        )
        // A blister pack, banded across the top.
        Repository.Section.MEDICATIONS -> listOf(
            rect(5f, 3f, 14f, 18f, 3f),
            "M5 10h14",
        )
        // A calendar.
        Repository.Section.APPOINTMENTS -> listOf(
            rect(3f, 5f, 18f, 16f, 2f),
            "M3 10h18",
            "M8 3v4",
            "M16 3v4",
        )
        // Composed: a bookmark, because a chapter is a place kept in the book.
        Repository.Section.CHAPTERS -> listOf(
            "M7 3h10a1 1 0 011 1v17l-6-4-6 4V4a1 1 0 011-1z",
        )
        // Composed: three routes running in parallel, each from its own node,
        // which is exactly what a care thread is and is deliberately not the
        // branching path the trail uses.
        Repository.Section.THREADS -> listOf(
            circle(5f, 6f, 1.5f), "M9 6h11",
            circle(5f, 12f, 1.5f), "M9 12h11",
            circle(5f, 18f, 1.5f), "M9 18h11",
        )
        // The trail: one winding route, with its nodes.
        Repository.Section.TRAIL -> listOf(
            "M4 19c4-1 5-4 5-7 0-3 1-6 5-7",
            "M9 12h10",
            circle(19f, 5f, 1.6f),
            circle(19f, 12f, 1.6f),
            circle(19f, 19f, 1.6f),
        )
        // Axes and a plotted line. No judgment is drawn into it, per 5.8.
        Repository.Section.PROGRESS -> listOf(
            "M3 3v18h18",
            "M7 14l4-4 4 3 5-6",
        )
        // A page with its corner turned.
        Repository.Section.DOCUMENTS -> listOf(
            "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z",
            "M14 2v6h6",
        )
        // Composed: a bill, torn along the bottom, with two written lines.
        Repository.Section.MONEY -> listOf(
            "M6 2h12a1 1 0 011 1v18l-3-2-3 2-3-2-3 2V3a1 1 0 011-1z",
            "M9 8h6",
            "M9 12h6",
        )
        // Composed: a clipboard, which is what was asked and of whom.
        Repository.Section.STANDING_INSTRUCTIONS -> listOf(
            rect(5f, 4f, 14f, 17f, 2f),
            "M9 2h6v4H9z",
            "M9 12h6",
            "M9 16h4",
        )
        // A question, waiting.
        Repository.Section.ASK_NEXT_TIME -> listOf(
            circle(12f, 12f, 9f),
            "M9.5 9a2.5 2.5 0 015 .3c0 1.7-2.5 2.2-2.5 3.7",
            "M12 16.9v.2",
        )
        // A shield. The one place `alert` belongs, per 2.2.
        Repository.Section.EMERGENCY_CARD -> listOf(
            "M12 3l8 4v5c0 5-3.5 8-8 9-4.5-1-8-4-8-9V7z",
            "M12 8v4",
            "M12 14.9v.2",
        )
        // A folder, from the bottom navigation, so Projects looks the same
        // wherever it appears.
        Repository.Section.PROJECTS -> listOf(
            "M3 7h6l2 3h10v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z",
        )
    }

    /**
     * The four navigation destinations, on the same grid as the twelve
     * sections.
     *
     * **One icon set, per `DESIGN.md` 5.17.** These are drawn to the same rules
     * as everything above: one 24 unit grid, a 1.7 stroke, round caps and
     * joins, no fill, and no more than three strokes of detail. A person should
     * not be able to tell which drawings came from the reference file and which
     * were composed.
     *
     * **Projects reuses its own section drawing**, because Projects is both a
     * destination and a section and drawing it twice is how two drawings for
     * one thing start to drift.
     *
     * The other three are composed, and each takes its shape from what the
     * destination actually is rather than from a stock metaphor:
     *
     * **Today** is a waypoint, the same ringed node the trail marks a milestone
     * with. Today is where the person is standing on the trail, and this app
     * already has a shape that means exactly that. A sun or a clock would have
     * been a stock icon that says nothing this app means.
     *
     * **Notebook** is a bound book seen from the spine edge, which is the one
     * object the whole screen is named after.
     *
     * **More** is three dots, because it is the one destination that is not a
     * thing but a drawer, and every convention for that is the same convention.
     */
    fun of(destination: Destination): List<String> = when (destination) {
        Destination.TODAY -> listOf(
            circle(12f, 12f, 3f),
            circle(12f, 12f, 8f),
        )
        Destination.NOTEBOOK -> listOf(
            "M6 4h11a2 2 0 012 2v14a2 2 0 01-2 2H6z",
            "M6 4a2 2 0 00-2 2v14a2 2 0 002 2",
            "M10 9h5",
        )
        Destination.PROJECTS -> of(Repository.Section.PROJECTS)
        Destination.MORE -> listOf(
            circle(5f, 12f, 1.2f),
            circle(12f, 12f, 1.2f),
            circle(19f, 12f, 1.2f),
        )
    }

    /**
     * A circle as path data, since the parser reads path syntax and the
     * reference file writes its circles as SVG circle elements. Two half arcs,
     * which is the standard way of expressing one.
     */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} $cy a$r $r 0 1 0 ${r * 2} 0 a$r $r 0 1 0 ${-r * 2} 0"

    /** A rounded rectangle as path data, for the same reason. */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val hRun = w - r * 2
        val vRun = h - r * 2
        return "M${x + r} $y h$hRun a$r $r 0 0 1 $r $r v$vRun " +
            "a$r $r 0 0 1 ${-r} $r h${-hRun} a$r $r 0 0 1 ${-r} ${-r} " +
            "v${-vRun} a$r $r 0 0 1 $r ${-r} z"
    }
}
