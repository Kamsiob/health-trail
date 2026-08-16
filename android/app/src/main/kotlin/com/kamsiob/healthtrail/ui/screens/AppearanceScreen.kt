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
import com.kamsiob.healthtrail.ui.components.TipsButton
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.tipForDestination
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.kamsiob.healthtrail.ui.components.ChoiceRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.Hairline
import com.kamsiob.healthtrail.ui.components.fabScrollClearance
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
    /**
     * What the surrounding destination wants **above** the theme group.
     *
     * More uses it for its destinations, which are what a person opens More to
     * reach. The theme sits under them because it is changed once and the
     * destinations are used weekly.
     */
    header: @Composable () -> Unit = {},
    /** The destination's own title, so More says More rather than Appearance. */
    titleKey: String = "appearance.title",
    subtitleKey: String = "appearance.subtitle",
    /** Which page's onboarding this screen offers, or null for none. #379. */
    tipsKey: String? = null,
    tab: @Composable () -> Unit = {},
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
            Spacer(Modifier.height(Space.sm))
            tab()
            Spacer(Modifier.height(Space.s))
            // **Page onboarding beside the title**, #379, and only where the
            // caller named a page for it. Appearance on its own is not a tab
            // root and gets none.
            var showTips by remember { mutableStateOf(false) }
            if (showTips && tipsKey != null) {
                TipsSheet(
                    tip = tipForDestination(tipsKey),
                    onDismiss = { showTips = false },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = strings[titleKey],
                    style = HealthTrail.type.displayM,
                    color = colors.ink,
                )
                if (tipsKey != null) {
                    TipsButton(onOpen = { showTips = true })
                }
            }
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings[subtitleKey],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            header()

            Spacer(Modifier.height(Space.sectionGap))
            GroupHeader(labelKey = "appearance.group")
            Spacer(Modifier.height(Space.m))

            // **One grouped surface, not three cards.** Three separate cards
            // for one question is a fourth pattern on a screen that already has
            // grouped rows above it, and law 2 has no costume for "card that is
            // one of three answers". It is a radio group, so it is one group.
            //
            // It is not chips, which law 2 gives to short answers: each option
            // carries a line explaining what it does, and a chip that needs a
            // sentence under it is a row.
            GroupedSurface {
                ThemeChoice.entries.forEachIndexed { index, option ->
                    Option(
                        choice = option,
                        selected = option == choice,
                        onClick = { onChoose(option) },
                        isLast = index == ThemeChoice.entries.lastIndex,
                    )
                }
            }

            footer()

            // Clearance for the corner FAB, from the token rather than from
            // arithmetic on the screen. D81.
            Spacer(Modifier.height(fabScrollClearance))
        }
    }
}

@Composable
private fun Option(choice: ThemeChoice, selected: Boolean, onClick: () -> Unit, isLast: Boolean) {
    val strings = LocalStrings.current
    ChoiceRow(
        label = strings[labelKey(choice)],
        detail = strings[detailKey(choice)],
        selected = selected,
        onClick = onClick,
        isLast = isLast,
        testTag = AppearanceTags.option(choice),
    )
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
