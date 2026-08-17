package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
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
 * Onboarding that is always there, instead of onboarding that happened once.
 *
 * **The owner asked for it by name, #379:** a small button beside the section
 * name on every page, opening a panel that says what this page is for and how
 * to use it. His words, "always on demand onboarding".
 *
 * **It is the honest answer to a real problem this app has.** A stranger could
 * not tell what Projects was or what belonged in the Notebook, and the fix
 * that does not clutter every screen with explanation is to put the
 * explanation one tap away and leave the screen alone. Rule 20: the
 * complexity lives in the code, and where it cannot, it lives behind a door
 * rather than on the surface.
 *
 * **Never a badge, never a dot, never a nag.** It does not know whether it has
 * been read and it never asks to be. A person who never taps it loses nothing,
 * which is what separates this from the tours that interrupt.
 */
@Composable
fun TipsButton(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.springy(),
        label = "tipsPress",
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(Space.touchTarget)
            .clip(CircleShape)
            .background(colors.goldWash)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = strings["tips.open"],
                onClick = onOpen,
            )
            .semantics { contentDescription = strings["tips.open"] }
            .testTag(TipsTags.BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        // **A drawn mark rather than a letter**, because "?" reads as help
        // with the app and this is help with the page. The lamp is the one
        // shape the whole culture already reads as "here is the idea".
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(Space.tipsBulb)
                    .clip(CircleShape)
                    .background(colors.goldInk),
            )
            Spacer(Modifier.height(Space.tipsGap))
            Box(
                modifier = Modifier
                    .size(width = Space.tipsBase, height = Space.tipsBaseThickness)
                    .clip(Radius.pill)
                    .background(colors.goldInk),
            )
        }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(Space.tipsBulbLarge)
                                .clip(CircleShape)
                                .background(colors.goldInk),
                        )
                        Spacer(Modifier.height(Space.tipsGap))
                        Box(
                            modifier = Modifier
                                .size(width = Space.tipsBaseLarge, height = Space.tipsBaseThicknessLarge)
                                .clip(Radius.pill)
                                .background(colors.goldInk),
                        )
                    }
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
