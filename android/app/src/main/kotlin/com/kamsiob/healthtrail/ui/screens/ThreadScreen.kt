package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.BigNumber
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RouteMark

object OneThreadTags {
    const val RENAME = "thread_rename"
    const val NAME = "one_thread"
    fun entry(id: String) = "one_thread_entry_$id"
    const val REMOVE = "threads_remove"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_one_thread"
}

/**
 * One care thread, and everything on it. Rewritten onto `ui/v4`, #386.
 *
 * **A thread is this app's own metaphor and could not be opened.** Its route
 * identifies it on the trail, on an entry, and on the threads screen, and
 * tapping the thread itself did nothing at all, which is exactly the dead end
 * wearing a disguise that rule 18 and #46 are about.
 *
 * **The route identifies the screen and no longer draws a line down it.** D187:
 * a thread is a filter over the record rather than anybody's path, so the road
 * stays where a road is real and the thread's own route sits beside its name,
 * which is what `DESIGN.md` 5.2.2 asks of it: opening "Nursing" from the trail
 * and opening it from the threads screen land somewhere recognizably the same.
 *
 * **Where it has got to, before the entries that got it there.** Somebody
 * opening a thread wants to know whether it is still going and when it last
 * moved, and a hundred and seventy four rows at one weight answers neither.
 * Stated from what is recorded, never interpreted. Rule 2.
 *
 * **The recent run leads and the rest are under a label.** A thread kept for
 * five years is a wall, and a wall answers nothing. Nothing is behind a door:
 * the earlier ones say what they are and the person scrolls to them. D185.
 */
@Composable
fun ThreadScreen(
    thread: Repository.CareThread,
    entries: List<Repository.TrailEntry>,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Renames the thread. #371.
     *
     * **A thread could be started and never corrected**, and its label titles
     * this screen, appears as a capture chip, is a filing target in the unfiled
     * tray, and shows on every entry in the trail that belongs to it.
     */
    onRename: () -> Unit,
    /**
     * Takes it out of the notebook, with the confirmation the caller owns.
     * **Anything that can be added can be removed**, the owner's rule,
     * 2026-08-16: a record started by mistake should not be forever.
     */
    onRemove: () -> Unit = {},
    backLabelKey: String = "section.back.threads",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.THREADS)
    val route = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size]

    val recent = entries.take(THREAD_RECENT)
    val earlier = entries.drop(THREAD_RECENT)

    Page(
        eyebrow = strings["notebook.section.threads"],
        eyebrowColor = hue.ink,
        title = Bidi.isolate(thread.label),
        subtitle = strings("thread.count", "count" to entries.size),
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(OneThreadTags.ROOT),
    ) {
        item {
            Block(tone = BlockTone.Section, hue = hue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    RouteMark(color = route, index = thread.colorIndex)
                    Eyebrow(text = strings["thread.where"], color = hue.ink)
                }
                // **The state, and not the count.** The count is the page's own
                // line already, and putting it here as well made this read "174
                // things on this one, last written on May 13" under a subtitle
                // reading "174 things on this one".
                BigNumber(
                    value = entries.firstOrNull()?.occurredEdtf?.takeIf { it.isNotBlank() }
                        ?.let {
                            strings(
                                "threads.moving",
                                "date" to EventDateText.render(strings, it),
                            )
                        }
                        ?: strings["threads.quiet"],
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence, with the thread's own
                    // label isolated into it.
                    Body(
                        text = strings("thread.empty", "label" to Bidi.isolate(thread.label)),
                        color = colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        items(recent, key = { it.id }) { entry -> ThreadEntry(entry, onOpenEntry) }

        if (earlier.isNotEmpty()) {
            item { Eyebrow(text = strings["thread.earlier"]) }
        }

        items(earlier, key = { it.id }) { entry -> ThreadEntry(entry, onOpenEntry) }

        // **Sized to their labels**, D118. Renaming is the one thing this
        // screen could not do, and removing is the rarer errand under it.
        item {
            Spacer(Modifier.height(Space.s))
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Action(
                    label = strings["threads.rename"],
                    onClick = onRename,
                    mark = Symbols.edit,
                    modifier = Modifier.testTag(OneThreadTags.RENAME),
                )
                Action(
                    label = strings["remove.action"],
                    onClick = onRemove,
                    modifier = Modifier.testTag(OneThreadTags.REMOVE),
                )
            }
        }
    }
}

/** One entry on the thread: when it happened, what it was, and three lines of it. */
@Composable
private fun ThreadEntry(
    entry: Repository.TrailEntry,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
) {
    val strings = LocalStrings.current
    Block(
        modifier = Modifier
            .semantics(mergeDescendants = true) { }
            .clickable(
                role = Role.Button,
                onClickLabel = strings["prep.change.open"],
                onClick = { onOpenEntry(entry) },
            )
            .testTag(OneThreadTags.entry(entry.id)),
    ) {
        entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
            Eyebrow(text = EventDateText.render(strings, it), fixed = false)
        }
        Body(
            text = entry.title?.takeIf { it.isNotBlank() }
                ?.let { Bidi.isolate(it) }
                ?: strings[kindNameKey(entry.kind)],
            color = HealthTrail.colors.ink,
            style = HealthTrail.type.rowTitle,
        )
        entry.body?.takeIf { it.isNotBlank() }?.let {
            Body(
                text = Bidi.isolate(it),
                // Three lines and then the block ends: the whole entry is one
                // tap away, and a list where one item is a page long stops
                // being a list.
                maxLines = ENTRY_PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** How much of an entry a block shows before the entry's own screen takes over. */
private const val ENTRY_PREVIEW_LINES = 3

/**
 * How much of a thread is open when the screen arrives.
 *
 * Twelve, which is a few months of an active thread and a couple of years of a
 * quiet one. Enough to see how it has been going without being the wall the
 * fold exists to prevent.
 */
private const val THREAD_RECENT = 12
