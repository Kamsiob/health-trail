package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.Trail
import java.time.ZoneId

object TrailTags {
    const val NAME = "trail"
    fun entry(id: String) = "trail_entry_$id"
    fun date(id: String) = "trail_date_$id"
    fun month(label: String) = "trail_month_$label"
}

/** The gutter the route runs down, and where in it the line sits. */
private val GutterWidth = 28.dp
private val LineCenter = 9.dp

/**
 * Where a node sits down the row, so it lands on the date rather than floating
 * at the card's top corner. Measured against the card's own padding and the
 * date's touch target rather than guessed, and checked on the device.
 */
private val NodeCenterY = 40.dp

/**
 * The trail: everything the person has written down, most recent first.
 *
 * **This screen is why capture was worth building.** Until it existed the app
 * took entries and never gave one back, which made every other screen a promise
 * it could not keep. The notebook counted twelve sections and opened none.
 *
 * **It is the app's signature element and it is specified exactly**, in
 * `DESIGN.md` section 5.2. A dashed gold route runs down the start edge, 2dp at
 * 65% opacity, and each entry sits on it as a 12dp node ringed in the current
 * background so it reads as sitting on the line rather than beside it. The node
 * carries the entry type: gold for a call, blue for a visit, alert for an
 * incident. It was first built as a plain list of cards, which was functionally
 * correct and visually plain, which rule 14 says is not done.
 *
 * **Months head their own runs**, through the section 5.13 group header, which
 * is how the reference file heads a month in the trail.
 *
 * **Ordered by when things happened, not by when they were typed.** A call
 * logged on Tuesday about Sunday belongs on Sunday. Entries whose date is not
 * known gather at the end under their own heading rather than being placed at
 * zero or at today, because inventing a position on a timeline is the app
 * claiming something the person never said.
 *
 * **The date is the one thing on a row that is tappable**, and tapping it opens
 * the same picker every other date in the app uses. Rule 17 requires a date to
 * be editable forever from the entry itself, and a date captured in a hallway is
 * the one most likely to be wrong. The rest of the row does not respond, which
 * is deliberate: rule 16 says a control that does nothing on press reads as
 * broken, so only the thing that responds is made to look touchable.
 *
 * **The route draws itself in once**, over 400ms with the nodes staggered 30ms
 * behind it, per section 6. It happens on entry to the screen and never on
 * scroll. With reduced motion on it renders immediately, through the same
 * tokens, because a spec built inline is one that setting cannot reach.
 */
