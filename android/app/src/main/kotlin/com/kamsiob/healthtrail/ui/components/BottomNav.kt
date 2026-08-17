package com.kamsiob.healthtrail.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import com.kamsiob.healthtrail.ui.theme.HealthTrail

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
    ShortNavigationBar(modifier = modifier.testTag(NavTags.BAR)) {
        Destination.entries.forEach { destination ->
            val selected = destination == current
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Symbol(
                        symbol = Symbols.of(destination),
                        // **Null, because the label beside it says the same
                        // word.** A reader that announces "Notebook, Notebook"
                        // is worse than one that announces it once, and the
                        // item itself carries the selected state.
                        contentDescription = null,
                    )
                },
                label = { NavLabel(labels(destination)) },
                modifier = Modifier.testTag(NavTags.tab(destination)),
                colors = ShortNavigationBarItemDefaults.colors(
                    // Unselected items are the app's secondary ink rather than
                    // Material's, which is the one place this bar overrides the
                    // component: `ink2` is the value measured against these
                    // surfaces in `check_contrast.py`.
                    unselectedIconColor = HealthTrail.colors.ink2,
                    unselectedTextColor = HealthTrail.colors.ink2,
                ),
            )
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
@Composable
private fun NavLabel(label: String) {
    val density = LocalDensity.current
    val capped = min(density.fontScale, NavLabelMaxScale) / density.fontScale
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
