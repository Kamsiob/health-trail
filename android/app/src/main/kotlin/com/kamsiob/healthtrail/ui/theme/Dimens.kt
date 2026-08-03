package com.kamsiob.healthtrail.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, radii, and motion from DESIGN.md sections 4.1, 4.2, and 6.
 *
 * These exist as tokens rather than literals so that a value cannot drift on one
 * screen, and so that changing one changes it everywhere.
 */
@Immutable
object Space {
    /** The 4dp grid. */
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val sm: Dp = 12.dp
    val m: Dp = 16.dp
    val ml: Dp = 20.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp

    /** Screen horizontal padding. */
    val screenHorizontal: Dp = 20.dp
    /** Card internal padding. */
    val cardPadding: Dp = 16.dp
    /** Gap between cards. */
    val cardGap: Dp = 12.dp
    /** Gap between a section header and its content. */
    val headerGap: Dp = 12.dp
    /** Gap between sections. */
    val sectionGap: Dp = 24.dp

    /**
     * Minimum touch target, everywhere, regardless of the visual size of the
     * thing being tapped. Several rows in the reference are visually shorter
     * than this and get invisible padding to reach it.
     */
    val touchTarget: Dp = 48.dp
}

@Immutable
object Radius {
    val card = RoundedCornerShape(20.dp)
    /** Inset tile, icon tile, chip container. */
    val tile = RoundedCornerShape(12.dp)
    val thumbnail = RoundedCornerShape(8.dp)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val navContainer = RoundedCornerShape(24.dp)
    /** Pills and buttons are fully rounded. */
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
     * Where the node sits down the row.
     *
     * It lands on the row's first line of text rather than in its vertical
     * middle, so a long entry does not push its own waypoint away from the date
     * it belongs to.
     */
    val nodeCenterY: Dp = 40.dp
}

/**
 * Elevation. DESIGN.md section 2.5.
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
    val cardBlur: Dp = 24.dp
    val cardOffsetY: Dp = 8.dp
    val cardBlurTight: Dp = 5.dp
    val cardOffsetYTight: Dp = 2.dp
    val printHairline: Dp = 1.dp
}
