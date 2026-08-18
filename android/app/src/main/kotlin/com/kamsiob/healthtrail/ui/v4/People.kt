package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import java.text.BreakIterator
import java.util.Locale
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * People, written from scratch off `m3v4-3`. #386.
 *
 * **The drawing of the care team is the one that stops being a list.** One
 * person is raised into a block of their own with the two things you do about
 * them, and everyone else is a row with the one thing. That is rule 15 on the
 * screen somebody opens when they need a number in the next ten seconds.
 */

/**
 * Initials in a circle.
 *
 * **There are no photographs of people anywhere in this app and it never asks
 * for one.** It holds photographs of paper. So this is initials rather than a
 * frame waiting for an image that is not coming.
 *
 * **Sized to the circle rather than to the system font.** The circle is a mark,
 * not words, so at font scale 2.0 the letters stay inside it instead of "AR"
 * rendering as "A" and turning Angela Reyes and Angela Ruiz into the same mark.
 * Every real word beside it still scales. That is the exception rule 11's "no
 * truncation" earns here.
 *
 * **Decorative for a reader**, because the name is always beside it.
 *
 * `initialsOf` lives in this file now. It was borrowed from the old component
 * rather than rewritten, because it is a unit tested string function about
 * names rather than a piece of the old design language, and it moved here when
 * that component was retired.
 */
