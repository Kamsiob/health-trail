package com.kamsiob.healthtrail.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.i18n.formatMoney
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.ChipPickerSheet
import com.kamsiob.healthtrail.ui.v4.DistanceMarker
import com.kamsiob.healthtrail.ui.v4.HeaderActionTags
import com.kamsiob.healthtrail.ui.v4.PickerOption
import com.kamsiob.healthtrail.ui.v4.ROW_SIZE
import com.kamsiob.healthtrail.ui.v4.RouteDash
import com.kamsiob.healthtrail.ui.v4.SpineRow
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.Thumbnail
import com.kamsiob.healthtrail.ui.v4.TipsSheet
import com.kamsiob.healthtrail.ui.v4.tipForDestination
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.hueForMeasure
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.AvatarOverflow
import com.kamsiob.healthtrail.ui.v4.Trace
import java.time.LocalDate

/**
 * Today, written fresh on Material 3 Expressive. D196, and this file replaces
 * `TodayFieldScreen.kt`, `TodayCard.kt` and `TodayLead.kt`, all three deleted.
 *
 * **Not a conversion.** The owner, 2026-08-17: "it's happening because you're
 * still just trying to change the existing UI code instead of getting rid of it
 * and building fresh." The three files this replaces were a hand built card
 * (`Box`, `clip`, `background`, a hand rolled press state), a hand built lead
 * that painted a section wash across the largest block on the screen, and a
 * screen that assembled them under a hand drawn masthead. What is here instead:
 *
 * | Now | Was |
 * |---|---|
 * | `Scaffold` + `LargeFlexibleTopAppBar`, which collapses the masthead itself | a `Row` in the first grid item, and a hand written "reduce the header" |
 * | `Card` for every widget, with Material's own state layer | `Box` + `clip` + `background` + the old `openableByTap` |
 * | `IconButton` / `FilledIconButton` for the corner and for edit mode | a hand drawn pencil on a `Canvas`, and three text pills that measured `Done` to zero at font scale 2.0 |
 * | `MaterialTheme.colorScheme` and `typography` | `HealthTrail.colors` and a second type ladder |
 * | `Icon` over Material Symbols, including the remove minus | a minus drawn with `drawLine` |
 *
 * **Neutral surfaces, and the color is a mark.** The measured complaint on this
 * screen was that 92% of it was two near identical beiges and the only saturated
 * thing was a full height wash behind the lead, which is a wall rather than an
 * accent. The hero is the screen's one filled block; every widget under it is a
 * neutral container with the section's hue on a small round mark, which is where
 * identity belongs and is what the approved drawings do.
 *
 * **What did not change, because none of it is drawing.** The card grammar
 * (D192: a half width square or a full width rectangle, one height either way),
 * what each of the seventeen card types says, the wording of a stored value, the
 * reader's sentence, the staged edit, and the drag. Those are this screen's
 * content and its behavior, and rebuilding the drawing is not license to
 * re-derive them.
 *
 * **Three things stay hand drawn**, per the brief: the trail's route, a
 * project's road, a measure's line. Material has no component for any of them
 * and they are this app's own.
 */

/**
 * The card type the hero replaced.
 *
 * **Named once, because it is matched rather than drawn now.** D192: the
 * appointment is a permanent fixture of this screen, so a stored `next_up` card
 * is skipped instead of being drawn a second time. The string is the stored
 * value in `today_card.card_type` and in the archive vocabulary, so it is not
 * something this screen may rename.
 */
private const val NEXT_UP = "next_up"

/**
 * The card type that told the person what they had just typed in themselves.
 *
 * **Owner ruling, 2026-08-17**, and it is a content decision rather than a layout
 * one: a digest is what a networked product owes you, because things happened
 * while you were away. In a local-first notebook with no account and no cloud
 * the only thing that can have changed is what the person wrote, so the screen's
 * loudest line was reading their own morning back to them. D193.
 *
 * **Skipped rather than deleted**, exactly as [NEXT_UP] is: the row stays in the
 * layout and in the archive vocabulary, which rule 3 puts behind the owner.
 */
private const val DIGEST = "digest"

object TodayFieldTags {
    const val ROOT = "today-field"
    const val LEAD = "today-lead"
    const val EDIT = "today-edit"
    const val DONE = "today-done"
    const val ADD = "today-add"
    const val SEARCH = "today-field-search"
    fun card(id: String) = "today-card-$id"

    /**
     * The mark that takes a card off Today, per card.
     *
     * **A tag rather than the spoken label**, which is what a test tried first
     * and is a trap: the label is built from the card's tab, and the tab has
     * been through `Bidi.join`, so it carries isolate characters a plain string
     * never matches.
     */
    fun remove(id: String) = "today-remove-$id"

    /** The outlined dialable number on a care team card pointed at one person. */
    fun dial(id: String) = "today-dial-$id"

    /** The control that opens the source picker, in edit mode. */
    fun who(id: String) = "today-who-$id"
}

/**
 * The picker's answer for "not one person, all of them".
 *
 * **A sentinel rather than null**, because `ChipPickerSheet` shows which row is
 * chosen and everyone is a real choice somebody made rather than the absence of
 * one. It is stored as no source at all, which is what makes the card render
 * the row of faces.
 */
private const val EVERYONE = "today-care-team-everyone"

/**
 * Where a large system font stops being a font size and starts being a layout.
 *
 * At and above this the field reflows to one column and a widget's height
 * becomes a floor rather than a fixed value. **One number, read by the grid and
 * by the widget**, because two copies of it is a wide card holding a fixed
 * height in a field that has already reflowed.
 */
const val WIDE_TYPE_SCALE = 1.5f

/**
 * How much room a widget takes on the field. `DESIGN.md` 21.3, D192.
 *
 * **Two sizes, and the owner has asked for two more than once.** A widget is a
 * square or it is the full width of the screen, because that is what a widget is
 * on the phone the person already owns, and the phone is the only frame of
 * reference anybody brings to this.
 */
enum class CardSize {
    /** Square, half the width. One answer and one line of context. */
    SMALL,

    /**
     * Full width, and the widget's whole rendering: the answer, its detail, and
     * the chart or mini spine where it has one.
     *
     * **Exactly as tall as [SMALL]**, owner 2026-08-17: two widths, one height,
     * and nothing in between.
     */
    WIDE,
    ;

    companion object {
        fun of(stored: String): CardSize = when (stored) {
            // `tall` is a row written before Today had two sizes. It is a full
            // width card and it always was.
            "wide", "tall" -> WIDE
            else -> SMALL
        }
    }
}

