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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
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
import com.kamsiob.healthtrail.ui.components.railWidth
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
private const val RAIL_HEIGHT_FRACTION = 0.55f

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
     * Re-reads the record when the person pulls the list down. D167.
     *
     * **A local notebook has nothing to fetch, and that is exactly why this is
     * honest here rather than decorative.** It re-reads what is on disk, which
     * genuinely changes: a restore replaces everything, an export runs beside
     * it, and a person who has just brought a notebook back wants to see that
     * it arrived. The gesture is the one every phone owner already has for
     * "show me that again", and Material 3 supplies its physics.
     *
     * Null on a screen with nothing worth re-reading, and then no indicator
     * appears at all.
     */
    onRefresh: (() -> Unit)? = LocalRefresh.current,
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
    /**
     * Changes what is already written on this screen. D173.
     *
     * **The corner, on every screen that has one.** It was a quiet button in
     * the body of five different detail screens, at five different distances
     * down the page depending on how much the entry happened to hold, so
     * finding it meant scanning the screen rather than reaching for a place.
     * The owner's rule: the same thing is in the same place everywhere.
     *
     * Null on a screen that lists rather than holds, where what gets edited is
     * whatever entry you open rather than the list itself.
     */
    onEdit: (() -> Unit)? = null,
    /**
     * What a reader announces for the pencil, in this screen's own words.
     *
     * **"Edit" alone is not enough for somebody listening.** The mark is
     * general on purpose; what it changes is not, so a reader hears "edit this
     * medication" rather than a verb with no object.
     */
    editLabel: String? = null,
    /** The screen's own tag for the pencil, so its tests keep their handle. */
    editTag: String? = null,
    /**
     * What the screen calls itself, which is not what the section is called.
     *
     * **The tab says where you are and the heading says what you came for.**
     * The grid draws "Care team" on the tab over "Who you call" as the heading,
     * and "Medications" over "What she takes". Saying the same words twice in
     * two type sizes is a heading that carries nothing, on the screen law 1
     * says must have one dominant thing.
     *
     * Defaults to [title] for a screen that has no separate question yet, which
     * is visibly redundant rather than quietly wrong.
     */
    headingKey: String? = null,
    /**
     * The heading in the person's own words, when it cannot come from the
     * catalog.
     *
     * **Detail screens need this and the twelve sections do not.** A section's
     * heading is a question the app wrote; one entry's heading is what somebody
     * typed at two in the morning. Before this existed a detail screen had to
     * pass its own words as [title], which put them in the tab chip **and** in
     * the heading: the same sentence twice, once in mono at 11sp and once at
     * display weight, which section 1 bans by name and which looked like a
     * rendering fault rather than emphasis.
     */
    heading: String? = null,
    /**
     * The list's own scroll state, for a screen that has to move it.
     *
     * **Only a screen with law 4's tools needs this.** The edge scrubber's whole
     * job is to jump the list to a year, and it cannot do that without the state
     * the list is scrolling. Every other section leaves it alone and gets the
     * remembered default, so passing it is visibly the exception.
     */
    listState: LazyListState = rememberLazyListState(),
    /**
     * A strip riding the trailing margin, full height, over the list.
     *
     * **It is the margin of a page, not a column of the layout.** The scrubber
     * sits in the space beside the text rather than taking width from it, which
     * is why it is an overlay here rather than a `Row` around the list. In
     * Arabic the trailing edge is the left one and it moves there without this
     * having to know, because it is aligned by direction rather than by side.
     *
     * Empty for every screen that does not have one, which is all of them but
     * the trail today.
     */
    rail: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **The screen arrives rather than cutting.** `DESIGN.md` 10 gives screen
    // transitions the standard spring and 240ms, and #371 found that every
    // navigation in the app was a hard cut: the tokens existed and nothing
    // reached them. This is the one place fifty five screens can gain it
    // together, which is the same argument that put the tab chip here.
    //
    // **A layer rather than an `AnimatedVisibility`.** The content is in the
    // tree and hittable from the first frame either way, which the bloom
    // learned the hard way: something present and not yet visible is a target
    // somebody can tap and not see, and a node a test can find and not click.
    // Here it only moves and fades, so nothing is ever absent.
    //
    // **Reduced motion needs no special case**: `standard()` is a snap, so the
    // value arrives at 1 on the first frame and the screen simply appears.
    // **The movement takes the spring and the opacity is done in 120ms**, which
    // is `Disclosure`'s own pairing and is not a detail. A screen at half
    // opacity is readable through, so for as long as the fade lasts the person
    // sees two screens at once and can tap the top one while still reading the
    // bottom: the same defect the bloom fixed by moving rather than fading.
    // Caught by photographing a frame mid transition rather than by reading it.
    // With `quick()` the double exposure is over before the eye settles, and
    // with reduced motion this pair becomes exactly what `DESIGN.md` 10 asks
    // for, an instant position and a 100ms fade.
    val motion = LocalMotion.current
    val rise = with(LocalDensity.current) { Space.m.toPx() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val arrival by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = motion.standard(),
        label = "section arrival",
    )
    val opacity by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = motion.quick(),
        label = "section opacity",
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = opacity
                translationY = (1f - arrival) * rise
            },
        color = colors.paper,
    ) {
        // Its own system bar padding, because a section screen renders over the
        // shell rather than inside it and does not inherit what the four
        // destinations get. The Unfiled tray learned this the same way.
        // **The bars and the keyboard are one inset here, not two.** Twelve
        // section screens carry a text field, and with `enableEdgeToEdge` the
        // window does not resize for the keyboard, so without this the keyboard
        // covers the field on Export, on Restore and on the change of
        // situation. #371 item 6.
        //
        // **A plain `.imePadding()` after `.systemBarsPadding()` was tried
        // first and is wrong**, which cost three test failures on 2026-08-13
        // in the shape `docs/TRAPS.md` names: the keyboard inset already
        // contains the navigation bar, so applying both adds that bar twice and
        // pushes the pinned footer off the bottom by its height. A click at a
        // node's center outside the viewport does nothing at all, and the
        // assertions read "expected the words but was null", which looks
        // exactly like a save that did not fire. **The union takes the larger
        // of the two rather than the sum**, so with no keyboard this is the
        // bars alone and nothing else changes.
        var showTips by remember { mutableStateOf(false) }
        if (showTips && section != null) {
            TipsSheet(tip = tipFor(section), onDismiss = { showTips = false })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            var refreshing by remember { mutableStateOf(false) }
            val refreshScope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    if (onRefresh != null) {
                        refreshing = true
                        onRefresh()
                        // **Held briefly on purpose.** Re-reading a local
                        // database is instant, and an indicator that vanishes
                        // before it is seen reads as a gesture that did
                        // nothing. This is the shortest pause that still says
                        // "I heard you".
                        refreshScope.launch {
                            kotlinx.coroutines.delay(450)
                            refreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(SectionTags.root(name))
                    .padding(horizontal = Space.screenHorizontal)
                    // The rail gets a margin of its own rather than sitting on
                    // top of the words. Direction aware, so in Arabic the
                    // reserved strip moves to the left with it.
                    .padding(end = if (rail != null) railWidth() + Space.s else 0.dp),
            ) {
                item {
                    Spacer(Modifier.height(Space.sm))
                    // **The tab and the tips button share the line.** #379:
                    // a button beside the section name on every page, opening
                    // what this place is for. It sits with the tab rather than
                    // beside the heading so it never competes with the one
                    // thing the screen leads with.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TabChipText(
                            hue = section?.let { hueFor(it) } ?: wholeAppHue(),
                            label = title,
                        )
                        Spacer(Modifier.weight(1f))
                        // **The same corner as every other screen**, D173, and
                        // routed through the one component so it cannot drift
                        // apart again. A section screen has no pencil of its
                        // own: what it lists is edited by opening the entry,
                        // not by an edit mode over the list. The slot is still
                        // held, so the lamp is where the lamp always is.
                        HeaderActions(
                            onTips = if (section != null) {
                                { showTips = true }
                            } else {
                                null
                            },
                            onEdit = onEdit,
                            editLabel = editLabel,
                            editTag = editTag,
                        )
                    }
                    Spacer(Modifier.height(Space.s))
                    // Display M rather than the largest type in the app. Under
                    // v4 the hero is the one thing on the screen, and a section
                    // title is furniture: it says where you are, and what
                    // matters is what is under it.
                    // **The heading settles as the list moves under it.**
                    // D168, and it is Google's own signature on a large app
                    // bar: the title starts at its full size and eases down
                    // toward the tab as the content scrolls, so the screen
                    // gives its space to what the person came to read.
                    //
                    // **Driven by the first item's offset**, which is the
                    // heading's own item, so the effect is exactly as long as
                    // the header is on screen and stops dead once it is gone.
                    // Reduced motion holds it at full size.
                    val motion = HealthTrail.motion
                    val shrink by remember {
                        derivedStateOf {
                            if (motion.isReduced || listState.firstVisibleItemIndex > 0) {
                                if (motion.isReduced) 0f else 1f
                            } else {
                                (listState.firstVisibleItemScrollOffset / HEADING_SETTLE_PX)
                                    .coerceIn(0f, 1f)
                            }
                        }
                    }
                    Text(
                        text = heading ?: headingKey?.let { strings[it] } ?: title,
                        style = HealthTrail.type.displayM,
                        color = colors.ink,
                        modifier = Modifier.graphicsLayer {
                            val scale = 1f - (shrink * HEADING_SETTLE_SCALE)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            alpha = 1f - (shrink * HEADING_SETTLE_FADE)
                        },
                    )
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
            }

                rail?.let { strip ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            // **Half the height, centered**, rather than the
                            // whole of it. Spread over a full screen the labels
                            // read as scattered digits beside the content; in a
                            // band they read as an index, which is the thing
                            // they are.
                            .fillMaxHeight(RAIL_HEIGHT_FRACTION)
                            .padding(end = Space.xs),
                        contentAlignment = Alignment.Center,
                    ) { strip() }
                }
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
            QuietButton(
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
private const val HEADING_SETTLE_SCALE = 0.16f
private const val HEADING_SETTLE_FADE = 0.35f
private const val HEADING_SETTLE_PX = 220f
