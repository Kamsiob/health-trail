package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider

object BinTags {
    const val NAME = "bin"
    fun row(id: String) = "bin_row_$id"
    fun restore(id: String) = "bin_restore_$id"
}

/**
 * Everything taken out of the notebook, and the way to put it back. #405.
 *
 * **The owner, 2026-08-18: "a universal trashcan feature in more where they can
 * restore anything deleted from anywhere in the app and have it go back to
 * whenever it was deleted from in the right spot on the timeline."**
 *
 * **Nothing here had to be built into the record, because it was already
 * there.** Rule 3: deletion is always a tombstone, and every screen reads a
 * `live_*` view that hides one. This screen is a reader over the rows those
 * views hide, and putting something back clears one column.
 *
 * **"Back in the right spot on the timeline" costs nothing**, and that is the
 * point of tombstones: the row kept its own dates, so a thing taken out in
 * March returns to March rather than to the top. Nothing is remembered
 * separately and nothing can drift.
 *
 * **There is no "delete forever" and that is deliberate.** The archive is the
 * record, D24, and a second permanent deletion is a way to lose something that
 * rule 3 exists to prevent. If the owner ever wants one it is his call and it
 * is not the default.
 *
 * **Each thing says what it is in the person's own words**, never a table name
 * or a row id, which is rule 20.
 *
 * **Nothing here counts anything against anybody.** Rule 13: taking things out
 * is ordinary, and a bin that says "you have deleted 14 things" would be a
 * score on somebody's own housekeeping.
 */
@Composable
fun BinScreen(
    discarded: List<Repository.Discarded>,
    onRestore: (Repository.Discarded) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Page(
        title = strings["bin.title"],
        onBack = onBack,
        backLabel = strings["section.back.bin"],
        modifier = modifier.testTag(SectionTags.root(BinTags.NAME)),
        subtitle = strings["bin.subtitle"],
    ) {
        if (discarded.isEmpty()) {
            item {
                // **A bin with nothing in it is a finished screen.** Rule 13,
                // and the sentence says what will happen rather than what has
                // not: things wait here, and they can come back.
                SectionEmpty(
                    name = BinTags.NAME,
                    lead = strings["bin.empty.lead"],
                    text = strings["bin.empty"],
                )
            }
            return@Page
        }

        item {
            Eyebrow(text = strings("bin.count", "count" to discarded.size))
            Spacer(Modifier.height(Space.withinGroup))
        }

        item {
            Block(padding = Space.none) {
                discarded.forEachIndexed { index, thing ->
                    if (index > 0) RowDivider(inset = true)
                    ListRow(
                        // **The mark of the place it came from**, so somebody
                        // recognizes it by the same color it wore there. D198.
                        mark = Symbols.of(thing.section),
                        markSize = Space.markCard,
                        markHue = hueFor(thing.section),
                        overline = strings(
                            "bin.removed",
                            // **The day, in the app's own words.** The archive's
                            // timestamp carries a zone offset because a record
                            // has to be unambiguous across devices; "taken out
                            // August 18, 2026 at 21:13 UTC-04:00" on a bin row
                            // is telling somebody about time zones when they
                            // are looking for something they lost. `EventDateText`
                            // is what every other date on every other screen
                            // goes through.
                            "date" to EventDateText.render(strings, dayOf(thing.deletedAt)),
                        ),
                        title = Bidi.isolate(
                            thing.label.takeIf { it.isNotBlank() }
                                ?: strings["bin.untitled"],
                        ),
                        trailing = {
                            Action(
                                label = strings["bin.restore"],
                                onClick = { onRestore(thing) },
                                modifier = Modifier.testTag(BinTags.restore(thing.id)),
                            )
                        },
                        modifier = Modifier.testTag(BinTags.row(thing.id)),
                    )
                }
            }
        }
    }
}

/**
 * The day something was taken out, as EDTF.
 *
 * **Through the app's own date rendering rather than a second formatter**, so a
 * bin row reads exactly like every other date in the notebook.
 */
private fun dayOf(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toString()