/**
 * Today, as the person arranged it. `DESIGN.md` 21.
 *
 * **Exactly one lead slot at the top, then the card field.** 21.1: a free-form
 * dashboard breaks law 1, because if six equal cards can be stacked then no
 * screen has one thing first. The resolution is a fixed structure with free
 * contents, and the singularity is not a convention here: the lead comes from
 * `Repository.TodayLayout`, which has nowhere to put zero or two.
 *
 * **This screen never rearranges anything.** 21.8. It renders the order it is
 * given: there is no promotion, no injection, no sorting by recency or urgency,
 * and no card appears because the data changed.
 *
 * **Nothing here interprets.** Rule 2. No trend is called good, nothing is
 * colored by value, and quiet is allowed to be good news.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayFieldScreen(
    layout: Repository.TodayLayout,
    /** One answer per card instance, keyed by the card's own id. */
    answers: Map<String, Repository.TodayAnswer>,
    onOpen: (Repository.TodayCard) -> Unit,
    modifier: Modifier = Modifier,
    /** Opens search, which is a whole screen with its own field. */
    onSearch: () -> Unit = {},
    /**
     * The soonest appointment still ahead, for the permanent hero.
     *
     * **Not a card and not in the layout.** D192: the hero is a fixture of this
     * screen, independent of the field and of arranging it.
     */
    nextAppointment: Repository.Appointment? = null,
    /** How many saved questions would come to that appointment. */
    questionsReady: Int = 0,
    /** Opens the appointment the hero is showing. */
    onOpenAppointments: () -> Unit = {},
    /** Opens the questions saved to ask. */
    onOpenQuestions: () -> Unit = {},
    /** Puts something new on the calendar, straight from the hero. */
    onAddAppointment: () -> Unit = {},
    /** Writes down a question to ask, straight from the hero. */
    onAddQuestion: () -> Unit = {},
    /**
     * Saves a rearranged layout, in the order given, the first card leading.
     *
     * **Called once, from Done.** 21.6 screen 5: nothing saves behind your back.
     */
    onSave: (List<Repository.TodayCard>) -> Unit = {},
    /** Opens the gallery, handing it the cards already on the draft. */
    onAddCard: (List<Repository.TodayCard>) -> Unit = {},
    /** What changed since the person was last here. */
    digest: com.kamsiob.healthtrail.data.Digest.Summary =
        com.kamsiob.healthtrail.data.Digest.nothing,
    /** What day it is, for the masthead. Passed rather than read here. */
    today: LocalDate = LocalDate.now(),
    /** Whose day this is, for the masthead. Null before setup names anybody. D170. */
    subjectName: String? = null,
    /** Whether anything has ever been written down. Only the digest asks. */
    hasAnything: Boolean = true,
    /** The care team, for the source picker and for what a chosen card shows. */
    people: List<Repository.Person> = emptyList(),
    /** Opens the dialer with a number filled in, per 21.7. `ACTION_DIAL`, never `ACTION_CALL`. */
    onDial: (String) -> Unit = {},
    /**
     * Told when this screen enters and leaves arranging.
     *
     * **Because the capture button is the shell's and the mode is this
     * screen's.** Somebody rearranging their front door is not also writing
     * something down.
     */
    onArrangingChanged: (Boolean) -> Unit = {},
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme

    // **Edit mode is entered by a visible button**, 21.6 screen 5. Touch and
    // hold is a shortcut and never the only path, because a gesture nobody is
    // told about is a feature that does not exist for most people.
    var editing by rememberSaveable { mutableStateOf(false) }
    var showTips by rememberSaveable { mutableStateOf(false) }

    if (showTips) {
        TipsSheet(tip = tipForDestination("today"), onDismiss = { showTips = false })
    }

    // **Reported rather than pushed at every call site.** `editing` is set from
    // five places, and telling the shell from each of them is five chances to
    // miss one.
    LaunchedEffect(editing) { onArrangingChanged(editing) }

    // **And put back when this screen goes away.** Leaving Today mid arrangement
    // by tapping another tab would otherwise take the hidden button with it.
    DisposableEffect(Unit) { onDispose { onArrangingChanged(false) } }

    // **The staged layout.** Held here while editing and written once, so a
    // person can move three cards and change their mind about all of them.
    var draft by remember(layout, editing) { mutableStateOf(layout.all) }

    // **Which card is choosing a source**, by id, or null. 21.6 screen 7.
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    // **Which card has its options open**, by id, or null. One sheet holds a
    // card's whole life, and it is opened from the card itself.
    var options by rememberSaveable { mutableStateOf<String?>(null) }

    // **What a card says while it is being edited.** The answers were read once
    // when Today gained focus, so a staged pick would leave the card naming the
    // person it pointed at before.
    fun answerFor(card: Repository.TodayCard): Repository.TodayAnswer? =
        staged(card, editing, people)
            ?: answers[card.id]
            ?: digestAnswer(card, digest, strings, hasAnything)

    // **One column once the type is large enough that half a screen is not a
    // card.** 21.6 screen 8, and 1.5 rather than the top of the slider because
    // somebody at 1.7 has the same problem as somebody at 2.0.
    val oneColumn = LocalDensity.current.fontScale >= WIDE_TYPE_SCALE

    // **Where the field is, so a drag knows what it is over.** The grid's own
    // layout information is the only honest source: it is already mirrored in
    // right to left, already reflowed, and already knows where a card is after
    // the last swap.
    val gridState = rememberLazyGridState()

    // Which card the finger is carrying, by id, and how far it has moved from
    // where the grid put it. Null when nothing is being dragged.
    var dragging by remember { mutableStateOf<String?>(null) }

    /**
     * The card that has just been let go and is springing back to its slot.
     *
     * **Separate from [dragging]**, because the card is no longer under a finger
     * but is still not where it belongs, and those are different states: one
     * follows the hand, the other follows physics. D169.
     */
    var settling by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val motion = HealthTrail.motion
    val carried = dragging != null
    val liftScale by animateFloatAsState(
        targetValue = if (carried) LIFT_SCALE else 1f,
        animationSpec = motion.springy(),
        label = "lift",
    )
    val liftShadowDp by animateDpAsState(
        targetValue = if (carried) Space.carryLift else Space.flat,
        animationSpec = motion.springy(),
        label = "liftShadow",
    )
    val settleX by animateFloatAsState(
        targetValue = if (dragging != null) dragOffset.x else 0f,
        animationSpec = if (dragging != null) snap() else motion.settle(),
        label = "settleX",
    )
    val settleY by animateFloatAsState(
        targetValue = if (dragging != null) dragOffset.y else 0f,
        animationSpec = if (dragging != null) snap() else motion.settle(),
        label = "settleY",
    )
    LaunchedEffect(settleX, settleY, dragging) {
        if (dragging == null && settleX == 0f && settleY == 0f) {
            settling = null
            dragOffset = Offset.Zero
        }
    }

    if (!editing && dragging != null) {
        dragging = null
        dragOffset = Offset.Zero
    }

    // **Back leaves the arrangement, and it keeps it.** That is what back does
    // to an arranged home screen on the phone, and a mode whose only way out is
    // one control at the top of a scrolling grid is a mode somebody gets stuck
    // in. Cancel is still there for changing your mind.
    BackHandler(enabled = editing) {
        onSave(draft)
        editing = false
    }

    /**
     * Moves the dragged card to whatever slot the finger is now over.
     *
     * **The center of the card decides, not the finger.** A finger on the bottom
     * edge of a tall card is already inside the card below it, and reordering on
     * that reads as the list flinching away from the touch.
     */
    fun dragTo(id: String) {
        val info = gridState.layoutInfo
        val held = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        val centerX = held.offset.x + held.size.width / 2f + dragOffset.x
        val centerY = held.offset.y + held.size.height / 2f + dragOffset.y
        val over = info.visibleItemsInfo.firstOrNull { other ->
            other.key != id &&
                centerX >= other.offset.x &&
                centerX <= other.offset.x + other.size.width &&
                centerY >= other.offset.y &&
                centerY <= other.offset.y + other.size.height
        } ?: return

        val from = draft.indexOfFirst { it.id == id }
        val to = draft.indexOfFirst { it.id == over.key }
        // Both have to be real field cards. The hero and the lead are grid items
        // too, and their keys are not card ids.
        if (from < 1 || to < 1 || from == to) return
        draft = draft.toMutableList().apply { add(to, removeAt(from)) }
        // **The offset resets because the card has moved under it.** Keeping it
        // would carry the whole distance already traveled into the new slot.
        dragOffset = Offset.Zero
    }

    // **Material's own collapse, rather than a `derivedStateOf` on the scroll.**
    // The masthead was a row in the first grid item and the owner asked twice for
    // it to take less room; a large flexible bar answers that by shrinking to a
    // title bar as the field comes up, which is what the phone does everywhere
    // else and is not something this app should be writing.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.background,
        // **The shell owns the insets.** It already pads for the status bar and
        // draws the navigation bar below this, so a scaffold that inset again
        // would leave a strip of canvas at each end.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = subjectName
                            ?.takeIf { it.isNotBlank() }
                            ?.let { strings("today.masthead", "name" to Bidi.isolate(it)) }
                            ?: strings["today.masthead.noname"],
                    )
                },
                subtitle = {
                    if (editing) {
                        // **The hint takes the subtitle in edit mode**, which is
                        // where a sentence about what to do belongs: it was a
                        // line under the header row, and moving it here means
                        // the whole bar swaps rather than the screen growing a
                        // row it only sometimes has.
                        Text(text = strings["today.edit.hint"])
                    } else {
                        // **Capitals are for the eye and not for the reader.**
                        // Compose has no text transform, so uppercasing changes
                        // the string the semantics tree carries, and a reader
                        // announcing "MONDAY, AUGUST 17" is being handed a shape
                        // rather than a date.
                        Text(
                            text = EventDateText.masthead(strings, today)
                                .uppercase(LocalConfiguration.current.locales[0]),
                            color = scheme.secondary,
                            modifier = Modifier.semantics {
                                contentDescription = EventDateText.masthead(strings, today)
                            },
                        )
                    }
                },
                actions = {
                    if (editing) {
                        // **Marks rather than three text pills.** The pills were
                        // unweighted children of a fixed row, so at font scale
                        // 2.0 the first took what it wanted and Done, the only
                        // control that saves the arrangement, was measured to
                        // zero: there was no way out of the mode at that size.
                        // #371. An icon button is a fixed 48dp target at every
                        // font scale, so the row cannot overflow, and a reader
                        // hears the whole verb.
                        IconButton(
                            onClick = { onAddCard(draft) },
                            modifier = Modifier.testTag(TodayFieldTags.ADD),
                        ) {
                            Icon(
                                painter = painterResource(Symbols.add),
                                contentDescription = strings["today.add"],
                            )
                        }
                        IconButton(onClick = { editing = false }) {
                            Icon(
                                painter = painterResource(Symbols.close),
                                contentDescription = strings["today.edit.cancel"],
                            )
                        }
                        // **Done carries the weight, and it is the only one that
                        // does.** Three controls at one weight made the person
                        // sort them, which is exactly what rule 15 says uniform
                        // weight costs. Keeping the arrangement is what this mode
                        // is for.
                        FilledIconButton(
                            onClick = {
                                onSave(draft)
                                editing = false
                            },
                            modifier = Modifier.testTag(TodayFieldTags.DONE),
                        ) {
                            Icon(
                                painter = painterResource(Symbols.check),
                                contentDescription = strings["today.edit.done"],
                            )
                        }
                    } else {
                        // **The order D173 fixed, read inward from the edge:**
                        // the pencil takes the corner, the lamp sits beside it,
                        // search on the inside. A control that moves between
                        // screens has to be found again every time.
                        IconButton(
                            onClick = onSearch,
                            modifier = Modifier.testTag(HeaderActionTags.SEARCH),
                        ) {
                            Icon(
                                painter = painterResource(Symbols.search),
                                contentDescription = strings["today.search"],
                            )
                        }
                        // **The one gold thing in the corner.** Material's tonal
                        // icon button takes `secondaryContainer`, which this
                        // theme maps to the gold wash, so the lamp is the app's
                        // accent without a hex being typed here.
                        FilledTonalIconButton(
                            onClick = { showTips = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(Symbols.tips),
                                contentDescription = strings["tips.open"],
                            )
                        }
                        IconButton(
                            onClick = { editing = true },
                            modifier = Modifier.testTag(TodayFieldTags.EDIT),
                        ) {
                            Icon(
                                painter = painterResource(Symbols.edit),
                                contentDescription = strings["today.arrange.action"],
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    scrolledContainerColor = scheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inset ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (oneColumn) 1 else 2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(TodayFieldTags.ROOT),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = inset.calculateTopPadding(),
                // **Clear of the capture button**, which the shell floats over
                // this list rather than putting in it.
                bottom = inset.calculateBottomPadding() + Space.fabScrollClearance,
            ),
            horizontalArrangement = Arrangement.spacedBy(Space.cardGap),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            val fullWidth = GridItemSpan(if (oneColumn) 1 else 2)

            // **The hero, and it is not a card.** D192: this section is a
            // permanent fixture of Today, independent of the field below it and
            // of arranging that field. It is drawn while editing too, which is
            // what says it is not part of the draft.
            item(span = { fullWidth }, key = "today-hero") {
                TodayHero(
                    appointment = nextAppointment,
                    questionsReady = questionsReady,
                    subjectName = subjectName,
                    today = today,
                    onOpenAppointment = onOpenAppointments,
                    onOpenQuestions = onOpenQuestions,
                    onAddAppointment = onAddAppointment,
                    onAddQuestion = onAddQuestion,
                )
            }

            // **The appointment card is not drawn, because the hero above says
            // the same thing permanently**, and the digest is not drawn at all,
            // D193. Not drawn rather than deleted: the rows stay in the layout
            // and in the archive, which rule 3 puts behind the owner.
            //
            // **Every move works on the stored list by id, never by what is on
            // screen.** With cards drawn out, a display index and a draft index
            // are two different numbers.
            val shown = (if (editing) draft else layout.all)
                .filterNot { it.type == NEXT_UP || it.type == DIGEST }
            val lead = shown.firstOrNull()
            val field = shown.drop(1)

            if (lead != null) {
                item(span = { fullWidth }, key = "today-lead-slot") {
                    LeadSlot(
                        card = lead,
                        answer = answerFor(lead),
                        today = today,
                        onOpen = onOpen,
                        onDial = onDial,
                        editing = editing,
                        // **Down is the lead's only control**, and it is how a
                        // lead is demoted. There is no Remove, because there is
                        // never zero.
                        canMoveDown = field.isNotEmpty(),
                        onMoveDown = {
                            val at = draft.indexOfFirst { it.id == lead.id }
                            if (at >= 0) {
                                draft = draft.toMutableList().apply { add(at + 1, removeAt(at)) }
                            }
                        },
                        onArrange = { editing = true },
                    )
                }
            }

            items(
                count = field.size,
                key = { field[it].id },
                span = { index ->
                    if (oneColumn) {
                        fullWidth
                    } else {
                        GridItemSpan(if (field[index].size == "small") 1 else 2)
                    }
                },
            ) { index ->
                val card = field[index]
                CardFor(
                    card = card,
                    answer = answerFor(card),
                    // **Full width is wide.** A card the person made small is
                    // rendering at full width in this layout, and 21.3 ties the
                    // second line of context to width rather than to the label
                    // on the size chip.
                    size = if (oneColumn && card.size == "small") {
                        CardSize.WIDE
                    } else {
                        CardSize.of(card.size)
                    },
                    onOpen = onOpen,
                    onDial = onDial,
                    modifier = Modifier
                        // **The others make room, and that is the half of a
                        // reorder that was missing.** Only while the screen is
                        // being arranged, and never on the card in the hand,
                        // which is already following the finger.
                        .then(
                            if (editing && dragging != card.id) {
                                Modifier.animateItem()
                            } else {
                                Modifier
                            },
                        )
                        // **Above the field while it is being carried.** Without
                        // this the dragged card slides under its neighbors,
                        // which reads as the card falling through the screen.
                        .zIndex(if (dragging == card.id || settling == card.id) 1f else 0f)
                        .graphicsLayer {
                            if (dragging == card.id || settling == card.id) {
                                translationX = settleX
                                translationY = settleY
                                // **It lifts as it is picked up**, D169. The
                                // phone's own home screen does exactly this.
                                scaleX = liftScale
                                scaleY = liftScale
                                shadowElevation = liftShadowDp.toPx()
                                clip = false
                            }
                        }
                        .testTag(TodayFieldTags.card(card.id)),
                    dragging = dragging == card.id,
                    onDragStart = {
                        dragging = card.id
                        dragOffset = Offset.Zero
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        dragTo(card.id)
                    },
                    onDragEnd = {
                        // **It settles rather than snapping.** D169, the owner:
                        // "there's no real animation during the movement. it's
                        // just sudden jerk."
                        settling = card.id
                        dragging = null
                    },
                    today = today,
                    editing = editing,
                    onOptions = { options = card.id },
                    onRemove = {
                        val at = draft.indexOfFirst { it.id == card.id }
                        if (at >= 0) {
                            draft = draft.toMutableList().apply { removeAt(at) }
                        }
                    },
                    onArrange = { editing = true },
                    ordinal = index,
                )
            }
        }
    }

    // **One card's whole life, opened from the card.** 21.6 screen 7. Every
    // change goes to the draft, so this sheet's own Done closes it and nothing
    // else: what saves is Done on Today.
    options?.let { cardId ->
        val at = draft.indexOfFirst { it.id == cardId }
        val card = draft.getOrNull(at)
        if (card == null || at < 1) {
            options = null
        } else {
            CardOptionsSheet(
                name = optionsName(card, answerFor(card)),
                size = card.size,
                onResize = { size ->
                    draft = draft.toMutableList().also { it[at] = it[at].copy(size = size) }
                },
                onPromote = {
                    draft = draft.toMutableList().apply { add(0, removeAt(at)) }
                    options = null
                },
                onMoveUp = if (at > 1) {
                    {
                        draft = draft.toMutableList().apply { add(at - 1, removeAt(at)) }
                        options = null
                    }
                } else {
                    null
                },
                onMoveDown = if (at < draft.lastIndex) {
                    {
                        draft = draft.toMutableList().apply { add(at + 1, removeAt(at)) }
                        options = null
                    }
                } else {
                    null
                },
                onRemove = {
                    draft = draft.toMutableList().apply { removeAt(at) }
                    options = null
                },
                // **The source picker, and only where a card takes one.** The
                // care team card is the one whose two variants are a choice
                // about a person rather than about a size.
                onPickSource = if (card.type == "care_team") {
                    { picking = card.id }
                } else {
                    null
                },
                onDismiss = { options = null },
            )
        }
    }

    picking?.let { cardId ->
        val card = draft.firstOrNull { it.id == cardId }
        if (card == null) {
            picking = null
        } else {
            ChipPickerSheet(
                title = strings["today.card.care_team.pick"],
                options = listOf(
                    PickerOption(
                        id = EVERYONE,
                        label = strings["today.card.care_team.everyone"],
                        detail = strings["today.card.care_team.everyone.detail"],
                    ),
                ) + people.map {
                    PickerOption(
                        id = it.id,
                        label = Bidi.isolate(it.displayName),
                        detail = it.roleLabel?.takeIf { role -> role.isNotBlank() }
                            ?.let { role -> Bidi.isolate(role) },
                    )
                },
                selectedId = card.sourceId ?: EVERYONE,
                onPick = { option ->
                    val at = draft.indexOfFirst { it.id == cardId }
                    if (at >= 0) {
                        val everyone = option.id == EVERYONE
                        draft = draft.toMutableList().also {
                            it[at] = it[at].copy(
                                sourceTable = if (everyone) null else "person",
                                sourceId = if (everyone) null else option.id,
                            )
                        }
                    }
                    picking = null
                },
                onDismiss = { picking = null },
            )
        }
    }
}

