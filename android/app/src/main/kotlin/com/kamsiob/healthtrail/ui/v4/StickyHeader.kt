package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The heading that sticks to the top of a long list while its own run scrolls
 * under it. #387, D196.
 *
 * **Written fresh on Material's `Surface` and the old file deleted.** What it
 * replaced was a `Row` with a `background`, an `indication = null` `clickable`,
 * a hand animated pressed surface and a hairline drawn with `drawBehind` at the
 * row's own height. Material owns the container, its color, the state layer and
 * the button role, and `HorizontalDivider` is the hairline.
 *
 * **A five year trail is sixty months.** The header is what makes scrolling
 * through it navigation rather than a wall: it says which month there is before
 * the person decides whether to read it.
 *
 * **It is the page's own furniture rather than an item in the list**, so it
 * takes the canvas color and is opaque. The rows genuinely disappear behind it
 * rather than showing through, which is what makes it read as sticking rather
 * than as floating.
 *
 * **It is a heading for a screen reader**, so somebody can jump between months
 * by heading rather than swiping through every entry in a five year record.
 * That is the single most useful thing this does for somebody not using their
 * eyes, and it is one line.
 *
 * **It may open the period it names, and then it wears the mark.** Law 2: a row
 * that opens another screen ends in a mark, and one that does not must not carry
 * one. The trail's month headers are the only caller that opts in, and they open
 * that month's review. Every other list leaves [onOpen] null and gets a header
 * that is exactly what it looks like: furniture that does nothing.
 *
 * **When not to use it.** On a list that cannot grow past a screenful, where it
 * is furniture with nothing to do. It is one of the four tools a list earns the
 * moment it can grow, not before.
 */
@Composable
fun StickySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    /** What the tap does, in the person's words, for the reader. Null does not open. */
    openLabel: String? = null,
    onOpen: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    val body: @Composable () -> Unit = {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Space.xs,
                        end = Space.xs,
                        top = Space.xs,
                        bottom = Space.s,
                    ),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // **Weighted, so the count and the mark survive.** Unweighted, a
                // month name at font scale 2.0 took the whole row and measured
                // both to zero, and the mark is the only thing saying the header
                // is a door to that month's review. #371.
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    count?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (onOpen != null) {
                        Spacer(Modifier.width(Space.xs))
                        Icon(
                            painter = painterResource(Symbols.forward),
                            contentDescription = null,
                            modifier = Modifier.size(Space.markInline),
                            tint = scheme.outline,
                        )
                    }
                }
            }
            // **Material's divider rather than a line drawn at the row's own
            // height.** The boundary is visible the moment a row slides beneath,
            // and removing it makes nothing unreadable, because the type carries
            // the heading on its own.
            HorizontalDivider(color = scheme.outlineVariant)
        }
    }

    // **The canvas color, so rows do not show through.** A sticky header that
    // is not opaque reads as floating rather than as sticking.
    if (onOpen == null) {
        Surface(
            modifier = modifier.fillMaxWidth().semantics { heading() },
            color = scheme.background,
        ) { body() }
    } else {
        Surface(
            onClick = onOpen,
            // **One node: the heading, the month, the count and the tap.** A
            // reader hears the month and its count together and is told the
            // heading is also a door, rather than being left to guess.
            // `docs/TRAPS.md`.
            modifier = modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    heading()
                    // **What the tap does, in the person's words.** `Surface`
                    // takes no click label of its own here, so the action is
                    // named in the semantics rather than left as "button".
                    openLabel?.let { label ->
                        onClick(label = label) { onOpen(); true }
                    }
                },
            color = scheme.background,
        ) { body() }
    }
}
