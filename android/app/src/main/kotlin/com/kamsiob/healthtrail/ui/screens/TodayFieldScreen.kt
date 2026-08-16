package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.Radius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.ui.components.Thumbnail
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.i18n.formatMoney
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Avatar
import com.kamsiob.healthtrail.ui.components.AvatarOverflow
import com.kamsiob.healthtrail.ui.components.AvatarSize
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.components.ChartHeight
import com.kamsiob.healthtrail.ui.components.ChipPickerSheet
import com.kamsiob.healthtrail.ui.components.DistanceMarker
import com.kamsiob.healthtrail.ui.components.PickerOption
import com.kamsiob.healthtrail.ui.components.Plot
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.components.ROW_SIZE
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.chartPoints
import com.kamsiob.healthtrail.ui.components.CardSize
import com.kamsiob.healthtrail.ui.components.TabChip
import com.kamsiob.healthtrail.ui.components.TodayCard
import com.kamsiob.healthtrail.ui.components.TodayLead
import com.kamsiob.healthtrail.ui.components.UniversalSearchDoor
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import java.time.LocalDate

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

    /** The grip that carries a card, in edit mode. */
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
 * Today, as the person arranged it. `DESIGN.md` 21.
 *
 * **Exactly one lead slot at the top, then the card field.** 21.1: a free-form
 * dashboard breaks law 1, because if six equal cards can be stacked then no
 * screen has one thing first. The resolution is a fixed structure with free
 * contents, and the singularity is not a convention here: the lead comes from
 * `Repository.TodayLayout`, which has nowhere to put zero or two.
 *
 * **The lead wears the lead costume and not the card costume**, per 21.1 and
 * the `TodayLead` component. It sat in a wide card for a while, which put the
 * most important thing on the screen at exactly the weight of the four things
 * under it, and rule 15 calls uniform weight what it is: the whole job of
 * sorting pushed onto somebody already exhausted.
 *
 * **Search and capture keep their places regardless of layout.** 21.1: finding
 * and recording are the two acts that must never move, so the search door sits
 * under the lead and the gold capture button sits in the shell, while
 * everything between them is the person's to arrange.
 *
 * **This screen never rearranges anything.** 21.8. It renders the order it is
 * given and does nothing else with it: there is no promotion, no injection, no
 * sorting by recency or urgency, and no card appears because the data changed.
 * The difference between a quiet day and the morning of an appointment is data
 * inside a layout the person owns, and that distinction is the whole trust
 * model of the surface.
 *
 * **Nothing here interprets.** No trend is called good, nothing is colored by
 * value, and quiet is allowed to be good news rather than being dressed up as
 * a problem to fix.
 */
