package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object OneThreadTags {
    const val RENAME = "thread_rename"
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
    /**
     * Renames the thread. #371.
     *
     * **A thread could be started and never corrected**, and its label titles
     * this screen, appears as a capture chip, is a filing target in the unfiled
     * tray, and shows on every entry in the trail that belongs to it.
     */
    onRename: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.threads",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val route = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size]
    var earlierOpen by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = OneThreadTags.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["notebook.section.threads"],
        heading = Bidi.isolate(thread.label),
        section = Repository.Section.THREADS,
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

        // **Where it has got to, before the sequence that got it there.** Law 1:
        // somebody opening a thread wants to know whether it is still going and
        // when it last moved, and a hundred and seventy four rows at one weight
        // answers neither. Stated from what is recorded, never interpreted.
        item(key = "where") {
            Text(
                text = strings["thread.where"],
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            // **The state, and not the count.** The count is already the
            // subtitle two lines above, and putting it here as well made the
            // hero read "174 things on this one, last written on May 13" under
            // a subtitle reading "174 things on this one". The same defect this
            // screen was fixed for an hour ago, reintroduced by the fix.
            Text(
                text = entries.firstOrNull()?.occurredEdtf?.takeIf { it.isNotBlank() }
                    ?.let {
                        strings("threads.moving", "date" to EventDateText.render(strings, it))
                    }
                    ?: strings["threads.quiet"],
                style = HealthTrail.type.hero,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **The recent run open, the rest behind one door.** The same shape the
        // trail uses, for the same reason: a thread kept for five years is a
        // wall, and a wall answers nothing. Nothing is hidden, and the door says
        // how much is behind it.
        val recent = entries.take(THREAD_RECENT)
        val earlier = entries.drop(THREAD_RECENT)
        val shown = if (earlierOpen) entries else recent

        shown.forEachIndexed { index, entry ->
            item(key = entry.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < shown.lastIndex,
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
                                // #231: a tap that opens says so, rather than
                                // announcing "remove" and offering a long press
                                // that runs an empty function.
                                .openableByTap(
                                    label = strings["prep.change.open"],
                                    onTap = { onOpenEntry(entry) },
                                    resting = colors.card,
                                )
                                .testTag(OneThreadTags.entry(entry.id))
                                .padding(Space.cardPadding),
                        ) {
                            entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = EventDateText.render(strings, it),
                                    style = HealthTrail.type.mono,
                                    color = colors.ink2,
                                )
                                Spacer(Modifier.height(Space.xs))
                            }
                            Text(
                                text = entry.title?.takeIf { it.isNotBlank() }
                                    ?.let { Bidi.isolate(it) }
                                    ?: strings[kindNameKey(entry.kind)],
                                style = HealthTrail.type.displayS,
                                color = colors.ink,
                            )
                            entry.body?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = Bidi.isolate(it),
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

        // **The fold stays and toggles.** It used to render only while closed
        // and to set open rather than toggle, so opening a 174 entry thread
        // made the control disappear and there was no way back to the twelve
        // entry view except leaving the screen. A sand row with a chevron is
        // the app's promise that something opens in place, and every other fold
        // in the app keeps it. #361.
        if (earlier.isNotEmpty()) {
            item(key = "earlier") {
                FoldRow(
                    labelKey = "thread.earlier",
                    expanded = earlierOpen,
                    onToggle = { earlierOpen = !earlierOpen },
                    count = earlier.size.toString(),
                )
            }
        }

        // **A pill sized to its label**, D118, above the scaffold's own way
        // back. Renaming is the one thing this screen could not do.
        item(key = "rename") {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["threads.rename"],
                onClick = onRename,
                modifier = Modifier.testTag(OneThreadTags.RENAME),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * How much of a thread is open when the screen arrives.
 *
 * Twelve, which is a few months of an active thread and a couple of years of a
 * quiet one. Enough to see how it has been going without being the wall the
 * fold exists to prevent.
 */
private const val THREAD_RECENT = 12
