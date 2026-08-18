package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.kamsiob.healthtrail.ui.v4.Eyebrow
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
 * One care thread, and everything on it. Written fresh on Material 3
 * Expressive, D196, and the file it replaces is deleted.
 *
 * **The wall came down.** The previous version put the section's wash behind a
 * display-scale sentence in a container as tall as a hand, which is the one
 * thing the owner has named twice about this app: "a full height container in a
 * section wash is a wall, not an accent". The surface is neutral now and the
 * color is the thread's own route, drawn beside the label that names it.
 *
 * **The two ragged pills are gone.** Renaming is an edit, so it goes where every
 * edit in this app goes: the pencil in the corner, D173. Removing is the rare
 * errand and is one full width outlined button at the end of the list, which is
 * where a person who scrolled the whole thread looking for it would look.
 *
 * **A thread is this app's own metaphor and could not be opened** before #386.
 * Its route identifies it on the trail, on an entry and on the threads screen,
 * and tapping the thread itself did nothing, which is the dead end wearing a
 * disguise that rule 18 is about.
 *
 * **The route identifies the screen and does not draw a line down it.** D187: a
 * thread is a filter over the record rather than anybody's path, so the road
 * stays where a road is real and the thread's route sits beside its name.
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val scheme = MaterialTheme.colorScheme
    val fonts = MaterialTheme.typography
    val hue = hueFor(Repository.Section.THREADS)
    val colors = HealthTrail.colors
    val route = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val recent = entries.take(THREAD_RECENT)
    val earlier = entries.drop(THREAD_RECENT)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.background,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = Bidi.isolate(thread.label)) },
                subtitle = {
                    Text(text = strings["notebook.section.threads"], color = hue.ink)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(SECTION_BACK)) {
                        Icon(
                            painter = painterResource(Symbols.back),
                            contentDescription = strings[backLabelKey],
                        )
                    }
                },
                actions = {
                    // **The pencil, not a pill in the body.** D173: a control
                    // that moves between screens has to be found again every
                    // time, and this one is the same edit every other screen
                    // puts in the same corner.
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.testTag(OneThreadTags.RENAME),
                    ) {
                        Icon(
                            painter = painterResource(Symbols.edit),
                            contentDescription = strings["threads.rename"],
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(OneThreadTags.ROOT),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = inset.calculateTopPadding(),
                bottom = inset.calculateBottomPadding() + Space.fabScrollClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            item {
                Text(
                    text = strings("thread.count", "count" to entries.size),
                    style = fonts.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }

            item {
                // **The one thing this screen leads with**, and it is neutral.
                // The route beside the label is the colored thing, which is the
                // same mark the trail and the threads list identify this thread
                // by, so the color means "this one" rather than decorating a
                // block.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
                ) {
                    Column(
                        modifier = Modifier.padding(Space.ml),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            RouteMark(color = route, index = thread.colorIndex)
                            Eyebrow(text = strings["thread.where"], color = hue.ink)
                        }
                        // **The state, and not the count.** The count is the
                        // line above already, and putting it here as well made
                        // this read "174 things on this one, last written on May
                        // 13" under a line reading "174 things on this one".
                        Text(
                            text = entries.firstOrNull()?.occurredEdtf?.takeIf { it.isNotBlank() }
                                ?.let {
                                    strings(
                                        "threads.moving",
                                        "date" to EventDateText.render(strings, it),
                                    )
                                }
                                ?: strings["threads.quiet"],
                            style = fonts.headlineSmall,
                            color = scheme.onSurface,
                        )
                        if (entries.isEmpty()) {
                            // **One block rather than two.** An empty thread had
                            // a display-scale "Nothing written on it yet" and
                            // then a second card underneath explaining how to
                            // file something, which is the same answer given
                            // twice at two weights.
                            //
                            // bidi-ok: the app's own sentence, with the thread's
                            // own label isolated into it.
                            Text(
                                text = strings(
                                    "thread.empty",
                                    "label" to Bidi.isolate(thread.label),
                                ),
                                style = fonts.bodyLarge,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(recent, key = { it.id }) { entry -> ThreadEntry(entry, onOpenEntry) }

            if (earlier.isNotEmpty()) {
                item {
                    Eyebrow(
                        text = strings["thread.earlier"],
                        modifier = Modifier.padding(top = Space.m),
                    )
                }
            }

            items(earlier, key = { it.id }) { entry -> ThreadEntry(entry, onOpenEntry) }

            item {
                // **One control, full width, at the end.** It was a pill sized
                // to its own label sitting under a wider pill sized to a longer
                // one, which reads as two things somebody forgot to line up.
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.m)
                        .testTag(OneThreadTags.REMOVE),
                ) {
                    Text(text = strings["remove.action"])
                }
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
    val scheme = MaterialTheme.colorScheme
    val fonts = MaterialTheme.typography
    Card(
        onClick = { onOpenEntry(entry) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .testTag(OneThreadTags.entry(entry.id)),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
    ) {
        Row(
            modifier = Modifier.padding(Space.ml),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            // **The kind's own mark, in the kind's own color.** D198. A thread
            // of a hundred and eighty entries was a hundred and eighty
            // identical beige blocks, and the owner's word for that page was
            // dreary. A call, a visit and a reading are three different things
            // and the notebook already colors them three different ways: this
            // is the same vocabulary, so nobody learns it twice.
            HueMark(hue = entryHue(entry.kind), mark = entryMark(entry.kind))
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                // **The date and the kind together**, because 2.2 says a color
                // never carries meaning alone: the disc is red for an incident
                // and the word beside it says incident.
                Eyebrow(
                    text = com.kamsiob.healthtrail.i18n.Bidi.join(
                        entry.occurredEdtf?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        strings[kindNameKey(entry.kind)],
                    ),
                    fixed = false,
                    color = scheme.onSurfaceVariant,
                )
            Text(
                text = entry.title?.takeIf { it.isNotBlank() }
                    ?.let { Bidi.isolate(it) }
                    ?: strings[kindNameKey(entry.kind)],
                style = fonts.titleMedium,
                color = scheme.onSurface,
            )
            entry.body?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = Bidi.isolate(it),
                    style = fonts.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    // Three lines and then the block ends: the whole entry is
                    // one tap away, and a list where one item is a page long
                    // stops being a list.
                    maxLines = ENTRY_PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            }
        }
    }
}

/** The tag the page frame uses for its way back, so journeys still find it. */
private const val SECTION_BACK = "section_back"

/** How much of an entry a block shows before the entry's own screen takes over. */
private const val ENTRY_PREVIEW_LINES = 3

/**
 * How much of a thread is open when the screen arrives.
 *
 * Twelve, which is a few months of an active thread and a couple of years of a
 * quiet one. Enough to see how it has been going without being the wall the fold
 * exists to prevent.
 */
private const val THREAD_RECENT = 12