@Composable
fun TodayFieldScreen(
    layout: Repository.TodayLayout,
    /** One answer per card instance, keyed by the card's own id. */
    answers: Map<String, Repository.TodayAnswer>,
    onOpen: (Repository.TodayCard) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Opens search, which is a whole screen with its own field.
     *
     * **`MASTER_SPEC.md` 4.8 puts universal search at the top of Today**, and
     * 21.1 says it stays there whatever the person does to the cards. When this
     * surface replaced the previous Today it arrived without one at all, so on
     * a real notebook there was no way to search from the front door.
     */
    onSearch: () -> Unit = {},
    /**
     * Saves a rearranged layout, in the order given, the first card leading.
     *
     * **Called once, from Done.** 21.6 screen 5: nothing saves behind your
     * back, so every change in edit mode is held here and written in one go.
     */
    onSave: (List<Repository.TodayCard>) -> Unit = {},
    /**
     * Opens the gallery, handing it the cards already on the draft.
     *
     * **The draft rather than the saved layout**, so a card added and then a
     * card removed in the same edit do not fight over what is already there.
     */
    onAddCard: (List<Repository.TodayCard>) -> Unit = {},
    /**
     * What changed since the person was last here.
     *
     * **The same summary the app already had**, computed once in the shell and
     * handed here, rather than a second digest written for this surface. Two
     * sentences about the same week that disagree is worse than one.
     */
    digest: com.kamsiob.healthtrail.data.Digest.Summary =
        com.kamsiob.healthtrail.data.Digest.nothing,
    /**
     * What day it is, for the lead's eyebrow.
     *
     * **Passed in rather than read here**, so a test can say what today is and
     * the screen has no clock of its own.
     */
    today: LocalDate = LocalDate.now(),
    /**
     * Whose day this is, for the masthead. Null before setup names anybody.
     *
     * **The screen says the person's name, D170.** Today opened with the word
     * "Today" in a chip, which is the one word already printed on the active
     * navigation tab an inch below it. A masthead that says whose day it is
     * puts the person at the top of the app that exists for them, and it is
     * what the round four mockups led with.
     */
    subjectName: String? = null,
    /**
     * Whether anything has ever been written down.
     *
     * **Only the digest asks**, and only to avoid telling somebody on their
     * first morning what has changed since a visit they never made. Counted in
     * the shell, which already has the section totals, rather than inferred
     * here from every card being empty: a broken query would look identical.
     */
    hasAnything: Boolean = true,
    /**
     * The care team, for the source picker and for what a chosen card shows.
     *
     * **The same roster the care team screen draws**, so a name on Today and a
     * name in the section can never be two different readings of one row.
     * Archived people are already out of it, which is the person saying they
     * are no longer involved: they are not offered as a choice, and a card
     * already pointing at one keeps rendering its source-closed rung from the
     * answer rather than being quietly repointed.
     */
    people: List<Repository.Person> = emptyList(),
    /**
     * Opens the dialer with a number filled in, per 21.7 and `DESIGN.md` 9.1.
     *
     * **`ACTION_DIAL` and never `ACTION_CALL`**, which the shell owns: nothing
     * happens until the person presses the green button themselves, so they see
     * what is about to go out before it does. The same argument the calendar
     * hand-off makes for `ACTION_INSERT`, and it holds harder for a phone call.
     */
    onDial: (String) -> Unit = {},
    /**
     * Told when this screen enters and leaves arranging.
     *
     * **Because the capture button is the shell's and the mode is this
     * screen's.** The button floats over the field, so while cards are being
     * moved it sits on top of them, and at a large system font it covered the
     * words on the card beneath it. Somebody rearranging their front door is
     * not also writing something down.
     */
    onArrangingChanged: (Boolean) -> Unit = {},
) {
    val colors = HealthTrail.colors
    val strings = LocalStrings.current

    // **Edit mode is entered by a visible button**, 21.6 screen 5. Touch and
    // hold is a shortcut and never the only path, because a gesture nobody is
    // told about is a feature that does not exist for most people.
    var editing by rememberSaveable { mutableStateOf(false) }

    // **Reported rather than pushed at every call site.** `editing` is set from
    // five places: the Arrange button, Done, Cancel, back, and a hold on any
    // card or the lead. Telling the shell from each of them is five chances to
    // miss one, and the one missed would leave the capture button hidden on a
    // screen nobody is arranging.
    LaunchedEffect(editing) { onArrangingChanged(editing) }

    // **And put back when this screen goes away.** Leaving Today mid arrangement
    // by tapping another tab would otherwise take the hidden button with it.
    DisposableEffect(Unit) { onDispose { onArrangingChanged(false) } }

    // **The staged layout.** Held here while editing and written once, so a
    // person can move three cards and change their mind about all of them.
    var draft by remember(layout, editing) { mutableStateOf(layout.all) }

    // **Adding a card writes immediately and comes back through the layout**,
    // rather than being staged like a move. A person who taps Add expects the
    // card to be there; staging it would mean the gallery's Add did nothing
    // visible until Done, which is the opposite of what that button says.

    // **Which card is choosing a source**, by id, or null. 21.6 screen 7 puts
    // the source picker in a card's options, and a card's options are what edit
    // mode puts on the card itself.
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    // **Which card has its options open**, by id, or null. 21.6 screen 7: one
    // sheet holds a card's whole life, and it is opened from the card itself.
    var options by rememberSaveable { mutableStateOf<String?>(null) }

    // **What a card says while it is being edited.** The answers were read once
    // when Today gained focus, so a staged pick would leave the card naming the
    // person it pointed at before, and nothing on the screen would say the
    // choice had landed. The roster is already here for the picker, so the card
    // is worded from it while editing and from the record the rest of the time.
    fun answerFor(card: Repository.TodayCard): Repository.TodayAnswer? =
        staged(card, editing, people)
            ?: answers[card.id]
            ?: digestAnswer(card, digest, strings, hasAnything)

    // **One column once the type is large enough that half a screen is not a
    // card.** 21.6 screen 8: the field reflows and every card renders at full
    // width with nothing clipped, and the layout order is preserved exactly,
    // which a one column grid gives for free. **Designed, not endured.**
    //
    // At scale 2.0 a half width cell on a 360dp screen is about 170dp, and a
    // card there is a tab, a number and a wrapped three word line stacked into
    // a column barely wider than the words. The threshold is 1.5 because that
    // is where the second column stops being able to hold a card rather than
    // where the person has reached the end of the slider: somebody at 1.7 has
    // the same problem as somebody at 2.0 and should not have to go further to
    // get the fix.
    val oneColumn = LocalDensity.current.fontScale >= WIDE_TYPE_SCALE

    // **Where the field is, so a drag knows what it is over.** The grid's own
    // layout information is the only honest source for that: it is already
    // mirrored in right to left, already reflowed to one column at large type,
    // and already knows where a card is after the last swap. Computing target
    // positions by hand would be a second layout that has to agree with the
    // real one, and it would be the half that gets right to left wrong.
    val gridState = rememberLazyGridState()

    // Which card the finger is carrying, by id, and how far it has moved from
    // where the grid put it. Null when nothing is being dragged.
    var dragging by remember { mutableStateOf<String?>(null) }

    /**
     * The card that has just been let go and is springing back to its slot.
     *
     * **Separate from [dragging]**, because the card is no longer under a
     * finger but is still not where it belongs, and those are different
     * states: one follows the hand, the other follows physics. D169.
     */
    var settling by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // **Dropped the moment edit mode ends**, so a card cannot be left visually
    // offset from the slot it actually occupies.
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
    // to an arranged home screen on the phone, and a mode with no way out but
    // one named control at the top of a scrolling grid is a mode somebody gets
    // stuck in. Cancel is still there for changing your mind, and it is the one
    // that throws the draft away.
    BackHandler(enabled = editing) {
        onSave(draft)
        editing = false
    }

    /**
     * Moves the dragged card to whatever slot the finger is now over.
     *
     * **The center of the card decides, not the finger.** A finger on the
     * bottom edge of a tall card is already inside the card below it, and
     * reordering on that reads as the list flinching away from the touch.
     *
     * **The lead is not a drop target.** 21.1: promoting demotes the card that
     * was there, which is a decision with a consequence, so it stays its own
     * named action rather than something a slip of the thumb can do.
     */
    fun dragTo(id: String) {
        val layout = gridState.layoutInfo
        val held = layout.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        val centerX = held.offset.x + held.size.width / 2f + dragOffset.x
        val centerY = held.offset.y + held.size.height / 2f + dragOffset.y
        val over = layout.visibleItemsInfo.firstOrNull { other ->
            other.key != id &&
                centerX >= other.offset.x &&
                centerX <= other.offset.x + other.size.width &&
                centerY >= other.offset.y &&
                centerY <= other.offset.y + other.size.height
        } ?: return

        val from = draft.indexOfFirst { it.id == id }
        val to = draft.indexOfFirst { it.id == over.key }
        // Both have to be real field cards. The header, the lead slot and the
        // search door are grid items too, and their keys are not card ids.
        if (from < 1 || to < 1 || from == to) return
        draft = draft.toMutableList().apply { add(to, removeAt(from)) }
        // **The offset resets because the card has moved under it.** Keeping it
        // would carry the whole distance already traveled into the new slot and
        // the card would shoot past the finger.
        dragOffset = Offset.Zero
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (oneColumn) 1 else 2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .testTag(TodayFieldTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Space.cardGap),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            val fullWidth = GridItemSpan(if (oneColumn) 1 else 2)

            item(span = { fullWidth }, key = "today-header") {
                Column {
                    Spacer(Modifier.height(Space.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Today belongs to no section, so gold and the base
                        // ladder, per 4.3. **A chip and not a display title**:
                        // the lead sentence directly under it is the display
                        // scale thing on this screen, and a heading at the same
                        // weight two lines above it would give the screen two
                        // first things. The word is already on the active
                        // navigation tab besides.
                        // **The masthead, D170.** A gold overline carrying the
                        // date, and whose day it is at display weight under
                        // it. This is the structural half of the redesign that
                        // tokens could not deliver: new color and new type on
                        // an old arrangement is still the old screen.
                        Column(modifier = Modifier.weight(1f)) {
                            // **Two faces on this screen, not three.** D172. The
                            // date was set in the mono face, which put a
                            // typewriter line directly above a display-weight
                            // sans name and a body-face sentence under that.
                            // The owner's words were that the fonts do not mix
                            // well, and this was where it was worst: mono is for
                            // figures that must line up in a column, and a date
                            // in a masthead is prose.
                            Text(
                                text = EventDateText.masthead(strings, today),
                                style = HealthTrail.type.bodyS,
                                color = HealthTrail.colors.goldInk,
                            )
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                text = subjectName
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { strings("today.masthead", "name" to Bidi.isolate(it)) }
                                    ?: strings["today.masthead.noname"],
                                style = HealthTrail.type.displayL,
                                color = HealthTrail.colors.ink,
                            )
                        }
                        Spacer(Modifier.width(Space.s))
                        if (editing) {
                            // **A flow row, because three pills do not fit.**
                            // They were unweighted children of a fixed row, so
                            // at font scale 2.0 the first took what it wanted
                            // and Done, the only control that saves the
                            // arrangement, was measured to zero and disappeared.
                            // There was no way out of edit mode at that size.
                            // #371.
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Space.s),
                                verticalArrangement = Arrangement.spacedBy(Space.s),
                            ) {
                                TextAction(
                                    label = strings["today.add"],
                                    onClick = { onAddCard(draft) },
                                    modifier = Modifier.testTag(TodayFieldTags.ADD),
                                )
                                TextAction(
                                    label = strings["today.edit.cancel"],
                                    onClick = { editing = false },
                                )
                                // **Done carries the weight, and it is the only
                                // one that does.** Three outlined pills at one
                                // weight made the person sort them, which is
                                // exactly what rule 15 says uniform weight
                                // costs. Keeping the arrangement is what this
                                // mode is for; adding a card and throwing the
                                // whole edit away are the other two things you
                                // might do, and they are not the same size.
                                //
                                // **The filled slot is free here**, because the
                                // capture button stands down while Today is
                                // being arranged, so this is still one filled
                                // action on the screen.
                                FilledButton(
                                    label = strings["today.edit.done"],
                                    onClick = {
                                        onSave(draft)
                                        editing = false
                                    },
                                    modifier = Modifier.testTag(TodayFieldTags.DONE),
                                )
                            }
                        } else {
                            TextAction(
                                label = strings["today.edit"],
                                onClick = { editing = true },
                                modifier = Modifier.testTag(TodayFieldTags.EDIT),
                            )
                        }
                    }
                    if (editing) {
                        Text(
                            text = strings["today.edit.hint"],
                            style = HealthTrail.type.bodyS,
                            color = colors.ink2,
                            modifier = Modifier.padding(top = Space.xs),
                        )
                    }
                }
            }

            val shown = if (editing) draft else layout.all
            val lead = shown.first()
            val field = shown.drop(1)

            item(span = { fullWidth }, key = "today-lead-slot") {
                LeadSlot(
                    card = lead,
                    answer = answerFor(lead),
                    today = today,
                    onOpen = onOpen,
                    onDial = onDial,
                    editing = editing,
                    // **Down is the lead's only control**, and it is how a lead
                    // is demoted: 21.1 says promoting a card demotes the one
                    // that was there, and this is the same move made from the
                    // other end. There is no Remove, because there is never
                    // zero, and no size chips, because a lead is not a size the
                    // person chose while it is leading.
                    canMoveDown = field.isNotEmpty(),
                    onMoveDown = {
                        draft = draft.toMutableList().apply { add(1, removeAt(0)) }
                    },
                    onArrange = { editing = true },
                )
            }

            // **Under the lead and above the field, always in that place.**
            // 21.1. Hidden only while editing, per grid screen 05, because
            // leaving for search mid-edit would throw away an unsaved draft and
            // a door that costs you your work is worse than no door.
            if (!editing) {
                item(span = { fullWidth }, key = "today-search") {
                    UniversalSearchDoor(
                        onOpen = onSearch,
                        modifier = Modifier.testTag(TodayFieldTags.SEARCH),
                    )
                }
            }

            items(
                count = field.size,
                key = { field[it].id },
                span = { index ->
                    if (oneColumn) fullWidth
                    else GridItemSpan(if (field[index].size == "small") 1 else 2)
                },
            ) { index ->
                val card = field[index]
                // Its place in the whole surface, which is what a move has to
                // act on: the field is the layout with the lead taken off.
                val position = index + 1
                CardFor(
                    card = card,
                    answer = answerFor(card),
                    // **Full width is wide.** A card the person made small is
                    // rendering at full width in this layout, and 21.3 ties the
                    // second line of context to width rather than to the label
                    // on the size chip. Leaving it at small would give a full
                    // width card one line and a lot of nothing.
                    size = if (oneColumn && card.size == "small") {
                        CardSize.WIDE
                    } else {
                        CardSize.of(card.size)
                    },
                    onOpen = onOpen,
                    onDial = onDial,
                    modifier = Modifier
                        // **The others make room, and that is the half of a
                        // reorder that was missing.** A card moved and every
                        // other card was simply somewhere else the next frame,
                        // so nothing on the screen said what had happened.
                        //
                        // **Only while the screen is being arranged**, and the
                        // card in the hand is excluded because it is already
                        // following the finger and two things moving it fight.
                        // Animating placement the rest of the time buys nothing
                        // and costs correctness: the field reflows to one column
                        // at a large system font, and with placement animating
                        // there is a moment where a card has not arrived at its
                        // slot, so anything reading positions then, including a
                        // person's eye, sees an order the layout does not have.
                        // Found by the reflow test, which read the last card as
                        // the first.
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
                                // **It lifts as it is picked up**, D169. A
                                // card that changes nothing when it leaves the
                                // grid reads as the screen having glitched
                                // rather than as something now in the hand.
                                // The phone's own home screen does exactly
                                // this: the icon swells slightly and casts a
                                // shadow while it is carried.
                                scaleX = liftScale
                                scaleY = liftScale
                                shadowElevation = liftShadowDp.toPx()
                                shape = Radius.cardLarge
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
                        // **It settles rather than snapping.** D169, the
                        // owner: "there's no real animation during the
                        // movement. it's just sudden jerk." Letting go used to
                        // set the offset to zero in one frame, so a card
                        // carried half a screen away teleported home. It
                        // springs back to its slot now, and the lift eases out
                        // with it.
                        settling = card.id
                        dragging = null
                    },
                    today = today,
                    editing = editing,
                    onOptions = { options = card.id },
                    onRemove = {
                        draft = draft.toMutableList().apply { removeAt(position) }
                    },
                    onArrange = { editing = true },
                    ordinal = index,
                )
            }

            item(span = { fullWidth }, key = "today-fab-clearance") {
                Spacer(Modifier.height(Space.fabScrollClearance))
            }
        }

        // **Who this card shows.** 21.6 screen 7 puts the source picker in a
        // card's options, and this is the choice between the two answers 21.7
        // draws for the care team card: one person and their number, or the row
        // of everyone.
        //
        // **The same picker the capture form uses**, rather than a chip row
        // that becomes a wall at ten people: a year five notebook has more
        // names than a card has width, and the person opening this is looking
        // for one they already have in mind.
        //
        // **Staged like every other change in edit mode**, per 21.6 screen 5:
        // nothing saves until Done. The card re-words itself from the roster
        // straight away, so the choice is visible the moment it is made.
        // **One card's whole life, opened from the card.** 21.6 screen 7. Every
        // change goes to the draft, so this sheet's own Done closes it and
        // nothing else: what saves is Done on Today.
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
                        draft = draft.toMutableList()
                            .also { it[at] = it[at].copy(size = size) }
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
                    // **The source picker, and only where a card takes one.**
                    // The care team card is the one whose two variants are a
                    // choice about a person rather than about a size; a measure
                    // or a project card is one per source by construction, since
                    // it arrives from the gallery already pointing at the thing
                    // the person picked there.
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
}

