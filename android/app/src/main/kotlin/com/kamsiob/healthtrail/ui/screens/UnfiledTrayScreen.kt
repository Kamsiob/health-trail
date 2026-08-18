package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.Suggest
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.ChipPickerSheet
import com.kamsiob.healthtrail.ui.v4.PickerOption
import com.kamsiob.healthtrail.ui.v4.RouteSwatch
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider

object UnfiledTags {
    const val ROOT = "unfiled_root"
    const val EMPTY = "unfiled_empty"
    const val CLOSE = "unfiled_close"
    const val LATER = "unfiled_later"
    fun behind(id: String) = "unfiled_behind_$id"
    fun entry(id: String) = "unfiled_entry_$id"
    /** The suggestion, which is the primary action on a card that has one. */
    fun file(id: String) = "unfiled_file_$id"
    fun alternate(entryId: String, threadId: String) = "unfiled_alt_${entryId}_$threadId"
    fun elsewhere(id: String) = "unfiled_elsewhere_$id"
}

/**
 * The Unfiled tray: everything saved without a home.
 *
 * **This screen exists because the capture form already promises it.** The form
 * tells the person their entry is going to the Unfiled tray, and until now
 * there was nowhere to see it. A promise the app makes and does not keep costs
 * more than a feature it never mentioned.
 *
 * **The app suggests and the person confirms.** `MASTER_SPEC.md` section 4.2:
 * a home is suggested by plain word matching, the person confirms with one tap,
 * and the app never files anything on its own. The suggestion arrives already
 * selected, which is what makes it one tap, and changing it is one more.
 * Nothing is written until the person taps the action.
 *
 * **"None of these" is a real answer**, not a way out of the question. It
 * clears the entry from the tray without a thread, because the tray is for
 * things nobody has looked at yet rather than for things without a thread.
 *
 * **The empty state is the common one.** Most notebooks will have an empty tray
 * most of the time, and it reads as "nothing waiting" rather than as an absence
 * of anything.
 *
 * Composed from Display L, Body M, Body S, the Mono metadata style, cards 5.3,
 * choice chips 5.11, and one text action. Nothing new was introduced.
 */