/**
 * The one thing at the top. `DESIGN.md` 21.1.
 *
 * **The same answer a widget would show, one container step up and at display
 * scale.** The body is composed once for both, so the lead and the field can
 * never word the same answer differently.
 *
 * **Not a colored wall.** It carried the section's wash across its whole height,
 * and the owner's measured complaint about this screen was exactly that: a full
 * height container in a section wash is the screen, not an accent. The hero
 * above it is Today's one saturated block, `docs/V4.md` 2.1, and the lead leads
 * by being first, full width, and louder in type.
 */
@Composable
private fun LeadSlot(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
    onOpen: (Repository.TodayCard) -> Unit,
    onDial: (String) -> Unit,
    editing: Boolean,
    canMoveDown: Boolean,
    onMoveDown: () -> Unit,
    onArrange: () -> Unit,
) {
    val strings = LocalStrings.current
    val tab = tabFor(card, answer, strings)
    val shown = worded(card.type, answer, today)

    TodayWidget(
        tab = tab,
        hue = hueForCard(card.type, card.sourceId),
        // **Raw parts, joined once.** `Bidi.join` isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest: the reader's sentence came out
        // `⁨⁨Next up⁩⁩ · ...`.
        description = Bidi.join(
            listOf(strings[cardTabKey(card.type)], answer?.sourceName) + answerParts(
                shown,
                strings,
                cardType = card.type,
                sourced = card.sourceId != null,
                showItems = true,
                // **The lead draws its chart**, so a reader hears a chart where
                // the eye sees one.
                drewChart = (shown?.series?.size ?: 0) > 1,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        openLabel = strings("today.card.open", "name" to tab),
        onOpen = { onOpen(card) },
        size = CardSize.WIDE,
        lead = true,
        modifier = Modifier.testTag(TodayFieldTags.LEAD),
        speaksAsOneNode = !editing,
        // **The biggest thing on the screen answers the hold too.** A gesture
        // that works everywhere except on the first thing a thumb lands on
        // teaches somebody the gesture is unreliable.
        //
        // long-press-twin: the edit mark in this screen's own app bar. D155.
        onLongPress = if (editing) null else onArrange,
        longPressLabel = strings["today.arrange.hold"],
        // **A promoted care team card brings its number with it.**
        action = shown?.phone
            ?.takeIf { dialable(card, shown, CardSize.WIDE) }
            ?.let { number -> { DialPill(number, card.id, onDial) } },
    ) {
        AnswerBody(
            answer = shown,
            cardType = card.type,
            sourced = card.sourceId != null,
            lead = true,
            showDetail = true,
            // **The lead has the height for a chart, always.** Grid screen 02
            // promotes a chart to the lead, and the lead could not draw one for
            // a while: the screen the design asks for could not exist.
            tall = true,
            hue = hueForCard(card.type, card.sourceId),
        )

        if (editing && canMoveDown) {
            // **Material's outlined button, not a tinted word.** One named
            // control, at the emphasis Material gives a secondary action.
            OutlinedButton(
                onClick = onMoveDown,
                modifier = Modifier
                    .padding(top = Space.s)
                    .semantics {
                        contentDescription = strings("today.edit.down", "name" to tab)
                    },
            ) {
                Text(text = strings["today.edit.down.short"])
            }
        }
    }
}

@Composable
private fun CardFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    size: CardSize,
    onOpen: (Repository.TodayCard) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    /** Opens this card's options sheet. 21.6 screen 7. */
    onOptions: () -> Unit = {},
    /** Takes the card off Today, from the mark in its corner. */
    onRemove: () -> Unit = {},
    /** Opens the dialer, for the one card that offers a number. */
    onDial: (String) -> Unit = {},
    /** Whether the finger is carrying this card right now. */
    dragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
    /** Touch and hold to start arranging, which is what a widget does on the phone. */
    onArrange: () -> Unit = {},
    /** Where this card sits, so neighbors do not tilt in lockstep. */
    ordinal: Int = 0,
) {
    val strings = LocalStrings.current
    val haptics = LocalHapticFeedback.current

    val tab = tabFor(card, answer, strings)
    val shown = worded(card.type, answer, today)

    TodayWidget(
        tab = tab,
        hue = hueForCard(card.type, card.sourceId),
        // **Raw parts, joined once**, for the same reason as the lead.
        description = Bidi.join(
            listOf(strings[cardTabKey(card.type)], answer?.sourceName) + answerParts(
                shown,
                strings,
                cardType = card.type,
                sourced = card.sourceId != null,
                spine = drewSpine(card.type, shown, tall = size == CardSize.WIDE),
                showItems = size != CardSize.SMALL,
                drewChart = size == CardSize.WIDE && (shown?.series?.size ?: 0) > 1,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        // **In edit mode the card is a door to its own options**, grid screen 07,
        // rather than to the section its answer lives in. Leaving it opening the
        // section would take somebody out of an unsaved edit.
        onOpen = { if (editing) onOptions() else onOpen(card) },
        openLabel = if (editing) {
            strings("today.options.open", "name" to tab)
        } else {
            strings("today.card.open", "name" to tab)
        },
        size = size,
        // **An empty answer centers itself.** The none line sat on the bottom
        // edge under a void, which on a fresh notebook made the whole first
        // screen read as four broken boxes. #376.
        centerContent = answer == null || (answer.isEmpty && !answer.sourceClosed),
        modifier = modifier
            .arrangeTilt(arranging = editing, ordinal = ordinal, held = dragging)
            // **The card itself is what you carry**, which is the whole of the
            // owner's note about the phone: nobody grabs a handle to move an
            // icon, they hold the icon.
            .then(
                if (editing) {
                    Modifier.pointerInput(card.id) {
                        // **After a long press, not on any drag.** D169:
                        // `detectDragGestures` claimed the first drag in arrange
                        // mode, so the grid could not be scrolled at all while
                        // arranging. The phone's own home screen does not work
                        // that way.
                        //
                        // long-press-twin: Move up and Move down in the card's
                        // options sheet, which is the path a reader and switch
                        // access take, so nothing here is reachable only by
                        // holding.
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                // **It answers in the hand before it answers on
                                // screen.** Picking something up is the one
                                // moment in this app where a finger is committed
                                // before the eye has caught up.
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart()
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, delta ->
                                change.consume()
                                onDrag(delta)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        // **Only outside arrange mode.** Holding a card that is already tilting
        // would offer to start something that has started.
        onLongPress = if (editing) null else onArrange,
        longPressLabel = strings["today.arrange.hold"],
        corner = if (editing) {
            {
                RemoveMark(
                    spoken = strings("today.edit.remove", "name" to tab),
                    onClick = onRemove,
                    modifier = Modifier.testTag(TodayFieldTags.remove(card.id)),
                )
            }
        } else {
            null
        },
        // In edit mode the card holds controls, and a node that speaks as one
        // thing would swallow them.
        speaksAsOneNode = !editing,
        // **The dialable number, at wide only**, per 21.3's budget of one inline
        // action and two touch targets.
        action = shown?.phone
            ?.takeIf { dialable(card, shown, size) }
            ?.let { number -> { DialPill(number, card.id, onDial) } },
    ) {
        AnswerBody(
            answer = shown,
            cardType = card.type,
            sourced = card.sourceId != null,
            lead = false,
            showDetail = size != CardSize.SMALL,
            // **Wide is the rich rendering**, which is what absorbing the third
            // size means: full width draws the chart or the mini spine the card
            // has, because a person choosing "full width" is asking for the card
            // rather than a taller version of the small one.
            tall = size == CardSize.WIDE,
            hue = hueForCard(card.type, card.sourceId),
        )
    }
}

/**
 * One widget on Today, on Material's own card. `DESIGN.md` 21.2, D196.
 *
 * **`Card`, and everything that comes with it**: the shape from
 * `MaterialTheme.shapes`, the container color from the scheme, the content color
 * that follows it, and the state layer under a press. This was a `Box` with a
 * `clip`, a `background`, a hand rolled resting surface and a hand rolled press,
 * which is what a card is made of rather than what a card is: nothing was
 * inherited, so every state had to be painted by hand and the surface was set in
 * two places, which is how a block came to be drawn in the old color with the new
 * ink on it. D192.
 *
 * **Neutral, with the section's hue on a mark.** Identity belongs on a 32dp
 * round tile at the head of the card, never as a field behind the words: a wash
 * across a whole widget is a wall, and a field of them is the rainbow that gave
 * the eye nowhere to land. D171, and the owner twice on the color of this screen.
 *
 * **One height, two widths, and nothing in between.** Owner ruling, 2026-08-17.
 * The height is the square's side, worked out from the grid the field actually
 * uses: the window less its two margins less the gap between two columns, halved.
 * **Except at a large system font**, where it is a floor, because there the field
 * has already reflowed to one column and a fixed height would clip somebody's own
 * medication name, which is rule 11's truncation wearing a tidy shape.
 *
 * **It is one node to a reader.** A card that announced its tab, its number and
 * its line as three stops would make somebody listen to three things to learn
 * one. The caller composes the sentence.
 *
 * **When to use it.** On Today, and in the gallery that offers cards for Today.
 * A card here is something the person chose to have and can remove, and using the
 * shape anywhere they cannot would make that promise where it is not true.
 */
@Composable
fun TodayWidget(
    /** The section this card's answer lives in. */
    tab: String,
    /** The tab's hue, from the tab pack. Gold for a whole-app surface. */
    hue: TabHue,
    /** What a reader says instead of the card, composed by the caller. */
    description: String,
    onOpen: () -> Unit,
    /** The verb a reader announces for the tap. */
    openLabel: String,
    size: CardSize,
    modifier: Modifier = Modifier,
    /**
     * Whether this is the lead rather than a card in the field.
     *
     * **One container step and one type step**, which is the whole difference.
     * The lead leads by being first, full width and louder, per rule 15's
     * hierarchy through size and position rather than color.
     */
    lead: Boolean = false,
    /**
     * Whether the card speaks as one node.
     *
     * **False in edit mode**, and that is not a preference:
     * `clearAndSetSemantics` clears every descendant, so the controls inside
     * would be unreachable by a screen reader and by switch access.
     */
    speaksAsOneNode: Boolean = true,
    /**
     * One inline control, outside everything the card says.
     *
     * It is here rather than in [content] for a reason that is not layout: the
     * answer is silenced for a reader so the card is one stop, and anything
     * inside that silence is silenced with it.
     */
    action: (@Composable () -> Unit)? = null,
    /** What sits in the corner. The remove mark, in edit mode, and nothing otherwise. */
    corner: (@Composable () -> Unit)? = null,
    /**
     * Touch and hold, which on Today starts arranging the screen.
     *
     * **Because that is what holding a widget does on the phone they own.**
     * Nobody has to be taught it and nobody has to find the word for it first.
     *
     * long-press-twin: the edit mark in Today's own app bar, which enters the
     * same mode and is the path a reader and switch access take. D155.
     */
    onLongPress: (() -> Unit)? = null,
    /** What a reader calls the hold. Required whenever [onLongPress] is set. */
    longPressLabel: String? = null,
    /**
     * Centers the answer instead of letting it sit at the top.
     *
     * **For the empty rung.** A filled square lines its answer up so a row of
     * cards reads across one baseline; an empty square doing that put one small
     * gray line under two thirds of blank white. Rule 11's blank area, on the
     * first screen of a first run. #376.
     */
    centerContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    // **The window's own width, not the configuration's.** Lint is right that
    // `Configuration.screenWidthDp` answers about the display rather than about
    // the space this composable was given.
    val width = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val cardHeight: Dp = (width - Space.screenHorizontal * 2 - Space.cardGap) / 2
    val fixed = LocalDensity.current.fontScale < WIDE_TYPE_SCALE

    val interaction = remember { MutableInteractionSource() }

    Card(
        // **The tap, the sentence and the caller's tag are one node**, and that
        // is not a detail. They were on the column inside the card for one
        // build: the tag rode the card and the semantics rode its content, so a
        // reader stopping on the card heard nothing, and twenty two instrumented
        // tests read an empty description off the node they had been handed.
        // Whatever the caller tags is what has to speak and what has to answer
        // the finger.
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (fixed) Modifier.height(cardHeight) else Modifier.heightIn(min = cardHeight),
            )
            // **Clipped before the click**, so Material's state layer takes the
            // card's own corner rather than painting a rectangle behind it.
            .clip(MaterialTheme.shapes.large)
            // **Material's own ripple over a combined click**, rather than
            // `Card(onClick =)`, which has no long press: the hold that starts
            // arranging is the phone's own gesture for a widget and it has to
            // reach this surface.
            //
            // long-press-twin: the edit mark in Today's app bar, which enters
            // the same mode. The hold is the shortcut and the corner is the path
            // a reader and switch access take. D155.
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = openLabel,
                role = Role.Button,
                // long-press-twin: the edit mark in Today's app bar.
                onLongClick = onLongPress,
                // long-press-twin: the edit mark in Today's app bar.
                onLongClickLabel = longPressLabel,
                onClick = onOpen,
            )
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            // **`surfaceContainer`, and the lead does not get its own step.**
            // This scheme's `surfaceContainerLow` is the canvas itself in light,
            // by design: the light palette has one quiet value above the paper
            // and does not invent a second. A field card drawn on it had no
            // surface at all, which on the phone read as three cards and one
            // orphan. Seen on the phone, rule 21, and invisible in the source.
            //
            // **So the lead leads by size and position, which is rule 15.** It
            // is first, it is full width, and its answer is set two rungs above
            // a card's. Reaching for a color to say the same thing again is what
            // put a section wash across the largest block on this screen.
            containerColor = scheme.surfaceContainer,
        ),
        // **Flat, because depth here is the canvas against the container.**
        // `docs/V4.md` 2.1, and the only shadow in this app is under the person's
        // own paper, which casts one.
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        // **An edge instead of a shadow**, `docs/V4.md` 6.1 item 4, and the same
        // hairline every group in the app wears now. A field of cards on a
        // canvas one tonal step below them needs the boundary stated: without
        // it the cards and the page are the same surface at a glance, which is
        // exactly what item 4 asks about.
        border = BorderStroke(Space.hairlineWidth, scheme.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Space.m),
            ) {
                // **Silenced in one place.** The caller has already composed the
                // tab and the answer into one sentence on the card's own node.
                // [action] and [corner] stay outside the silence, because a
                // control inside it is a control no reader can reach.
                val silence = if (speaksAsOneNode) Modifier.clearAndSetSemantics { } else Modifier

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = silence.fillMaxWidth(),
                ) {
                    // **This is where the section's color belongs**: one small
                    // round mark that says which part of the notebook this is
                    // and leaves the reading surface alone. The same mark the
                    // section lists wear at the head of every row.
                    // **The saturated disc every row in the app wears.** D198:
                    // the wash version made a field of cards read as one beige
                    // page with hints of color in it, which is the owner's word
                    // for the notebook, and Today is the same page one layer up.
                    HueMark(hue = hue, mark = symbolForCard(tab, hue))
                    Spacer(Modifier.width(Space.s))
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        color = hue.ink,
                        // **Two lines rather than an ellipsis.** D173, and rule
                        // 11 bans truncation by name: a tracked measure's tab is
                        // "Tracking, " plus whatever the person called the thing
                        // they are tracking, and their own words are exactly what
                        // must not be cut.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (corner == null) Space.none else Space.xl),
                    )
                }
                Column(
                    modifier = silence
                        .weight(1f)
                        .padding(top = Space.xs),
                    // **The answer takes the room the mark leaves and sits in
                    // the middle of it.** It used to be pushed to the bottom
                    // edge by a `SpaceBetween`, which on a card with three short
                    // lines opened a hand's width of nothing in the middle: rule
                    // 11's blank area, and the first thing anybody saw on this
                    // screen. Centering puts whatever slack the fixed height
                    // leaves above and below the answer, where it reads as air
                    // rather than as a hole. A chart carries its own weight and
                    // still fills the room.
                    verticalArrangement = Arrangement.Center,
                ) {
                    content()
                }
                action?.invoke()
            }
            corner?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Space.xs),
                ) {
                    it()
                }
            }
        }
    }
}

