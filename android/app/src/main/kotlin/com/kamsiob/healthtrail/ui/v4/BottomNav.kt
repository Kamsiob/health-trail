package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import kotlin.math.min
import com.kamsiob.healthtrail.ui.components.Symbols

object NavTags {
    const val BAR = "nav_bar"
    const val CAPTURE = "nav_capture"
    fun tab(destination: Destination) = "nav_tab_${destination.name.lowercase()}"
}

/**
 * The four destinations, always in this order. DESIGN.md section 5.5.
 *
 * They never reorder and none is ever hidden, because a person who learned where
 * something was must find it there next month.
 */
enum class Destination { TODAY, NOTEBOOK, PROJECTS, MORE }

/**
 * How far the navigation label is allowed to grow with the system font.
 *
 * 1.4 rather than unbounded. Everything else in the app scales without limit;
 * this bar cannot, because four labels and a fixed clearance for the capture
 * button share one row.
 */
private const val NavLabelMaxScale = 1.4f

/**
 * The bottom navigation, on Material's own short bar. #386, `docs/V4.md` 2.
 *
 * **The hand-built row is gone.** It was a `Row` of four `Column`s drawing
 * their own pill, their own press state and their own focus ring, with the
 * app's own stroked glyphs inside. `docs/V4.md` 4 lists that under what
 * disappears, and V4 2 asks for a short navigation bar by name.
 *
 * **The selection treatment did not have to be argued for again, because the
 * theme already carries it.** Material draws the selected item's indicator in
 * `secondaryContainer` on `onSecondaryContainer`, and #385 pointed that pair at
 * `goldWash` and `goldInk`. So the gold pill behind the current destination,
 * which is what the approved mockups draw on every one of the six, is what this
 * component produces with no color passed to it.
 *
 * **Selection is still carried by three things, not one**: the indicator's
 * shape, the icon's tint, and the label. `DESIGN.md` 4.4 asks that of every
 * state that has a color, and the platform component satisfies it.
 *
 * The capture button is not in here. It is a corner FAB, [CaptureFab], since
 * D76.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNav(
    current: Destination,
    onSelect: (Destination) -> Unit,
    labels: (Destination) -> String,
    modifier: Modifier = Modifier,
) {
    // **The bar is a separate object, and that is a named requirement rather
    // than a finding.** #388 section 2, the owner on 2026-08-18: "I don't want
    // the taskbar area ... to blend into any content above it. so maybe we need
    // to give it a different shape and make it a little bit transparent? or
    // maybe we need to give it a different color?"
    //
    // **Why it blended.** `ShortNavigationBar` with no `containerColor` takes
    // `surfaceContainer`, which in this palette is `sand`, which is the color
    // every `Block` and every group of rows is drawn in. On the notebook the
    // last group and the bar met with no boundary at all, and `m3v4-1` draws
    // them the same `#F3F1EC` deliberately. The owner's words are newer than
    // the drawing and this follows them. D201.
    //
    // **Three separations rather than one, because no single one is enough
    // here.** M3's own elevation guidance says an overlapping element may take
    // a container change, a tonal step or a scrim to keep it from blending, and
    // this palette has no step above `sand` in light:
    //
    // 1. **A surface of its own**, `navSurface`, deeper than the page rather
    //    than brighter. White was tried first and separates, but it made the
    //    bottom of every screen the brightest thing on it; furniture should be
    //    the ground the content sits on. See the token's own note.
    // 2. **A corner**, [Radius.navBar], so the canvas shows at both top corners
    //    and the bar reads as an object rather than as where the page stops.
    // 3. **The hairline every other surface in the app carries**, in
    //    `outlineVariant` at `Space.hairlineWidth`, following that corner.
    //
    // **No shadow.** `Surfaces.kt` and `docs/V4.md` 2.1: this language is flat
    // and tonal, depth is color against color, and one exception at the bottom
    // of every screen is how a language stops being one.
    Surface(
        modifier = modifier.testTag(NavTags.BAR),
        color = HealthTrail.colors.navSurface,
        shape = Radius.navBar,
        border = BorderStroke(Space.hairlineWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        ShortNavigationBar(
            // **Transparent, because the surface above already painted it.**
            // Letting the bar paint its own default here would put `sand` back on
            // top of the separation this component exists to make.
            containerColor = Color.Transparent,
        ) {
            Destination.entries.forEach { destination ->
                val selected = destination == current
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(destination) },
                    icon = { NavMark(destination, selected) },
                    label = { NavLabel(labels(destination), selected) },
                    modifier = Modifier.testTag(NavTags.tab(destination)),
                    colors = ShortNavigationBarItemDefaults.colors(
                        // **Material's own indicator is turned off, and
                        // [NavMark] draws the selected state instead.** The
                        // owner, 2026-08-18: "the little yellow oval highlight
                        // for the active tab in the taskbar looks ugly and
                        // lazy. not polished and premium." Both halves of that
                        // are fixable and both are named: the shape is a
                        // stadium, which is a shape this app draws nowhere
                        // else, and the fill is `goldWash`, which is pale
                        // enough on any surface to read as a smudge rather
                        // than as a decision.
                        selectedIndicatorColor = Color.Transparent,
                        // Unselected items are the app's secondary ink rather than
                        // Material's, which is the one place this bar overrides the
                        // component: `ink2` is the value measured against these
                        // surfaces in `check_contrast.py`.
                        unselectedIconColor = HealthTrail.colors.ink2,
                        unselectedTextColor = HealthTrail.colors.ink2,
                        selectedIconColor = HealthTrail.colors.ink,
                        // The vertical arrangement's own name for it: this bar
                        // puts the icon above the word.
                        selectedTextColorTopIconPosition = HealthTrail.colors.ink,
                    ),
                )
            }
        }
    }
}

/**
 * A destination's word, and **the one place in the app where type stops
 * growing.**
 *
 * At font scale 2.0 "Notebook" broke mid-word into "Notebo" and "k" and
 * collided with the capture button. A single word cannot wrap, so the only
 * choices are to break it, to clip it, or to stop it growing, and a word broken
 * across two lines is less legible than the same word slightly smaller. Found
 * by looking at the phone with the system font at maximum.
 *
 * **Capped, not fixed.** It still grows up to [NavLabelMaxScale], so somebody
 * who has raised their font still gets a larger label. Beyond that the label
 * holds and the icon and the position carry it, which is the same set of things
 * a person navigates by after two weeks. Nothing else in the app is capped.
 */
