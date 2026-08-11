package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

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
 * The bottom navigation. `DESIGN.md` section 7.
 *
 * **Four tabs, evenly spaced, with the active tab marked by a tonal pill behind
 * its icon.** `card` container, 24dp radius, 8dp inset from the screen edges.
 *
 * **The capture button is no longer in here, and that is the v4 change.** It
 * used to sit in a reserved column in the middle of this bar, which cost the
 * bar a quarter of its width and put a notch between two tabs. It is now a
 * corner FAB, [CaptureFab], and this bar is four equal tabs across the full
 * width. `DECISIONS.md` D76.
 *
 * **Selection is carried by three things, not one.** The tonal pill behind the
 * icon, the icon's own tint, and the label's weight. A person who cannot
 * separate the two blues still has the pill's shape and the bolder word, which
 * is what `DESIGN.md` 4.4 requires of every state that has a color.
 *
 * Labels are 11sp, one of only two exemptions from the 13sp floor, because each
 * is paired with an icon and a content description and neither ever carries
 * meaning alone.
 */
@Composable
fun BottomNav(
    current: Destination,
    onSelect: (Destination) -> Unit,
    labels: (Destination) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s, vertical = Space.s)
            .testTag(NavTags.BAR)
            .clip(Radius.navContainer)
            .background(HealthTrail.colors.card)
            .padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // **Four equal columns across the full width.** Equal weights rather
        // than SpaceEvenly, so each label centers in its own quarter and a long
        // label in one tab cannot push its neighbors around.
        Destination.entries.forEach { destination ->
            NavTab(
                destination = destination,
                label = labels(destination),
                selected = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.weight(1f).testTag(NavTags.tab(destination)),
            )
        }
    }
}

@Composable
private fun NavTab(
    destination: Destination,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, Color.Transparent)
    val ring by focusRingAlpha(interaction)
    Column(
        modifier = modifier
            .sizeIn(minWidth = Space.touchTarget, minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // **The active tab is marked by a tonal pill behind its icon**, per
        // `DESIGN.md` section 7. The pill replaces the 4dp dot that used to sit
        // under the icon, and it does the same job better: it is a larger shape
        // signal, so selection survives for somebody who cannot separate the two
        // blues, and it is what the reference file draws.
        Box(
            modifier = Modifier
                .clip(Radius.pill)
                .background(if (selected) colors.blueWash else Color.Transparent)
                .padding(horizontal = Space.sm, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            NavIcon(
                destination = destination,
                tint = if (selected) colors.blueDeep else colors.ink2,
            )
        }
        Spacer(Modifier.height(3.dp))
        // **The one place in the app where type stops growing, and it is
        // stated rather than quiet.**
        //
        // At font scale 2.0 "Notebook" broke mid-word into "Notebo" and "k" and
        // collided with the capture button. A single word cannot wrap, so the
        // only choices are to break it, to clip it, or to stop it growing, and
        // a word broken across two lines is less legible than the same word
        // slightly smaller. Found by looking at the phone with the system font
        // at maximum, which is the pass `DESIGN.md` section 9 requires.
        //
        // **Capped, not fixed.** It still grows with the person's setting, up
        // to [NavLabelMaxScale], so somebody who has raised their font still
        // gets a larger label. Beyond that the label holds and the icon, the
        // position, and the content description carry it, which is the same
        // set of things a person navigates by after two weeks.
        //
        // Nothing else in the app is capped. A four item bar with a fixed
        // clearance for the capture button has a width budget nothing else has.
        val density = LocalDensity.current
        val capped = min(density.fontScale, NavLabelMaxScale) / density.fontScale
        Text(
            text = label,
            style = HealthTrail.type.navLabel.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = HealthTrail.type.navLabel.fontSize * capped,
                lineHeight = HealthTrail.type.navLabel.lineHeight * capped,
            ),
            color = if (selected) colors.blueDeep else colors.ink2,
            textAlign = TextAlign.Center,
            // Two lines rather than one, because the longest word in the
            // longest language will not fit on one at any size, and a second
            // line is better than an ellipsis on a word somebody is navigating
            // by. **The ellipsis is the floor under that, not a change to
            // it**: at two lines it is unreachable in all four languages, and
            // if a fifth ever reaches it, a mark is better than a word cut
            // off with nothing to show for it.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