@Composable
fun Avatar(
    name: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    size: Dp = Space.avatarRow,
    /**
     * Filled in the hue's own base with the letters knocked out.
     *
     * **This is what a mark is**, D198 rule 1: `TabHue.base` with
     * `TabHue.onBase` on top, saturated, at the size a mark actually is. It
     * defaulted to the pale wash, so two call sites asked for the real thing
     * and seven got the faint one: the care team was fifteen barely tinted
     * discs, which is the reading the owner called dull on every other list
     * that had the same defect.
     *
     * False is for an avatar sitting **on** a colored surface, where the base
     * would fight the container it is drawn on.
     */
    solid: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (solid) hue.base else hue.wash)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = 1f,
            ),
        ) {
            Text(
                text = initialsOf(name),
                style = if (size >= Space.avatarLead) {
                    HealthTrail.type.displayS
                } else {
                    HealthTrail.type.label
                },
                color = if (solid) hue.onBase else hue.ink,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The last circle in a row of faces, saying how many are not drawn.
 *
 * **A row of faces that stops has to say it stopped.** A card with room for
 * three faces and nine people would otherwise show three, which reads as a care
 * team of three: the app would be deciding which three of somebody's people
 * matter.
 *
 * **The same circle as an [Avatar] and deliberately not a different object.** It
 * is the last member of the row rather than a control or a label, so it takes
 * the row's hue, the row's size, and the same pinned text scale.
 *
 * **Not tappable.** The card behind it is the door, and a second door inside the
 * row would spend a touch target saying the same thing.
 *
 * @param label already worded and already counted, because the plus and the
 *   number belong to the reader's language.
 */
@Composable
fun AvatarOverflow(
    label: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    size: Dp = Space.avatarFace,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(hue.wash)
            // Decorative, exactly as an avatar is: the card's own sentence
            // already says how many more there are, and a reader stopping here
            // would hear the same fact twice.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = 1f,
            ),
        ) {
            Text(
                text = label,
                style = HealthTrail.type.label,
                // **Quiet, because it is a remainder and not a person.** The
                // faces beside it carry the hue's ink; this carries the second
                // ink so the eye reads the names first.
                color = HealthTrail.colors.ink2,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The first character of each of the first two words, taken **by grapheme
 * cluster rather than by character index**.
 *
 * This is the part that has to be right rather than approximately right. A name
 * in Arabic, a name with a combining accent, and a name whose first character is
 * outside the basic plane all have to survive being cut to two, and **cutting by
 * code unit produces half a character**, which renders as a replacement box. In
 * a record-keeping app the thing most likely to be cut wrong is a person's name,
 * which is exactly the content that must never render as a box.
 *
 * Uppercased against the catalog's own locale rather than the device's, so a
 * Turkish phone showing the English catalog cannot turn an "i" into a dotted
 * capital.
 *
 * **A name that was never recorded gets no initials**, and the caller shows the
 * care team drawing instead. A question mark would read as the app asking the
 * person something.
 *
 * **A string function about names rather than a piece of the old design
 * language**, which is why the rebuild moved it here rather than rewriting it:
 * it is unit tested, and two algorithms for one answer is how they drift apart.
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

/**
 * The hue a person's mark wears, decided once from who they are.
 *
 * **The drawing gives the unit three different pale circles**, not three rose
 * ones: `m3v4-3` draws a mint, a slate and a manila down the same list. It is
 * decoration in the exact place decoration helps, which is a column of names
 * somebody scans rather than reads, and it says nothing about the person: rule
 * 2 forbids a color that carries a judgment, and this one carries an identity.
 *
 * **From the id rather than from the name**, so somebody correcting a spelling
 * does not watch their mark change color, and so two people with the same
 * initials get different circles. Stable for the life of the row. D186.
 */
fun hueForPerson(id: String, hues: List<TabHue>): TabHue =
    hues[(id.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFF }) % hues.size]

/**
 * The one person the screen leads with, and the two things you do about them.
 *
 * **`m3v4-3` raises them into a block in the section's own wash**, with the mark
 * filled rather than pale, the name at the top of the ladder, what they do under
 * it, and the actions inside the block rather than under the screen. One
 * saturated block per screen, `docs/V4.md` 2.1, and this is the screen's one.
 *
 * **Calling is the filled action and it is the only filled action here**, which
 * is what this whole section exists for: the value of writing somebody down is
 * that reaching them is one tap later rather than a search through a phone, an
 * email and a discharge folder.
 *
 * **Somebody with no number is a complete person, not a broken one**, rule 13.
 * The block says there is no number in the same quiet register as everything
 * else, and never as a field they failed to fill in.
 */
@Composable
fun PersonHero(
    name: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    /** What they do, in the person's own words. */
    role: String? = null,
    /** The line under the role: where they work, or anything else worth raising. */
    support: String? = null,
    /** What is said in place of the call action when there is no number. */
    noNumber: String? = null,
    call: (@Composable () -> Unit)? = null,
    email: (@Composable () -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    openLabel: String? = null,
) {
    Block(
        modifier = modifier.then(
            if (onOpen == null) {
                Modifier
            } else {
                Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
            },
        ),
        tone = BlockTone.Section,
        hue = hue,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = name, hue = hue, size = Space.avatarLead, solid = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Bidi.isolate(name),
                    style = HealthTrail.type.displayS,
                    color = HealthTrail.colors.ink,
                )
                role?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = Bidi.isolate(it),
                        style = HealthTrail.type.bodyL,
                        color = HealthTrail.colors.ink2,
                    )
                }
                support?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = Bidi.isolate(it),
                        style = HealthTrail.type.bodyM,
                        color = hue.ink,
                    )
                }
                noNumber?.takeIf { call == null }?.let {
                    Text(
                        // bidi-ok: the app's own sentence about a person with no
                        // number yet, never anything somebody typed.
                        text = it,
                        style = HealthTrail.type.bodyM,
                        color = HealthTrail.colors.ink2,
                    )
                }
            }
        }
        if (call != null || email != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                call?.invoke()
                email?.invoke()
            }
        }
    }
}

/**
 * One person in a list: the mark, the name, what they do, and the one action.
 *
 * **Its own block with air around it**, which is how `m3v4-3` draws the unit:
 * three separated tonal rows on the canvas rather than one surface cut by
 * hairlines. Measured at 59dp of row and 19dp of air. D183.
 *
 * **The whole row is one stop for a reader**, and the action beside it is a
 * second: a name, a role and a number is one thing, and fifteen rows at three
 * stops each is forty five swipes down one list.
 */
@Composable
fun PersonRow(
    name: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    support: String? = null,
    onOpen: (() -> Unit)? = null,
    openLabel: String? = null,
    /** The one action a row offers, drawn at its end. */
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(HealthTrail.colors.sand)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
                },
            )
            .sizeIn(minHeight = Space.touchTarget)
            .padding(horizontal = Space.sm, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Avatar(name = name, hue = hue)
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { },
        ) {
            Text(
                text = Bidi.isolate(name),
                style = HealthTrail.type.rowTitle,
                color = HealthTrail.colors.ink,
            )
            support?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = Bidi.isolate(it),
                    style = HealthTrail.type.bodyM,
                    color = HealthTrail.colors.ink2,
                )
            }
        }
        action?.invoke()
    }
}
