package com.kamsiob.healthtrail.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.IconTile
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.railWidth
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import kotlinx.coroutines.launch

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

/**
 * Re-reads the record, provided once by the shell. D167.
 *
 * **A composition local rather than a parameter on twelve screens**, the same
 * shape `LocalSectionBackKey` already uses, because every section list wants
 * the same gesture and none of them should have to be told about it.
 */
val LocalRefresh = androidx.compose.runtime.compositionLocalOf<(() -> Unit)?> { null }

/**
 * How much of the screen's height the edge rail occupies.
 *
 * Slightly over half, which is what the reference file draws: enough travel for
 * a drag to be worth making, short enough that the labels read as one index
 * rather than as digits scattered down the margin.
 */

object SectionTags {
    const val BACK = "section_back"
    fun root(name: String) = "section_root_$name"
    fun empty(name: String) = "section_empty_$name"
    fun emptyAction(name: String) = "section_empty_action_$name"
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
/**
 * What a section says when it holds nothing, and **it is a state that was
 * designed rather than the content taken away.**
 *
 * **It reads as "not yet", never as an error and never as a deficiency**, per
 * rule 13. It says what will turn up here and how, so an empty section teaches
 * the person what the section is for at the moment they are most likely to be
 * wondering.
 *
 * **What this was, and why it changed.** #388 finding 1, worst first, seen on
 * `empty-trail-light.png`: a title, a sentence, a hairline gray squiggle about
 * 40dp wide floating in the middle of the screen, a second sentence a third of
 * the way down, and two thirds of the screen blank. Rule 11 calls a blank area
 * unfinished and `docs/V4.md` 6.1 item 9 asks for eight states designed rather
 * than handled. **And the app had two of these, not one**: fourteen screens
 * centered a drawing and a gray line in 62% of the list's height, and six more
 * put a plain `Block` with one sentence in it under the subtitle. Two answers
 * to one question is the same as no answer, rule 16.
 *
 * **So it is one block, in the flow, carrying four things.** The block is the
 * page's one tonal block, item 3, and an empty page is the only page that has
 * room for it:
 *
 * 1. **The section's own mark, saturated, in its own hue.** D198: a mark is
 *    `TabHue.base` with `TabHue.onBase` on top, **never a faded base**, and the
 *    old drawing was `onSurface` at 14% alpha, which is exactly the faded mark
 *    that rule forbids. It is also the mark the person will navigate by for the
 *    next two years, taught at the moment they have nothing else to look at.
 * 2. **An edge the eye can find**, item 4, which a drawing floating in the
 *    canvas does not have.
 * 3. **The sentence, in `ink` rather than `ink2`**, because inside a block it
 *    is the content rather than a caption on empty space.
 * 4. **The way in, where the screen has one.**
 *
 * **Top of the page rather than centered in it.** Item 6 asks for one left edge
 * down the screen and the centered version broke it; more than that, an empty
 * state placed where the content will be teaches the shape of the page. The air
 * below it is then air below a designed object rather than a void with
 * something small floating in it.
 */
@Composable
fun SectionEmpty(
    name: String,
    text: String,
    /**
     * **No longer passed `fillParentMaxHeight`.** Every caller used to hand
     * this the fraction of the list it should center itself in, which is what
     * put the block in the middle of an otherwise blank screen.
     */
    modifier: Modifier = Modifier,
    /**
     * Which section this is, which is what gives the block its hue and its mark.
     *
     * Null draws the words alone in a quiet block, which is right for a place
     * that is not one of the twelve sections: search results and the conflict
     * list are not a section and have no mark of their own to teach.
     */
    section: Repository.Section? = null,
    /**
     * One line above the paragraph, saying what this place is for.
     *
     * **Rule 15: something has to lead.** Where a section has a sentence worth
     * reading first, it goes here and takes the size, and the paragraph below
     * it recedes. Null is the ordinary case: the screen's own title is already
     * leading and a headline here would restate it.
     */
    lead: String? = null,
    /**
     * The one thing to do from here, if there is one.
     *
     * **Quiet rather than filled**, [ActionEmphasis] leaves that to the screen's
     * own primary action. A section screen's way in floats on the scaffold since
     * D200, so this is a second route to the same place for somebody who is
     * looking at the words rather than at the corner.
     */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /**
     * The tag the action carries, where a screen's own test already names it.
     *
     * Defaults to this block's own tag. Chapters passes `ChapterTags.MOVED`,
     * because #377 is what put a way out of an empty Chapters screen there and
     * its test walks that tag.
     */
    actionTag: String? = null,
    /** The mark beside the action's label, where the screen draws one. */
    @DrawableRes actionMark: Int? = null,
) {
    val hue: TabHue? = section?.let { hueFor(it) }
    Block(
        modifier = modifier.testTag(SectionTags.empty(name)),
        tone = if (hue != null) BlockTone.Section else BlockTone.Quiet,
        hue = hue,
        padding = Space.l,
    ) {
        if (section != null && hue != null) {
            IconTile(
                section = section,
                tint = hue.onBase,
                background = hue.base,
                tileSize = Space.emptyMark,
                iconSize = Space.emptyMarkGlyph,
            )
            Spacer(Modifier.height(Space.xs))
        }
        if (lead != null) {
            Text(
                text = lead,
                style = HealthTrail.type.displayS,
                color = HealthTrail.colors.ink,
            )
        }
        Text(
            text = text,
            style = HealthTrail.type.bodyL,
            color = HealthTrail.colors.ink,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.xs))
            Action(
                label = actionLabel,
                onClick = onAction,
                mark = actionMark,
                modifier = Modifier.testTag(actionTag ?: SectionTags.emptyAction(name)),
            )
        }
    }
}

