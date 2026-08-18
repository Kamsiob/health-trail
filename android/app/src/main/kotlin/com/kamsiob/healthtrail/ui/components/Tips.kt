package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object TipsTags {
    const val BUTTON = "tips_button"
    const val SHEET = "tips_sheet"
    const val CLOSE = "tips_close"
}

/**
 * The lamp that opens what a page is for. D167 and #379.
 *
 * **Material's tonal icon button over Google's own lamp**, D196. This was a
 * `Box` with a clip, a background, a hand rolled press scale and a bulb built
 * out of a circle and a rounded bar: a drawing of a control rather than one.
 *
 * **The gold comes from the scheme.** `secondaryContainer` is this theme's gold
 * wash and `onSecondaryContainer` its gold ink, which is exactly the pair D167
 * asked for, so nothing here names a color.
 *
 * **A lamp rather than a question mark**, because "?" reads as help with the app
 * and this is help with the page. The lamp is the one shape the whole culture
 * already reads as "here is the idea".
 */
@Composable
fun TipsButton(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = onOpen,
        modifier = modifier
            .semantics { contentDescription = strings["tips.open"] }
            .testTag(TipsTags.BUTTON),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        ),
    ) {
        Icon(painter = painterResource(Symbols.tips), contentDescription = null)
    }
}

/**
 * What one page is for, in the app's own voice.
 *
 * **Three parts and no more**: what this place holds, how it is used, and the
 * one thing worth knowing that nobody guesses. Longer than that and it is a
 * manual, which nobody standing in a hallway will read.
 */
data class Tip(
    val titleKey: String,
    val bodyKey: String,
    val pointKeys: List<String> = emptyList(),
)

/**
 * The panel itself: a sheet, because it is a passing glance rather than a
 * destination, and back closes it the way back closes everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSheet(
    tip: Tip,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheet = rememberSheet()

    Sheet(
        onDismiss = onDismiss,
        state = sheet,
        modifier = Modifier.testTag(TipsTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Space.tipsHeaderTile)
                        .clip(Radius.tile)
                        .background(colors.goldWash),
                    contentAlignment = Alignment.Center,
                ) {
                    // **The same lamp the button carries**, which is Google's
                    // own. It was a circle and a rounded bar stacked in a
                    // column, drawn twice at two sizes, and the two could drift.
                    Icon(
                        painter = painterResource(Symbols.tips),
                        contentDescription = null,
                        tint = colors.goldInk,
                    )
                }
                Spacer(Modifier.width(Space.sm))
                Text(
                    text = strings[tip.titleKey],
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                )
            }

            Spacer(Modifier.height(Space.m))
            Text(
                text = strings[tip.bodyKey],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )

            if (tip.pointKeys.isNotEmpty()) {
                Spacer(Modifier.height(Space.l))
                tip.pointKeys.forEach { key ->
                    Row(
                        modifier = Modifier.padding(bottom = Space.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = Space.tipsPointDotTop)
                                .size(Space.tipsPointDot)
                                .clip(CircleShape)
                                .background(colors.goldInk),
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(
                            text = strings[key],
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["tips.close"],
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TipsTags.CLOSE),
            )
        }
    }
}

/**
 * The tip for one section, built from its own key prefix.
 *
 * **Derived rather than listed**, so a section added later fails loudly at a
 * missing key rather than silently having no tip. The same argument the
 * notebook's counts use, D133.
 */
fun tipFor(section: Repository.Section): Tip {
    val slug = section.name.lowercase()
    return Tip(
        titleKey = "tips.$slug.title",
        bodyKey = "tips.$slug.body",
        pointKeys = listOf("tips.$slug.point1", "tips.$slug.point2"),
    )
}

/** The tips for the four destinations, which are not sections. */
fun tipForDestination(key: String): Tip = Tip(
    titleKey = "tips.$key.title",
    bodyKey = "tips.$key.body",
    pointKeys = listOf("tips.$key.point1", "tips.$key.point2"),
)
