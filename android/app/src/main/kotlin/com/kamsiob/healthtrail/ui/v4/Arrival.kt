package com.kamsiob.healthtrail.ui.v4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import kotlinx.coroutines.delay

/**
 * A screen's parts arriving, rather than being there already. `docs/V4.md` 6.1
 * item 8, and it is the half of the bar that had been skipped every session.
 *
 * **Nothing in this app moved on arrival.** Every screen cut into place fully
 * formed, which is the thing item 8 calls cheap: the difference between an
 * interface that was built and one that was assembled is almost entirely in the
 * quarter second after a screen opens.
 *
 * **A rise and a fade, and nothing else.** No scale, no slide from the side, no
 * spring that overshoots. `DESIGN.md` 10 reserves overshoot for three moments
 * and a screen opening is not one of them; this app is used at two in the
 * morning by somebody tired and it should never be the loudest thing in the
 * room.
 *
 * **Staggered, and the stagger is the point.** Everything moving together is
 * one block sliding, which reads as a transition. A short offset per item is
 * what makes a page read as being laid down: the lead lands, then what is under
 * it. The offset is small and the whole thing is over in well under half a
 * second.
 *
 * **Only the first screenful.** Past [STAGGERED] the delay is zero, so a list
 * of six hundred entries does not animate its way down the screen as somebody
 * scrolls, and the wait before the last visible item lands never grows with the
 * length of the list.
 *
 * **Off under reduced motion**, and it is off rather than shortened: the spec
 * comes from [LocalMotion] and the offset is zero, so the parts are simply
 * there. Rule 19, and it is verified with the setting actually on rather than
 * by reading this.
 */
@Composable
fun Modifier.arrives(index: Int = 0): Modifier = composed {
    val motion = LocalMotion.current
    if (motion.isReduced) return@composed this

    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // The stagger, in the order the page declares its parts. Past the first
        // screenful there is no wait at all.
        delay(motion.arrivalStaggerMillis * index.coerceAtMost(STAGGERED).toLong())
        landed = true
    }

    val progress by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = motion.standard(),
        label = "arrival",
    )
    val rise = with(LocalDensity.current) { motion.arrivalRise.toPx() }
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * rise
    }
}

/**
 * Why there is no per-item version of this, and why there must not be.
 *
 * **It was tried and it broke two screens.** Wrapping every lazy item in a
 * container so each could arrive on its own changed the scope its content is
 * composed in: a `Box` laid an item's children on top of each other and the
 * restore screen drew its unlock button over its own password field, and a
 * `Column` fixed the drawing but still put a second layout between a caller and
 * the list, which broke `performScrollTo` and `performScrollToNode` on the
 * document form and the capture form. `docs/TRAPS.md` names the second one
 * already: a weighted child of a column with no bounded height measures to
 * zero.
 *
 * **And it was wrong even where it worked.** A lazy list composes a row when it
 * scrolls into view, so a per-item arrival animates every row of a six hundred
 * entry trail as the thumb moves. That is not a screen arriving, it is a list
 * that will not settle.
 *
 * **The page arrives as one thing**, which is what item 8 is about: the quarter
 * second after a screen opens. [arrives] goes on the list itself, in [Page].
 */
private const val ARRIVAL_IS_PER_PAGE = true

/**
 * How many steps of stagger there are between a page's own parts.
 *
 * The list arrives as one thing and the parts above it, the top bar and the
 * band, arrive fractionally after, so the page reads as being laid down rather
 * than as one block sliding.
 */
private const val STAGGERED = 3