// **The two centering fractions are gone with the centering.** They said how
// much of the list's height an empty state should center itself in, 0.62 for a
// section and 0.82 for a screen carrying a lead and an action. Both existed to
// place a small thing in a large void, and #388 finding 1 is that the void was
// the defect. The empty state is now a block at the top of the flow, so there
// is no height to center in and no fraction to pick.

/**
 * How far the heading settles, and over how much scroll.
 *
 * **A sixth smaller, not half.** The heading is furniture that gives way, and
 * a title that visibly collapses draws more attention on the way out than it
 * had sitting still, which is the opposite of the point.
 */

/**
 * The hue an entry wears, from what kind of thing it was. D198.
 *
 * **Every list of entries in this app was one color, and one color is what the
 * owner called dreary.** 2026-08-17, on the notebook: "the colors are
 * overwhelmingly depressing... there could never be a page that is
 * overwhelmingly one color." A trail, a thread and a chapter are all lists of
 * the same six kinds of thing, and each kind already belongs somewhere in the
 * binder, so the color is not decoration: a call is the care team's red, a visit
 * is appointments slate, a reading is progress green. Somebody who learns the
 * marks on the notebook reads every list in the app without being taught twice.
 *
 * **Identity, never state.** Rule 2 and `docs/V4.md` 2.1: nothing here says an
 * entry is good, bad, urgent or overdue, and an incident is alert red because
 * incidents are alert red everywhere, not because this one went badly.
 *
 * **Paired with [entryMark], because a color never carries meaning alone.**
 * Section 2.2: the mark and the words beside it say what the color says, so it
 * survives grayscale and every color vision difference.
 */
@Composable
fun entryHue(kind: String): TabHue = when (kind) {
    "call" -> hueFor(Repository.Section.CARE_TEAM)
    "visit" -> hueFor(Repository.Section.APPOINTMENTS)
    "incident" -> hueFor(Repository.Section.EMERGENCY_CARD)
    "measurement" -> hueFor(Repository.Section.PROGRESS)
    "question" -> hueFor(Repository.Section.ASK_NEXT_TIME)
    "document" -> hueFor(Repository.Section.DOCUMENTS)
    // A note belongs to the trail itself, which is the app's own gold.
    else -> hueFor(Repository.Section.TRAIL)
}

/** The mark for one kind of entry, from the same catalog every section uses. */
@DrawableRes
fun entryMark(kind: String): Int = when (kind) {
    "call" -> Symbols.call
    "visit" -> Symbols.stethoscope
    "incident" -> Symbols.incidents
    "measurement" -> Symbols.monitorWeight
    "question" -> Symbols.askNextTime
    "document" -> Symbols.documents
    else -> Symbols.noteStack
}

/**
 * The round mark a row or a card wears at its head. D198.
 *
 * **One shape, one size, one place, app-wide.** The notebook draws it, the
 * medication rows draw it, a card on Today draws it, and every list of entries
 * draws it now: a wash disc with the mark in the matching ink. It is the whole
 * of this app's color strategy, because it puts the color on something small
 * that means something rather than behind a paragraph.
 */
@Composable
fun HueMark(
    hue: TabHue,
    @DrawableRes mark: Int,
    modifier: Modifier = Modifier,
    size: Dp = Space.markCard,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(hue.base),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(mark),
            contentDescription = null,
            tint = hue.onBase,
            modifier = Modifier.size(size * MARK_GLYPH_RATIO),
        )
    }
}

/** The drawing inside the disc, which leaves it a ring of its own wash. */
private const val MARK_GLYPH_RATIO = 0.62f
