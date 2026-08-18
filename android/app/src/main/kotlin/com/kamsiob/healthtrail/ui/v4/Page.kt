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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.HeaderActions
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.railWidth
import com.kamsiob.healthtrail.ui.components.tipFor
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.RowDivider

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
    /**
     * The way back, or null where there is none.
     *
     * **A destination has no back arrow**, because it is where back lands: the
     * bottom bar is how somebody got there and how they leave. Every interior
     * page passes one, and rule 18 forbids a page that can only be left by a
     * gesture.
     */
    onBack: (() -> Unit)?,
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
    /** What sits in the trailing corner, where a page wants its own controls. */
    actions: (@Composable () -> Unit)? = null,
    /**
     * Which part of the notebook this page belongs to.
     *
     * **It decides two things and the page does not have to be told either
     * twice**: the ink the eyebrow takes, so a page says which section it is
     * without a second color system, and the tips lamp in the trailing corner,
     * which opens what this place is for. #379.
     *
     * Null for a page that belongs to no section, which then wears gold and has
     * no lamp.
     */
    section: Repository.Section? = null,
    /**
     * Changes what is already written on this page. D173.
     *
     * **The corner, on every page that has one.** It was a quiet button in the
     * body of five different detail screens, at five different distances down
     * the page depending on how much the entry happened to hold, so finding it
     * meant scanning the screen rather than reaching for a place.
     */
    onEdit: (() -> Unit)? = null,
    /**
     * What a reader announces for the pencil, in this page's own words.
     *
     * **"Edit" alone is not enough for somebody listening.** The mark is general
     * on purpose; what it changes is not, so a reader hears "edit this
     * medication" rather than a verb with no object.
     */
    editLabel: String? = null,
    /** The page's own tag for the pencil, so its tests keep their handle. */
    editTag: String? = null,
    /**
     * The list's own scroll state, for a page that has to move it.
     *
     * **Only a page with a rail needs this.** The edge scrubber's whole job is
     * to jump the list to a year, and it cannot do that without the state the
     * list is scrolling.
     */
    listState: LazyListState = rememberLazyListState(),
    /**
     * Re-reads the record when the person pulls the list down. D167.
     *
     * **A local notebook has nothing to fetch, and that is exactly why this is
     * honest here rather than decorative.** It re-reads what is on disk, which
     * genuinely changes: a restore replaces everything, an export runs beside
     * it, and somebody who has just brought a notebook back wants to see that it
     * arrived. Null on a page with nothing worth re-reading, and then no
     * indicator appears at all.
     */
    onRefresh: (() -> Unit)? = null,
    /**
     * A strip riding the trailing margin, full height, over the list.
     *
     * **It is the margin of a page, not a column of the layout.** The scrubber
     * sits in the space beside the text rather than taking width from it, which
     * is why it is an overlay here rather than a `Row` around the list. In
     * Arabic the trailing edge is the left one and it moves there without this
     * having to know, because it is aligned by direction rather than by side.
     */
    rail: (@Composable () -> Unit)? = null,
    /**
     * How much air sits between one item of the page and the next.
     *
     * **Every item of a page is a group, so the default is the between-groups
     * gap**, D188. A page whose items carry their own spacers passes
     * [Space.none] rather than paying both, and that is a page still waiting to
     * be put on the three gaps.
     */
    itemSpacing: androidx.compose.ui.unit.Dp = Space.betweenGroups,
    /**
     * The actions of a form, pinned under the list rather than scrolled with it.
     *
     * **`docs/V4.md` 2.1: the actions go last, sized to their labels, one
     * filled at most, on the band if the screen is a form.** Pinned, because a
     * form's save is the one control the person is looking for and a control
     * that scrolls away is a control somebody has to hunt for. A page that is
     * not a form passes nothing and there is no band at all.
     */
    band: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = HealthTrail.colors
    var showTips by remember { mutableStateOf(false) }
    if (showTips && section != null) {
        TipsSheet(tip = tipFor(section), onDismiss = { showTips = false })
    }

    // **The page arrives rather than cutting.** `DESIGN.md` 10 gives screen
    // transitions the standard spring and 240ms, and every navigation in the app
    // was a hard cut until #371: the tokens existed and nothing reached them.
    // This is the one place every interior page gains it together.
    //
    // **A layer rather than an `AnimatedVisibility`.** The content is in the tree
    // and hittable from the first frame either way, and something present but
    // not yet visible is a target somebody can tap and not see. Here it only
    // moves and fades, so nothing is ever absent.
    //
    // **The movement takes the spring and the opacity is done in 120ms.** A page
    // at half opacity is readable through, so for as long as the fade lasts the
    // person sees two pages at once. Caught by photographing a frame mid
    // transition rather than by reading it. Reduced motion needs no special
    // case: `standard()` is a snap, so the value arrives at 1 on the first frame.
    val motion = LocalMotion.current
    val rise = with(LocalDensity.current) { Space.m.toPx() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val arrival by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = motion.standard(),
        label = "page arrival",
    )
    val opacity by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = motion.quick(),
        label = "page opacity",
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = opacity
                translationY = (1f - arrival) * rise
            },
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // **The page owns its insets**, because a page opens over the
                // shell rather than inside it: without this the back arrow sits
                // under the status bar and the first thing the person sees is
                // their own paper with a clock on top of it. Seen on the phone,
                // which is the only place it was visible. Rule 21.
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
        ) {
            // **The way back is pinned, not scrolled.** It used to be the first
            // item of the list, so on a long screen it scrolled away and the
            // only way out was a gesture some people do not know, which is the
            // dead end rule 18 forbids. Found by a journey that saved something
            // from the foot of a list and then could not leave.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onBack?.let { back ->
                    IconButton(onClick = back, modifier = Modifier.testTag(PageTags.BACK)) {
                        Symbol(
                            symbol = Symbols.back,
                            contentDescription = backLabel,
                            tint = colors.ink,
                        )
                    }
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
                            // Capitals are for the eye; a reader gets the words
                            // as they were written. D183.
                            .semantics { contentDescription = state },
                    )
                    Spacer(Modifier.width(Space.s))
                }
                // **The same corner as every other page**, D173, routed
                // through the one component so the lamp and the pencil cannot
                // drift apart again.
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
                actions?.invoke()
            }

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
                        // before it is seen reads as a gesture that did nothing.
                        // This is the shortest pause that still says "I heard
                        // you".
                        refreshScope.launch {
                            delay(REFRESH_HELD_MS)
                            refreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    // **The caller's modifier lands on the list, not on the
                    // surface around it.** A test that scrolls to a control does
                    // `performScrollToNode` on the screen's own tag, and that
                    // only works when the tagged node is the scrolling one: on
                    // the surface it reported "no node found in scrollable
                    // container" for a button sitting at the foot of the list.
                    // #386.
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.screenHorizontal)
                        // The rail gets a margin of its own rather than sitting
                        // on top of the words. Direction aware, so in Arabic the
                        // reserved strip moves to the left with it.
                        .padding(end = if (rail != null) railWidth() + Space.s else Space.none),
                    // **Every item of a page is a group, so the air between them
                    // is the between-groups gap.** At twelve points a labeled
                    // block, a road and an action all sat the same distance
                    // apart and the screen read as one column of things.
                    // Material puts sections 24dp apart and its own research
                    // measured what that buys. D188.
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    hero?.let { lead -> item { lead() } }

                    item {
                        // **Four points between the eyebrow and the name.**
                        // Measured on `m3v4-3`: 13.3dp of air where the app's
                        // line boxes alone gave 9.1dp, so the label sat closer
                        // to its title than the drawing sets it. D183.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Space.xs),
                        ) {
                            eyebrow?.let {
                                Eyebrow(
                                    text = it,
                                    color = section?.let { s -> hueFor(s).ink } ?: eyebrowColor,
                                )
                            }
                            // **The heading settles as the list moves under
                            // it.** D168, and it is Google's own signature on a
                            // large app bar: the title starts at its full size
                            // and eases down as the content scrolls, so the page
                            // gives its space to what the person came to read.
                            // Driven by the first item's offset, which is this
                            // item, so the effect lasts exactly as long as the
                            // header is on screen. Reduced motion holds it at
                            // full size.
                            val shrink by remember {
                                derivedStateOf {
                                    when {
                                        motion.isReduced -> 0f
                                        listState.firstVisibleItemIndex > 0 -> 1f
                                        else -> (
                                            listState.firstVisibleItemScrollOffset /
                                                HEADING_SETTLE_PX
                                            ).coerceIn(0f, 1f)
                                    }
                                }
                            }
                            Lead(
                                text = title,
                                modifier = Modifier.graphicsLayer {
                                    val scale = 1f - (shrink * HEADING_SETTLE_SCALE)
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    alpha = 1f - (shrink * HEADING_SETTLE_FADE)
                                },
                            )
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
                                        // bidi-ok: this line is the app's own
                                        // sentence about what the page is for. A
                                        // page whose subtitle is somebody's
                                        // words isolates them at its call site.
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

            rail?.let { strip ->
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        // **Half the height, centered**, rather than the whole
                        // of it. Spread over a full screen the labels read as
                        // scattered digits beside the content; in a band they
                        // read as an index, which is the thing they are.
                        .fillMaxHeight(RAIL_HEIGHT_FRACTION)
                        .padding(end = Space.xs),
                    contentAlignment = Alignment.Center,
                ) { strip() }
            }
        }

        band?.let {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(Space.m))
                it()
                Spacer(Modifier.height(Space.m))
            }
        }
        }
    }
}

/** How long the refresh indicator is held so the gesture is seen to land. */
private const val REFRESH_HELD_MS = 450L

/** How much of the screen the rail's band of labels takes. */
private const val RAIL_HEIGHT_FRACTION = 0.55f

/** How far the heading settles, and over how much scroll. D168. */
private const val HEADING_SETTLE_SCALE = 0.16f
private const val HEADING_SETTLE_FADE = 0.35f
private const val HEADING_SETTLE_PX = 220f

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
    /**
     * The ink the label takes, where a group's own state has something to say.
     *
     * Money's leading group wears the alert ink, so the one wanting a decision
     * reads as that rather than as an arbitrary first group. #345. Everything
     * else is the quiet ink, because a label is furniture.
     */
    labelColor: Color? = null,
    /** A tag on the label itself, for a caller whose test asserts on it. */
    labelTag: String? = null,
) {
    if (rows.isEmpty()) return
    item {
        // A label belongs to the block under it, so it sits close to it.
        Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
            label?.let {
                Eyebrow(
                    text = it,
                    fixed = fixedLabel,
                    color = labelColor ?: HealthTrail.colors.ink2,
                    modifier = labelTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
                )
            }
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
