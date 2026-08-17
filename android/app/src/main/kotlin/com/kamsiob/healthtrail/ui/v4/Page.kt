package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * An interior page, written from scratch. #386.
 *
 * **This is what `m3v4-2`, `m3v4-3` and `m3v4-5` all open with**, measured off
 * the drawings: a back arrow in the top corner, an optional state pill opposite
 * it, a small colored label naming where you are, the page's own name at
 * display size, and a quiet line under it. Then the content, on the canvas,
 * with the blocks providing their own color.
 *
 * **No full width back footer.** `docs/V4.md` 4 deleted it: it took a row of
 * the most valuable space on the phone to repeat what the platform's own
 * gesture does. The arrow and the gesture are the two ways back.
 *
 * **No tab chip.** It named the section directly above a heading that names the
 * section, which is two names for one place.
 *
 * **A `LazyColumn`, so a long list costs what a long list costs.** A screen
 * whose content is short passes a handful of items and pays nothing for it.
 */
@Composable
fun Page(
    title: String,
    onBack: () -> Unit,
    backLabel: String,
    modifier: Modifier = Modifier,
    /** The section or place this page belongs to, drawn as the quiet label. */
    eyebrow: String? = null,
    /** The ink the eyebrow takes, which is how a page says which section it is. */
    eyebrowColor: Color = HealthTrail.colors.goldInk,
    /** One line under the name, where the drawing puts what this page is for. */
    subtitle: String? = null,
    /**
     * A mark before the subtitle, which is how `m3v4-5` sets a date.
     *
     * Never announced: it draws what the words beside it already say,
     * `docs/V4.md` 2.1.
     */
    @DrawableRes subtitleMark: Int? = null,
    /**
     * What the page leads with, above its own name.
     *
     * **`m3v4-5` puts the photograph of the paper here**, and the name of the
     * document under it, because on a screen whose subject is an object the
     * object is the heading and the words are the caption. Null on every page
     * whose subject is its own title, which is most of them.
     */
    hero: (@Composable () -> Unit)? = null,
    /** A short state, drawn as a tonal pill opposite the back arrow. */
    badge: String? = null,
    /** What sits in the trailing corner: the tips lamp, an edit mark. */
    actions: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = HealthTrail.colors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.paper) {
        LazyColumn(
            // **The caller's modifier lands on the list, not on the surface
            // around it.** A test that scrolls to a control does
            // `performScrollToNode` on the screen's own tag, and that only
            // works when the tagged node is the scrolling one: on the surface
            // it reported "no node found in scrollable container" for a button
            // sitting at the foot of the list. #386.
            modifier = modifier
                .fillMaxSize()
                // **The page owns its insets**, because a page opens over the
                // shell rather than inside it: without this the back arrow sits
                // under the status bar and the first thing the person sees is
                // their own paper with a clock on top of it. Seen on the phone,
                // which is the only place it was visible. Rule 21.
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
                .padding(horizontal = Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(PageTags.BACK)) {
                        Symbol(
                            symbol = Symbols.back,
                            contentDescription = backLabel,
                            tint = colors.ink,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    badge?.let { state ->
                        Text(
                            text = state.uppercase(LocalConfiguration.current.locales[0]),
                            style = HealthTrail.type.eyebrow,
                            color = colors.goldInk,
                            modifier = Modifier
                                .clip(Radius.pill)
                                .background(colors.goldWash)
                                .padding(horizontal = Space.sm, vertical = Space.xs)
                                // Capitals are for the eye; a reader gets the
                                // words as they were written. D183.
                                .semantics { contentDescription = state },
                        )
                        Spacer(Modifier.width(Space.s))
                    }
                    actions?.invoke()
                }
            }

            hero?.let { lead -> item { lead() } }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    eyebrow?.let { Eyebrow(text = it, color = eyebrowColor) }
                    Lead(text = title)
                    subtitle?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            subtitleMark?.let { mark ->
                                Symbol(
                                    symbol = mark,
                                    contentDescription = null,
                                    tint = colors.ink2,
                                    modifier = Modifier.size(Space.markInline),
                                )
                            }
                            Body(
                                // bidi-ok: this line is the app's own sentence
                                // about what the page is for. A page whose
                                // subtitle is somebody's words isolates them at
                                // its call site.
                                text = it,
                                style = HealthTrail.type.bodyL,
                            )
                        }
                    }
                }
            }

            content()

            item { Spacer(Modifier.height(Space.xxl)) }
        }
    }
}

object PageTags {
    /**
     * The way back.
     *
     * **The same tag the old footer carried**, so the tests that press back
     * still press back rather than being rewritten to follow a redesign. #386.
     */
    const val BACK = "section_back"
}

/**
 * A group of rows under a label, which is the shape most pages are made of.
 *
 * The label belongs to the block under it and arrives with it, rather than
 * floating halfway between two groups, which is the one thing a heading has to
 * get right. Rule 15.
 */
fun LazyListScope.labeledBlock(
    label: String?,
    rows: List<@Composable () -> Unit>,
    /**
     * Whether the label is the app's own fixed words or somebody's.
     *
     * **Capitals and tracking only where the words are fixed and short**,
     * `docs/V4.md` 2.1: a group named after a role somebody typed keeps its own
     * case, because capitals cost about fifteen percent of the width and the
     * result is their own words cut off. D183.
     */
    fixedLabel: Boolean = true,
) {
    if (rows.isEmpty()) return
    item {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            label?.let { Eyebrow(text = it, fixed = fixedLabel) }
            Block(padding = Space.none) {
                rows.forEachIndexed { index, row ->
                    row()
                    if (index != rows.lastIndex) RowDivider()
                }
            }
        }
    }
}

/** Isolated where a page's own name is somebody's words. */
fun personsOwnWords(text: String): String = Bidi.isolate(text)
