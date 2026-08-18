package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The one thing a screen leads with, and what it says. #387, D196.
 *
 * **Never a count of the person's own diligence**, per rule 13, which rules out
 * "4 of 7 steps done" as a hero forever.
 */
@Composable
fun Hero(
    /** A quiet eyebrow saying what this is. */
    eyebrowKey: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Space.l))
        Eyebrow(text = LocalStrings.current[eyebrowKey])
        Spacer(Modifier.height(Space.sm))
        content()
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * One line of a hero, at real size, opening what it names.
 *
 * **Written fresh on Material's `Surface` and the old file deleted.** The line
 * was a `Row` with a clip, a background, a transparent border kept around to be
 * a focus ring, an `indication = null` `clickable` and a hand animated pressed
 * surface. Material owns the shape, the state layer and the button role.
 *
 * **Transparent at rest, because the hero has no surface of its own.** The
 * press still answers: Material's state layer over a transparent container is a
 * tint rather than a surface, which is the right weight for something that is
 * not a button.
 *
 * **A hero block may carry two of these and never three.** Two things needing
 * attention is a fact about a notebook; three is a list, and a list at display
 * size is a screen shouting. Where there would be three, the block carries the
 * two and the rest stay in their own sections where they already live.
 *
 * `headlineLarge` rather than the display sizes, because two of them on a 360dp
 * screen is most of the fold. The size is what makes it a hero; the eyebrow and
 * the space around the block are what make it one thing.
 */
@Composable
fun HeroLine(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        // **One stop for a reader, on the node that takes the tap**, which is
        // `docs/TRAPS.md`'s first entry. The line and its mark are one thing.
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Space.sm))
            Icon(
                painter = painterResource(Symbols.forward),
                contentDescription = null,
                modifier = Modifier.size(Space.markInline),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
