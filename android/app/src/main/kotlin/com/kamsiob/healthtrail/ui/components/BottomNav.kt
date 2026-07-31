package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destination ->
                NavTab(
                    label = labels(destination),
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.testTag(NavTags.tab(destination)),
                )
            }
        }

        // Overlaps the container's top edge by 16dp, per section 5.5.
        Box(
            modifier = Modifier
                .offset(y = (-16).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.blaze)
                .clickable(role = Role.Button, onClick = onCapture)
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
                    .background(colors.onBlaze),
            )
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 22.dp)
                    .clip(Radius.pill)
                    .background(colors.onBlaze),
            )
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .sizeIn(minWidth = Space.touchTarget, minHeight = Space.touchTarget)
            .clip(Radius.tile)
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
        // The icon slot. Icons arrive with the icon set; until then the dot
        // carries the active state as a shape, so color is not the only signal.
        Box(
            modifier = Modifier
                .size(20.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) colors.blueDeep else Color.Transparent),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = HealthTrail.type.navLabel.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (selected) colors.blueDeep else colors.ink2,
            textAlign = TextAlign.Center,
        )
    }
}
