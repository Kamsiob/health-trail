package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.Suggest
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object UnfiledTags {
    const val ROOT = "unfiled_root"
    const val EMPTY = "unfiled_empty"
    const val CLOSE = "unfiled_close"
    fun entry(id: String) = "unfiled_entry_$id"
    fun file(id: String) = "unfiled_file_$id"
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
    val colors = HealthTrail.colors

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.paper,
    ) {
        // **Its own system bar padding.** This screen renders over the shell
        // rather than inside it, so it does not inherit the padding the four
        // destinations get. Without it the title sat on top of the clock, which
        // the screenshot showed immediately and the code did not.
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(UnfiledTags.ROOT)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item {
                    Spacer(Modifier.height(Space.l))
                    Text(
                        text = strings["unfiled.title"],
                        style = HealthTrail.type.displayL,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        // Says plainly that nothing was filed for them, which
                        // is the promise underneath this whole screen.
                        text = strings["unfiled.subtitle"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
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

                items@ for (entry in entries) {
                    item(key = entry.id) {
                        UnfiledRow(
                            entry = entry,
                            threads = threads,
                            onFile = { threadId -> onFile(entry.id, threadId) },
                        )
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }

                item { Spacer(Modifier.height(Space.l)) }
            }

            // The pinned action footer, per 5.15, with its required gap.
            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings["common.close"],
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(UnfiledTags.CLOSE),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * One waiting entry, with its answer.
 *
 * The date renders through [EventDateText], so an entry saved with "Not sure"
 * says the date is not known rather than showing the day it was typed. This
 * tray is the likeliest place in the app to hold one, since a person who could
 * not say where something belonged often could not say when either.
 */
@Composable
private fun UnfiledRow(
    entry: Repository.UnfiledEntry,
    threads: List<Repository.CareThread>,
    onFile: (String?) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // The suggestion is the starting selection, which is what makes confirming
    // it one tap. It is computed once per entry rather than on every
    // recomposition, and it is allowed to be null.
    val suggested = remember(entry.id, threads) {
        Suggest.threadFor(listOfNotNull(entry.title, entry.body).joinToString(" "), threads)
    }
    var chosen by remember(entry.id) { mutableStateOf(suggested?.id) }
    var noneChosen by remember(entry.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(UnfiledTags.entry(entry.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = EventDateText.render(strings, entry.occurredEdtf),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.height(Space.xs))

        // A blank title is normal here. The kind is what the app knows for
        // certain, and it is never left as an empty line.
        Text(
            text = entry.title?.takeIf { it.isNotBlank() } ?: strings[kindKey(entry.kind)],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        entry.body?.takeIf { it.isNotBlank() }?.let { body ->
            Spacer(Modifier.height(Space.xs))
            Text(text = body, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        if (threads.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            // The same question the capture form asks, in the same words and
            // the same control. This screen exists because the person did not
            // answer it there, so asking it differently here would make it a
            // second question rather than the same one, still open.
            ChoiceChipGroup(label = strings["capture.thread"]) {
                threads.forEach { thread ->
                    ChoiceChip(
                        label = thread.label,
                        selected = !noneChosen && chosen == thread.id,
                        onClick = { chosen = thread.id; noneChosen = false },
                        dotColor = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size],
                    )
                }
                ChoiceChip(
                    label = strings["unfiled.none"],
                    selected = noneChosen,
                    onClick = { noneChosen = true; chosen = null },
                )
            }
        }

        Spacer(Modifier.height(Space.sm))

        // A text action rather than a filled button. There is one of these per
        // card, and a column of filled buttons would turn a quiet list into a
        // wall of blue, which is section 2.2's accent spent on repetition.
        TextAction(
            label = strings["unfiled.file"],
            onClick = { onFile(if (noneChosen) null else chosen) },
            modifier = Modifier.testTag(UnfiledTags.file(entry.id)),
        )
    }
}

private fun kindKey(kind: String): String = when (kind) {
    "call" -> "capture.call"
    "visit" -> "capture.visit"
    "incident" -> "capture.incident"
    "measurement" -> "capture.measurement"
    "question" -> "capture.question"
    "document" -> "capture.document"
    else -> "capture.title"
}
