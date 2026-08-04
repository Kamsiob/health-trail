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

/** The capture button itself, per section 5.5. */
private val CaptureSize = 56.dp

/**
 * How far the navigation label is allowed to grow with the system font.
 *
 * 1.4 rather than unbounded. Everything else in the app scales without limit;
 * this bar cannot, because four labels and a fixed clearance for the capture
 * button share one row.
 */
private const val NavLabelMaxScale = 1.4f

/**
 * The column the navigation leaves empty for the capture button.
 *
 * The button plus 8dp of air on each side, so it never sits shoulder to
 * shoulder with a label.
 */
private val CaptureClearance = CaptureSize + 16.dp

/**
 * The bottom navigation, and the capture button that sits in it.
 *
 * `card` container, 24dp radius, 8dp inset from the screen edges, elevated. The
 * active state is `blue_deep` on both icon and label plus the label at weight
 * 700, so color is never the only signal.
 *
 * **The capture button is the one element whose color does not shift between
 * themes.** It is the single way data enters the app, it sits on every screen in
 * all four tabs, it never moves, never hides on scroll, and never changes color,
 * because it has to be findable without thought. Its glyph is dark rather than
 * white, for the contrast reason in section 2.4.
 *
 * Labels are 11sp, which is one of the only two exemptions from the 13sp floor,
 * because each is paired with an icon and a content description and neither ever
 * carries meaning alone.
 */
@Composable
fun BottomNav(
    current: Destination,
    onSelect: (Destination) -> Unit,
    onCapture: () -> Unit,
    labels: (Destination) -> String,
    captureDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s, vertical = Space.s)
            .testTag(NavTags.BAR),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.navContainer)
                .background(colors.card)
                .padding(vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // **The middle is reserved for the capture button rather than
            // shared with it.** The four tabs used to spread evenly across the
            // whole width, which put the seam between Notebook and Projects
            // exactly where the button sits, so it crowded both of them. Two
            // tabs, a gap the width of the button, two tabs. The gap is a real
            // column in the layout, so no label can ever grow into it: at the
            // largest font scale the tabs get narrower and the button keeps its
            // clearance.
            //
            // Equal weights rather than SpaceEvenly, so each label centers in
            // its own quarter and the two halves stay symmetrical.
            Destination.entries.take(2).forEach { destination ->
                NavTab(
                    destination = destination,
                    label = labels(destination),
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f).testTag(NavTags.tab(destination)),
                )
            }

            Spacer(Modifier.width(CaptureClearance))

            Destination.entries.drop(2).forEach { destination ->
                NavTab(
                    destination = destination,
                    label = labels(destination),
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f).testTag(NavTags.tab(destination)),
                )
            }
        }

        // Overlaps the container's top edge by 16dp, per section 5.5.
        //
        // It answers a finger like everything else. Section 5.5 says it never
        // changes color, which is about its resting state: it is always gold,
        // on every screen. A press is not a change of color, it is the control
        // saying it heard you, and the one thing data enters through must say
        // that most of all.
        val captureInteraction = remember { MutableInteractionSource() }
        val captureSurface by pressedSurface(captureInteraction, colors.gold)
        Box(
            modifier = Modifier
                .offset(y = (-16).dp)
                .size(CaptureSize)
                .clip(CircleShape)
                .background(captureSurface)
                .clickable(
                    interactionSource = captureInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onCapture,
                )
                .semantics { contentDescription = captureDescription }
                .testTag(NavTags.CAPTURE),
            contentAlignment = Alignment.Center,
        ) {
            // A plus, drawn from two bars rather than a glyph, so it needs no
            // icon font and cannot fall back to a box in any language.
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .clip(Radius.pill)
                    .background(colors.onGold),
            )
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 22.dp)
                    .clip(Radius.pill)
                    .background(colors.onGold),
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
        // **The icon, which was a placeholder dot until 2026-08-03.** The slot
        // was always here and the dot stood in it, carrying the selected state
        // as a shape so that color was never the only signal.
        //
        // The icon does not replace that job, it shares it: selection is
        // carried by the icon's tint, the label's weight, and the dot beneath
        // it together, so somebody who cannot separate the two blues still has
        // two other signals. Section 2.2.
        NavIcon(
            destination = destination,
            tint = if (selected) colors.blueDeep else colors.ink2,
        )
        Spacer(Modifier.height(3.dp))
        // The selected dot, now under the icon rather than instead of it. Four
        // dp, because it is a mark and not a control.
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) colors.blueDeep else Color.Transparent),
        )
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
            // by.
            maxLines = 2,
        )
    }
}
