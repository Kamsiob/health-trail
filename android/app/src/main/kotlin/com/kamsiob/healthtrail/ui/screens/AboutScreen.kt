package com.kamsiob.healthtrail.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.net.toUri
import com.kamsiob.healthtrail.BuildConfig
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page

object AboutTags {
    const val NAME = "about"
    const val PRIVACY = "about_privacy"
    const val SUPPORT = "about_support"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_about"
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

/**
 * About: what this app is, what it does with what you write, and who made it.
 * Rewritten onto `ui/v4`, #386.
 *
 * **Privacy is stated here in full rather than only linked.** Somebody who wants
 * to know whether their notes leave the phone should get the answer on this
 * screen, in one sentence, without opening a browser. The hosted policy is
 * offered underneath for anyone who wants the whole thing.
 *
 * **The support offer sits under the sentence saying the app is free**, the same
 * order the disclaimer gate uses and for the same reason: it is an offer, and an
 * offer that arrives before the reassurance reads as a request. D59.
 *
 * **The licenses are named because the template content is separately licensed
 * and that is deliberate.** It is published so it is useful with a paper binder
 * or a spreadsheet, by people who never install anything, and saying so is part
 * of what the project is.
 *
 * **Three labeled blocks, which is the shape of every page that is mostly
 * words.** `docs/V4.md` 2.1: a quiet eyebrow names what follows, the words sit
 * on a tonal block, and the action that leaves the app is sized to its label
 * underneath. D118, and no full width pills.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val context = LocalContext.current

    Page(
        // About is reached from More, so More is where you are. The heading says
        // what you came for and the eyebrow says where you came from, which is
        // the one thing this screen used to get wrong: the same sentence twice,
        // in two type sizes.
        eyebrow = strings["nav.more"],
        title = strings["about.title"],
        subtitle = strings["about.lead"],
        onBack = onBack,
        backLabel = strings["section.back.more"],
        modifier = modifier.testTag(AboutTags.ROOT),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Eyebrow(text = strings["about.group.privacy"])
                Block {
                    // bidi-ok: the app's own sentence about its own behavior.
                    Body(
                        text = strings["about.privacy.body"],
                        color = colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
                Action(
                    label = strings["about.privacy.link"],
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri()))
                    },
                    modifier = Modifier.testTag(AboutTags.PRIVACY),
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Eyebrow(text = strings["about.group.support"])
                Block {
                    // bidi-ok: the app's own words about how it is paid for.
                    Body(text = strings["about.support.body"])
                }
                // **It must never read as a request.** It sits after the sentence
                // saying the app is free and asks for nothing, and the screen is
                // fully passable without noticing it. So it is the quiet weight,
                // never the filled one. D59, D93.
                Action(
                    label = strings["disclaimer.support"],
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()))
                    },
                    modifier = Modifier.testTag(AboutTags.SUPPORT),
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Eyebrow(text = strings["about.group.licenses"])
                Block {
                    // bidi-ok: license names and the app's own sentence about them.
                    Body(text = strings["about.licenses.body"])
                    Body(
                        // bidi-ok: a version string the build wrote, never words
                        // somebody typed.
                        text = strings("about.version", "version" to BuildConfig.VERSION_NAME),
                        style = HealthTrail.type.bodyS,
                    )
                }
            }
        }
    }
}
