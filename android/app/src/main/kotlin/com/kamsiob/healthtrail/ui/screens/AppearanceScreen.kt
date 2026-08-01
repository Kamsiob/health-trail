package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.ThemeChoice

object AppearanceTags {
    const val ROOT = "appearance_root"
    fun option(choice: ThemeChoice) = "appearance_option_${choice.name}"
}

/**
 * Appearance. The first real thing in More.
 *
 * **Three choices, and the default is to follow the phone.** Overriding what a
 * person has already told their device is presumptuous. The setting is for the
 * case where they want this one app to differ, which is a real case: reading
 * long text in a dark room, or a bright corridor at three in the morning, is
 * the situation this app exists for.
 *
 * **Composed, not invented**, per `DESIGN.md` section 10.2. The group header is
 * the same mono eyebrow the notebook uses, the rows are the notebook's list row
 * with its press surface and focus ring, and the selected marker is the dot
 * the bottom navigation already uses to say "this one". Nothing here is new.
 *
 * **The selected row announces as selected**, through `selected` in its
 * semantics rather than only through the dot. A screen reader user who cannot
 * see the marker gets the same information, which is the difference between a
 * label and a state.
 */
@Composable
fun AppearanceScreen(
    choice: ThemeChoice,
    onChoose: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Anything the surrounding destination wants below the theme group.
     *
     * More uses it to say what else is coming, which is the difference between
     * a screen that ends and a screen that stops.
     */
    footer: @Composable () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = modifier.fillMaxSize().testTag(AppearanceTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal),
        ) {
            Spacer(Modifier.height(Space.l))
            Text(
                text = strings["appearance.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["appearance.subtitle"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.l))

            GroupHeader(labelKey = "appearance.group")
            Spacer(Modifier.height(Space.m))

            ThemeChoice.entries.forEachIndexed { index, option ->
                Option(
                    choice = option,
                    selected = option == choice,
                    onClick = { onChoose(option) },
                )
                if (index < ThemeChoice.entries.lastIndex) {
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            footer()

            // Clearance for the capture button, which overlaps the navigation
            // bar and would otherwise sit on the last row.
            Spacer(Modifier.height(Space.xxl + Space.l))
        }
    }
}

@Composable
private fun Option(choice: ThemeChoice, selected: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                // The row's own surface is the answer to the touch, per section
                // 5.14. A ripple over it would be a second, louder one.
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            // Selection is state, not decoration. Without this the dot is the
            // only carrier and a reader user gets three identical rows.
            .semantics { this.selected = selected }
            .testTag(AppearanceTags.option(choice))
            .padding(horizontal = Space.cardPadding, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings[labelKey(choice)],
                style = HealthTrail.type.label,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings[detailKey(choice)],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }

        if (selected) {
            Spacer(Modifier.width(Space.sm))
            // The bottom navigation's selected dot, which is already this
            // app's way of saying "this one". A checkmark would be a new path
            // for a job an existing shape does, and section 10.2 says compose
            // rather than add.
            Box(
                modifier = Modifier
                    .size(Space.s)
                    .clip(CircleShape)
                    .background(colors.blueDeep),
            )
        }
    }
}

private fun labelKey(choice: ThemeChoice) = when (choice) {
    ThemeChoice.FOLLOW_SYSTEM -> "appearance.system.label"
    ThemeChoice.LIGHT -> "appearance.light.label"
    ThemeChoice.DARK -> "appearance.dark.label"
}

private fun detailKey(choice: ThemeChoice) = when (choice) {
    ThemeChoice.FOLLOW_SYSTEM -> "appearance.system.detail"
    ThemeChoice.LIGHT -> "appearance.light.detail"
    ThemeChoice.DARK -> "appearance.dark.detail"
}