/**
 * The destination's mark, and **the selected state the app draws itself.**
 *
 * The owner, 2026-08-18: "the little yellow oval highlight for the active tab
 * in the taskbar looks ugly and lazy. not polished and premium."
 *
 * **What was wrong with it, named rather than judged.** Material's own
 * indicator is a stadium in `secondaryContainer`, and #385 pointed that pair at
 * `goldWash` because the approved mockups draw a gold pill. Three things came
 * out of that:
 *
 * 1. **A stadium is a shape this app draws nowhere else.** Every mark in the
 *    interface sits in a rounded square: the section rows, the capture kinds,
 *    Today's card heads, the capture button itself. One oval at the bottom of
 *    every screen is the generic Material component showing through, which is
 *    `docs/V4.md` 6.1 item 11 exactly.
 * 2. **`goldWash` is a pale tint**, and against the bar's own surface it reads
 *    as a smudge rather than as a decision. It measures 1.07:1 on the old bar.
 * 3. **It wrapped the icon only**, so the label sat outside it and the pill
 *    looked like something that had come loose.
 *
 * **So the selected destination is the app's own mark tile**, at
 * [Radius.iconTile], which is the same shape and the same idea as every other
 * mark in the interface: a saturated tile with its own ink on top, D198.
 *
 * **In `ink`, not in gold.** Gold in this app means the way things enter the
 * notebook: the capture button is gold and it floats a thumb's width above this
 * bar. A second saturated gold object beside it would spend the one color the
 * app reserves for one job on a second job. Ink is the app's own darkest value,
 * it measures 9.3:1 against the bar, and a dark key pressed out of a warm
 * surface is the thing this reads as.
 *
 * **The unselected mark keeps the tile's footprint and not its fill**, so
 * nothing moves or resizes when the destination changes: the tile appears under
 * the mark that was already there.
 */
@Composable
private fun NavMark(destination: Destination, selected: Boolean) {
    Surface(
        modifier = Modifier.size(Space.navMark),
        shape = Radius.iconTile,
        color = if (selected) HealthTrail.colors.ink else Color.Transparent,
        contentColor = if (selected) HealthTrail.colors.paper else HealthTrail.colors.ink2,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Symbols.of(destination)),
                // **Null, because the label beside it says the same word.** A
                // reader that announces "Notebook, Notebook" is worse than one
                // that announces it once, and the item itself carries the
                // selected state.
                contentDescription = null,
                modifier = Modifier.size(Space.navMarkGlyph),
            )
        }
    }
}

@Composable
private fun NavLabel(label: String, selected: Boolean) {
    val density = LocalDensity.current
    val capped = min(density.fontScale, NavLabelMaxScale) / density.fontScale
    // **The current destination's word is the app's ink and the rest are
    // `ink2`.** Selection is carried by the tile, the mark's own color and the
    // label together, which is what `DESIGN.md` 4.4 asks of any state that has
    // a color, and it survives grayscale.
    val style = HealthTrail.type.navLabel
    Text(
        text = label,
        style = style.copy(
            fontSize = style.fontSize * capped,
            lineHeight = style.lineHeight * capped,
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
