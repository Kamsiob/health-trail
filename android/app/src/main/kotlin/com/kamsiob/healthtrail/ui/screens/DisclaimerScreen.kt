package com.kamsiob.healthtrail.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.R
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.SupportButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object DisclaimerTags {
    const val ROOT = "disclaimer_root"
    const val ACCEPT = "disclaimer_accept"
    const val SUPPORT = "disclaimer_support"
    fun block(index: Int) = "disclaimer_block_$index"
}

/**
 * The first thing anyone sees, and a gate rather than a notice.
 *
 * No part of the app is reachable until it is explicitly accepted, the
 * acceptance is recorded with a timestamp so it is never shown twice, and there
 * is no version of the app that skips it. `PROJECT-DELTAS.md` section 4.
 *
 * **The wording is fixed** by `DESIGN.md` section 7 and carried verbatim in the
 * message catalogs. It is not paraphrased, shortened, or softened here, and the
 * same substance appears on the About screen and in the store listing.
 *
 * **Structure is part of the wording, not decoration on top of it.** The first
 * version of this screen was one heading and two long paragraphs, and it was
 * skipped rather than read, which is a failure of the screen rather than of the
 * reader. The three things a person actually has to take away now sit in three
 * cards, each with its own heading, so someone can read one block, look up at a
 * nurse, and come back without losing their place.
 *
 * Nothing was cut to get there. Everything the old wording disclosed is still
 * disclosed, which `DESIGN.md` section 7 states as a constraint on any future
 * edit to this copy.
 *
 * Composed from the mark at 44dp, Display L, Body L, the card from section 5.3,
 * Display S, Body M, and one filled button. Nothing new was introduced.
 *
 * **On states.** This screen has fixed content, so it has no empty or many item
 * state. What it does have to survive is the largest system font size and the
 * longest translation, which is why the text column scrolls rather than assuming
 * it fits. The mark is decorative and is hidden from the screen reader: the
 * heading immediately below it already says where the person is, and a reader
 * announcing a logo before every title is noise.
 */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DisclaimerTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
            verticalArrangement = Arrangement.Top,
        ) {
            // The text scrolls, the action does not. This is what keeps the
            // accept button in the lower half of the screen, where a person
            // holding a large phone in one hand can reach it, while still
            // letting the wording grow to any font size or translation length
            // without pushing the action off the bottom. Looking at the built
            // screen on a device is what surfaced it: with the whole column
            // scrolling, the button sat in the upper third.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_mark),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.gold),
                    modifier = Modifier
                        .size(44.dp)
                        .clearAndSetSemantics { },
                )

                Spacer(Modifier.height(Space.l))

                Text(
                    text = strings["disclaimer.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(Modifier.height(Space.sm))

                Text(
                    text = strings["disclaimer.lead"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                // The three things a person has to take away. Cards rather than
                // paragraphs, because a card is where this app puts a thing that
                // stands on its own, and each of these does.
                (1..BLOCK_COUNT).forEach { index ->
                    DisclaimerBlock(
                        title = strings["disclaimer.block.$index.title"],
                        body = strings["disclaimer.block.$index.body"],
                        modifier = Modifier.testTag(DisclaimerTags.block(index)),
                    )
                    if (index != BLOCK_COUNT) Spacer(Modifier.height(Space.cardGap))
                }

                // **The offer sits after the sentence that says the app asks
                // for nothing, and never before it.** Order is the whole
                // difference between an offer and a pitch. The gate is fully
                // passable without noticing this, and it is the last thing on
                // the scroll rather than something between the person and the
                // button they came for.
                Spacer(Modifier.height(Space.l))

                Text(
                    text = strings["disclaimer.support.note"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.sm))

                SupportButton(
                    label = strings["disclaimer.support"],
                    onClick = {
                        // Leaves the app, which makes this the first outbound
                        // link in the product. Nothing is sent and nothing is
                        // recorded about the tap.
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()),
                        )
                    },
                    // **Sized to its label, because it is an offer.**
                    // `SupportButton`'s own comment sets the bar: it must
                    // never read as a request, and both screens are fully
                    // passable without noticing it. Drawn the full width of
                    // the screen it was the second loudest thing on the gate,
                    // directly under the sentence saying the app is free and
                    // asks for nothing, which is the sentence it undoes. D59
                    // put it after the reassurance for that reason and the
                    // width was working against the same argument. #204.
                    modifier = Modifier.testTag(DisclaimerTags.SUPPORT),
                )
            }

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["disclaimer.accept"],
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DisclaimerTags.ACCEPT),
            )
        }
    }
}

/**
 * How many blocks the screen carries.
 *
 * Held here rather than counted from the catalog, so a translation that is
 * missing one fails loudly at that key instead of quietly rendering a shorter
 * disclaimer than English does. A disclaimer that discloses less in one language
 * than in another is the one failure mode this screen cannot have.
 */
private const val BLOCK_COUNT = 3

/**
 * The canonical support link, the same one the README and the website carry.
 * `MASTER_SPEC.md` section 4.1 names it, and this is the only place in the app
 * that writes it down.
 */
private const val SUPPORT_URL = "https://buymeacoffee.com/kamsiob"

@Composable
private fun DisclaimerBlock(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = HealthTrail.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .padding(Space.cardPadding),
    ) {
        Text(
            text = title,
            style = HealthTrail.type.displayS,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = body,
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}

@Preview(name = "Disclaimer light", showBackground = true)
@Composable
private fun DisclaimerPreviewLight() {
    HealthTrailTheme(darkTheme = false) {
        CompositionLocalProvider(LocalStrings provides Strings.preview()) {
            DisclaimerScreen(onAccept = {})
        }
    }
}

@Preview(name = "Disclaimer dark", showBackground = true)
@Composable
private fun DisclaimerPreviewDark() {
    HealthTrailTheme(darkTheme = true) {
        CompositionLocalProvider(LocalStrings provides Strings.preview()) {
            DisclaimerScreen(onAccept = {})
        }
    }
}
