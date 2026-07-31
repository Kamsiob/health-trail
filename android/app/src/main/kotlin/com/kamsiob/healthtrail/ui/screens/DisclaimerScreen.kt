package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.R
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.Space
import androidx.compose.runtime.CompositionLocalProvider

object DisclaimerTags {
    const val ROOT = "disclaimer_root"
    const val ACCEPT = "disclaimer_accept"
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
 * **On states.** This screen has fixed content, so it has no empty or many item
 * state. What it does have to survive is the largest system font size and the
 * longest translation, which is why the whole column scrolls rather than
 * assuming it fits. The mark is decorative and is hidden from the screen reader:
 * the heading immediately below it already says where the person is, and a
 * reader announcing a logo before every title is noise.
 */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
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
                colorFilter = ColorFilter.tint(colors.blaze),
                modifier = Modifier
                    .size(44.dp)
                    .clearAndSetSemantics { },
            )

            Spacer(Modifier.height(Space.l))

            Text(
                text = strings["disclaimer.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.m))

            Text(
                text = strings["disclaimer.body.1"],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.m))

            Text(
                text = strings["disclaimer.body.2"],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
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
