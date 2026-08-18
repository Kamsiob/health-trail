package com.kamsiob.healthtrail.ui.v4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
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
 * The same, for every item a page declares, without each page asking.
 *
 * **A page describes its content as lazy items and the arrival belongs to the
 * language rather than to the page.** Wrapping this by hand at every call site
 * is how a treatment ends up on eleven screens out of eighty six, which is what
 * happened to the press state before [opensOnTap]. The wrapper counts the items
 * as they are declared, so the order on screen is the order they land in.
 *
 * Delegation rather than reimplementation: everything a caller can put in a
 * lazy list keeps working, and only the two forms that produce visible items
 * are intercepted.
 */
internal class ArrivingScope(private val inner: LazyListScope) : LazyListScope by inner {

    /** How many items have been declared, which is the order they arrive in. */
    private var declared = 0

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        val index = declared++
        inner.item(key, contentType) {
            val scope = this
            // **A `Column`, and a `Box` here was a defect.** A page's `item`
            // may emit several composables side by side and rely on the lazy
            // list stacking them; a `Box` lays them on top of each other
            // instead, so the restore screen drew its unlock button over its
            // own password field. Seen on the phone the moment the seed
            // stalled on it.
            Column(modifier = Modifier.arrives(index)) { with(scope) { content() } }
        }
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        val first = declared
        declared += count
        inner.items(count, key, contentType) { index ->
            val scope = this
            Column(modifier = Modifier.arrives(first + index)) { with(scope) { itemContent(index) } }
        }
    }
}

/**
 * How many parts are staggered before the rest simply land.
 *
 * About a screenful. Past this the delay stops growing, so the bottom of a long
 * list is never waiting on the arithmetic of everything above it.
 */
private const val STAGGERED = 7
