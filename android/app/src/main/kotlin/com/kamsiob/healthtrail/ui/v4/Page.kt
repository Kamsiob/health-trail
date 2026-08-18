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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
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
    var showTips by remember { mutableStateOf(false) }
    if (showTips && section != null) {
        TipsSheet(tip = tipFor(section), onDismiss = { showTips = false })
    }

    // **Material's own scaffold and its own large flexible app bar.** The bar
    // collapses as the list moves under it, which is the behavior this file
    // used to hand roll out of `derivedStateOf`, a scale and an alpha; it owns
    // the insets, the title and subtitle styles, the navigation slot and the
    // action row. `LargeFlexibleTopAppBar` is the Expressive one, which is the
    // whole reason this project pins an alpha. D179.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.background,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = Bidi.isolate(title)) },
                subtitle = eyebrow?.let {
                    {
                        // The section this page belongs to, in its own ink. It
                        // sits under the name rather than over it, because that
                        // is where Material's bar puts a subtitle and the bar
                        // is the thing being used rather than imitated.
                        Text(
                            text = it,
                            color = section?.let { s -> hueFor(s).ink } ?: eyebrowColor,
                        )
                    }
                },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back, modifier = Modifier.testTag(PageTags.BACK)) {
                            Icon(
                                painter = painterResource(Symbols.back),
                                contentDescription = backLabel,
                            )
                        }
                    }
                },
                actions = {
                    // **A label rather than a chip.** A state says what is
                    // true; it does nothing when pressed, and a chip that
                    // announces itself as a button would be telling a reader
                    // otherwise.
                    badge?.let { state ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = scheme.secondaryContainer,
                            contentColor = scheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = state,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = Space.sm,
                                    vertical = Space.xs,
                                ),
                            )
                        }
                    }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    scrolledContainerColor = scheme.surfaceContainer,
                    titleContentColor = scheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            band?.let {
                Surface(color = scheme.surfaceContainer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(vertical = Space.sm),
                    ) { it() }
                }
            }
        },
    ) { inset ->
        Box(modifier = Modifier.padding(inset)) {
            var refreshing by remember { mutableStateOf(false) }
            val refreshScope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    if (onRefresh != null) {
                        refreshing = true
                        onRefresh()
                        // Held briefly: re-reading a local database is instant,
                        // and an indicator that vanishes before it is seen reads
                        // as a gesture that did nothing.
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
                    // The caller's modifier lands on the list, not on the
                    // surface around it: a test that scrolls to a control needs
                    // the tagged node to be the scrolling one.
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.screenHorizontal)
                        .padding(end = if (rail != null) railWidth() + Space.s else Space.none),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    hero?.let { lead -> item { lead() } }

                    subtitle?.let { line ->
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.s),
                            ) {
                                subtitleMark?.let { mark ->
                                    Icon(
                                        painter = painterResource(mark),
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(Space.markInline),
                                    )
                                }
                                // bidi-ok: the app's own sentence about what the
                                // page is for. A page whose line is somebody's
                                // words isolates them at its call site.
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant,
                                )
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
                        .fillMaxHeight(RAIL_HEIGHT_FRACTION)
                        .padding(end = Space.xs),
                    contentAlignment = Alignment.Center,
                ) { strip() }
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
