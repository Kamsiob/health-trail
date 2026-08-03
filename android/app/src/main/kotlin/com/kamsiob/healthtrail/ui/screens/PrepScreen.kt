package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object PrepTags {
    const val NAME = "prep"
    const val SHARE = "prep_share"
    const val WRITE_UP = "prep_write_up"
    fun question(id: String) = "prep_question_$id"
    fun change(id: String) = "prep_change_$id"
}

/**
 * What to walk into an appointment carrying.
 *
 * `MASTER_SPEC.md` 4.5: the questions waiting for that person plus a change
 * summary composed from real entries, every line tapping through to its source.
 *
 * **This is the app's most useful two minutes, and it is entirely
 * composition.** Nothing on this screen is generated, inferred, or summarized.
 * The questions are the ones somebody wrote down and never asked. The changes
 * are the entries themselves, not a description of them. 4.11 requires that,
 * and rule 2 is why.
 *
 * **Since the last appointment, not since some window.** Somebody walking into
 * a care plan meeting wants what has happened since the last time they sat in
 * that room. A fixed thirty days would either repeat what was already covered
 * or silently drop what was not, and the screen says which date it is counting
 * from rather than leaving it to be guessed.
 *
 * **Every line opens its entry**, which is the requirement in the spec's own
 * words and the reason journey five needed the entry screen first.
 */
@Composable
fun PrepScreen(
    prep: Repository.Prep,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onShare: () -> Unit,
    onWriteUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.appointments",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val appointment = prep.appointment

    SectionScaffold(
        name = PrepTags.NAME,
        title = appointment.title.ifBlank { strings["prep.untitled"] },
        subtitle = appointment.scheduledEdtf?.takeIf { it.isNotBlank() }
            ?.let { EventDateText.render(strings, it) }
            ?: strings["date.unknown"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        appointment.locationNote?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **The questions first.** They are the reason to prepare at all, and
        // the thing most easily forgotten in the room.
        item {
            GroupHeader(labelKey = "prep.questions")
            Spacer(Modifier.height(Space.headerGap))
        }

        if (prep.questions.isEmpty()) {
            item {
                Text(
                    text = strings["prep.questions.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else {
            prep.questions.forEachIndexed { index, question ->
                item(key = "q_${question.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < prep.questions.lastIndex,
                        node = colors.blue,
                        // Hollow, because a question nobody has asked yet has
                        // not happened. Section 5.2.1: shape carries the state.
                        state = Waypoint.UPCOMING,
                        routeColor = colors.blue,
                    ) {
                        Column {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) { }
                                    .clip(Radius.card)
                                    .background(colors.card)
                                    .testTag(PrepTags.question(question.id))
                                    .padding(Space.cardPadding),
                            ) {
                                question.roleLabel?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = HealthTrail.type.mono,
                                        color = colors.ink3Text,
                                    )
                                    Spacer(Modifier.height(Space.xs))
                                }
                                Text(
                                    text = question.text,
                                    style = HealthTrail.type.bodyL,
                                    color = colors.ink,
                                )
                            }
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item {
            GroupHeader(labelKey = "prep.changes")
            Spacer(Modifier.height(Space.headerGap))
            // **Says what window it is showing**, rather than leaving somebody
            // to work out whether something is missing.
            Text(
                text = prep.sinceEdtf?.takeIf { it.isNotBlank() }
                    ?.let {
                        strings(
                            "prep.changes.since",
                            "date" to EventDateText.render(strings, it),
                        )
                    }
                    ?: strings["prep.changes.all"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.m))
        }

        if (prep.changes.isEmpty()) {
            item {
                Text(
                    text = strings["prep.changes.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
            }
        } else {
            prep.changes.forEachIndexed { index, entry ->
                item(key = "c_${entry.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < prep.changes.lastIndex,
                        node = colors.blaze,
                        routeColor = colors.blaze,
                        dash = RouteDash.TRAIL,
                    ) {
                        Column {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) { }
                                    .clip(Radius.card)
                                    .background(colors.card)
                                    .removableByLongPress(
                                        label = strings["remove.hint"],
                                        onLongPress = {},
                                        onTap = { onOpenEntry(entry) },
                                    )
                                    .testTag(PrepTags.change(entry.id))
                                    .padding(Space.cardPadding),
                            ) {
                                entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = EventDateText.render(strings, it),
                                        style = HealthTrail.type.mono,
                                        color = colors.ink3Text,
                                    )
                                    Spacer(Modifier.height(Space.xs))
                                }
                                Text(
                                    text = entry.title?.takeIf { it.isNotBlank() }
                                        ?: strings[kindLabelKey(entry.kind)],
                                    style = HealthTrail.type.displayS,
                                    color = colors.ink,
                                )
                                entry.body?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(Space.xs))
                                    Text(
                                        text = it,
                                        style = HealthTrail.type.bodyM,
                                        color = colors.ink2,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["prep.share"],
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().testTag(PrepTags.SHARE),
            )
            Spacer(Modifier.height(Space.cardGap))
            // **Writing it up afterward is the other half of the journey**, and
            // it opens the ordinary capture form rather than a special one, so
            // what comes out is an ordinary entry on the trail.
            QuietButton(
                label = strings["prep.writeup"],
                onClick = onWriteUp,
                modifier = Modifier.fillMaxWidth().testTag(PrepTags.WRITE_UP),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
