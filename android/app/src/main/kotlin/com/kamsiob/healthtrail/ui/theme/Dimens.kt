package com.kamsiob.healthtrail.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, radii, and elevation from DESIGN.md sections 6 and 4.7.
 *
 * These exist as tokens rather than literals so that a value cannot drift on one
 * screen, and so that changing one changes it everywhere.
 */
@Immutable
object Space {
    /** The 4dp grid, kept for anything that genuinely steps on it. */
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val sm: Dp = 12.dp
    /** Nothing, named, so a resting animation value is a token like the rest. */
    val none: Dp = 0.dp

    val m: Dp = 16.dp
    val ml: Dp = 20.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp

    /**
     * Screen horizontal padding, DESIGN.md section 6.
     *
     * 13dp under v4, down from 20dp. The grid is drawn tighter to the edge than
     * the previous direction, which is what lets a dense row carry a leading
     * element, two lines, a trailing value, and a chevron without crowding.
     */
    val screenHorizontal: Dp = 13.dp

    /** Group row padding, 11 to 13dp. This is the outer value. */
    val cardPadding: Dp = 13.dp

    /** Group row padding, the tighter value, for a row carrying two lines. */
    val rowPaddingTight: Dp = 11.dp

    /** Between stacked elements. */
    val cardGap: Dp = 10.dp

    /**
     * The width of a hairline, wherever one is drawn rather than bordered.
     *
     * **A token because it was a literal in every screen that drew one**, and a
     * measurement typed into a screen is invisible to every other check here,
     * D142. `Modifier.border` and `HorizontalDivider` take their own; this is
     * for the `drawBehind` cases, where the stroke is in pixels and the
     * conversion is the caller's.
     */
    val hairlineWidth: Dp = 1.dp

    /**
     * The rule a focused field row draws in place of its hairline.
     *
     * Two, so it reads as a state change rather than as the divider having
     * shifted color: `DESIGN.md` section 9 says a focus treatment survives
     * grayscale, and a line that only changed color would not.
     */
    val focusRule: Dp = 2.dp

    /** Gap between a section header and its content. */
    val headerGap: Dp = 10.dp

    /** Gap between sections. */
    val sectionGap: Dp = 20.dp

    /**
     * Minimum row height, DESIGN.md section 6.
     *
     * A row may be this tall visually and still carry a 48dp touch target
     * through invisible padding. **The touch floor is never traded for
     * density**, which is why these are two tokens rather than one.
     */
    val rowMinHeight: Dp = 46.dp

    /**
     * Minimum touch target, everywhere, regardless of the visual size of the
     * thing being tapped.
     */
    val touchTarget: Dp = 48.dp


    /**
     * The FAB's own size, and the clearance everything else owes it.
     *
     * DESIGN.md section 8 and D81: on any screen where the FAB is present, a
     * bottom-anchored action bar ends before the FAB's zone, leaving the FAB's
     * width plus [fabGap] clear on the trailing side, and scrolling content gets
     * [fabScrollClearance] at the bottom so the last item can scroll fully clear
     * of it. **Nothing tappable ever sits underneath the FAB.**
     *
     * In right to left the FAB is in the start corner and the clearance moves
     * with it, which is why call sites use start and end rather than left and
     * right.
     */
    val fabSize: Dp = 48.dp

    /**
     * The mark on a choice in the capture bloom, and the drawing inside it.
     *
     * **A mark rather than words**, so it is sized to itself the way an avatar
     * is, per section 15: the label beside it carries every real word and
     * scales with the person's font.
     */
    val bloomMark: Dp = 32.dp
    val bloomDrawing: Dp = 20.dp
    val fabGap: Dp = 12.dp
    val fabScrollClearance: Dp = 48.dp + 12.dp + 16.dp
    /**
     * The height above which a closed notebook leaves the screen half empty.
     *
     * **A breakpoint rather than a spacing step**, and it lives here because
     * measurements live here: the closed notebook is about this tall, so a
     * screen taller than it has room for the folded sections and a shorter one
     * does not. `NotebookScreen` reads it to decide whether its fold starts
     * open. 2026-08-12.
     */
    val roomBelowTheClosedNotebook = 700.dp

}

@Immutable
object Radius {
    /** Cards and groups, 16 to 18dp under v4, with no border. */
    /**
     * **18dp, which is what both grid files draw**, `.group` in each. It was
     * 17dp, inside the 16 to 18 range section 6 states and one off the drawing.
     * D142 makes the grids authoritative on measurement, so it is 18.
     */
    val card = RoundedCornerShape(18.dp)

    /** Inset tile, icon in its wash, chip container, the search bar's siblings. */
    val tile = RoundedCornerShape(13.dp)

    /** A fold, `.fold` in both grids at 14px. `FoldRow` drew its own before. */
    val fold = RoundedCornerShape(14.dp)

