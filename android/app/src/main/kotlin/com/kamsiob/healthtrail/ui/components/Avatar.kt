package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.TabHue
import java.text.BreakIterator
import java.util.Locale

/**
 * Initials in a tonal circle, for a person. `DESIGN.md` section 7.
 *
 * **Twelve names in a column is a wall. Twelve names with initial marks is
 * scannable in one pass**, which is the whole reason this exists rather than
 * the list simply being text.
 *
 * **There are no photographs of people anywhere in this app, and it never asks
 * for one.** It stores photographs of paper. That is a product decision as much
 * as a visual one, and it is why this is initials rather than a placeholder
 * waiting for an image that will never arrive.
 *
 * **The circle is tonal and the tone is the section's, not the person's.**
 * A per-person color would be a second accent system and would imply a
 * categorization this app does not have and would not be entitled to make.
 * People and chapters are `rose`, so a care team roster is a column of rose
 * circles and the eye reads the initials rather than sorting the colors.
 *
 * **One exception, recorded rather than assumed:** the person the notebook is
 * about carries `blue`. There is exactly one of them, the app already spends
 * blue on the subject at hand, and it is what tells a roster of twelve who the
 * notebook belongs to without a label saying so. Pass [subject] for that one
 * person and nothing else.
 *
 * **Decorative for a screen reader**, because the name is always beside it. An
 * avatar announced as "avatar, A R" before every name is noise, not access.
 */
@Composable
fun Avatar(
    name: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    size: Dp = AvatarSize.row,
    subject: Boolean = false,
) {
    val colors = HealthTrail.colors
    val background = if (subject) colors.blueWash else hue.wash
    val foreground = if (subject) colors.blueDeep else hue.ink

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            style = HealthTrail.type.label,
            color = foreground,
        )
    }
}

/**
 * Three sizes and no others, so an avatar is the same object everywhere it
 * appears rather than whatever size its screen felt like.
 */
object AvatarSize {
    /** Inside a dense row. */
    val row: Dp = 32.dp
    /** Inside a card. */
    val card: Dp = 40.dp
    /** On a detail screen's identity header. */
    val header: Dp = 56.dp
}

/**
 * The first character of each of the first two words, taken **by grapheme
 * cluster rather than by character index**.
 *
 * This is the part that has to be right rather than approximately right. A name
 * in Arabic, a name with a combining accent, and a name whose first character
 * is outside the basic plane all have to survive being cut to two, and **cutting
 * by code unit produces half a character**, which renders as a replacement box.
 * In a record-keeping app the thing most likely to be cut wrong is a person's
 * name, which is exactly the content that must never render as a box.
 *
 * Uppercased against the catalog's own locale rather than the device's, so a
 * Turkish phone showing the English catalog cannot turn an "i" into a dotted
 * capital.
 *
 * **A name that was never recorded gets no initials**, and the caller shows the
 * care team drawing instead. A question mark would read as the app asking the
 * person something.
 */
internal fun initialsOf(name: String, locale: Locale = Locale.ROOT): String {
    val words = name.trim().split(WHITESPACE)
        .filter { it.isNotEmpty() }
        .dropTitles()
    if (words.isEmpty()) return ""

    val take = if (words.size == 1) listOf(words[0]) else listOf(words[0], words[words.size - 1])
    return take.joinToString("") { firstGrapheme(it) }.uppercase(locale)
}

/**
 * Drops a leading honorific, so "Dr. Priya Raman" gives PR rather than DR.
 *
 * **Found by the test rather than by looking**, which is the reason it is worth
 * a comment. The care team in the reference file lists "Dr. Priya Raman" beside
 * an avatar reading PR, and the obvious implementation gives DR: a title becomes
 * a name, and every doctor on the roster gets the same initials as every other
 * doctor, which is precisely the wall this component exists to avoid.
 *
 * **The rule is shape, not a vocabulary list.** A leading short word ending in a
 * period is an abbreviation rather than a name. That covers Dr., Mr., Ms., Mrs.,
 * Prof., Fr., Sr., and the Spanish Sr. and Sra. without this app maintaining a
 * list of honorifics in four languages and getting it wrong in the fifth.
 * Arabic and Chinese do not use the pattern at all, so it is a no-op there,
 * which is correct rather than a gap.
 *
 * **It never drops everything.** A person recorded only as "Dr." keeps it, since
 * an empty avatar helps nobody and that is the name the person actually wrote
 * down.
 */
private fun List<String>.dropTitles(): List<String> {
    val trimmed = dropWhile { it.endsWith(".") && it.length <= MAX_TITLE_LENGTH }
    return trimmed.ifEmpty { this }
}

/** "Prof." is the longest this app expects. Beyond it, treat it as a name. */
private const val MAX_TITLE_LENGTH = 5

private fun firstGrapheme(word: String): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(word)
    val end = iterator.next()
    return if (end == BreakIterator.DONE) word else word.substring(0, end)
}

private val WHITESPACE = Regex("\\s+")
