package com.kamsiob.healthtrail.ui.components

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
import androidx.compose.ui.unit.Dp
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * Content arriving rather than appearing. D168.
 *
 * **This is the difference between a screen that is correct and one that is
 * considered.** A list that is simply there has no rhythm; a list whose rows
 * arrive a beat apart gives the eye an order to read them in, and the research
 * on staggered entrance is that comprehension measurably improves because the
 * reader is handed a sequence rather than a wall.
 *
 * **The offset is small on purpose.** Thirty five milliseconds between rows is
 * felt and not watched: eight rows finish inside a third of a second, so a
 * person who opens a section and immediately reaches for the fourth row is
 * never waiting on the animation. Anything slower turns a list into a
 * performance, which is the failure mode this pattern usually has.
 *
 * **It rises as it fades**, a few dp, because pure opacity reads as a screen
 * loading and a small translation reads as paper being set down.
 *
 * **Reduced motion draws it instantly.** The stagger, the fade and the rise
 * all collapse, which is correct: the information was never in the motion.
 *
 * **It runs once per composition.** Scrolling a long list does not re-animate
 * rows coming back into view, which would be an interface that will not settle.
 */
fun Modifier.arrivesInOrder(
    index: Int,
    rise: Dp = Space.arrivalRise,
): Modifier = composed {
    val motion = LocalMotion.current
    var arrived by remember { mutableStateOf(motion.isReduced) }

    LaunchedEffect(Unit) {
        if (!motion.isReduced) {
            // Capped, so a list of forty does not make the fortieth row wait
            // a second and a half for its turn.
            delay((index.coerceAtMost(MAX_STAGGERED) * STAGGER_MILLIS).toLong())
            arrived = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = motion.settle(),
        label = "arrival",
    )
    val risePx = with(LocalDensity.current) { rise.toPx() }

    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * risePx
    }
}

/** How far apart two rows arrive. Felt, not watched. */
private const val STAGGER_MILLIS = 35

/**
 * After this many rows the stagger stops growing.
 *
 * **A long list must not make its tail wait.** Twelve rows is the notebook's
 * own length and comfortably inside half a second; past that everything
 * arrives together, which nobody notices because it is below the fold anyway.
 */
private const val MAX_STAGGERED = 12
