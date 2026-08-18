package com.kamsiob.healthtrail.ui.v4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

/**
 * Where somebody is in a staged conversation. `DESIGN.md` section 7, and law 3.
 *
 * **It says where you are and never how much is left to do.** Rule 13 rules out
 * progress meters on the person's own diligence, and three dots with one filled
 * is the line between orientation and a scoreboard: it answers "am I nearly
 * done" without ever implying that finishing is required. **Every stage of every
 * flow this appears in can be skipped and saved from.**
 *
 * **The current dot is wider rather than a different color.** Color alone
 * carries no meaning, per section 9, and a shape difference survives grayscale,
 * every color vision difference, and a phone in sunlight.
 *
 * **It is one node to a screen reader, not three.** A reader that stopped on
 * each dot would say "dot, dot, dot" and mean nothing; it announces which stage
 * of how many, in words, once.
 *
 * **When not to use it.** On anything that is not a conversation the person
 * walks through in order. A screen with sections is not a staged flow, and dots
 * over sections would be the app inventing a sequence somebody did not agree to.
 */
@Composable
fun StageDots(
    count: Int,
    current: Int,
    /** Said by a reader in place of the dots, already composed by the caller. */
    description: String,
    modifier: Modifier = Modifier,
) {
    require(count in 2..5) {
        // More than five and it stops reading as a short conversation and
        // starts reading as a form with a progress bar, which is the thing
        // law 3 exists to prevent.
        "A staged flow has two to five stages, not $count. DESIGN.md section 7."
    }

    Row(
        modifier = modifier
            .height(Space.touchTarget)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        repeat(count) { index -> Dot(filled = index == current) }
    }
}

@Composable
private fun Dot(filled: Boolean) {
    val motion = LocalMotion.current

    // The width moves rather than appearing, so the dots read as one thing
    // travelling rather than as three lights switching.
    val width by animateFloatAsState(
        targetValue = if (filled) FilledWidth else DotSize,
        animationSpec = motion.standard(),
        label = "stageDot",
    )

    // **Material's surface rather than a clip over a background.** The shape,
    // the color and the content color are its own. It stays a drawing rather
    // than becoming a progress indicator: law 3 rules a progress bar out, and
    // this says which part of a short conversation you are in, not how far
    // along you are.
    Surface(
        modifier = Modifier
            .width(width.dp)
            .height(DotSize.dp)
            // The row above says the whole thing in words. A dot is a mark.
            .clearAndSetSemantics { },
        shape = CircleShape,
        color = if (filled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
    ) {}
}

private const val DotSize = 7f
private const val FilledWidth = 20f
