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
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object OneThreadTags {
    const val NAME = "one_thread"
    fun entry(id: String) = "one_thread_entry_$id"
}

/**
 * One care thread, and everything on it.
 *
 * **A thread is this app's own metaphor and could not be opened.** Its route
 * identifies it on the trail, on an entry, and on the threads screen, and
 * tapping the thread itself did nothing at all, which is exactly the dead end
 * wearing a disguise that rule 18 and #46 are about.
 *
 * **The route is the whole screen's identity.** The spine runs in the thread's
 * own color and its own dash, so opening "Nursing" from the trail and opening
 * it from the threads screen land somewhere recognizably the same. `DESIGN.md`
 * 5.2.2 asked for exactly that and this is the last place it was missing.
 *
 * Dashed, because a thread is a filter over the record rather than the
 * person's own path, which is the distinction 5.2.3 draws and which an incident
 * thread answers the other way with a continuous line.
 */
@Composable
fun ThreadScreen(
    thread: Repository.CareThread,
    entries: List<Repository.TrailEntry>,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.threads",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val route = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size]

    SectionScaffold(
        name = OneThreadTags.NAME,
        title = thread.label,
        subtitle = strings("thread.count", "count" to entries.size),
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        if (entries.isEmpty()) {
            item {
                SectionEmpty(
                    name = OneThreadTags.NAME,
                    text = strings("thread.empty", "label" to thread.label),
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                    section = Repository.Section.THREADS,
                )
            }
            return@SectionScaffold
        }

        entries.forEachIndexed { index, entry ->
            item(key = entry.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < entries.lastIndex,
                    node = route,
                    routeColor = route,
                    dash = RouteDash.forIndex(thread.colorIndex),
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
                                .testTag(OneThreadTags.entry(entry.id))
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
}