@Composable
fun UnfiledTrayScreen(
    entries: List<Repository.UnfiledEntry>,
    threads: List<Repository.CareThread>,
    onFile: (entryId: String, threadId: String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    // Which ones the person has said "not now" to, this time through. Not a
    // stored state: it is about this sitting, and it must not become a quiet
    // second inbox the record knows about.
    var passed by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val colors = HealthTrail.colors

    Page(
        title = strings["unfiled.title"],
        onBack = onClose,
        backLabel = strings["common.cancel"],
        modifier = modifier,
        eyebrow = strings[labelKey(Repository.Section.TRAIL)],
        section = Repository.Section.TRAIL,
        // **The form's own gaps, not the page's.** A form is one
        // question after another rather than a column of groups, and
        // it spaces itself inside its single item.
        itemSpacing = Space.none,
        band = {
        // The pinned action footer, per 5.15, with its required gap.
        Spacer(Modifier.height(Space.m))
        },
    ) {
        item {
            // The lead says plainly that nothing was filed for them,
            // which is the promise underneath this whole screen.
                Spacer(Modifier.height(Space.m))
                FactBlock(
                    label = null,
                    text = strings["unfiled.subtitle"],
                    tone = BlockTone.Section,
                    mark = Symbols.of(Repository.Section.TRAIL),
                    hue = hueFor(Repository.Section.TRAIL),
                )
            Spacer(Modifier.height(Space.l))
        }

        if (entries.isEmpty()) {
            item {
                Text(
                    text = strings["unfiled.empty"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink2,
                    modifier = Modifier.testTag(UnfiledTags.EMPTY),
                )
            }
        } else if (threads.isEmpty()) {
            // A notebook with no situation template has no threads, so
            // there is nowhere to file anything. Said plainly rather
            // than shown as an empty chip row, which would read as a
            // question with no answers.
            item {
                Text(
                    text = strings["unfiled.no_threads"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
            }
        }

        // **One at a time, and this screen was showing eighty six.**
        // Law 1: the one thing is where *this* one lives. A wall of
        // items each asking the same question is the pile the person
        // came here to get out from under, reproduced on a screen.
        //
        // Everything else is counted behind one fold, so nothing is
        // hidden and the number is honest.
        val waiting = entries.filter { it.id !in passed }
        val current = waiting.firstOrNull()

        if (current != null) {
            item(key = current.id) {
                UnfiledRow(
                    entry = current,
                    threads = threads,
                    onFile = { threadId -> onFile(current.id, threadId) },
                )
                Spacer(Modifier.height(Space.m))
                // **Leaving it is a real answer, not a failure.** Rule
                // 13: partial is a finished state. Somebody who does not
                // know where this belongs should be able to say so and
                // move on, and the thing stays exactly where it was.
                // Sized to its label, D137: this screen already ends
                // in a full width outlined bar and that one is the way
                // out. #371 item 5.
                Action(
                    label = strings["unfiled.later"],
                    onClick = { passed = passed + current.id },
                    modifier = Modifier.testTag(UnfiledTags.LATER),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else if (entries.isNotEmpty()) {
            // Everything has been passed over at least once. Said
            // plainly, with the way to go round again, rather than an
            // empty screen that looks like the work is gone.
            item(key = "passed") {
                Text(
                    text = strings["unfiled.all_passed"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.m))
                Action(
                    label = strings["unfiled.title"],
                    onClick = { passed = emptySet() },
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        val behind = entries.filter { it.id != current?.id }
        if (behind.isNotEmpty()) {
            item(key = "behind") {
                Eyebrow(text = strings["unfiled.behind"])
                Spacer(Modifier.height(Space.cardGap))
            }

            item(key = "behind_list") {
                Block(padding = Space.none) {
                    behind.forEachIndexed { index, entry ->
                        ListRow(
                            title = Bidi.isolate(
                                entry.title?.takeIf { it.isNotBlank() }
                                    ?: strings[kindKey(entry.kind)],
                            ),
                            value = EventDateText.render(
                                strings, entry.occurredEdtf,
                            ),
                            // Tapping one brings it to the front,
                            // which is the only thing this list can
                            // usefully do: it is a queue, and the
                            // person is choosing what to answer next.
                            // **Brings this one to the front.** The
                            // list is a queue and the only useful
                            // thing a person can do with it is
                            // choose what to answer next, so
                            // everything currently ahead of it steps
                            // aside and it is no longer passed over.
                            onClick = {
                                passed = entries
                                    .map { it.id }
                                    .filterNot { it == entry.id }
                                    .toSet()
                            },
                            modifier = Modifier.testTag(UnfiledTags.behind(entry.id)),
                        )
                        if (index < behind.size - 1) RowDivider(inset = false)
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item { Spacer(Modifier.height(Space.l)) }
    }
}

/**
 * One waiting entry, led by what the app thinks rather than by every answer.
 *
 * **This was inverted on 2026-08-03 and the old shape is worth naming.** Every
 * card offered every care thread as a chip, plus "None of these". On a notebook
 * with seven threads and eighty six waiting entries that is six hundred and
 * eighty eight pills in one scroll, which is the app handing its own problem to
 * a person who came here because they already could not answer it once.
 *
 * **The matcher already produces a suggestion, so the card leads with it.** One
 * tap files it. Two alternates sit under it at lower weight, and everything
 * else, including "None of these", is behind one control. Three visible answers
 * rather than eight, and the first one is usually right.
 *
 * **Tapping files. It does not select.** Every control in the block does the
 * same thing to a different destination, which is why none of them is a choice
 * chip: 5.11 chips choose an answer and something else commits it, and reusing
 * that shape for a control that commits immediately would teach two meanings
 * for one thing. The suggestion is emphasized by surface and weight, per 2.2,
 * never by being the only thing colored.
 *
 * **When the matcher finds nothing there is no suggestion and no eyebrow.** The
 * three quiet options are the threads in their own order, offered rather than
 * recommended. An entry reached this tray because it was hard to place, and a
 * confident wrong guess is worse than an honest blank.
 *
 * The date renders through [EventDateText], so an entry saved with "Not sure"
 * says the date is not known rather than showing the day it was typed.
 */
@Composable
private fun UnfiledRow(
    entry: Repository.UnfiledEntry,
    threads: List<Repository.CareThread>,
    onFile: (String?) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // bidi-ok: matched against, never rendered.
    val text = listOfNotNull(entry.title, entry.body).joinToString(" ")
    // Computed once per entry rather than on every recomposition. Both come
    // from the same scoring, so the alternates can never be better matches
    // than the thing above them.
    val suggested = remember(entry.id, threads) { Suggest.threadFor(text, threads) }
    val ranked = remember(entry.id, threads) { Suggest.ranked(text, threads) }
    val alternates = remember(entry.id, threads) {
        ranked.filter { it.id != suggested?.id }.take(if (suggested == null) 3 else 2)
    }
    var pickerOpen by remember(entry.id) { mutableStateOf(false) }

    val heading = headingFor(entry.title, entry.body, strings[kindKey(entry.kind)])

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.card)
            .testTag(UnfiledTags.entry(entry.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = EventDateText.render(strings, entry.occurredEdtf),
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.xs))

        // The entry's own words, per the entry screen's rule. An untitled entry
        // is the ordinary case here and it leads with what the person wrote
        // rather than with a stock phrase at the largest size on the card.
        // **Isolated**, per `DESIGN.md` 15 and #226: this is the entry's own
        // words, and a Latin sentence inside an Arabic layout lays out against
        // the surrounding direction rather than within its own box.
        Text(
            text = Bidi.isolate(heading.text),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        if (heading.repeatBody) {
            entry.body?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Bidi.isolate(body),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
            }
        }

        if (threads.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))

            if (suggested != null) {
                Eyebrow(text = strings["unfiled.suggested"])
                Spacer(Modifier.height(Space.s))
                FileHere(
                    thread = suggested,
                    emphasized = true,
                    onClick = { onFile(suggested.id) },
                    modifier = Modifier.testTag(UnfiledTags.file(entry.id)),
                )
                Spacer(Modifier.height(Space.s))
            }

            // Side by side, so two alternates cost one row rather than two.
            // Eighty six cards is a queue somebody works through, and every row
            // saved is a scroll they do not make.
            //
            // **Stacked above font scale 1.3, and that is a fix rather than a
            // preference.** At 2.0 a third of the width holds about four
            // characters, and "Speech therapy" rendered as "Speec / h /
            // therapy" and "Activities" as "Activiti / es". Breaking a word
            // across lines is what section 3 item 4 and rule 11 both rule out,
            // and it is the same failure as the Arabic display headings: the
            // layout has to give way, never the type. Found by looking at this
            // screen at 2.0, not in the code.
            if (LocalDensity.current.fontScale > STACK_ABOVE) {
                alternates.forEach { thread ->
                    FileHere(
                        thread = thread,
                        emphasized = false,
                        onClick = { onFile(thread.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UnfiledTags.alternate(entry.id, thread.id)),
                    )
                    Spacer(Modifier.height(Space.s))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    alternates.forEach { thread ->
                        FileHere(
                            thread = thread,
                            emphasized = false,
                            onClick = { onFile(thread.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag(UnfiledTags.alternate(entry.id, thread.id)),
                        )
                    }
                    // A row that is not full keeps its columns, so one
                    // alternate does not stretch to the width of two and read
                    // as a second suggestion.
                    repeat((if (suggested == null) 3 else 2) - alternates.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(Space.xs))

            // Everything else, including "None of these", which is a real
            // answer rather than a way out of the question: it clears the entry
            // from the tray without a thread, because the tray is for things
            // nobody has looked at yet rather than for things without a thread.
            Action(
                label = strings["unfiled.choose"],
                onClick = { pickerOpen = true },
                modifier = Modifier.testTag(UnfiledTags.elsewhere(entry.id)),
            )
        }
    }

    if (pickerOpen) {
        ChipPickerSheet(
            title = strings["capture.thread"],
            options = threads.map { thread ->
                PickerOption(
                    id = thread.id,
                    label = Bidi.isolate(thread.label),
                    routeColor = colors.threadRoutes[
                        thread.colorIndex.mod(colors.threadRoutes.size),
                    ],
                    routeIndex = thread.colorIndex,
                )
            } + PickerOption(id = NONE_OF_THESE, label = strings["unfiled.none"]),
            selectedId = null,
            onPick = { option ->
                pickerOpen = false
                onFile(option.id.takeIf { it != NONE_OF_THESE })
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

/**
 * One destination, as a control that puts the entry there.
 *
 * `blue_soft` with a `blue_deep` label when it is the suggestion and `sand`
 * with an `ink` label when it is an alternate, which is the same surface plus
 * weight language 5.11 uses for a chosen chip rather than a second one. The
 * route swatch is how a thread identifies itself everywhere, per 5.2.2, and it
 * is never the only difference between two of these, because each carries the
 * thread's name.
 */
@Composable
private fun FileHere(
    thread: Repository.CareThread,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val scheme = MaterialTheme.colorScheme

    // **Material's surface owns the container, the press and the focus ring.**
    // #392. The emphasized thread keeps its accent wash, which is the one thing
    // here that carries meaning: it is the thread the app has guessed at.
    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = Space.touchTarget),
        shape = MaterialTheme.shapes.small,
        color = if (emphasized) scheme.primaryContainer else scheme.surfaceContainer,
    ) {
    Row(
        modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteSwatch(
            color = colors.threadRoutes[thread.colorIndex.mod(colors.threadRoutes.size)],
            index = thread.colorIndex,
        )
        Spacer(Modifier.width(Space.s))
        Text(
            text = Bidi.isolate(thread.label),
            style = if (emphasized) HealthTrail.type.label else HealthTrail.type.bodyM,
            color = if (emphasized) colors.blueDeep else colors.ink,
        )
    }
    }
}

/** The id the picker uses for "None of these", which is not a thread. */
private const val NONE_OF_THESE = "unfiled_none_of_these"

/**
 * Where the side by side row gives way to a stack.
 *
 * The same 1.3 the tile grid uses in 11.2, and for the same reason: past it a
 * fraction of the width cannot hold a word.
 */
private const val STACK_ABOVE = 1.3f

private fun kindKey(kind: String): String = when (kind) {
    "call" -> "capture.call"
    "visit" -> "capture.visit"
    "incident" -> "capture.incident"
    "measurement" -> "capture.measurement"
    "question" -> "capture.question"
    "document" -> "capture.document"
    else -> "capture.title"
}
