package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue

object MemosAboutTags {
    const val BLOCK = "memos_about"
    const val ADD = "memos_about_add"
    fun memo(id: String) = "memos_about_$id"
}

/**
 * The memos written about one thing, on that thing's own screen. #397, D207.
 *
 * **This is rule 18's other half and it is the hard half.** If A shows B, B
 * shows A. A memo attached to Tuesday's visit appears on that appointment, and
 * the appointment appears on the memo. One component rather than seven, because
 * seven screens each growing their own version of this block is how they come
 * to disagree about what a memo looks like.
 *
 * **The link is read from both sides**, `Repository.notesAbout`, so a row
 * written from either end is found. A link is a fact about two things, not a
 * property of one of them.
 *
 * **The way in carries the context with it.** Writing a memo from here already
 * knows what it is about, which is rule 18's other instruction: carry the
 * context forward rather than asking again. Nobody picks the appointment out of
 * a list they just came from.
 *
 * **Nothing here is a count of anything the person should have done.** Rule 13:
 * no memos on a thing is an ordinary state and says so plainly.
 */
@Composable
fun MemosAbout(
    memos: List<Repository.TrailEntry>,
    onOpen: (Repository.TrailEntry) -> Unit,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **One column, because a lazy item is not one.** A `LazyColumn`'s `item`
    // measures its content as a single layout node, so several siblings emitted
    // into it stack on top of each other rather than running down the page.
    // Seen on the phone: the label, the sentence and the way in were all drawn
    // at the same place, and the tap landed on whichever was on top, so the
    // control did nothing. `docs/TRAPS.md` has the sibling of this for weighted
    // children in an unbounded column.
    Column(modifier = modifier.testTag(MemosAboutTags.BLOCK)) {
    Eyebrow(text = strings["notes.attached"])
    Spacer(Modifier.height(Space.withinGroup))

    if (memos.isEmpty()) {
        // **A sentence, not an empty block.** `SectionEmpty` is for a screen
        // with nothing on it; this is one quiet rung on a screen that is full
        // of other things, and giving it the whole designed empty state would
        // make the absence of memos the loudest thing on somebody's incident.
        Text(
            text = strings["notes.none"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    } else {
        Block(padding = Space.none) {
            memos.forEachIndexed { index, memo ->
                if (index > 0) RowDivider(inset = true)
                ListRow(
                    mark = Symbols.notes,
                    markSize = Space.markCard,
                    markHue = goldHue(),
                    title = Bidi.isolate(
                        memo.title?.takeIf { it.isNotBlank() } ?: strings["notes.untitled"],
                    ),
                    // **The words, never the marks.** `RichText.plain` strips
                    // the three so a row never shows somebody the storage.
                    support = RichText.plain(memo.body.orEmpty())
                        .takeIf { it.isNotBlank() },
                    value = EventDateText.render(strings, memo.occurredEdtf),
                    valueBelow = true,
                    isDoor = true,
                    onClick = { onOpen(memo) },
                    clickLabel = strings["note.open"],
                    modifier = Modifier.testTag(MemosAboutTags.memo(memo.id)),
                )
            }
        }
    }

    Spacer(Modifier.height(Space.withinGroup))
    Action(
        label = strings["notes.add"],
        mark = Symbols.notes,
        onClick = onWrite,
        modifier = Modifier.testTag(MemosAboutTags.ADD),
    )
    }
}
