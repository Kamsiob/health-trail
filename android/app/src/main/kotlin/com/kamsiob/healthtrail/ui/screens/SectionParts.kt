package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import com.kamsiob.healthtrail.ui.v4.Action
import kotlinx.coroutines.launch
import com.kamsiob.healthtrail.ui.components.HeaderActions
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.tipFor
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.EmptyDrawing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.TabChipText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.components.railWidth
import com.kamsiob.healthtrail.ui.theme.Radius
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
    /**
     * One line above the paragraph, saying what this place is for.
     *
     * **Rule 15: something has to lead.** A drawing over one gray paragraph is
     * uniform weight, and uniform weight pushes the sorting onto the reader.
     * Where a section has a sentence worth reading first, it goes here and
     * takes the size, and the paragraph below it recedes.
     *
     * Null keeps the older shape, which is right where the paragraph is the
     * whole thought and a headline would only restate it.
     */
    lead: String? = null,
    /**
     * The one thing to do from here, if there is one.
     *
     * **Outlined, never filled.** Every screen that passes this also carries a
     * capture control, and two filled actions on an otherwise empty screen is
     * the competition section 10.8 is about.
     */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
        // The drawing draws nothing for a null section since 2026-08-16, so
        // its gap goes with it rather than floating above the lead.
        if (section != null) Spacer(Modifier.height(Space.l))
        if (lead != null) {
            Text(
                text = lead,
                style = HealthTrail.type.displayS,
                color = HealthTrail.colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.s))
        }
        Text(
            text = text,
            style = HealthTrail.type.bodyL,
            color = HealthTrail.colors.ink2,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.l))
            Action(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.testTag(SectionTags.emptyAction(name)),
            )
        }
    }
}

/**
 * How much of the list's height an empty state is given to center itself in.
 *
 * Not all of it, because the title and subtitle above are already using some
 * and a block centered in the full height would sit visibly low. Section 5.10.
 */
const val EMPTY_HEIGHT_FRACTION = 0.62f

/**
 * The same, for an empty state that carries a lead and an action rather than
 * one line, and whose screen drops its subtitle while empty.
 *
 * **A taller block needs more room to center in, not less.** At the section
 * fraction it settled into the upper half and left the bottom third blank,
 * which rule 11 rules out and which reads as a screen that did not finish
 * loading.
 */
const val EMPTY_HEIGHT_TALL = 0.82f

/**
 * How far the heading settles, and over how much scroll.
 *
 * **A sixth smaller, not half.** The heading is furniture that gives way, and
 * a title that visibly collapses draws more attention on the way out than it
 * had sitting still, which is the opposite of the point.
 */
