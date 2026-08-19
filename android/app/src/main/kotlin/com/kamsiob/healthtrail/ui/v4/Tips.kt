package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space

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
 *
 * **Written fresh on Material's own, the old file deleted.** The header tile
 * was a `Box` with a clip and a background, each bullet was a `Box` with a clip
 * and a background, and every line of type named a rung of the second ladder
 * and a color by hand. `Surface` is the tile and the bullet; the scheme and the
 * type roles are the rest, so this panel changes when the theme does rather
 * than when somebody remembers it exists.
 *
 * **The lamp inside is the same lamp on the button.** It was drawn twice at two
 * sizes and the two could drift; there is one Material Symbol now and the tile
 * carries the gold pair the button carries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSheet(
    tip: Tip,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val sheet = rememberSheet()

    Sheet(
        onDismiss = onDismiss,
        state = sheet,
        modifier = Modifier.testTag(TipsTags.SHEET),
    ) {
        // **Top buffer, and it had none at all.** #460, reported by the owner
        // on 2026-08-19: the panel sits clipped up against the top edge and
        // needs more room above it.
        //
        // It was a hand rolled `Column` with horizontal and bottom padding and
        // **no top padding**, it did not use `SheetBody`, which is the component
        // that exists to own a sheet's margins and insets, and `Sheet` passes
        // `dragHandle = null`, so nothing anywhere contributed space above the
        // title. The lamp tile sat against the sheet's own edge.
        //
        // The top now matches the bottom rather than merely existing, because a
        // panel with more air below than above reads as having slipped upward.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(top = Space.xl, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                Surface(
                    modifier = Modifier.size(Space.tipsHeaderTile),
                    shape = MaterialTheme.shapes.small,
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Symbols.tips),
                            contentDescription = null,
                        )
                    }
                }
                Text(
                    text = strings[tip.titleKey],
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                )
            }

            Text(
                text = strings[tip.bodyKey],
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )

            if (tip.pointKeys.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    tip.pointKeys.forEach { key ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // The bullet is the gold ink the lamp is drawn in,
                            // so the panel reads as one thing rather than a
                            // gold header over a gray list.
                            Surface(
                                modifier = Modifier
                                    .padding(top = Space.tipsPointDotTop)
                                    .size(Space.tipsPointDot),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = scheme.secondary,
                            ) {}
                            Text(
                                text = strings[key],
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

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