/**
 * The one thing at the top. `DESIGN.md` 21.1.
 *
 * **The same answer the card would show, in the lead costume.** The body is
 * composed once for both, so the lead and the field can never word the same
 * answer differently, which is what would happen the first time either was
 * touched if there were two copies of it.
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

    // **The day for the digest, the card's own name for anything else.** The
    // digest's eyebrow is what day it is, which is a fact worth stating above a
    // sentence about today and is what the grid draws. Naming it "Today" there
    // would be the third time that word appears on the screen, after the chip
    // and the navigation tab. A card the person promoted says which card it is,
    // so they can see what they put at the top.
    // **The day only for the digest**, and null otherwise, because for any
    // other card the eyebrow is the tab itself and the reader's sentence is
    // built from that tab's own raw parts below. Carrying the joined eyebrow
    // into those parts would say the card's name twice and nest its isolates.
    // **The masthead already says the date**, D170, so the digest repeating it
    // as its own eyebrow printed the day twice within an inch. The digest's
    // eyebrow is its own name now, like every other card's.
    val day = null
    val eyebrow = day ?: tab

    val shown = worded(card.type, answer, today)

    TodayLead(
        eyebrow = eyebrow,
        hue = leadHue(hueForCard(card.type)),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest.
        // **Raw parts, joined once.** The tab is already a joined string, so
        // passing it here wrapped it a second time and the isolate marks
        // nested: the reader's sentence came out `⁨⁨Next up⁩⁩ · ...`. Seen by
        // walking the semantics tree on the phone, and invisible in English.
        description = Bidi.join(
            listOf(strings[cardTabKey(card.type)], answer?.sourceName, day) + answerParts(
                shown,
                strings,
                cardType = card.type,
                sourced = card.sourceId != null,
                showItems = true,
                // **The lead draws its chart**, so a reader hears a chart where
                // the eye sees one. This said false while the lead could not
                // draw one at all, and both halves were wrong together.
                drewChart = (shown?.series?.size ?: 0) > 1,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        openLabel = strings("today.card.open", "name" to tab),
        onOpen = { onOpen(card) },
        modifier = Modifier.testTag(TodayFieldTags.LEAD),
        speaksAsOneNode = !editing,
        // **The biggest thing on the screen answers the hold too.** A gesture
        // that works everywhere except on the first thing a thumb lands on
        // teaches somebody the gesture is unreliable.
        //
        // long-press-twin: the Arrange action in this screen's header. D155.
        onLongPress = if (editing) null else onArrange,
        longPressLabel = strings["today.arrange.hold"],
        // **A promoted care team card brings its number with it.** The lead is
        // full width, which is what 21.3 ties the inline action to.
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
            // promotes a chart to the lead, and the lead could not draw one:
            // `tall` was left at its default, so a measure at the top of
            // somebody's Today listed its readings instead. The screen the
            // design asks for could not exist. The lead is full width and sits
            // at display scale, which is more room than a tall card has.
            tall = true,
            hue = hueForCard(card.type),
        )

        if (editing && canMoveDown) {
            Row(modifier = Modifier.padding(top = Space.s)) {
                EditAction(
                    label = strings["today.edit.down.short"],
                    spoken = strings("today.edit.down", "name" to tab),
                    onClick = onMoveDown,
                )
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
    /** Takes the card off Today, from the dot in its corner. */
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

    TodayCard(
        tab = tab,
        hue = hueForCard(card.type),
        // **An empty answer centers itself in the square.** The none line sat
        // on the bottom edge under a void, which on a fresh notebook made the
        // whole first screen read as four broken boxes. #376.
        centerContent = answer == null || (answer.isEmpty && !answer.sourceClosed),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest. The same defect was fixed on the
        // project screen four commits ago and written straight back in here.
        // **Raw parts, joined once**, for the same reason as the lead: `tab` has
        // already been through `Bidi.join`, and joining a joined string nests
        // the isolates.
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
        // **In edit mode the card is a door to its own options**, grid screen
        // 07, rather than to the section its answer lives in. Leaving it
        // opening the section would take somebody out of an unsaved edit.
        onOpen = { if (editing) onOptions() else onOpen(card) },
        openLabel = if (editing) {
            strings("today.options.open", "name" to tab)
        } else {
            strings("today.card.open", "name" to tab)
        },
        size = size,
        modifier = modifier
            .arrangeTilt(arranging = editing, ordinal = ordinal, held = dragging)
            // **The card itself is what you carry**, which is the whole of the
            // owner's note about the phone: nobody grabs a handle to move an
            // icon, they hold the icon. The drag detector sits before the tap,
            // so a finger that does not travel past touch slop is still a tap
            // and still opens this card's options.
            .then(
                if (editing) {
                    Modifier.pointerInput(card.id) {
                        // **After a long press, not on any drag.** D169, and
                        // it is the owner's complaint exactly: "there's very
                        // little room to be able to actually scroll up and
                        // down when you're trying to rearrange it and so when
                        // I try to move my finger up to move something I'm
                        // accidentally grabbing and moving a tile."
                        //
                        // `detectDragGestures` claimed the first drag in
                        // arrange mode, so the grid could not be scrolled at
                        // all while arranging: every attempt picked a card up.
                        // **The phone's own home screen does not work that
                        // way.** In its edit mode a plain swipe still scrolls
                        // and a card is picked up by pressing and holding it,
                        // which is what this does now. Two gestures, two
                        // meanings, no ambiguity.
                        // long-press-twin: the grip handle on the card, which
                        // drags immediately and is visible throughout arrange
                        // mode, so nothing here is reachable only by holding.
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                // **It answers in the hand before it answers
                                // on screen.** Picking something up is the one
                                // moment in this app where a person's finger
                                // is committed before their eye has caught up.
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
        //
        // long-press-twin: the Arrange action in this screen's own header, a few
        // lines up, which enters the same mode. The hold is the shortcut and the
        // header is the path a reader and switch access take. D155.
        onLongPress = if (editing) null else onArrange,
        longPressLabel = strings["today.arrange.hold"],
        corner = if (editing) {
            {
                RemoveDot(
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
        // **The dialable number, at wide and tall only**, per 21.3's budget of
        // one inline action and two touch targets. It goes in the action slot
        // rather than in the answer, because the answer is silenced so the card
        // is one stop and a control inside that silence is unreachable.
        action = shown?.phone
            ?.takeIf { dialable(card, shown, size) }
            ?.let { number -> { DialPill(number, card.id, onDial) } },
    ) {
        // The second line appears at wide only, per 21.3: at small the card
        // carries one answer and one line of context, and that line is the
        // answer's own.
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
            hue = hueForCard(card.type),
        )

        // **In edit mode a card carries a grip and nothing else.** Grid screen
        // 05 and the spec under it: the remove dot and the drag handle live on
        // the card, and size, position, source and removal live in the card's
        // own options sheet, screen 07. **Inline, all of it was a wall**: three
        // chips and four named actions on every card is about a hundred and
        // forty controls on a twenty card Today, every one of them the same
        // weight, which is precisely what rule 15 says uniform weight does.
        //
        // **The card itself opens its options**, which is what "opened from the
        // card in edit mode" means and what makes this one target rather than
        // two. The remove dot is the one shortcut on the face, in the corner
        // where the chevron sits when the card is a door.
        // **The word "Options" used to sit under every card here and it is
        // gone.** Seen on the phone: five cards being arranged carried five
        // copies of the same word, in mono, under five different answers, which
        // is the app explaining its own mechanics on the screen rather than
        // absorbing them. Rule 20, and the owner's note that arranging is
        // cluttered.
        //
        // **Nothing was lost.** The screen's own hint says "Tap one for its
        // options" once, at the top, where somebody entering the mode reads it.
        // A reader is told the same thing on each card by [openLabel], which
        // says what the tap does and is a better sentence than a loose noun.
        // The remove mark in the corner is the visible affordance, and it is
        // the one a phone shows.
        //
        // **The grip went with it.** It was a 48dp target in the corner of a
        // card that is itself a target, which is the opposite of the phone:
        // nobody grabs a handle to move an icon. Move up and move down in the
        // options sheet are still the reorder path that works one handed, with
        // a reader on, and with switch access, and 23.2 is why that path rather
        // than the drag is the one that must exist.
    }
}

/**
 * What the record says, in one place for both costumes.
 *
 * **The lead and a card show the same answer at different scales**, never
 * different answers. Two renderings of one query is two renderings that
 * disagree the first time either is touched.
 */
@Composable
private fun AnswerBody(
    answer: Repository.TodayAnswer?,
    /** The card's type, for the one line of context that names what the count counts. */
    cardType: String,
    /**
     * Whether this card points at one row.
     *
     * **It is what tells the care team card's two variants apart**, 21.7: with
     * a source it is one chosen person and their number, without one it is the
     * row of everyone. The type is the same card either way, because the
     * question is the same question.
     */
    sourced: Boolean,
    lead: Boolean,
    /**
     * Whether this card is wide enough for a second line of context.
     *
     * **Small still gets one**, per 21.3: half width carries one answer and one
     * line of context, not an answer alone. A small Next up card showing a name
     * and nothing else is not answering the question it names.
     */
    showDetail: Boolean,
    /** Whether the card has the height for a chart. Tall only, per 21.3. */
    tall: Boolean = false,
    /** The card's hue, so a chart drawn here is the section's line and not a new color. */
    hue: TabHue = wholeAppHue(),
    /**
     * Where the person's own pictures are read from, for the documents card.
     *
     * Opened against the context the same way `DocumentsScreen` does, rather
     * than threaded down from the shell, because it is a reader over a folder
     * and not state anybody owns.
     */
    attachments: Attachments = Attachments.open(LocalContext.current),
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val strings = LocalStrings.current

    val spine = drewSpine(cardType, answer, tall)

    // **When it is, which nothing rendered until 2026-08-08.** `whenEdtf` is
    // read from the record for Next up, Milestones and a measure, carried
    // through `TodayAnswer`, and was dropped on the floor by every renderer, so
    // the card whose whole question is "what is the next dated thing" showed a
    // name and no date. Same family as a control that kept its repository call
    // through a supersession: read, carried, compiled, never on screen.
    //
    // **Through `EventDateText`, so precision is never invented**, per rule 17
    // and 9.2. A month stays a month here exactly as it does in the trail.
    val whenText = answer?.whenEdtf?.takeIf { it.isNotBlank() }
        ?.let { EventDateText.render(strings, it) }

    // **A zero is not an answer worth shouting.** Rendering the number whatever
    // it was put a large 0 directly above "Nothing waiting", which says the
    // same thing twice and, at that weight, reads as a score on a person who
    // has just started. The empty rung is the sentence alone.
    val count = answer?.count?.takeIf { it > 0 }
    if (count != null && answer.title == null) {
        Text(
            text = Bidi.isolate(count.toString()),
            style = if (lead) type.hero else type.monoL,
            color = colors.ink,
        )
        // **A number with no noun is not an answer.** 21.3 gives every size one
        // line of context and the smallest size is not exempt: a card reading
        // "6" alone makes the person supply the word themselves, on a screen
        // whose whole job is that they should not have to. The grid says "on
        // the current list" under the 6, and this is that line.
        countLineKey(cardType)?.let {
            Text(text = strings[it], style = type.bodyS, color = colors.ink2)
        }
    }
    answer?.title?.takeIf { !spine }?.let {
        Text(
            text = Bidi.isolate(it),
            style = if (lead) type.hero else type.displayS,
            color = colors.ink,
            // **The lead wraps freely and a card does not.** D105: a fixed cap
            // is a cap at the smallest type size and a truncation at the
            // largest, and the lead's sentence is the one thing the screen
            // exists to say. A card in a half-width cell has four others beside
            // it and cannot grow to six lines without taking the fold.
            maxLines = if (lead) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // **The source-closed rung**, 21.4. It says so plainly and keeps working as
    // a door, and the card stays until the person's own hand removes it.
    if (answer?.sourceClosed == true) {
        Text(
            text = strings["today.card.source_closed"],
            style = type.bodyS,
            color = colors.ink2,
        )
    }
    if (answer == null) {
        // **Not the same thing as nothing waiting.** A card whose question
        // could not be asked has not learned that the record is empty, and
        // saying "Nothing waiting" here would be the app asserting something
        // false about somebody's record. One broken query once made every card
        // on this surface say it at the same time.
        Text(
            text = strings["today.card.unread"],
            style = if (lead) type.hero else type.bodyS,
            color = if (lead) colors.ink else colors.ink2,
        )
    } else if (answer.isEmpty && !answer.sourceClosed) {
        // **A calm state, never a scold**, 21.4. Quiet is allowed to be good
        // news, and this says so rather than leaving a hole. At the lead it is
        // said at lead scale, because the answer to the question the person
        // opened the app to ask is still the answer when it is a quiet one.
        //
        // **Each card says its own nothing.** Every card on the surface saying
        // "Nothing waiting" is the app answering fourteen different questions
        // with one sentence, and on a new notebook that is the whole screen
        // saying the same four words in a column. 21.4 wants the rung to name
        // what is not there, and "Nothing scheduled" also tells the person
        // which door this is without them having to work it out.
        //
        // **No count of what has not been done**, per rule 13: an unfilled slot
        // reads as not yet, never as an error and never as a tally.
        Text(
            text = strings[emptyLineKey(cardType)],
            style = if (lead) type.hero else type.bodyS,
            color = if (lead) colors.ink else colors.ink2,
        )
    }

    // **The recent shape, at tall only.** 21.7 asks the measure card "what is
    // the latest value, and its recent shape", and 21.3 puts a chart at tall
    // and nowhere smaller: a line drawn across half a screen width is a
    // decoration rather than a shape anybody can read.
    //
    // **The same plot the Progress screen draws**, through the same
    // `chartPoints`, so two charts of one measure cannot disagree about the
    // same silence. Every chart rule comes with it: one color from the section
    // hue and never from the value, no target, no zone, no axis, and **gaps
    // drawn as gaps**, because joining the dots across three missing months
    // invents a line nobody recorded.
    val drewChart = tall && answer != null && answer.series.size > 1
    if (drewChart) {
        Spacer(Modifier.height(Space.xs))
        Plot(
            readings = chartPoints(answer.series),
            line = hue.base,
            height = ChartHeight.standard,
        )
    }

    // **The list, at wide and tall only.** 21.3 uses this card as its example:
    // the medications card at small is a count and at wide it is the list, and
    // both are the one question "what is on the list right now" asked once.
    // Growing reveals more of the same answer, never a new kind of content, so
    // this is never a different query and never a feed.
    // **A chart or a list, never both.** 21.3 gives tall a chart, a mini spine,
    // or a short list, and a card carrying a line and then the same readings
    // written out underneath it says one thing twice and stops being a shape.
    // **The row of everyone, which is the care team card's other answer.** 21.7
    // draws it as faces rather than as a column of names, and rule 22 puts an
    // avatar wherever the app shows a person. Three names written out is the
    // same information taking three lines on a card that has two.
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
                Avatar(name = item.label, hue = hue, size = AvatarSize.row)
            }
            val hidden = (answer.count ?: 0) - answer.items.size
            if (hidden > 0) {
                AvatarOverflow(
                    // **Isolated, because a plus is a neutral character.** In
                    // Arabic the paragraph direction reordered "+12" to "12+",
                    // which is a different claim: one is a remainder and the
                    // other reads as a floor. Seen on the phone in Arabic and
                    // invisible in every other language.
                    label = Bidi.isolate(
                        strings("today.card.care_team.overflow", "count" to hidden),
                    ),
                    hue = hue,
                    size = AvatarSize.row,
                )
            }
        }
    }

    // **The mini spine, at tall only.** 21.7. The last few entries as waypoints
    // on the trail's own gold route, with the gap markers 5.2.4 exists for: two
    // calls a week apart read as a week of calls, and the same two rows four
    // months apart read as somebody left alone until something happened. A list
    // shows the same rows either way, which is the whole reason this card is
    // drawn rather than listed.
    if (spine) {
        MiniSpine(items = answer!!.items, hue = hue)
    }

    // **The care team card draws its people rather than listing them**, above,
    // and the trail card draws them as a spine at tall, so neither is in the
    // general list. Both at once would say the same three things twice.
    if (showDetail && !drewChart && !spine && cardType != "care_team" &&
        answer != null && answer.items.isNotEmpty()
    ) {
        Column(
            modifier = Modifier.padding(top = Space.xs),
            // **Room between rows only where the rows are pictures.** A list of
            // medications is text and reads as a block; three thumbnails
            // touching each other read as one strip of paper rather than three
            // documents. Everything else keeps the tighter rhythm it had.
            verticalArrangement = if (cardType == "recent_documents") {
                Arrangement.spacedBy(Space.xs)
            } else {
                Arrangement.Top
            },
        ) {
            // **The trail card's newest entry is already the answer above it**,
            // so the list starts at the second. Every other card lists all of
            // what it has. Dropped here rather than in the query, because at
            // tall all three are waypoints on the spine and a query that had
            // skipped one would have left the spine without its head.
            val listed = if (cardType == "trail_lately") {
                answer.items.drop(1)
            } else {
                answer.items
            }
            for (item in listed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // **The person's own paper, at thumbnail size and no
                    // larger.** 21.7 says this card never renders private
                    // content bigger than this, which is the whole reason the
                    // hash travels and the screen decides the size. A document
                    // nobody photographed gets the section's fallback drawing
                    // rather than a hole.
                    if (cardType == "recent_documents") {
                        // **`ROW_SIZE`, which is the component's own smallest
                        // named size**, rather than a spacing token borrowed as
                        // a dimension. At 24dp a photograph of a letter is a
                        // speck: the card claimed to show the person's paper
                        // and showed a dot beside a title, which is the shape
                        // of a rule kept in the code and broken on the screen.
                        // Seen on the phone, and the code read as correct.
                        Thumbnail(
                            sha256 = item.imageSha,
                            attachments = attachments,
                            section = Repository.Section.DOCUMENTS,
                            size = ROW_SIZE,
                        )
                        Spacer(Modifier.width(Space.s))
                    }
                    Text(
                        // The parts joined here rather than in the query,
                        // because joining is wording. `Bidi.join` isolates each
                        // part, so a medication somebody typed in Arabic keeps
                        // its own direction beside a dose typed in English, and
                        // a date never renders as the EDTF it is stored as.
                        text = Bidi.join(
                            item.label.ifBlank { strings["project.steps.ungrouped"] },
                            item.note,
                            item.noteEdtf?.let { EventDateText.render(strings, it) },
                        ),
                        style = type.bodyS,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // **Amounts at the end edge, in tabular mono.** 21.7 says
                    // amounts only at wide and right aligned, and section 7
                    // puts every amount in the mono face so a column of them
                    // lines up on the decimal point. `Alignment.End` rather
                    // than right, so it follows the reading direction.
                    item.amountMinor?.let { minor ->
                        Spacer(Modifier.width(Space.s))
                        Text(
                            text = Bidi.isolate(
                                formatMoney(strings, minor, item.currency ?: "USD"),
                            ),
                            style = type.mono,
                            color = colors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }
            // **What is not here, said rather than cropped.** The count is the
            // true total, so a card showing three of eleven says so. Silently
            // showing the first three would be the app deciding which
            // medications matter, which is exactly what it must not do.
            val hidden = if (answer.itemsSampleTheCount) {
                (answer.count ?: 0) - answer.items.size
            } else {
                0
            }
            if (hidden > 0) {
                Text(
                    text = strings("today.card.more", "count" to hidden),
                    style = type.bodyS,
                    color = colors.ink2,
                )
            }
        }
    }

    // **The context lines, and growing reveals more of the same answer.** 21.3:
    // the first line is the one line of context every size carries, the second
    // appears at wide and tall. **When it is comes first** where the record has
    // it, because a date is the context on every card that carries one, and the
    // detail follows. Neither is a new kind of content at the larger size.
    // A counted second line, worded here because its plural belongs to the
    // reader's language. The key is a literal on the repository side.
    val countedLine = answer?.detailKey?.let { key ->
        answer.detailCount?.let { strings(key, "count" to it) }
    }
    val lines = listOfNotNull(
        // **Not above a spine.** The newest entry's date is already the first
        // waypoint's eyebrow, and repeating it over the top of the drawing
        // would say the same date twice on a card whose whole point is that the
        // dates are laid out along a line.
        whenText?.takeIf { !spine },
        answer?.detail?.takeIf { it.isNotBlank() },
        countedLine,
        // **Not everybody has a number, and that is not a deficiency.** Rule
        // 13: an unfilled slot reads as not yet, never as an error, so the card
        // says what is true and stays a door to the person, where a number can
        // be added whenever there is one to add.
        noNumberLine(cardType, sourced, answer)?.let { strings[it] },
    )
        .let { if (showDetail) it else it.take(1) }
    for (line in lines) {
        Text(
            text = Bidi.isolate(line),
            style = type.bodyS,
            color = colors.ink2,
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
 * disagree: whether the spine draws, whether the title and the date above it
 * are suppressed so the card does not say the newest entry twice, and what a
 * reader hears. Section 9 is the whole reason the third one is in this list.
 *
 * **Tall only**, per 21.3: a spine is what tall is for, and drawing one across
 * a half-width cell is a decoration rather than a shape anybody can read.
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
 * **The same spine the trail draws, not a second drawing of it.** `SpineRow`,
 * `DistanceMarker`, the gold route with the trail's dash, and the node colors
 * the trail already gives a call, a visit and an incident: somebody who learns
 * the vocabulary on the trail reads this card without being taught twice, which
 * is what 5.2 means by a system rather than one screen's drawing.
 *
 * **The gap markers are the reason this is drawn and not listed.** 5.2.4: two
 * calls a week apart read as a week of calls, and the same two rows four months
 * apart read as somebody left alone until something happened. A list shows the
 * same rows either way. **Under fourteen days there is no marker at all**,
 * because a line between every pair is wallpaper.
 *
 * **No judgment and no color by value**, per rule 2 and 21.8. The marker is
 * arithmetic on two dates the person recorded, nothing says a gap was too long,
 * and no gap is colored.
 *
 * **A coarsely given date produces no marker.** Rule 17: the distance between
 * "sometime in April" and a day in June is not a number anybody gave, and a
 * confident "two months later" derived from it would be invented precision.
 *
 * **Not doors.** 21.3 gives tall three touch targets and the card is already
 * one; three more would be four. The card opens the trail, where every entry is
 * its own row and its own door.
 */
@Composable
private fun MiniSpine(items: List<Repository.TodayItem>, hue: TabHue) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type
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
                    // **The date and the kind, which is the eyebrow every trail
                    // row carries.** The kind is here because 2.2 says every
                    // color carries a word alongside it: the node is gold for a
                    // call and blue for a visit, and without the word the color
                    // is carrying that meaning on its own, which fails in
                    // grayscale and for a colorblind reader. The date renders
                    // through `EventDateText` and never as the EDTF it is
                    // stored as.
                    val eyebrow = Bidi.join(
                        item.noteEdtf?.let { EventDateText.render(strings, it) },
                        item.kind?.takeIf { it.isNotBlank() }
                            ?.let { strings[kindNameKey(it)] },
                    )
                    if (eyebrow.isNotBlank()) {
                        Text(
                            text = eyebrow,
                            style = type.bodyS,
                            color = colors.ink2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = Bidi.isolate(item.label),
                        style = type.bodyS,
                        color = colors.ink,
                        // **Two lines, not one.** These are sentences somebody
                        // wrote, and a card that exists to show what was said
                        // lately cutting each of them to a single line shows
                        // three beginnings. D105's rule read the other way: the
                        // cap is what is being read, so it is generous.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The dialable number, outlined. `DESIGN.md` 21.3 and 21.7.
 *
 * **The one inline action wide and tall are allowed**, and the card itself is
 * the other of the two touch targets that size gives. It sits in the card's
 * action slot rather than in its answer, because the answer is silenced for a
 * reader and a control inside that silence cannot be reached.
 *
 * **The number is the visible label**, because that is what somebody checks
 * before pressing it. A reader hears the verb as well: "5 5 5 0 1 4 2, button"
 * does not say what the button does.
 *
 * **It opens the dialer and places no call**, which is the shell's `ACTION_DIAL`
 * and the same argument the calendar hand-off makes in 9.1.
 */
@Composable
private fun DialPill(number: String, cardId: String, onDial: (String) -> Unit) {
    val strings = LocalStrings.current
    QuietButton(
        label = Bidi.isolate(number),
        onClick = { onDial(number) },
        modifier = Modifier
            .padding(top = Space.s)
            .testTag(TodayFieldTags.dial(cardId))
            // One stop, not two. The pill's own words are the number, and the
            // sentence a reader hears is the number with the verb in front.
            .semantics(mergeDescendants = true) {
                contentDescription = strings("careteam.call.number", "number" to number)
            },
    )
}

/**
 * The key for "nobody recorded a number for this person", or null.
 *
 * **Only where the card is answering for one person**, because the row of
 * everyone is not short of anything: it is answering "who" rather than "what is
 * their number". And never for somebody archived, whose card is already saying
 * the source is closed and would otherwise say two things about one absence.
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
 * disagree: whether the pill renders, and whether the card may still speak as
 * one node. A card that gave up its single-node reading without drawing a pill
 * would make a reader stop twice for nothing, and one that drew a pill while
 * keeping it would hide the pill from a reader entirely.
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
 * tab, and the grid heads a measure card "Progress · Weight" and a project's
 * date card "Waiver · the next date". Four project cards on one Today all
 * reading "Project" is four cards nobody can tell apart, and the name belongs
 * here rather than in the answer: the answer is the reading or the countdown,
 * and this is what it is a reading of.
 */
private fun tabFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    strings: Strings,
): String = Bidi.join(strings[cardTabKey(card.type)], answer?.sourceName)

/**
 * The catalog key for a card type's tab. **Literal, per [countLineKey].**
 *
 * `check_string_keys.py` cannot see a key built from a variable, and this file
 * asked for `"today.card.${card.type}"` for as long as the surface has existed.
 * `TodayCardKeyTest` now holds the schema's seventeen types to the catalog, so
 * the dynamic form is covered; this stays a `when` anyway, because the check
 * that reads the code should be able to see the code's keys.
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
    // A type the schema refuses cannot reach here, and `TodayCardKeyTest`
    // fails the build if the schema ever gains one this does not know.
    else -> "today.card.digest"
}

/**
 * The stored values in an answer, turned into words. `DESIGN.md` section 9.2.
 *
 * **A stored value is not display text and this is where that stops being
 * true.** A project's status arrives as `waiting`, which is a database value
 * and not a word anybody wrote, and a project date arrives as the date rather
 * than as the countdown 21.7 asks for. Rendering either one raw is the same
 * defect as putting EDTF on the screen, which this surface nearly did.
 *
 * **The countdown is computed here rather than in the query**, because how many
 * days away something is is a fact about now, and a query result is a fact
 * about when the query ran.
 */
@Composable
private fun worded(
    cardType: String,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
): Repository.TodayAnswer? = wordedAnswer(cardType, answer, today, LocalStrings.current)

/**
 * The same wording, without a composition. `DESIGN.md` 9.2.
 *
 * **One function, two callers, and that is the point.** The gallery previews
 * what a card would say if it were added, and it was reading the stored values
 * instead: a project card the screen would show as "Waiting on somebody"
 * previewed as "Nothing waiting", because status lives in a field the raw
 * answer counts as empty. A preview that disagrees with the card it is
 * previewing is worse than no preview.
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
            // **Whose hands is the answer and the status is the context.**
            // 21.7 asks "whose hands, since when", and a person waiting on the
            // county wants the county, not the word Waiting.
            title = answer.title ?: strings[projectStatusKey(answer.detail)],
            detail = answer.title?.let { strings[projectStatusKey(answer.detail)] },
        )

        "project_date" -> {
            val due = answer.whenEdtf
                ?.let { com.kamsiob.healthtrail.time.Edtf.parse(it) }
                ?.let {
                    com.kamsiob.healthtrail.time.Edtf.resolve(it, java.time.ZoneId.systemDefault())
                }
                ?.start
                ?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }
            // **A date the person gave coarsely gets no countdown.** Rule 17:
            // "sometime in April" is not a number of days away, and turning it
            // into one would invent the precision the whole date model exists
            // to protect. The card shows the date it was given instead.
            val exact = answer.whenEdtf?.let {
                com.kamsiob.healthtrail.time.Edtf.parse(it)?.precision
            } == com.kamsiob.healthtrail.time.Edtf.Precision.DAY
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
                // The kind, which is what the date is, in the person's own
                // words. It is free text they typed, so it is isolated.
                detail = (if (days == null) answer.detail else answer.title)
                    ?.let { Bidi.isolate(it) },
            )
        }

        // **What happens next, said the way somebody in a kitchen says it.**
        // The grid draws "Tomorrow 10:15" and the card was rendering "April 9,
        // 2026 at 10:15 AM", which is correct and makes the person count.
        // Anything further out than tomorrow keeps its full date, and a coarse
        // date is never given a day word at all: rule 17 again.
        // **Only what shares the day with the answer.** 21.7 wants two on one
        // day to become two lines, and nothing more: a card listing next week
        // as well has stopped answering "what is the next dated thing" and
        // started being an agenda, which is a screen the app already has.
        "next_up" -> answer.copy(
            items = answer.items.filter { sameDay(it.noteEdtf, answer.whenEdtf) },
            detail = EventDateText.nearby(strings, answer.whenEdtf, today)
                ?: answer.detail,
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
 * **Falls back to the general sentence rather than to a key that may not
 * exist**, because `Strings.resolve` throws in debug and the empty rung is the
 * state a brand new notebook is entirely made of. A crash there would be a
 * crash on the first screen anybody ever sees.
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
    // The digest always has a sentence, and the two other project cards say
    // their own thing when there is nothing: a project with no steps and no
    // waiting-on is a project somebody just started.
    else -> "today.card.nothing"
}

/**
 * Whether two stored dates fall on the same calendar day.
 *
 * **Both have to be day precise for the question to mean anything.** "Sometime
 * in April" is not on any particular day, so it shares one with nothing, and
 * saying otherwise would be the invented precision rule 17 exists to prevent.
 */
private fun sameDay(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    fun day(text: String): String? {
        val date = com.kamsiob.healthtrail.time.Edtf.parse(text) ?: return null
        return when (date.precision) {
            com.kamsiob.healthtrail.time.Edtf.Precision.DAY -> date.canonical
            com.kamsiob.healthtrail.time.Edtf.Precision.MOMENT -> date.canonical.substringBefore('T')
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
 * **skips anything built at runtime**, by design, because a key assembled from a
 * variable cannot be resolved by reading the source. Its stated safety net for
 * those is the instrumented suite rendering the screen, which is a net with a
 * hole in it on any night the phone cannot be driven. `"today.card.$type.count"`
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
    // cards all answer with a thing rather than a quantity, so their context
    // line is the answer's own date or detail.
    else -> null
}

/**
 * What a reader hears instead of the card.
 *
 * One sentence: the tab, then what the record says. A reader stopping on the
 * number, the line and the chevron separately would make somebody listen to
 * three things to learn one.
 */
private fun answerParts(
    answer: Repository.TodayAnswer?,
    strings: Strings,
    /** The card's type, so the empty rung reads aloud as it reads on screen. */
    cardType: String,
    /** Whether the card points at one row, per [AnswerBody]'s own parameter. */
    sourced: Boolean = false,
    /**
     * Whether the card drew its mini spine, so the ear gets what the eye gets.
     *
     * Section 9. With the spine the newest entry is the first waypoint rather
     * than a title above it, and the gaps between waypoints are on the screen
     * in words, so both have to be true of the sentence too.
     */
    spine: Boolean = false,
    /**
     * Whether the card is showing its list.
     *
     * **The reader hears what is on the screen and not what is in the model.**
     * Section 9: what is read aloud says the same thing the screen says. A small
     * card announcing three medications it is not displaying is as wrong as a
     * wide one that shows them and stays silent.
     */
    showItems: Boolean = false,
    /** The word under the number, which the eye gets and the ear was not getting. */
    countLine: String? = null,
    /**
     * Whether the card drew its chart, so the ear gets what the eye gets.
     *
     * 21.3 gives tall a chart or a list and the screen picks the chart when
     * there is a series. Section 9: what is read aloud says the same thing the
     * screen says.
     */
    drewChart: Boolean = false,
): List<String?> = when {
    answer == null -> listOf(strings["today.card.unread"])
    answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
    answer.isEmpty -> listOf(strings[emptyLineKey(cardType)])
    else -> buildList {
        // **The number only where the screen shows one**, which is where the
        // card has no title of its own. It was announced either way, so the
        // trail card told a reader there were 182 entries while the eye saw a
        // sentence and no number at all. Section 9.
        if (answer.title == null) {
            add(answer.count?.takeIf { it > 0 }?.toString())
            if ((answer.count ?: 0) > 0) add(countLine)
        }
        if (!spine) add(answer.title)
        // **The date the screen shows**, per section 9. It was absent here for
        // as long as it was absent there, so the omission was consistent and
        // wrong twice.
        if (!spine) {
            add(
                answer.whenEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) },
            )
        }
        // **The ear gets what the eye gets: a chart or a list, never both.**
        // 21.3 gives tall one or the other and the screen picks the chart when
        // there is a series, so announcing the list too made a reader listen to
        // three readings that were not on the screen and then be told there
        // were twelve. Section 9.
        if ((showItems || spine) && !drewChart) {
            // **The rows the screen is showing and no others.** The trail card
            // lists from the second entry at wide, because the first one is
            // already the answer above it, and shows all three on the spine.
            val rows = if (cardType == "trail_lately" && !spine) {
                answer.items.drop(1)
            } else {
                answer.items
            }
            // **Raw parts, never a joined line.** `Bidi.join` isolates every
            // part it is handed, so adding an already joined item wrapped it a
            // second time and the sentence came out `⁨⁨137.3⁩ · ⁨May 5⁩⁩`. The
            // caller joins once, at the end, over flat parts.
            rows.forEachIndexed { index, item ->
                // **The gaps, because they are on the screen in words.** A
                // reader hearing three entries with no distances between them
                // is hearing a list, which is the thing the spine exists not to
                // be. Same rule and same threshold as the drawing.
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
        // **A chart is one sentence to a reader, never a list of points.**
        // `ChartCard` says the same thing about itself: a chart announced as
        // coordinates is useless. How many readings there are is the part
        // somebody listening can act on.
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
        // it.** Section 9, and it is a second line, which 21.3 gives to wide
        // and tall. The number itself is not added here: where there is one it
        // is an outlined pill, which is its own stop and announces itself, and
        // adding it would make a reader hear it twice.
        if (showItems) add(noNumberLine(cardType, sourced, answer)?.let { strings[it] })
    }
}

/**
 * The card's hue, from the tab pack. `DESIGN.md` 21.2 and 21.7.
 *
 * **Identity, never state.** An appointments card is slate because appointments
 * are slate everywhere in the binder, and the hue does not change with what the
 * answer says. Cards for whole-app surfaces wear gold.
 */
@Composable
// **Internal rather than private since the gallery draws real cards.** A card
// being offered wears the hue it will wear once it is on Today, which is the
// whole point of previewing it as itself. D157.
internal fun hueForCard(type: String): TabHue = when (type) {
    "next_up" -> hueFor(Repository.Section.APPOINTMENTS)
    "medications" -> hueFor(Repository.Section.MEDICATIONS)
    "ask_next_time" -> hueFor(Repository.Section.ASK_NEXT_TIME)
    "measure", "milestones" -> hueFor(Repository.Section.PROGRESS)
    "money" -> hueFor(Repository.Section.MONEY)
    "recent_documents" -> hueFor(Repository.Section.DOCUMENTS)
    "care_team" -> hueFor(Repository.Section.CARE_TEAM)
    "standing_instructions" -> hueFor(Repository.Section.STANDING_INSTRUCTIONS)
    // **The one card where alert is an identity**, 21.7, and the emergency card
    // screen already wears it for the same reason: alert is what it is for.
    "emergency_card", "incidents" -> hueFor(Repository.Section.EMERGENCY_CARD)
    // The digest, the trail, the projects and the unfiled tray belong to no
    // section, so gold and the base ladder. 4.3.
    else -> wholeAppHue()
}

/**
 * The lead's hue, which is not always the card's. D172.
 *
 * **Gold is an accent color and the lead is a field.** It works at the size of a
 * tab chip, an icon and the capture button, which is everywhere else it appears.
 * Filled across the largest block on the screen it is a mustard slab, and the
 * owner had already said the yellow background was off-putting once, about the
 * mockups, before this made it four times the size.
 *
 * **So a whole-app card promoted to the lead wears slate there.** Slate is the
 * calm blue the approved mockup leads with, and it is the only substitution: a
 * section's card keeps its own hue, because that hue is identity and 21.2 does
 * not bend for where the card happens to sit.
 *
 * **The card's own tab dot stays gold**, so nothing is lost about what the card
 * belongs to; it is stated at the size the color works at.
 */
@Composable
private fun leadHue(hue: TabHue): TabHue =
    if (hue == wholeAppHue()) hueFor(Repository.Section.APPOINTMENTS) else hue

/**
 * What a care team card says while its source is still being chosen.
 *
 * **Nothing saves until Done**, 21.6 screen 5, so a picked person cannot come
 * back through the record until then, and the answers were read when Today
 * gained focus. Without this the card would name whoever it pointed at before
 * and the pick would look like it had done nothing, which is the failure mode a
 * staged control has to avoid: a control that appears to do nothing reads as
 * broken.
 *
 * **The same two variants and the same shape**, built from the roster the
 * picker is already showing rather than from a second query, so what the card
 * says while editing and what it says after Done cannot differ.
 *
 * **Null for everything else**, including a card pointing at somebody archived,
 * who is not in the roster: that card keeps rendering the source-closed rung
 * from the record, which is the truth about it.
 */
private fun staged(
    card: Repository.TodayCard,
    editing: Boolean,
    people: List<Repository.Person>,
): Repository.TodayAnswer? {
    if (!editing || card.type != "care_team") return null
    // Sorted by name, which is the order the card's own query uses, so the
    // three faces here are the three faces after Done.
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
 * The repository takes the same handful for the same reason, and the two have
 * to agree or a staged pick would redraw the row on Done.
 */
private const val TODAY_CARD_FACES = 3

/**
 * The digest card's answer, from the summary the shell already computed.
 *
 * **Null for any other card**, so this cannot quietly become a fallback that
 * makes an unanswered card look answered.
 */
private fun digestAnswer(
    card: Repository.TodayCard,
    digest: com.kamsiob.healthtrail.data.Digest.Summary,
    strings: Strings,
    /** Whether anything has ever been written down. */
    hasAnything: Boolean,
): Repository.TodayAnswer? {
    if (card.type != "digest") return null
    // **A notebook made thirty seconds ago has no last time.** The lead read
    // "Nothing new since you were last here" on the first screen anybody sees
    // after onboarding, which refers to a visit that did not happen. The
    // previous Today had this right and showed no digest at all on a first
    // run; this surface cannot, because 21.1 says the lead is never empty.
    //
    // **So it says what is true on day one and nothing more.** Not a task, not
    // a setup prompt, not a count of what is missing: rule 13 rules all three
    // out, and this is the one screen where a new person decides whether the
    // app is going to nag them. It states what the card is for and stops.
    if (!hasAnything) {
        return Repository.TodayAnswer(
            title = strings["today.card.digest.first"],
            detail = strings["today.card.digest.first.detail"],
        )
    }
    // **Counted, never judged.** It says how many things are new, which is a
    // fact about the record. It never says whether that is a lot, whether the
    // person has been away too long, or what any of it means, and a quiet week
    // is stated as a quiet week rather than as a gap to explain. 21.4's
    // returning rung: no card ever measures the person's absence back at them.
    val fresh = digest.newThings
    return Repository.TodayAnswer(
        title = if (fresh > 0) {
            strings("today.card.digest.new", "count" to fresh)
        } else {
            strings["today.card.digest.quiet"]
        },
        // **The other two counts, which the previous Today showed and this one
        // dropped.** 21.6 screen 4 asks the return voice for a calm paragraph
        // of plain counts, and a correction and a removal are counts about the
        // record just as much as an addition is. The catalog has carried both
        // sentences since the first Today and this surface asked for neither.
        //
        // **Only where there is something to say.** A week with nothing
        // corrected does not get told so: rule 13 rules out a tally of the
        // person's own diligence, and a zero here would be exactly that.
        detail = Bidi.join(
            digest.corrected.takeIf { it > 0 }
                ?.let { strings("today.digest.corrected", "count" to it) },
            digest.removed.takeIf { it > 0 }
                ?.let { strings("today.digest.removed", "count" to it) },
        ).takeIf { it.isNotBlank() },
    )
}

/**
 * One of the three sizes, as a chip. `DESIGN.md` 21.6 screen 5.
 *
 * **Selected is a fill and not only a color**, per section 9, so it survives
 * grayscale and every color vision difference.
 */
@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HealthTrail.colors
    Text(
        text = label,
        style = HealthTrail.type.mono,
        color = if (selected) colors.paper else colors.ink2,
        modifier = Modifier
            .clip(Radius.pill)
            .background(if (selected) colors.ink else colors.sand)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.xs),
    )
}

/**
 * The grip that carries a card. `DESIGN.md` 21.6 screen 5.
 *
 * **A shortcut on top of the named controls, never instead of them.** 23.2:
 * Move up, Move down and To top are the reorder path, because they work one
 * handed, with a reader on, and with switch access, and a drag works for none
 * of those. This is the fast way for somebody who can make the gesture, and the
 * screen is complete without it.
 *
 * **Two bars rather than an icon font**, drawn like every other mark in this app
 * so it cannot fall back to a box in a locale the font does not cover, and so it
 * takes the theme's own ink.
 *
 * **It answers the finger before anything moves**, per rule 16 and 5.14: the
 * grip darkens the moment it is held, which is what tells somebody the card is
 * theirs to move rather than that the app is thinking about it.
 *
 * **Invisible to a screen reader, and that is the point.** A reader cannot make
 * this gesture, so announcing it offers somebody a control they cannot use and
 * puts a stop in the way of the ones they can. The reorder a reader performs is
 * beside it in words, on every card, always. Same rule the avatars and the
 * chevron follow: decorative, because the thing it does is named next to it.
 */
/**
 * The tilt that says a card can be picked up, and the lift that says one is.
 *
 * **Borrowed from the phone on purpose**, per the owner's note that a home
 * screen is the only frame of reference anybody brings to a grid of cards. A
 * card being arranged rocks by well under a degree; the card in the hand stops
 * rocking, grows very slightly, and rides above its neighbors.
 *
 * **The rock stops on the held card**, which is the detail that makes it read as
 * physical rather than as an effect: what you are holding is steady and
 * everything you are not holding is loose.
 *
 * **Neighbors are out of phase with each other.** Cards tilting in perfect
 * unison read as the whole screen shaking, which is alarming rather than
 * inviting, and alarming is the wrong note for an app used during hard times.
 *
 * **Reduced motion turns the tilt off and nothing else changes.** The degrees
 * come from `LocalMotion`, which is zero there, so the cards sit still and the
 * remove marks and the worded move actions carry the whole mode. Section 10:
 * every spec comes from the tokens, because one built here is one the setting
 * cannot reach.
 */
@Composable
private fun Modifier.arrangeTilt(arranging: Boolean, ordinal: Int, held: Boolean): Modifier {
    val motion = LocalMotion.current
    val degrees = motion.arrangeTiltDegrees

    // **The animation exists only while the mode does.** An infinite transition
    // composed unconditionally would run a frame loop on the front door forever
    // and multiply it by every card on the screen, to render a rotation of zero.
    // It is inside the branch rather than outside with its output multiplied by
    // nothing, which is the same picture and a very different battery.
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
        // Still. The card in the hand is deliberately in here too: what you are
        // holding is steady and everything you are not holding is loose, which
        // is the detail that makes it read as physical rather than as an effect.
        0f
    }

    return this.graphicsLayer {
        rotationZ = tilt
        // The lift is information rather than decoration, so it survives
        // reduced motion: it says which card the finger has.
        scaleX = if (held) HELD_SCALE else 1f
        scaleY = scaleX
    }
}

/**
 * How much bigger the card in the hand is.
 *
 * **Barely.** Enough that the eye can tell which one is being carried without
 * the card covering what it is being moved past.
 */
private const val HELD_SCALE = 1.04f

/** How many cards go by before the tilt repeats its starting point. */
private const val TILT_PHASES = 3

@Suppress("unused")
@Composable
private fun DragHandle(
    dragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val ink by animateColorAsState(
        targetValue = if (dragging) colors.ink else colors.ink3,
        animationSpec = LocalMotion.current.quick(),
        label = "grip",
    )

    Canvas(
        modifier = modifier
            .size(GRIP_TARGET)
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                // **The grip drags at once, and that is the difference.**
                // D169: the card itself needs a long press because a swipe on
                // a card is how somebody scrolls, but a grip exists only to be
                // dragged, so asking for a hold on it would be a control that
                // ignores the first thing you do to it.
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, delta ->
                        // Consumed, so the grid does not scroll under the card
                        // being carried, which is two things answering one
                        // finger.
                        change.consume()
                        onDrag(delta)
                    },
                )
            },
    ) {
        val width = GRIP_WIDTH.toPx()
        val gap = GRIP_GAP.toPx()
        val left = (size.width - width) / 2f
        for (bar in -1..1) {
            val y = size.height / 2f + bar * gap
            drawLine(
                color = ink,
                start = Offset(left, y),
                end = Offset(left + width, y),
                strokeWidth = GRIP_STROKE.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The grip's touch target, which is the floor rather than the drawing.
 *
 * Section 9's minimum, because a control small enough to look tidy is a control
 * somebody with a tremor cannot hold.
 */
private val GRIP_TARGET = 48.dp

/** How wide each bar is drawn. Narrower than the target, on purpose. */
private val GRIP_WIDTH = 18.dp

/** The distance between bars. */
private val GRIP_GAP = 5.dp

/** How heavy each bar is. */
private val GRIP_STROKE = 2.dp

/**
 * One edit control: a short word to look at, a whole sentence to hear.
 *
 * **Both are needed and they are not the same string.** A card in the field is
 * half a screen wide, so a label naming the card does not fit; a reader moving
 * through eight of these hears "Up" eight times and learns nothing. The word is
 * for the eye and [spoken] is for the ear.
 */
@Composable
private fun EditAction(
    label: String,
    spoken: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    Text(
        text = label,
        style = HealthTrail.type.mono,
        color = colors.blueDeep,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(Radius.pill)
            .clickable(onClickLabel = spoken, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken }
            .padding(horizontal = Space.s, vertical = Space.s),
    )
}

/**
 * The text scale at which Today's field becomes one column.
 *
 * `DESIGN.md` 21.6 screen 8. Not the top of the slider: somebody at 1.7 has the
 * same half width card as somebody at 2.0, and making them go further to get a
 * readable screen is the opposite of designing for it.
 */
private const val WIDE_TYPE_SCALE = 1.5f

/**
 * The dot that takes a card off Today. `DESIGN.md` 21.6 screen 5.
 *
 * **It exists only inside edit mode**, which is what keeps "nothing bare
 * responds to touch" true in ordinary reading: outside edit mode this corner is
 * the chevron, and the card is a door rather than a thing with controls on it.
 *
 * **A minus rather than a cross.** A cross reads as "delete", and this deletes
 * nothing: the card comes off an arrangement and everything it was answering
 * about is still written down. The sheet says so in words, and the shape should
 * not say something louder than the words.
 *
 * **Drawn rather than an icon glyph**, like every other mark here, so it cannot
 * fall back to a box and it takes the theme's own ink.
 */
@Composable
private fun RemoveDot(spoken: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)

    Box(
        modifier = modifier
            .padding(Space.xs)
            .size(Space.touchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = spoken,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(DOT_SIZE)) {
            drawCircle(color = surface)
            drawLine(
                color = colors.ink2,
                start = Offset(size.width * 0.3f, size.height / 2f),
                end = Offset(size.width * 0.7f, size.height / 2f),
                strokeWidth = DOT_STROKE.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The dot itself, inside a touch target that is section 9's floor. */
private val DOT_SIZE = 24.dp

/** How heavy the minus is. */
private val DOT_STROKE = 2.dp

/**
 * What the options sheet calls the card it is about.
 *
 * **The tab and what it points at**, which is the same name the card wears, so
 * somebody who opened the options of one of four project cards can tell which
 * one they are looking at.
 */
@Composable
private fun optionsName(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
): String = tabFor(card, answer, LocalStrings.current)

/**
 * How much a card grows while it is being carried, and how far it lifts.
 *
 * **Four percent and eight dp.** The phone's own home screen swells an icon
 * about this much: enough that the card is unmistakably in the hand and not
 * so much that the grid under it is obscured by the thing being moved.
 */
private const val LIFT_SCALE = 1.04f
