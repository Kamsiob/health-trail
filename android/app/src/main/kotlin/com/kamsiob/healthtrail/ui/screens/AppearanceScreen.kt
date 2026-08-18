package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.components.TipsButton
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.v4.fabScrollClearance
import com.kamsiob.healthtrail.ui.components.tipForDestination
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import com.kamsiob.healthtrail.ui.v4.ChoiceRow
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object AppearanceTags {
    const val ROOT = "appearance_root"
    fun option(choice: ThemeChoice) = "appearance_option_${choice.name}"
}

/**
 * Appearance. The first real thing in More. Rewritten onto `ui/v4`, #386.
 *
 * **Three choices, and the default is to follow the phone.** Overriding what a
 * person has already told their device is presumptuous. The setting is for the
 * case where they want this one app to differ, which is a real case: reading
 * long text in a dark room, or a bright corridor at three in the morning, is
 * the situation this app exists for.
 *
 * **One block, not three.** Three separate blocks for one question would be
 * three answers pretending to be three subjects. It is a radio group, so it is
 * one group, and each row says what it does under its own name.
 *
 * **The chosen row announces as chosen**, through `selected` in its semantics
 * rather than only through the mark.
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
    /**
     * The way back, or null on a destination.
     *
     * **More is one of the four places the bottom bar goes**, so it is where
     * back lands rather than something to come back from. Appearance opened on
     * its own from anywhere else passes one.
     */
    onBack: (() -> Unit)? = null,
    backLabelKey: String = "section.back.more",
) {
    val strings = LocalStrings.current
    var showTips by remember { mutableStateOf(false) }

    if (showTips && tipsKey != null) {
        TipsSheet(tip = tipForDestination(tipsKey), onDismiss = { showTips = false })
    }

    Page(
        title = strings[titleKey],
        subtitle = strings[subtitleKey],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        // **The lamp in its gold circle, which is where it is everywhere
        // else.** `m3v4-1` draws it that way in the top corner, and every
        // section page gets it through the page's own corner. A destination
        // built on this frame drew a bare bulb instead, so the same control
        // had two costumes depending on which screen you were on. Seen on the
        // phone, rule 21.
        actions = tipsKey?.let {
            {
                TipsButton(onOpen = { showTips = true })
            }
        },
        modifier = modifier.testTag(AppearanceTags.ROOT),
    ) {
        item { header() }

        labeledBlock(
            label = strings["appearance.group"],
            rows = ThemeChoice.entries.map { option ->
                {
                    ChoiceRow(
                        label = strings[labelKey(option)],
                        detail = strings[detailKey(option)],
                        selected = option == choice,
                        onClick = { onChoose(option) },
                        modifier = Modifier.testTag(AppearanceTags.option(option)),
                    )
                }
            },
        )

        item { footer() }

        // Clearance for the corner capture button, from the token rather than
        // from arithmetic on the screen. D81.
        item { Spacer(Modifier.height(fabScrollClearance)) }
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