/**
 * The mark for a widget, from the same catalog the sections use.
 *
 * **The tab is what is on the card**, and it carries the section's name plus
 * whatever the person called the thing it points at, so the hue is what says
 * which section this is: the two are chosen together in [hueForCard] and this
 * reads the answer back rather than mapping the seventeen types twice.
 */
@Composable
private fun symbolForCard(tab: String, hue: TabHue): Int {
    val sections = Repository.Section.entries
    val match = sections.firstOrNull { hueFor(it) == hue }
    // Gold belongs to no section: the digest, the trail lately card, the project
    // cards and the unfiled tray. The app's own mark stands for all of them.
    return match?.let { Symbols.of(it) } ?: Symbols.today
}

/**
 * What the record says, in one place for both costumes.
 *
 * **The lead and a card show the same answer at different scales**, never
 * different answers. Two renderings of one query is two renderings that disagree
 * the first time either is touched.
 */
@Composable
private fun ColumnScope.AnswerBody(
    answer: Repository.TodayAnswer?,
    /** The card's type, for the one line of context that names what the count counts. */
    cardType: String,
    /**
     * Whether this card points at one row.
     *
     * **It is what tells the care team card's two variants apart**, 21.7: with a
     * source it is one chosen person and their number, without one it is the row
     * of everyone.
     */
    sourced: Boolean,
    lead: Boolean,
    /** Whether this card is wide enough for a second line of context. */
    showDetail: Boolean,
    /** Whether the card has the height for a chart. Wide only, per 21.3. */
    tall: Boolean = false,
    /** The card's hue, so a chart drawn here is the section's line and not a new color. */
    hue: TabHue = goldHue(),
    /** Where the person's own pictures are read from, for the documents card. */
    attachments: Attachments = Attachments.open(LocalContext.current),
) {
    val scheme = MaterialTheme.colorScheme
    val fonts = MaterialTheme.typography
    val strings = LocalStrings.current

    val spine = drewSpine(cardType, answer, tall)

    // **When it is, through `EventDateText`, so precision is never invented**,
    // per rule 17 and 9.2. A month stays a month here exactly as it does in the
    // trail.
    val whenText = answer?.whenEdtf?.takeIf { it.isNotBlank() }
        ?.let { EventDateText.render(strings, it) }

    // **A zero is not an answer worth shouting.** Rendering the number whatever
    // it was put a large 0 directly above "Nothing waiting", which says the same
    // thing twice and, at that weight, reads as a score on somebody who has just
    // started.
    val count = answer?.count?.takeIf { it > 0 }
    // **Nothing under it and nothing beside it**: no rows, no plot, no spine,
    // and not the lead, whose own sentence is already the largest thing on the
    // screen. Anything else on the card is what the smaller rung exists for.
    //
    // **What is drawn, not what the answer holds.** The first version asked
    // whether `items` was empty and the incidents card kept the small rung: it
    // carries the open incident in `items` and never draws it, because every
    // list, every plot and the row of faces is gated on [showDetail], which a
    // half width card does not have. A card is only as full as what reaches the
    // screen.
    val countIsTheAnswer = !lead && !spine &&
        !(tall && answer != null && answer.series.size > 1) &&
        (!showDetail || answer == null || answer.items.isEmpty())
    if (count != null && answer.title == null) {
        Text(
            // **The reading face, not the mono one.** D173: mono is for figures
            // that line up in a column, and a number a card leads with is a
            // headline.
            text = Bidi.isolate(count.toString()),
            // **The rung is the card's, and it depends on what else is on it.**
            //
            // `displayMedium` everywhere was measured and rejected: a count at
            // that size with its noun, two rows and an "and 3 more" under it ran
            // past the bottom edge and cut the last line off, and rule 11 bans
            // truncation. So a card carrying a list keeps `displaySmall`.
            //
            // **A card whose whole answer is a figure and its noun takes the
            // rung up.** #388 finding 2, the owner on `today-now-light.png`: the
            // incidents card is a label, then a hand's width of nothing, then "1
            // open", and item 1 asks where the eye lands. It landed in the hole.
            // The card's height is the field's grid, D192, and the arrangement
            // is the person's, `a6a86f8b`, so **neither may change**: what was
            // left was a figure two rungs smaller than the room it had. Filling
            // it with the thing the card exists to say is the component fix.
            //
            // **`displayLarge`, and the Material role names are inverted here.**
            // `Theme.kt` maps `displayMedium` to `type.hero` at 30sp and
            // `displaySmall` to `type.displayM` at 36sp, so the role that sounds
            // bigger is smaller. Reaching for `displayMedium` made the figure
            // *shrink*, which is the opposite of the finding. 40sp is the rung
            // above 36 in this app's own ladder.
            style = if (countIsTheAnswer) fonts.displayLarge else fonts.displaySmall,
            color = scheme.onSurface,
        )
        // **A number with no noun is not an answer.** 21.3 gives every size one
        // line of context and the smallest size is not exempt.
        countLineKey(cardType)?.let {
            Text(text = strings[it], style = fonts.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
    answer?.title?.takeIf { !spine }?.let {
        // **A figure is the loudest thing on its card, and a sentence is not.**
        // A short answer with a line under it is a figure and takes the display
        // size; anything longer is words and stays smaller, because forty points
        // of a wrapped sentence in a half width cell is a card that has stopped
        // being readable.
        val figure = !lead && it.length <= FIGURE_MAX &&
            (answer.series.isNotEmpty() || answer.count != null)
        Text(
            text = Bidi.isolate(it),
            style = when {
                lead -> fonts.displayMedium
                figure -> fonts.displaySmall
                else -> fonts.headlineSmall
            },
            color = scheme.onSurface,
            // **The lead wraps freely and a card does not.** D105: a fixed cap
            // is a cap at the smallest type size and a truncation at the
            // largest, and the lead's sentence is the one thing the screen
            // exists to say.
            maxLines = if (lead) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // **The source-closed rung**, 21.4. It says so plainly and keeps working as
    // a door, and the card stays until the person's own hand removes it.
    if (answer?.sourceClosed == true) {
        Text(
            text = strings["today.card.source_closed"],
            style = fonts.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
    if (answer == null) {
        // **Not the same thing as nothing waiting.** A card whose question could
        // not be asked has not learned that the record is empty, and saying
        // "Nothing waiting" here would be the app asserting something false
        // about somebody's record.
        Text(
            text = strings["today.card.unread"],
            style = if (lead) fonts.displayMedium else fonts.bodySmall,
            color = if (lead) scheme.onSurface else scheme.onSurfaceVariant,
        )
    } else if (answer.isEmpty && !answer.sourceClosed) {
        // **A calm state, never a scold**, 21.4. Quiet is allowed to be good
        // news. Each card says its own nothing, so "Nothing scheduled" also tells
        // the person which door this is. **No count of what has not been done**,
        // per rule 13.
        Text(
            text = strings[emptyLineKey(cardType)],
            style = if (lead) fonts.displayMedium else fonts.bodySmall,
            color = if (lead) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }

    // **The recent shape, at wide only.** 21.7 asks the measure card "what is
    // the latest value, and its recent shape", and a line drawn across half a
    // screen width is a decoration rather than a shape anybody can read.
    //
    // **The same plot the Progress screen draws**, so two charts of one measure
    // cannot disagree about the same silence. One color from the section hue and
    // never from the value, no target, no zone, no axis, and **gaps drawn as
    // gaps**, because joining the dots across three missing months invents a line
    // nobody recorded.
    val drewChart = tall && answer != null && answer.series.size > 1
    if (drewChart) {
        Spacer(Modifier.height(Space.xs))
        Trace(
            readings = answer.series,
            // A mark cannot be drawn in the color of the thing it is drawn on.
            line = hue.base,
            // **The card's height is the grid's**, D192, so the line takes the
            // room left after the figure and the date rather than a band of its
            // own.
            modifier = Modifier.weight(1f),
            fillHeight = true,
        )
    }

    // **The row of everyone, which is the care team card's other answer.** 21.7
    // draws it as faces rather than as a column of names, and rule 22 puts an
    // avatar wherever the app shows a person.
    //
    // **The overflow says how many are not drawn**, so a card showing three of
    // nine says nine. Cropping to three quietly would be the app deciding which
    // three of somebody's care team matter.
    if (cardType == "care_team" && !sourced && showDetail &&
        answer != null && answer.items.isNotEmpty()
    ) {
        Row(
            modifier = Modifier.padding(top = Space.s),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in answer.items) {
                Avatar(name = item.label, hue = hue, size = Space.avatarFace)
            }
            val hidden = (answer.count ?: 0) - answer.items.size
            if (hidden > 0) {
                AvatarOverflow(
                    // **Isolated, because a plus is a neutral character.** In
                    // Arabic the paragraph direction reordered "+12" to "12+",
                    // which is a different claim.
                    label = Bidi.isolate(
                        strings("today.card.care_team.overflow", "count" to hidden),
                    ),
                    hue = hue,
                    size = Space.avatarFace,
                )
            }
        }
    }

    // **The mini spine, at wide only.** 21.7. The last few entries as waypoints
    // on the trail's own route, with the gap markers 5.2.4 exists for: two calls
    // a week apart read as a week of calls, and the same two rows four months
    // apart read as somebody left alone until something happened.
    if (spine) {
        MiniSpine(items = answer!!.items, hue = hue)
    }

    // **The list, at wide only.** 21.3 uses this card as its example: the
    // medications card at small is a count and at wide it is the list, and both
    // are the one question asked once. Growing reveals more of the same answer,
    // never a new kind of content.
    //
    // **A chart or a list, never both.** A card carrying a line and then the same
    // readings written out underneath says one thing twice and stops being a
    // shape.
    if (showDetail && !drewChart && !spine && cardType != "care_team" &&
        answer != null && answer.items.isNotEmpty()
    ) {
        Column(
            modifier = Modifier.padding(top = Space.xs),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            // **The trail card's newest entry is already the answer above it**,
            // so the list starts at the second. Dropped here rather than in the
            // query, because at wide all three are waypoints on the spine and a
            // query that had skipped one would leave the spine without its head.
            val listed = (
                if (cardType == "trail_lately") answer.items.drop(1) else answer.items
                )
                // **As many as the card's one height holds, and no more.** Owner,
                // 2026-08-17: a widget is a square or a full width rectangle and
                // the height is the same either way, so a list that grew the card
                // was inventing a third size, and "a card whose content does not
                // fit shows less content and says what it is not showing".
                //
                // **One, on a card that already leads with a count.** The number
                // and the noun under it take two rungs off the top, and with two
                // items and an "and 3 more" under them the last line ran past
                // the bottom edge and was cut in half: rule 11's truncation,
                // seen on the phone. The line below says what is not drawn, so
                // nothing is hidden by showing fewer.
                .take(if (count != null && answer.title == null) LISTED_WITH_A_COUNT else LISTED_ON_A_CARD)
            for (item in listed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // **The person's own paper, at thumbnail size and no larger.**
                    // 21.7 says this card never renders private content bigger
                    // than this. A document nobody photographed gets the section's
                    // fallback drawing rather than a hole.
                    if (cardType == "recent_documents") {
                        Thumbnail(
                            sha256 = item.imageSha,
                            attachments = attachments,
                            section = Repository.Section.DOCUMENTS,
                            size = ROW_SIZE,
                        )
                        Spacer(Modifier.width(Space.s))
                    }
                    // **The name leads and its detail recedes, on one line.**
                    // Rule 15. It was one joined line per item at one size and one
                    // ink, so five medications read as five identical gray lines
                    // and the eye had to read every word to find a name.
                    //
                    // **Beside it rather than under it**, because the card has one
                    // fixed height and stacking the two doubled every item. D193.
                    Text(
                        text = Bidi.isolate(
                            item.label.ifBlank { strings["project.steps.ungrouped"] },
                        ),
                        style = fonts.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The parts joined here rather than in the query, because
                    // joining is wording. `Bidi.join` isolates each part, so a
                    // dose typed in Arabic keeps its own direction beside one
                    // typed in English.
                    Bidi.join(
                        item.note,
                        item.noteEdtf?.let { EventDateText.render(strings, it) },
                    ).takeIf { it.isNotBlank() }?.let { detail ->
                        Spacer(Modifier.width(Space.s))
                        Text(
                            text = detail,
                            style = fonts.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // **Amounts at the end edge, in tabular mono.** 21.7 says
                    // amounts only at wide and right aligned, and the mono face
                    // is what makes a column of them line up on the decimal
                    // point. It is not a second type ladder: Material has no
                    // tabular role and an amount that does not align is a column
                    // of figures nobody can compare.
                    item.amountMinor?.let { minor ->
                        Spacer(Modifier.width(Space.s))
                        Text(
                            text = Bidi.isolate(
                                formatMoney(strings, minor, item.currency ?: "USD"),
                            ),
                            style = HealthTrail.type.mono,
                            color = scheme.onSurface,
                            // **No cap.** A cropped amount is a different amount,
                            // and rule 11 bans truncation.
                        )
                    }
                }
            }
            // **What is not here, said rather than cropped.** The count is the
            // true total, so a card showing two of eleven says so. Silently
            // showing the first two would be the app deciding which medications
            // matter.
            val hidden = if (answer.itemsSampleTheCount) {
                (answer.count ?: 0) - listed.size
            } else {
                (answer.items.size - listed.size).coerceAtLeast(0)
            }
            if (hidden > 0) {
                Text(
                    text = strings("today.card.more", "count" to hidden),
                    style = fonts.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }

    // **The context lines, and growing reveals more of the same answer.** 21.3:
    // the first line is the one line of context every size carries, the second
    // appears at wide. **When it is comes first** where the record has it,
    // because a date is the context on every card that carries one.
    val countedLine = answer?.detailKey?.let { key ->
        answer.detailCount?.let { strings(key, "count" to it) }
    }
    val lines = listOfNotNull(
        // **Not above a spine.** The newest entry's date is already the first
        // waypoint's eyebrow.
        whenText?.takeIf { !spine },
        answer?.detail?.takeIf { it.isNotBlank() },
        countedLine,
        // **Not everybody has a number, and that is not a deficiency.** Rule 13:
        // an unfilled slot reads as not yet, never as an error.
        noNumberLine(cardType, sourced, answer)?.let { strings[it] },
    )
        .let { if (showDetail) it else it.take(1) }
    for (line in lines) {
        Text(
            text = Bidi.isolate(line),
            style = fonts.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = if (lead) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Space.xs),
        )
    }
}

/**
 * Whether the trail card is drawing its mini spine rather than listing.
 *
 * **Asked in one place**, because three things turn on it and they must never
 * disagree: whether the spine draws, whether the title and the date above it are
 * suppressed so the card does not say the newest entry twice, and what a reader
 * hears.
 */
private fun drewSpine(
    cardType: String,
    answer: Repository.TodayAnswer?,
    tall: Boolean,
): Boolean = cardType == "trail_lately" &&
    tall &&
    answer != null &&
    !answer.sourceClosed &&
    answer.items.isNotEmpty()

/**
 * The last few entries as waypoints on the trail's own route. `DESIGN.md` 21.7.
 *
 * **The same spine the trail draws, not a second drawing of it**, and it stays
 * hand drawn: a care trail is this app's own mark and Material has no component
 * for it. The brief names it as one of the three that stay.
 *
 * **The gap markers are the reason this is drawn and not listed.** 5.2.4. **Under
 * fourteen days there is no marker at all**, because a line between every pair is
 * wallpaper. **A coarsely given date produces no marker**: rule 17, the distance
 * between "sometime in April" and a day in June is not a number anybody gave.
 *
 * **No judgment and no color by value**, per rule 2 and 21.8.
 */
@Composable
private fun MiniSpine(items: List<Repository.TodayItem>, hue: TabHue) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val fonts = MaterialTheme.typography
    val zone = java.time.ZoneId.systemDefault()

    Column(modifier = Modifier.padding(top = Space.s)) {
        items.forEachIndexed { index, item ->
            // The row above is the newer one, because the card reads newest
            // first, which is the order the trail itself uses.
            val newer = items.getOrNull(index - 1)
            val gap = if (Edtf.isDayPrecise(item.noteEdtf) && Edtf.isDayPrecise(newer?.noteEdtf)) {
                Distance.between(
                    olderMillis = item.noteStart,
                    newerMillis = newer?.noteStart,
                    zone = zone,
                )
            } else {
                null
            }

            if (gap != null) {
                SpineRow(
                    continuesAbove = true,
                    continuesBelow = true,
                    routeColor = hue.base,
                    dash = RouteDash.TRAIL,
                ) {
                    DistanceMarker(strings(gap.key, "count" to gap.count))
                }
            }

            SpineRow(
                continuesAbove = index > 0,
                continuesBelow = index < items.lastIndex,
                node = nodeColor(item.kind.orEmpty()),
                routeColor = hue.base,
                dash = RouteDash.TRAIL,
            ) {
                Column(modifier = Modifier.padding(bottom = Space.s)) {
                    // **The date and the kind.** The kind is here because 2.2
                    // says every color carries a word alongside it: the node is
                    // gold for a call and blue for a visit, and without the word
                    // the color is carrying that meaning on its own, which fails
                    // in grayscale and for a colorblind reader.
                    val eyebrow = Bidi.join(
                        item.noteEdtf?.let { EventDateText.render(strings, it) },
                        item.kind?.takeIf { it.isNotBlank() }
                            ?.let { strings[kindNameKey(it)] },
                    )
                    if (eyebrow.isNotBlank()) {
                        Text(
                            text = eyebrow,
                            style = fonts.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = Bidi.isolate(item.label),
                        style = fonts.bodySmall,
                        color = scheme.onSurface,
                        // **Two lines, not one.** These are sentences somebody
                        // wrote, and a card that exists to show what was said
                        // lately cutting each to a single line shows three
                        // beginnings.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The dialable number. `DESIGN.md` 21.3 and 21.7.
 *
 * **Material's outlined button**, which is what an outlined inline action is: it
 * was a hand rolled pill with its own press scale and its own tinted surface.
 *
 * **The number is the visible label**, because that is what somebody checks
 * before pressing it. A reader hears the verb as well: "5 5 5 0 1 4 2, button"
 * does not say what the button does.
 *
 * **It opens the dialer and places no call.**
 */
@Composable
private fun DialPill(number: String, cardId: String, onDial: (String) -> Unit) {
    val strings = LocalStrings.current
    OutlinedButton(
        onClick = { onDial(number) },
        modifier = Modifier
            .padding(top = Space.s)
            .testTag(TodayFieldTags.dial(cardId))
            // One stop, not two. The button's own words are the number, and the
            // sentence a reader hears is the number with the verb in front.
            .semantics(mergeDescendants = true) {
                contentDescription = strings("careteam.call.number", "number" to number)
            },
    ) {
        Icon(
            painter = painterResource(Symbols.call),
            contentDescription = null,
            modifier = Modifier.size(Space.markInline),
        )
        Spacer(Modifier.width(Space.s))
        Text(text = Bidi.isolate(number))
    }
}

/**
 * The mark that takes a card off Today. `DESIGN.md` 21.6 screen 5.
 *
 * **Material's tonal icon button over Google's own `remove`**, rather than a
 * minus drawn with `drawLine` inside a hand rolled target. The app authors no
 * glyphs, D196.
 *
 * **A minus rather than a cross.** A cross reads as "delete", and this deletes
 * nothing: the card comes off an arrangement and everything it was answering
 * about is still written down.
 *
 * **It exists only inside edit mode**, which is what keeps "nothing bare responds
 * to touch" true in ordinary reading.
 */
@Composable
private fun RemoveMark(spoken: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Icon(painter = painterResource(Symbols.remove), contentDescription = null)
    }
}

/**
 * The key for "nobody recorded a number for this person", or null.
 *
 * **Only where the card is answering for one person**, because the row of
 * everyone is not short of anything. And never for somebody archived, whose card
 * is already saying the source is closed.
 */
private fun noNumberLine(
    cardType: String,
    sourced: Boolean,
    answer: Repository.TodayAnswer?,
): String? = if (
    cardType == "care_team" && sourced && answer != null &&
    !answer.sourceClosed && answer.phone.isNullOrBlank() && !answer.title.isNullOrBlank()
) {
    "careteam.no_phone"
} else {
    null
}

/**
 * Whether this card is showing a number somebody can press.
 *
 * **Asked in one place**, because two things turn on it and they must never
 * disagree: whether the button renders, and whether the card may still speak as
 * one node.
 */
private fun dialable(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    size: CardSize,
): Boolean = card.type == "care_team" &&
    size != CardSize.SMALL &&
    answer != null &&
    !answer.sourceClosed &&
    !answer.phone.isNullOrBlank()

/**
 * The card's index tab, naming which one it is.
 *
 * **A card pointing at one thing says which thing.** 21.2 puts identity on the
 * tab: four project cards on one Today all reading "Project" is four cards
 * nobody can tell apart.
 */
private fun tabFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    strings: Strings,
): String = Bidi.join(strings[cardTabKey(card.type)], answer?.sourceName)

/**
 * The catalog key for a card type's tab. **Literal, per [countLineKey].**
 *
 * `check_string_keys.py` cannot see a key built from a variable.
 * `TodayCardKeyTest` holds the schema's seventeen types to the catalog, so the
 * dynamic form is covered; this stays a `when` anyway, because the check that
 * reads the code should be able to see the code's keys.
 */
private fun cardTabKey(cardType: String): String = when (cardType) {
    "digest" -> "today.card.digest"
    "next_up" -> "today.card.next_up"
    "medications" -> "today.card.medications"
    "measure" -> "today.card.measure"
    "milestones" -> "today.card.milestones"
    "ask_next_time" -> "today.card.ask_next_time"
    "project_standing" -> "today.card.project_standing"
    "project_date" -> "today.card.project_date"
    "project_steps" -> "today.card.project_steps"
    "incidents" -> "today.card.incidents"
    "money" -> "today.card.money"
    "unfiled" -> "today.card.unfiled"
    "emergency_card" -> "today.card.emergency_card"
    "care_team" -> "today.card.care_team"
    "trail_lately" -> "today.card.trail_lately"
    "recent_documents" -> "today.card.recent_documents"
    "standing_instructions" -> "today.card.standing_instructions"
    // A type the schema refuses cannot reach here, and `TodayCardKeyTest` fails
    // the build if the schema ever gains one this does not know.
    else -> "today.card.digest"
}

/** The stored values in an answer, turned into words. `DESIGN.md` section 9.2. */
@Composable
private fun worded(
    cardType: String,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
): Repository.TodayAnswer? = wordedAnswer(cardType, answer, today, LocalStrings.current)

/**
 * The same wording, without a composition. `DESIGN.md` 9.2.
 *
 * **One function, two callers, and that is the point.** The gallery previews what
 * a card would say if it were added, and it was reading the stored values
 * instead: a project card the screen would show as "Waiting on somebody"
 * previewed as "Nothing waiting". A preview that disagrees with the card it is
 * previewing is worse than no preview.
 *
 * **A stored value is not display text and this is where that stops being true.**
 * A project's status arrives as `waiting`, which is a database value and not a
 * word anybody wrote.
 */
internal fun wordedAnswer(
    cardType: String,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
    strings: Strings,
): Repository.TodayAnswer? {
    if (answer == null || answer.sourceClosed) return answer
    return when (cardType) {
        "project_standing" -> answer.copy(
            // **Whose hands is the answer and the status is the context.** 21.7
            // asks "whose hands, since when", and a person waiting on the county
            // wants the county, not the word Waiting.
            title = answer.title ?: strings[projectStatusKey(answer.detail)],
            detail = answer.title?.let { strings[projectStatusKey(answer.detail)] },
        )

        "project_date" -> {
            val due = answer.whenEdtf
                ?.let { Edtf.parse(it) }
                ?.let { Edtf.resolve(it, java.time.ZoneId.systemDefault()) }
                ?.start
                ?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }
            // **A date the person gave coarsely gets no countdown.** Rule 17:
            // "sometime in April" is not a number of days away, and turning it
            // into one would invent the precision the whole date model exists to
            // protect.
            val exact = answer.whenEdtf?.let { Edtf.parse(it)?.precision } == Edtf.Precision.DAY
            val days = if (due != null && exact) {
                java.time.temporal.ChronoUnit.DAYS.between(today, due)
            } else {
                null
            }
            answer.copy(
                title = when {
                    days == null -> answer.title
                    days == 0L -> strings["project.countdown.today"]
                    days > 0 -> strings("project.countdown.days", "count" to days)
                    // **Passed is plain words and no urgency color**, 21.4.
                    else -> strings("project.countdown.passed", "count" to -days)
                },
                // The kind, which is what the date is, in the person's own words.
                // It is free text they typed, so it is isolated.
                detail = (if (days == null) answer.detail else answer.title)
                    ?.let { Bidi.isolate(it) },
            )
        }

        // **What happens next, said the way somebody in a kitchen says it.** The
        // card was rendering "April 9, 2026 at 10:15 AM", which is correct and
        // makes the person count. A coarse date is never given a day word at all:
        // rule 17 again.
        //
        // **Only what shares the day with the answer.** A card listing next week
        // as well has stopped answering "what is the next dated thing" and started
        // being an agenda, which is a screen the app already has.
        "next_up" -> answer.copy(
            items = answer.items.filter { sameDay(it.noteEdtf, answer.whenEdtf) },
            detail = EventDateText.nearby(strings, answer.whenEdtf, today) ?: answer.detail,
            whenEdtf = if (EventDateText.nearby(strings, answer.whenEdtf, today) != null) {
                null
            } else {
                answer.whenEdtf
            },
        )

        else -> answer
    }
}

/**
 * What a card says when the record has nothing for it yet. `DESIGN.md` 21.4.
 *
 * **Literal keys, per [countLineKey] and [cardTabKey]**, so the checker that
 * reads the code's keys can see them.
 *
 * **Falls back to the general sentence rather than to a key that may not exist**,
 * because `Strings.resolve` throws in debug and the empty rung is the state a
 * brand new notebook is entirely made of.
 */
internal fun emptyLineKey(cardType: String): String = when (cardType) {
    "next_up" -> "today.card.next_up.none"
    "medications" -> "today.card.medications.none"
    "ask_next_time" -> "today.card.ask_next_time.none"
    "money" -> "today.card.money.none"
    "incidents" -> "today.card.incidents.none"
    "unfiled" -> "today.card.unfiled.none"
    "care_team" -> "today.card.care_team.none"
    "recent_documents" -> "today.card.recent_documents.none"
    "standing_instructions" -> "today.card.standing_instructions.none"
    "emergency_card" -> "today.card.emergency_card.none"
    "trail_lately" -> "today.card.trail_lately.none"
    "measure" -> "today.card.measure.none"
    "milestones" -> "today.card.milestones.none"
    "project_date" -> "today.card.project_date.none"
    // The digest always has a sentence, and the two other project cards say their
    // own thing when there is nothing.
    else -> "today.card.nothing"
}

/**
 * Whether two stored dates fall on the same calendar day.
 *
 * **Both have to be day precise for the question to mean anything.** "Sometime in
 * April" is not on any particular day, so it shares one with nothing.
 */
private fun sameDay(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    fun day(text: String): String? {
        val date = Edtf.parse(text) ?: return null
        return when (date.precision) {
            Edtf.Precision.DAY -> date.canonical
            Edtf.Precision.MOMENT -> date.canonical.substringBefore('T')
            else -> null
        }
    }
    val left = day(a) ?: return false
    return left == day(b)
}

/** A project status value as the word the rest of the app uses for it. */
private fun projectStatusKey(status: String?): String = when (status) {
    "waiting" -> "projects.status.waiting"
    "stalled" -> "projects.status.stalled"
    "done" -> "projects.status.done"
    "abandoned" -> "projects.status.abandoned"
    else -> "projects.status.active"
}

/**
 * The word under a card's number, naming what it counted.
 *
 * **Every key here is a literal and that is the whole reason this function
 * exists.** `check_string_keys.py` reads the code's keys against the catalog and
 * **skips anything built at runtime**, by design. `"today.card.$type.count"`
 * would have been exactly the shape that took the app down once already:
 * `ChangeSituationScreen` asked for `more.title`, which has never existed, and
 * `Strings.resolve` throws rather than falling back.
 *
 * **Null for a card that does not answer with a number**, so a type that gains a
 * count later fails the `when` here rather than inventing a key.
 */
internal fun countLineKey(cardType: String): String? = when (cardType) {
    "medications" -> "today.card.medications.count"
    "incidents" -> "today.card.incidents.count"
    "unfiled" -> "today.card.unfiled.count"
    "money" -> "today.card.money.count"
    "care_team" -> "today.card.care_team.count"
    "recent_documents" -> "today.card.recent_documents.count"
    "standing_instructions" -> "today.card.standing_instructions.count"
    "ask_next_time" -> "today.card.ask_next_time.count"
    "emergency_card" -> "today.card.emergency_card.count"
    "trail_lately" -> "today.card.trail_lately.count"
    "project_steps" -> "today.card.project_steps.count"
    "measure" -> "today.card.measure.count"
    // The digest, the next dated thing, a measure, a milestone and the project
    // cards all answer with a thing rather than a quantity.
    else -> null
}

/**
 * What a reader hears instead of the card.
 *
 * One sentence: the tab, then what the record says. A reader stopping on the
 * number, the line and the mark separately would make somebody listen to three
 * things to learn one.
 */
private fun answerParts(
    answer: Repository.TodayAnswer?,
    strings: Strings,
    /** The card's type, so the empty rung reads aloud as it reads on screen. */
    cardType: String,
    /** Whether the card points at one row, per [AnswerBody]'s own parameter. */
    sourced: Boolean = false,
    /** Whether the card drew its mini spine, so the ear gets what the eye gets. */
    spine: Boolean = false,
    /** Whether the card is showing its list. */
    showItems: Boolean = false,
    /** The word under the number, which the eye gets and the ear was not getting. */
    countLine: String? = null,
    /** Whether the card drew its chart, so the ear gets what the eye gets. */
    drewChart: Boolean = false,
): List<String?> = when {
    answer == null -> listOf(strings["today.card.unread"])
    answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
    answer.isEmpty -> listOf(strings[emptyLineKey(cardType)])
    else -> buildList {
        // **The number only where the screen shows one**, which is where the card
        // has no title of its own. It was announced either way, so the trail card
        // told a reader there were 182 entries while the eye saw a sentence and no
        // number at all. Section 9.
        if (answer.title == null) {
            add(answer.count?.takeIf { it > 0 }?.toString())
            if ((answer.count ?: 0) > 0) add(countLine)
        }
        if (!spine) add(answer.title)
        // **The date the screen shows**, per section 9.
        if (!spine) {
            add(
                answer.whenEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) },
            )
        }
        // **The ear gets what the eye gets: a chart or a list, never both.**
        if ((showItems || spine) && !drewChart) {
            // **The rows the screen is showing and no others.**
            val rows = if (cardType == "trail_lately" && !spine) {
                answer.items.drop(1)
            } else {
                answer.items
            }
            // **Raw parts, never a joined line.** `Bidi.join` isolates every part
            // it is handed, so adding an already joined item wrapped it a second
            // time and the sentence came out `⁨⁨137.3⁩ · ⁨May 5⁩⁩`.
            rows.forEachIndexed { index, item ->
                // **The gaps, because they are on the screen in words.** A reader
                // hearing three entries with no distances between them is hearing
                // a list, which is the thing the spine exists not to be.
                if (spine) {
                    val newer = rows.getOrNull(index - 1)
                    if (Edtf.isDayPrecise(item.noteEdtf) && Edtf.isDayPrecise(newer?.noteEdtf)) {
                        Distance.between(
                            olderMillis = item.noteStart,
                            newerMillis = newer?.noteStart,
                            zone = java.time.ZoneId.systemDefault(),
                        )?.let { gap -> add(strings(gap.key, "count" to gap.count)) }
                    }
                }
                add(item.label.ifBlank { strings["project.steps.ungrouped"] })
                add(item.note)
                add(item.noteEdtf?.let { EventDateText.render(strings, it) })
                // The word the node's color is saying, per 2.2, and only where
                // the screen shows it.
                if (spine) {
                    add(item.kind?.takeIf { it.isNotBlank() }?.let { strings[kindNameKey(it)] })
                }
                add(
                    item.amountMinor?.let {
                        formatMoney(strings, it, item.currency ?: "USD")
                    },
                )
            }
            val hidden = if (answer.itemsSampleTheCount) {
                (answer.count ?: 0) - answer.items.size
            } else {
                0
            }
            if (answer.items.isNotEmpty() && hidden > 0) {
                add(strings("today.card.more", "count" to hidden))
            }
        }
        // **A chart is one sentence to a reader, never a list of points.** How
        // many readings there are is the part somebody listening can act on.
        if (drewChart) {
            add(strings("progress.readings", "count" to answer.series.size))
        }
        add(answer.detail)
        add(
            answer.detailKey?.let { key ->
                answer.detailCount?.let { strings(key, "count" to it) }
            },
        )
        // **The line about a missing number, and only where the screen shows
        // it.** The number itself is not added here: where there is one it is an
        // outlined button, which is its own stop and announces itself.
        if (showItems) add(noNumberLine(cardType, sourced, answer)?.let { strings[it] })
    }
}

/**
 * The card's hue, from the tab pack. `DESIGN.md` 21.2 and 21.7.
 *
 * **Identity, never state.** An appointments card is slate because appointments
 * are slate everywhere in the binder, and the hue does not change with what the
 * answer says. Cards for whole-app surfaces wear gold.
 *
 * **Internal rather than private since the gallery draws real cards.** A card
 * being offered wears the hue it will wear once it is on Today. D157.
 */
@Composable
internal fun hueForCard(type: String, sourceId: String? = null): TabHue = when {
    // **A measure card wears the measure's own color, D204**, so the thing
    // somebody tracks is the same color on Today as it is on the Progress
    // screen. Without the source it is still the section's, which is what a
    // card whose measure has not been chosen yet has to be.
    type == "measure" && sourceId != null -> hueForMeasure(sourceId)
    else -> hueForCardType(type)
}

@Composable
private fun hueForCardType(type: String): TabHue = when (type) {
    "next_up" -> hueFor(Repository.Section.APPOINTMENTS)
    "medications" -> hueFor(Repository.Section.MEDICATIONS)
    "ask_next_time" -> hueFor(Repository.Section.ASK_NEXT_TIME)
    "measure", "milestones" -> hueFor(Repository.Section.PROGRESS)
    "money" -> hueFor(Repository.Section.MONEY)
    "recent_documents" -> hueFor(Repository.Section.DOCUMENTS)
    "care_team" -> hueFor(Repository.Section.CARE_TEAM)
    "standing_instructions" -> hueFor(Repository.Section.STANDING_INSTRUCTIONS)
    // **The one card where alert is an identity**, 21.7, and the emergency card
    // screen already wears it for the same reason.
    "emergency_card", "incidents" -> hueFor(Repository.Section.EMERGENCY_CARD)
    // The digest, the trail, the projects and the unfiled tray belong to no
    // section, so gold and the base ladder. 4.3.
    else -> goldHue()
}

/**
 * What a care team card says while its source is still being chosen.
 *
 * **Nothing saves until Done**, 21.6 screen 5, so a picked person cannot come
 * back through the record until then. Without this the card would name whoever
 * it pointed at before and the pick would look like it had done nothing, which is
 * the failure mode a staged control has to avoid: a control that appears to do
 * nothing reads as broken.
 *
 * **Null for everything else**, including a card pointing at somebody archived,
 * who is not in the roster: that card keeps rendering the source-closed rung from
 * the record, which is the truth about it.
 */
private fun staged(
    card: Repository.TodayCard,
    editing: Boolean,
    people: List<Repository.Person>,
): Repository.TodayAnswer? {
    if (!editing || card.type != "care_team") return null
    // Sorted by name, which is the order the card's own query uses, so the three
    // faces here are the three faces after Done.
    val roster = people.sortedBy { it.displayName }
    val chosen = card.sourceId
    if (chosen != null) {
        val person = roster.firstOrNull { it.id == chosen } ?: return null
        return Repository.TodayAnswer(
            // bidi-ok: isolated where it is rendered, not where it is built.
            title = person.displayName,
            // bidi-ok: the same, one line up.
            detail = person.roleLabel?.takeIf { it.isNotBlank() },
            phone = person.phone?.takeIf { it.isNotBlank() },
        )
    }
    return Repository.TodayAnswer(
        count = roster.size,
        items = roster.take(TODAY_CARD_FACES).map {
            // bidi-ok: isolated where it is rendered, not where it is built.
            Repository.TodayItem(label = it.displayName, note = it.roleLabel)
        },
    )
}

/**
 * How many faces a care team card draws before it says how many more.
 *
 * The repository takes the same handful for the same reason, and the two have to
 * agree or a staged pick would redraw the row on Done.
 */
private const val TODAY_CARD_FACES = 3

/**
 * The digest card's answer, from the summary the shell already computed.
 *
 * **Null for any other card**, so this cannot quietly become a fallback that
 * makes an unanswered card look answered.
 *
 * **Kept although the card is no longer drawn**, D193: the row survives in the
 * layout and in the archive vocabulary, and a build that changed its mind would
 * put it back where the person had it.
 */
private fun digestAnswer(
    card: Repository.TodayCard,
    digest: com.kamsiob.healthtrail.data.Digest.Summary,
    strings: Strings,
    /** Whether anything has ever been written down. */
    hasAnything: Boolean,
): Repository.TodayAnswer? {
    if (card.type != "digest") return null
    // **A notebook made thirty seconds ago has no last time.** So it says what is
    // true on day one and nothing more. Not a task, not a setup prompt, not a
    // count of what is missing: rule 13 rules all three out.
    if (!hasAnything) {
        return Repository.TodayAnswer(
            title = strings["today.card.digest.first"],
            detail = strings["today.card.digest.first.detail"],
        )
    }
    // **Counted, never judged.** It says how many things are new, which is a fact
    // about the record. It never says whether that is a lot, and a quiet week is
    // stated as a quiet week rather than as a gap to explain.
    val fresh = digest.newThings
    return Repository.TodayAnswer(
        title = if (fresh > 0) {
            strings("today.card.digest.new", "count" to fresh)
        } else {
            strings["today.card.digest.quiet"]
        },
        // **Only where there is something to say.** A week with nothing corrected
        // does not get told so: rule 13 rules out a tally of the person's own
        // diligence, and a zero here would be exactly that.
        detail = Bidi.join(
            digest.corrected.takeIf { it > 0 }
                ?.let { strings("today.digest.corrected", "count" to it) },
            digest.removed.takeIf { it > 0 }
                ?.let { strings("today.digest.removed", "count" to it) },
        ).takeIf { it.isNotBlank() },
    )
}

/**
 * The tilt that says a card can be picked up, and the lift that says one is.
 *
 * **Borrowed from the phone on purpose**, per the owner's note that a home screen
 * is the only frame of reference anybody brings to a grid of cards. A card being
 * arranged rocks by well under a degree; the card in the hand stops rocking,
 * grows very slightly, and rides above its neighbors.
 *
 * **The rock stops on the held card**, which is the detail that makes it read as
 * physical rather than as an effect: what you are holding is steady and
 * everything you are not holding is loose.
 *
 * **Neighbors are out of phase with each other.** Cards tilting in perfect unison
 * read as the whole screen shaking, which is alarming rather than inviting.
 *
 * **Reduced motion turns the tilt off and nothing else changes.** The degrees
 * come from `LocalMotion`, which is zero there, so the cards sit still and the
 * remove marks and the options sheet carry the whole mode.
 */
@Composable
private fun Modifier.arrangeTilt(arranging: Boolean, ordinal: Int, held: Boolean): Modifier {
    val motion = LocalMotion.current
    val degrees = motion.arrangeTiltDegrees

    // **The animation exists only while the mode does.** An infinite transition
    // composed unconditionally would run a frame loop on the front door forever
    // and multiply it by every card on the screen, to render a rotation of zero.
    val tilt = if (arranging && degrees != 0f && !held) {
        val cycle = rememberInfiniteTransition(label = "arrange")
        val phase by cycle.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.arrangeTiltMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                // Every third card starts at the far end of the swing, so a grid
                // reads as loose rather than as one object shaking.
                initialStartOffset = StartOffset(
                    (ordinal % TILT_PHASES) * motion.arrangeTiltMillis / TILT_PHASES,
                ),
            ),
            label = "tilt",
        )
        phase * degrees
    } else {
        // Still. The card in the hand is deliberately in here too.
        0f
    }

    return this.graphicsLayer {
        rotationZ = tilt
        // The lift is information rather than decoration, so it survives reduced
        // motion: it says which card the finger has.
        scaleX = if (held) HELD_SCALE else 1f
        scaleY = scaleX
    }
}

/**
 * How much bigger the card in the hand is.
 *
 * **Barely.** Enough that the eye can tell which one is being carried without the
 * card covering what it is being moved past.
 */
private const val HELD_SCALE = 1.04f

/** How many cards go by before the tilt repeats its starting point. */
private const val TILT_PHASES = 3

/**
 * How much a card grows while it is being carried, and how far it lifts.
 *
 * **Four percent and eight dp.** The phone's own home screen swells an icon about
 * this much.
 */
private const val LIFT_SCALE = 1.04f

/**
 * What the options sheet calls the card it is about.
 *
 * **The tab and what it points at**, which is the same name the card wears, so
 * somebody who opened the options of one of four project cards can tell which one
 * they are looking at.
 */
@Composable
private fun optionsName(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
): String = tabFor(card, answer, LocalStrings.current)

/**
 * How many items a card draws before it says how many it did not.
 *
 * Two, which is what fits inside the field's one card height under a mark, a
 * count and its line of context. Owner, 2026-08-17: two widths, one height, and
 * no third size.
 */
private const val LISTED_ON_A_CARD = 2

/**
 * How many it draws when the number is already the answer.
 *
 * One, because the count and the word naming what it counted are two rungs of
 * the same fixed height, and what is left is a line for the item and a line
 * saying how many are not drawn.
 */
private const val LISTED_WITH_A_COUNT = 1

/**
 * How short an answer has to be to read as a figure rather than as words.
 *
 * "131.2 lb" and "6 readings" are figures; "Brighter than yesterday. Ate most of
 * her lunch." is a sentence. Twelve characters is the widest thing in the fixture
 * that still sets at display size inside a half width card without wrapping.
 */
private const val FIGURE_MAX = 12