    /**
     * The capture button, `.fab` at 17px in all three grids.
     *
     * **A circle here would be the one fully round object on a screen made of
     * soft rectangles**, which is why the drawing is not one.
     */
    val fab = RoundedCornerShape(17.dp)

    /** The reference line, `.refline` at 7px in the Projects grid. */
    val referenceLine = RoundedCornerShape(7.dp)

    /** A card's source tab, `.wtab` at 5px in the Today grid. */
    val sourceTab = RoundedCornerShape(5.dp)

    val thumbnail = RoundedCornerShape(13.dp)

    /** A sheet rises with 24dp top corners, DESIGN.md section 6. */
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    val navContainer = RoundedCornerShape(24.dp)

    /**
     * The tab chip, DESIGN.md section 7.
     *
     * Rounded at the top and square at the bottom, because it is an index tab
     * on a page rather than a pill. That asymmetry is the whole reason it reads
     * as a binder tab, and it is why the chip is not [pill].
     */
    val tabChip = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)

    /** Pills, buttons, and chips are fully rounded. */
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * The trail, specified exactly in DESIGN.md section 5.2. The signature element,
 * and the one bold thing in the whole app.
 */
@Immutable
object Trail {
    /** Route line stroke. */
    val strokeWidth: Dp = 2.dp
    /** Dash pattern, on and off. */
    val dashOn: Dp = 6.dp
    val dashOff: Dp = 6.dp
    /** The route line's opacity. */
    const val ROUTE_ALPHA = 0.65f
    /** An ended thread keeps its color and drops to this, so it reads as finished rather than deleted. */
    const val ENDED_THREAD_ALPHA = 0.35f

    /** Node circle. */
    val nodeSize: Dp = 12.dp
    /** Ring in the current background color, so the node sits on the line rather than beside it. */
    val nodeRing: Dp = 3.dp

    /**
     * How much route a swatch shows beside a thread's name.
     *
     * Enough for four dashes of the widest pattern, so the rhythm is
     * recognizable rather than reading as a mark. Section 5.2.2.
     */
    val swatchWidth: Dp = 28.dp

    /** An upcoming waypoint is hollow at the same 12dp, so it is not a larger thing. */
    val hollowStroke: Dp = 2.dp

    /**
     * An open waypoint's ring, heavier than [hollowStroke].
     *
     * Open and upcoming are both "not done", and the difference is load
     * bearing: a person scanning a thread has to tell "I am waiting on
     * somebody" from "this has not come round yet", because only the first is
     * theirs to chase. The grid draws 3 against 2.5.
     */
    val openStroke: Dp = 3.dp

    /** A milestone's outer ring, and the air between it and the disc. Section 5.2.1. */
    val milestoneRing: Dp = 1.5.dp
    val milestoneGap: Dp = 4.dp

    /**
     * The spine's geometry, shared by every screen that draws one.
     *
     * These lived as private constants on the trail screen, which is exactly
     * how the vocabulary stayed on one screen. Section 5.2.3.
     */
    val gutterWidth: Dp = 28.dp
    val lineCenter: Dp = 9.dp

    /**
     * Where the node sits down the row, at most.
     *
     * It lands on the row's first line of text rather than in its vertical
     * middle, so a long entry does not push its own waypoint away from the date
     * it belongs to. **Measured against a trail card, which carries a date line
     * above its title**, and that is the assumption that failed: a card with
     * one line of text and no eyebrow is shorter than this, so a fixed offset
     * put the node below the text it marks, near the card's bottom edge. Seen
     * on the prep sheet, where questions with a role label and questions
     * without one sit next to each other and their waypoints visibly drift
     * apart.
     *
     * [com.kamsiob.healthtrail.ui.components.SpineRow] clamps it to half the
     * row's own height, so a short row centers and a tall one still anchors at
     * its first line.
     */
    val nodeCenterY: Dp = 40.dp
}

/**
 * Elevation. DESIGN.md section 4.7.
 *
 * Light theme carries a soft, warm, low contrast two layer shadow. Dark theme
 * carries no shadow at all: elevation there is paper to card to sand, plus an
 * optional hairline where two surfaces of the same value must separate.
 *
 * A note for anyone generating print or PDF output from these styles: large
 * soft shadows rasterize as dark smudges. Print and PDF paths substitute a 1dp
 * hairline. This was learned the hard way producing the concept document.
 */
@Immutable
object Elevation {
    /** The v4 card shadow, outer layer. */
    val cardBlur: Dp = 26.dp
    val cardOffsetY: Dp = 10.dp
    /** The v4 card shadow, tight layer. */
    val cardBlurTight: Dp = 6.dp
    val cardOffsetYTight: Dp = 2.dp
    /** Rows, thumbnails, and anything raised only slightly. */
    val smallBlur: Dp = 8.dp
    val smallOffsetY: Dp = 2.dp
    val printHairline: Dp = 1.dp
}
