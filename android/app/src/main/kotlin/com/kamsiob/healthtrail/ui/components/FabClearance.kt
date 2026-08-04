package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The clearance everything on a FAB screen owes the FAB. `DESIGN.md` section 8
 * and `DECISIONS.md` D81.
 *
 * **The grid draws several screens with a full width button running underneath
 * the corner FAB**, and the owner identified it as an error in the drawing
 * rather than an instruction. A control the person cannot reach because another
 * control sits on top of it is the most basic possible defect, **and it is
 * invisible in a static mockup** because nothing overlaps until the FAB is real.
 *
 * These exist as modifiers rather than as a rule somebody has to remember,
 * because a rule that has to be remembered on twenty screens is a rule that
 * will be missed on one of them, and the one it is missed on will be the one
 * with the important button.
 *
 * **Everything here is start and end rather than left and right**, so in Arabic
 * the FAB sits in the start corner and the clearance moves with it without any
 * screen thinking about direction.
 */

/**
 * For a bottom-anchored action bar on a screen that has the FAB.
 *
 * **It does not span the full width.** It ends before the FAB's zone, leaving
 * the FAB's own width plus a 12dp gap clear on the trailing side, so button and
 * FAB sit side by side rather than one on top of the other.
 */
fun Modifier.fabSafeActionBar(): Modifier =
    this.padding(end = Space.fabSize + Space.fabGap)

/**
 * How much bottom padding a scrolling list needs on a screen that has the FAB.
 *
 * **The last item has to be able to scroll fully clear of the FAB**, not merely
 * to exist beneath it. A list whose final row stops under the button is a row
 * the person can see and cannot tap, which is worse than one they cannot see at
 * all, because it looks like the app is ignoring them.
 *
 * A value rather than a `PaddingValues` helper on purpose: composing padding
 * objects means resolving start and end, and every way of doing that outside a
 * layout has to be told the direction, which is exactly the thing this file
 * exists to stop screens getting wrong. **The caller adds this to its own bottom
 * padding and keeps its own start and end**, which stay direction-aware because
 * they never leave the layout that owns them.
 */
val fabScrollClearance: Dp get() = Space.fabScrollClearance
