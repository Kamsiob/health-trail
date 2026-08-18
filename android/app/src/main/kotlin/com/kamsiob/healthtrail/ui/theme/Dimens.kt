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
     * **16dp, measured off the approved mockups**: the notebook's containers
     * start 16.0dp from the edge and Today's hero 17.7dp. It was 13, which is
     * part of what the owner meant by "look how cluttered it looks". The v4
     * grid file it came from was drawn tighter to the edge than the mockups
     * that superseded it. D183.
     */
    val screenHorizontal: Dp = 16.dp

    /** Group row padding, 11 to 13dp. This is the outer value. */
    val cardPadding: Dp = 13.dp

    /** Group row padding, the tighter value, for a row carrying two lines. */
    val rowPaddingTight: Dp = 11.dp

    /**
     * The air above and below a row's own content, and **it is why the app read
     * as denser than the drawing it was built from.**
     *
     * The owner, comparing the two: "the mockup is much nicer. it breathes.
     * it's more clear and user friendly." Measured rather than adjusted by eye:
     * the icon tiles in `m3v4-1` sit on a **64dp pitch** and the built rows sat
     * on **54**, with tiles the same size in both. The whole difference was
     * eight points of vertical padding where the drawing has thirteen.
     *
     * **A token because it is every list in the app at once**, which is the
     * point: one row that breathes and forty that do not is worse than either.
     */
    val rowVertical: Dp = 13.dp

    /**
     * The squircle a row's mark sits in, and the drawing inside it.
     *
     * Measured off `m3v4-1` and `m3v4-3`: a rounded square that reads as a
     * square, not the near-circle the old tile drew at 16dp on a 36dp box.
     */
    val markTile: Dp = 44.dp

    /**
     * The same mark on a widget, where the height is fixed and the answer is
     * what the card is for.
     *
     * **Smaller than a row's, because a widget pays for it in the answer.** A
     * card on Today is one square of the field's grid and its height is set by
     * that grid, D192, so every point the head takes is a point the count and
     * its list do not have. At the row's 44 the lead cut its own last line off,
     * which is the truncation rule 11 bans. Measured on the phone.
     */
    val markCard: Dp = 24.dp

    /** The drawing inside [markCard]. */
    val markCardGlyph: Dp = 16.dp

    /**
     * The section's mark on its own empty screen, and **the largest a mark gets
     * anywhere in the app.**
     *
     * An empty section has one object on it and this is the object. At the
     * row's 44 it read as a row that had lost its words; half again as large it
     * reads as the section's own sign. #388 finding 1, and `docs/V4.md` 6.1
     * item 9 asks for a designed empty state rather than the content removed.
     */
    val emptyMark: Dp = 64.dp

    /** The drawing inside [emptyMark], on the tile's own 0.55 proportion. */
    val emptyMarkGlyph: Dp = 34.dp

    /**
     * The destination mark in the navigation bar, and the tile the selected one
     * wears.
     *
     * **Smaller than a row's 44dp**, because the bar has a label under it and a
     * fixed height, and the tile has to hold four of these plus their words
     * inside `ShortNavigationBar`'s own row without pushing it taller. Measured
     * on the phone at font scale 1.0 and 2.0.
     */
    val navMark: Dp = 38.dp

    /** The drawing inside [navMark], at Material's own 24dp icon size. */
    val navMarkGlyph: Dp = 24.dp

    /**
     * A mark set inside a line of type, or inside an action beside its label.
     *
     * **Smaller than the 24dp a symbol draws at on its own**, because a glyph
     * matched to a 24dp box beside 16sp text reads as a second word rather than
     * as a mark on the line. `m3v4-5` sets the calendar this way beside its
     * date and the zoom mark this way inside its button. D183.
     */
    val markInline: Dp = 20.dp

    /**
     * A person's mark, at the two sizes the drawings set.
     *
     * `m3v4-3` puts a 42dp circle in a 59dp row and raises the one you call to
     * 56dp in a block of their own. **Fixed rather than scaled with the type**,
     * because a circle of initials is a mark and not words: every real word
     * beside it grows with the person's font. D183, D186.
     */
    val avatarRow: Dp = 42.dp
    val avatarLead: Dp = 56.dp

    /**
     * A face in a row of faces, which is smaller than a face in a row of one.
     *
     * A card that shows who is on the care team draws three or four of these
     * overlapping, and at the row size they would be the card. `m3v4-0` sets
     * them at this, and Today's next block and the care team card are the two
     * places it appears.
     */
    val avatarFace: Dp = 32.dp

    /**
     * The three gaps that carry the structure of a screen, and there are only
     * three.
     *
     * **Uniform air is why a screen reads as one long thing.** Google's own
     * research on Material 3 Expressive measured people finding what they came
     * for **up to four times faster** when related things were grouped and the
     * groups were separated, and containment plus spacing is how it names that.
     * A screen where every gap is the same has told the eye nothing about what
     * belongs to what.
     *
     * - [withinGroup]: two things that answer one question, a field and the
     *   chips that fill it in.
     * - [betweenGroups]: one question and the next. Material's own section
     *   value, and the baseline it calls the most common spacing there is.
     * - [betweenZones]: the part that asks and the part that acts.
     *
     * Every one of them is on the 4dp grid Material is built on. 2026-08-17,
     * the owner: "it needs to be able to breathe and some visual structure."
     */
    val withinGroup: Dp = 8.dp
    val betweenGroups: Dp = 24.dp
    val betweenZones: Dp = 32.dp

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
     * The switch in [ToggleRow], which this app draws rather than imports.
     *
     * **Here rather than in the component**, because a measurement written into
     * a screen comes from nowhere and is invisible to every check: the drift
     * check caught these seven the hour they were written, which is what it is
     * for.
     *
     * The track is wide enough that the thumb's travel reads as a state change
     * rather than a nudge, and the inset is what keeps the thumb from touching
     * the track's edge at either end. **The switch is never the touch target**:
     * the whole row is, at [touchTarget] or more.
     */
    /**
     * The breathing room above and below what somebody types in a field.
     *
     * With the 20dp minimum on the editable line itself this clears the 48dp
     * target, which is what section 12 asks of anything a finger lands on.
     */
    val fieldVertical: Dp = 14.dp

    /**
     * The focus ring, which is one width everywhere it is drawn.
     *
     * Section 9's accessibility floor: a keyboard or switch user has to see
     * where they are, and a ring that is 2dp on a button and 1dp on a field is
     * two treatments where the app promises one.
     */
    val focusRing: Dp = 2.dp

    val switchTrackWidth: Dp = 52.dp
    val switchTrackHeight: Dp = 32.dp
    val switchThumb: Dp = 24.dp
    val switchThumbInset: Dp = 4.dp


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
    val fabSize: Dp = 56.dp

    /**
     * The squircle the lamp sits in at the head of the tips sheet.
     *
     * **The mark inside it is Google's, D196**, so the measurements that used to
     * describe a bulb and its base are gone: the drawing was a circle over a
     * rounded bar, repeated at two sizes, and two copies of one mark drift the
     * moment either is nudged.
     */
    val tipsHeaderTile: Dp = 44.dp
    val tipsPointDot: Dp = 6.dp
    val tipsPointDotTop: Dp = 7.dp

    /** How far a row rises as it arrives. D168. */
    val arrivalRise: Dp = 10.dp

    /** The dot that tells one section from another without shouting. D171. */
    val sectionDot: Dp = 7.dp

    /** How far a carried card lifts off the field. D169. */
    val carryLift: Dp = 8.dp

    /** Flat on the field, which is where a card rests and lands. */
    val flat: Dp = 0.dp

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
    val fabScrollClearance: Dp = 56.dp + 12.dp + 16.dp
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
    // **There is no `card` radius any more, and its absence is the point.**
    // It was 22dp against the 26dp the containers use, so a fold sat under a
    // card drawn by a different hand: the tell the v4 check calls `old-shape`,
    // on 58 surfaces, which is most of the app's furniture. Every one of them
    // was a clip, a raised card or a focus ring on a card-sized container, so
    // there was no site where the smaller corner was doing a job.
    //
    // It is deleted rather than left pointing at [cardLarge], because a token
    // that still exists gets reached for, and the next component drawn at 22dp
    // would be invisible to the check that just finished removing them.

    /**
     * The hero, and only the hero. **Extra large, and asymmetric.**
     *
     * D167: shape variety is the loudest signal of Material 3 Expressive, and
     * one radius on every surface is what made this app read as clean rather
     * than as designed. The one thing a screen leads with gets a corner
     * treatment nothing else has, so the eye lands on it before a word is
     * read. The diagonal, large on one pair and small on the other, is the
     * shape library's own device: it gives a rectangle a direction.
     */
    val hero = RoundedCornerShape(
        topStart = 36.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 36.dp,
    )

    /** The hero's mirror, for a pair that should not look stamped. */
    val heroMirrored = RoundedCornerShape(
        topStart = 18.dp, topEnd = 36.dp, bottomStart = 36.dp, bottomEnd = 18.dp,
    )

    /**
     * **The card radius, for every container in the app.** Generous, so a grid
     * of them reads as pebbles rather than as boxes.
     *
     * It began as Today's square card and became the only card corner when the
     * 22dp `card` token was deleted. The name is kept because 58 sites and the
     * v4 mockups already speak it, and renaming a token that is drawn correctly
     * everywhere buys nothing.
     */
    val cardLarge = RoundedCornerShape(26.dp)

    /** The leading edge of a connected button row. */
    val groupStart = RoundedCornerShape(
        topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp,
    )

    /** The middle of a connected button row. */
    val groupMiddle = RoundedCornerShape(8.dp)

    /** The trailing edge of a connected button row. */
    val groupEnd = RoundedCornerShape(
        topEnd = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, bottomStart = 8.dp,
    )

    /** Inset tile, chip container, the search bar's siblings. */
    val tile = RoundedCornerShape(16.dp)

    /**
     * The squircle behind a section's mark, and **it is not [tile].**
     *
     * At 16dp on a 36dp box the corner was within two points of half the box,
     * so the icon tile on every section row rendered as a circle. The approved
     * mockups draw a rounded square that reads as a square: `m3v4-1`'s notebook
     * rows and `m3v4-3`'s care team. A separate token because the two shapes
     * are now different sizes as well as different jobs. D182.
     */
    val iconTile = RoundedCornerShape(14.dp)

    /** A fold, `.fold` in both grids at 14px. `FoldRow` drew its own before. */
    val fold = RoundedCornerShape(18.dp)

    /**
     * The capture button, `.fab` at 17px in all three grids.
     *
     * **A circle here would be the one fully round object on a screen made of
     * soft rectangles**, which is why the drawing is not one.
     */
    val fab = RoundedCornerShape(16.dp)

    /**
     * The capture button's corner at rest, as a value the morph animates.
     *
     * **A third of the 48dp button, not half.** At 26 it was past half and
     * rendered as a plain circle, which is the one shape section 6 says this
     * button must not be: a fully round object among soft rectangles. Seen on
     * the phone. The morph goes rounder under a finger and all the way to a
     * circle only when it becomes the control that closes.
     */
    val fabCorner = 16.dp

    /** Rounder while it is held, which is where the morph goes. D167. */
    val fabCornerPressed = 22.dp

    /**
     * The corner it reaches when the capture sheet is open.
     *
     * Half the button's side, which is a circle: the control has become the way
     * out of the thing it opened, and the roundest shape in the app is the one
     * that says so.
     */
    val fabCornerOpen = 28.dp

    /** The reference line, `.refline` at 7px in the Projects grid. */
    val referenceLine = RoundedCornerShape(7.dp)

    /** A card's source tab, `.wtab` at 5px in the Today grid. */
    val sourceTab = RoundedCornerShape(5.dp)

    val thumbnail = RoundedCornerShape(16.dp)

    /** A sheet rises with 24dp top corners, DESIGN.md section 6. */
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    val navContainer = RoundedCornerShape(24.dp)

    /**
     * **The navigation bar's own corner, so it reads as an object rather than
     * as where the page happens to stop.** #388 section 2.
     *
     * The owner, 2026-08-18: "I don't want the taskbar area with the today
     * button and notebook button, project button and more button to blend into
     * any content above it. so maybe we need to give it a different shape".
     * `m3v4-1` draws the bar and every block in the same `#F3F1EC`, and on the
     * notebook the last group and the bar met with nothing between them.
     *
     * **The sheet's 24dp, and the same value on purpose.** Both are surfaces
     * that meet the bottom of the screen and rise off it, and the shape scale
     * says the same thing twice rather than inventing a fourth value for the
     * second one. At 20 the corner was there and did no work; measured off the
     * capture, 24 is where it starts reading as an object.
     *
     * Square at the bottom, because the bar's surface runs to the bottom edge
     * of the screen: rounding there would put a strip of canvas under it and
     * end the screen twice, which is what the owner objected to on 2026-08-17.
     */
    val navBar = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    /**
     * The tab chip, DESIGN.md section 7.
     *
     * Rounded at the top and square at the bottom, because it is an index tab
     * on a page rather than a pill. That asymmetry is the whole reason it reads
     * as a binder tab, and it is why the chip is not [pill].
     */
    val tabChip = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)

    /** Pills, buttons, and chips are fully rounded. */
    /**
     * A button, and **a corner of its own on purpose.**
     *
     * D167: shape variety is the loudest signal of Material 3 Expressive, and
     * one radius on every surface is what made this app read as clean rather
     * than as designed. The v4 pass then deleted the 22dp card corner and put
     * 58 surfaces on 26dp, which made it more uniform rather than less. This is
     * the beginning of undoing that: an action is not a card and does not wear
     * a card's corner.
     *
     * The approved mockups draw it. `m3v4-2`'s three actions under the hero are
     * rounded rectangles at roughly this radius, not pills and not cards.
     */
    val button = RoundedCornerShape(18.dp)

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
     * The road, measured off `m3v4-2`, which is the one drawing that has a
     * spine on it. D183, and every number here is px/3 off the 1080 capture.
     *
     * **Nothing like the old spine's geometry**, which is why these are their
     * own tokens rather than edits to the ones above: the drawing's node is
     * 24dp where the old one is 12, its line is 4dp where the old one is 2, and
     * its gutter is 56dp where the old one is 28. Twice the size, and it is the
     * difference between a decoration beside a list and a road the list is
     * standing on.
     */
    val roadGutter: Dp = 56.dp

    /** Where the line runs inside the gutter, from the content's leading edge. */
    val roadLineCenter: Dp = 22.dp
    val roadLine: Dp = 4.dp

    /** Arrived: a filled disc carrying a check. */
    val roadNodeDone: Dp = 24.dp

    /** Where it is now: a disc in a halo, so the eye lands on it first. */
    val roadNodeNow: Dp = 20.dp
    val roadNodeNowHalo: Dp = 28.dp

    /** Ahead: an outline, the same size as the disc it will become. */
    val roadNodeAhead: Dp = 18.dp
    val roadNodeAheadStroke: Dp = 2.dp

    /**
     * How far down a row the node sits, at most.
     *
     * It lands on the row's first line rather than in its vertical middle, so a
     * long stop does not push its own node away from the words it marks, and it
     * is clamped to half the row so a short one still centers. The same rule the
     * old spine learned the hard way.
     */
    val roadNodeCenterY: Dp = 26.dp

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
     * [com.kamsiob.healthtrail.ui.v4.SpineRow] clamps it to half the
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
