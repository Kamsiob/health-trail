package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.TextAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.EmptyDrawing
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.TabChipText
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * What a section's way back says, when the section does not name it itself.
 *
 * **The twelve sections are opened from more than one place.** They are rows in
 * the notebook and they are also rows in Today's digest and its coached steps.
 * A screen reached from Today whose only way back says "Back to the notebook"
 * is the small lie this scaffold's own documentation warns about: it names the
 * wrong destination, and the person only finds out by being surprised.
 *
 * Provided once by the shell rather than threaded through twelve screens, so
 * there is no way for one of them to be forgotten.
 */
val LocalSectionBackKey = androidx.compose.runtime.compositionLocalOf { "section.back" }

object SectionTags {
    const val BACK = "section_back"
    fun root(name: String) = "section_root_$name"
    fun empty(name: String) = "section_empty_$name"
}

/**
 * The shape every section screen shares: a title, a line saying what the section
 * is for, the section's own contents, and one way back.
 *
 * **It exists so that twelve sections cannot become twelve slightly different
 * screens.** They open from one table of contents whose whole promise is that
 * the places never move, and that promise is broken as easily by twelve
 * inconsistent screens as by a shifting order.
 *
 * **The way back is a real control rather than the system gesture alone.** Rule
 * 18 counts taps and forbids dead ends. A screen reachable in one tap that can
 * only be left by a gesture some people do not know is a dead end for exactly
 * the people this app is for.
 *
 * The header renders through [LazyColumn] rather than above it, so a long title
 * at the largest font scale scrolls with the content instead of eating the
 * screen. That was found on the device at font scale 2.0, not in the code.
 */
@Composable
fun SectionScaffold(
    name: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What the way back says.
     *
     * **It names where the person is actually going**, which is the notebook
     * for the twelve sections and somewhere else for anything reached from
     * another screen. A back action that names the wrong destination is a small
     * lie the person only notices by being surprised.
     */
    backLabelKey: String = LocalSectionBackKey.current,
    /**
     * Which section this is, so the screen can wear its own tab.
     *
     * **The tab chip is the first element on every section screen**, per
     * `DESIGN.md` law 2 and 4.3, and it is the single element that makes the app
     * read as a binder rather than as a list of screens. It lives here rather
     * than on each screen so that all twelve get it the same way and none can
     * quietly differ.
     *
     * Null for a screen that belongs to no section, which then wears gold and
     * the base ladder instead, per 4.3.
     */
    section: Repository.Section? = null,
    content: LazyListScope.() -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        // Its own system bar padding, because a section screen renders over the
        // shell rather than inside it and does not inherit what the four
        // destinations get. The Unfiled tray learned this the same way.
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(SectionTags.root(name))
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item {
                    Spacer(Modifier.height(Space.sm))
                    TabChipText(
                        hue = section?.let { hueFor(it) } ?: wholeAppHue(),
                        label = title,
                    )
                    Spacer(Modifier.height(Space.s))
                    // Display M rather than the largest type in the app. Under
                    // v4 the hero is the one thing on the screen, and a section
                    // title is furniture: it says where you are, and what
                    // matters is what is under it.
                    Text(text = title, style = HealthTrail.type.displayM, color = colors.ink)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = subtitle,
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }

                content()

                item { Spacer(Modifier.height(Space.l)) }
            }

            // The pinned action footer, per DESIGN.md 5.15, with its required gap.
            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings[backLabelKey],
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(SectionTags.BACK),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * What a section says when it holds nothing.
 *
 * **It reads as "not yet", never as an error and never as a deficiency**, per
 * rule 13. It says what will turn up here and how, so an empty section teaches
 * the person what the section is for at the moment they are most likely to be
 * wondering.
 */
@Composable
fun SectionEmpty(
    name: String,
    text: String,
    /**
     * Which drawing to stand on the trail map ground.
     *
     * Null draws the ground alone, which is right for a place that is not one
     * of the twelve sections. **It is never a substitute for passing the
     * section**, per 5.17: a section's empty state uses its own drawing, so the
     * empty screen is already teaching where you are.
     */
    /**
     * Passed `Modifier.fillParentMaxHeight(...)` by every caller, so the block
     * centers in the space the list actually has.
     *
     * **Without it the empty state sat jammed under the subtitle with the whole
     * screen empty beneath it**, which reads as a screen that failed to load
     * rather than as a place waiting for something. Found by looking at it on
     * the phone rather than in the code.
     */
    modifier: Modifier = Modifier,
    section: Repository.Section? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SectionTags.empty(name))
            .padding(vertical = Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyDrawing(section = section)
        Spacer(Modifier.height(Space.l))
        Text(
            text = text,
            style = HealthTrail.type.bodyL,
            color = HealthTrail.colors.ink2,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * How much of the list's height an empty state is given to center itself in.
 *
 * Not all of it, because the title and subtitle above are already using some
 * and a block centered in the full height would sit visibly low. Section 5.10.
 */
const val EMPTY_HEIGHT_FRACTION = 0.62f