@Composable
fun TrailScreen(
    entries: List<Repository.TrailEntry>,
    onEditDate: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val strings = LocalStrings.current
    val motion = LocalMotion.current

    // Grouped by the month a thing happened in, keeping the order the query
    // already put them in. Entries with no date fall into their own group at
    // the end, which is where the ordering already placed them.
    val groups = remember(entries, strings, zone) {
        entries.groupBy { entry ->
            entry.occurredStart
                ?.let { EventDateText.monthHeading(strings, it, zone) }
                ?: strings["date.unknown"]
        }
    }

    // The one ambient flourish, per section 6, and it runs once per screen
    // entry rather than on every recomposition.
    var drawn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { drawn = true }
    val draw by animateFloatAsState(
        targetValue = if (drawn) 1f else 0f,
        animationSpec = motion.trailDraw(),
        label = "trailDraw",
    )

    SectionScaffold(
        name = TrailTags.NAME,
        title = strings["notebook.section.trail"],
        subtitle = strings["trail.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (entries.isEmpty()) {
            item { SectionEmpty(name = TrailTags.NAME, text = strings["trail.empty"]) }
        }

        var index = 0
        for ((month, inMonth) in groups) {
            item(key = "month_$month") {
                // The route runs behind the heading too, so the path reads as
                // one path rather than as several that happen to line up.
                RouteRow(draw = draw, continuesAbove = index > 0, continuesBelow = true) {
                    Column {
                        Spacer(Modifier.height(Space.s))
                        GroupHeaderText(
                            label = month,
                            modifier = Modifier.testTag(TrailTags.month(month)),
                        )
                        Spacer(Modifier.height(Space.headerGap))
                    }
                }
            }

            for (entry in inMonth) {
                val position = index
                index += 1
                item(key = entry.id) {
                    RouteRow(
                        draw = draw,
                        continuesAbove = true,
                        continuesBelow = position < entries.size - 1,
                        node = nodeColor(entry.kind),
                        nodeDelayMillis = position * motion.trailNodeStaggerMillis,
                    ) {
                        Column {
                            TrailRow(entry = entry, onEditDate = { onEditDate(entry) })
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One position on the route: the dashed line, an optional node, and whatever
 * sits beside them.
 *
 * The line is drawn to the full height of the row including the gap beneath the
 * card, which is what makes it continuous rather than a series of segments with
 * daylight between them. `IntrinsicSize.Min` is what lets the gutter know how
 * tall the content beside it turned out to be.
 *
 * **It mirrors for free.** The gutter is the start edge rather than the left
 * one, so in Arabic the whole trail moves to the other side and the content
 * flows from it, per section 8. A timeline that reads left to right in a right
 * to left layout is broken rather than stylish.
 */
@Composable
private fun RouteRow(
    draw: Float,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    node: Color? = null,
    nodeDelayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val motion = LocalMotion.current

    var nodeShown by remember { mutableStateOf(motion.isReduced) }
    LaunchedEffect(Unit) {
        if (!motion.isReduced && nodeDelayMillis > 0) {
            kotlinx.coroutines.delay(nodeDelayMillis.toLong())
        }
        nodeShown = true
    }
    val nodeAlpha by animateFloatAsState(
        targetValue = if (nodeShown) 1f else 0f,
        animationSpec = motion.quick(),
        label = "trailNode",
    )

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(GutterWidth).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxHeight().width(GutterWidth)) {
                val x = LineCenter.toPx()
                val nodeY = NodeCenterY.toPx()
                val top = if (continuesAbove) 0f else nodeY
                val bottom = if (continuesBelow) size.height else nodeY
                // The line strokes in from the top, which is the direction the
                // person reads and the direction the trail travels.
                val end = top + (bottom - top) * draw
                if (end > top) {
                    drawLine(
                        color = colors.blaze.copy(alpha = Trail.ROUTE_ALPHA),
                        start = Offset(x, top),
                        end = Offset(x, end),
                        strokeWidth = Trail.strokeWidth.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(Trail.dashOn.toPx(), Trail.dashOff.toPx()),
                        ),
                    )
                }
            }

            if (node != null) {
                // The ring is the current background rather than a border
                // color, which is what makes the node read as sitting on the
                // line rather than beside it.
                Box(
                    modifier = Modifier
                        .padding(
                            start = LineCenter - (Trail.nodeSize / 2) - Trail.nodeRing,
                            top = NodeCenterY - (Trail.nodeSize / 2) - Trail.nodeRing,
                        )
                        .size(Trail.nodeSize + Trail.nodeRing * 2)
                        .clip(CircleShape)
                        .background(colors.paper.copy(alpha = nodeAlpha)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(Trail.nodeSize)
                            .clip(CircleShape)
                            .background(node.copy(alpha = nodeAlpha)),
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * What the node's color says the entry is, per section 5.2.
 *
 * The three the design names each get their own. Everything else takes the
 * quiet non-text ink rather than borrowing one of the three, because a
 * measurement wearing the incident color would be the app saying something
 * about it that is not true.
 */
@Composable
private fun nodeColor(kind: String): Color {
    val colors = HealthTrail.colors
    return when (kind) {
        "call" -> colors.blaze
        "visit" -> colors.blue
        "incident" -> colors.alert
        else -> colors.ink3NonText
    }
}

/**
 * One entry, as the trail shows it.
 *
 * The hierarchy is rule 15's order rather than uniform weight: what happened
 * carries the most, in Display S; the date is a quiet mono eyebrow above it;
 * the note recedes into Body M; the threads it runs through sit last as the
 * route dots the rest of the app already uses for them.
 */
@Composable
private fun TrailRow(entry: Repository.TrailEntry, onEditDate: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(TrailTags.entry(entry.id))
            .padding(Space.cardPadding),
    ) {
        EditableDate(entry = entry, onClick = onEditDate)

        Spacer(Modifier.height(Space.xs))

        // A blank title is normal and always has been: the capture form
        // requires nothing. The kind is what the app knows for certain, so the
        // row never shows an empty line where the subject should be.
        Text(
            text = entry.title?.takeIf { it.isNotBlank() } ?: strings[kindLabelKey(entry.kind)],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        entry.body?.takeIf { it.isNotBlank() }?.let { body ->
            Spacer(Modifier.height(Space.xs))
            Text(text = body, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        if (entry.threads.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            ThreadTrace(threads = entry.threads)
        }

        // The tray already offers to file this. Saying so here is the other
        // half of that link, per rule 18: the tray shows the entry, so the
        // entry says it is in the tray. Stated rather than styled as a warning,
        // because being unfiled is a state and not a mistake.
        if (entry.isUnfiled) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["trail.unfiled"],
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
        }
    }
}

/**
 * The date, as an eyebrow that can be corrected.
 *
 * It renders through [EventDateText] like every other date in the app, so an
 * entry saved with "Not sure" says the date is not known rather than showing
 * the day it was typed, and a month never collapses to its first day.
 *
 * It carries a content description naming what tapping it does, because
 * "August 18, 2026" read aloud on its own tells a reader user nothing about why
 * it is focusable.
 */
@Composable
private fun EditableDate(entry: Repository.TrailEntry, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val rendered = EventDateText.render(strings, entry.occurredEdtf)

    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = Modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
            .clickable(
                interactionSource = interaction,
                // The surface is the answer to the touch, per 5.14. A ripple on
                // top would be a second, louder answer to the same tap.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(TrailTags.date(entry.id))
            .semantics {
                contentDescription = strings("trail.date.action", "date" to rendered)
            }
            .padding(vertical = Space.s, horizontal = Space.s),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = rendered, style = HealthTrail.type.mono, color = colors.ink3Text)
    }
}

/**
 * Which threads this entry runs through, as the route dots the rest of the app
 * already uses for them.
 *
 * The color is an index into the theme's routes rather than a stored color, so
 * the dark theme substitution happens in the theme and a stored color can never
 * fail contrast. The label always sits next to the dot: color alone carries no
 * meaning, per section 9.
 */
@Composable
private fun ThreadTrace(threads: List<Repository.CareThread>) {
    val colors = HealthTrail.colors

    Column {
        for (thread in threads) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Space.s)
                        .clip(CircleShape)
                        .background(
                            colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size],
                        ),
                )
                Spacer(Modifier.width(Space.s))
                Text(text = thread.label, style = HealthTrail.type.bodyS, color = colors.ink2)
            }
        }
    }
}

/**
 * The catalog key naming a kind of entry, for rows the person left untitled.
 *
 * An entry always has a kind and may have nothing else, which is what the
 * capture form promises when it says a half note beats no note.
 */
internal fun kindLabelKey(kind: String): String = when (kind) {
    "call" -> "capture.call"
    "visit" -> "capture.visit"
    "incident" -> "capture.incident"
    "measurement" -> "capture.measurement"
    "question" -> "capture.question"
    "document" -> "capture.document"
    else -> "capture.title"
}
