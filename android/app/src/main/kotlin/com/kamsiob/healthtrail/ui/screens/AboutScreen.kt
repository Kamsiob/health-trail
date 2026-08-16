package com.kamsiob.healthtrail.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.net.toUri
import com.kamsiob.healthtrail.BuildConfig
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.SupportButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object AboutTags {
    const val NAME = "about"
    const val PRIVACY = "about_privacy"
    const val SUPPORT = "about_support"
}

/**
 * The canonical hosted privacy policy.
 *
 * **This exact URL and no other**, per B3 in `DECISIONS.md`. That entry records
 * a mistake worth not repeating: following a link from this page to a longer
 * all-products policy and concluding the longer one governs. It does not. An
 * instruction naming a specific URL is not an invitation to find a more
 * authoritative one.
 */
private const val PRIVACY_URL = "https://kamsiob.com/health-trail.html#privacy"

/** The same support link the gate and the README carry. `MASTER_SPEC.md` 4.1. */
private const val SUPPORT_URL = "https://buymeacoffee.com/kamsiob"

/**
 * About: what this app is, what it does with what you write, and who made it.
 *
 * **Privacy is stated here in full rather than only linked.** Somebody who
 * wants to know whether their notes leave the phone should get the answer on
 * this screen, in one sentence, without opening a browser. The hosted policy is
 * offered underneath for anyone who wants the whole thing.
 *
 * **The support offer sits under the sentence saying the app is free**, the
 * same order the disclaimer gate uses and for the same reason: it is an offer,
 * and an offer that arrives before the reassurance reads as a request. D59.
 *
 * **The licenses are named because the template content is separately
 * licensed and that is deliberate.** It is published so it is useful with a
 * paper binder or a spreadsheet, by people who never install anything, and
 * saying so is part of what the project is.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val context = LocalContext.current

    SectionScaffold(
        name = AboutTags.NAME,
        // **The tab says where you are and the heading says what you came
        // for**, which is the scaffold's own rule and the one this screen was
        // breaking: passing the title alone put "About Health Trail" in the
        // 11sp mono chip and again underneath at display weight, the same
        // sentence twice in two type sizes. Section 1 bans it and #189 gave
        // the scaffold a heading for exactly this. About is reached from More,
        // so More is where you are.
        title = strings["nav.more"],
        headingKey = "about.title",
        subtitle = strings["about.lead"],
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
        item {
            GroupHeader(labelKey = "about.group.privacy")
            Spacer(Modifier.height(Space.headerGap))
            Text(
                text = strings["about.privacy.body"],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.m))
            QuietButton(
                label = strings["about.privacy.link"],
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri()))
                },
                // **Sized to its label**, D118. This screen ended in three
                // identical full width outlined pills of which only the last
                // leaves, which is the exact shape that decision names.
                modifier = Modifier.testTag(AboutTags.PRIVACY),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        item {
            GroupHeader(labelKey = "about.group.support")
            Spacer(Modifier.height(Space.headerGap))
            Text(
                text = strings["about.support.body"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.m))
            SupportButton(
                label = strings["disclaimer.support"],
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()))
                },
                modifier = Modifier.testTag(AboutTags.SUPPORT),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        item {
            GroupHeader(labelKey = "about.group.licenses")
            Spacer(Modifier.height(Space.headerGap))
            Text(
                text = strings["about.licenses.body"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.l))
            Text(
                text = strings("about.version", "version" to BuildConfig.VERSION_NAME),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}
