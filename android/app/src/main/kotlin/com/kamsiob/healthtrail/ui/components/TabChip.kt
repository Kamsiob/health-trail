package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * The section's identity, at the top of every section screen. `DESIGN.md`
 * section 7 and 4.3.
 *
 * **A tab chip is identity, never state.** It says which section you are in and
 * nothing more. It never changes to report that something is open, overdue, or
 * unread, and a screen that wants to say one of those says it in words
 * somewhere else.
 *
 * **It is not a chip in the law 2 sense and must never be mistaken for one.**
 * A chip is a choice, outlined when open and filled when chosen, and it
 * responds to touch. This does none of that: it has no press state, no handler,
 * and no chevron. In the costume vocabulary it is **bare**, and law 2's promise
 * about bare things holds here exactly, which is that it does nothing. It is
 * named a chip because the grid names it one.
 *
 * **The shape carries the whole metaphor.** Rounded at the top, square at the
 * bottom, sitting on a 2dp underline in its own ink. That is an index tab on a
 * page rather than a pill, and it is the single element that makes the app read
 * as a binder rather than as a list of screens. [Radius.tabChip] holds the
 * asymmetry so no call site can round the wrong corners.
 *
 * **The label is drawn in the hue's ink, not its base.** Every base hue fails
 * the small-text floor, measured between 3.23:1 and 4.56:1 on the surfaces it
 * lands on, and this label is roughly 11sp and is the first thing on the
 * screen. `DECISIONS.md` D80. The base fills the underline, which is a shape
 * and is held to the 3:1 control floor instead.
 *
 * **It is decorative for a screen reader**, and that is deliberate rather than
 * an omission. The screen's own title says which section this is, immediately
 * below and in the largest type on the screen. A reader announcing "Care team"
 * twice in a row before the person reaches any content is noise, not access.
 * A section whose title does not name it would be a defect in that screen
 * rather than a reason to give this a label.
 */
@Composable
fun TabChip(
    hue: TabHue,
    labelKey: String,
    modifier: Modifier = Modifier,
) {
    TabChipText(hue = hue, label = LocalStrings.current[labelKey], modifier = modifier)
}

/**
 * The same tab over a label the catalog cannot hold, which in practice means a
 * label composed from the person's own data, such as a chapter's name or the
 * date on a single entry.
 *
 * It shares this component rather than growing a second one, because two
 * implementations of one shape drift the moment either is touched.
 */
@Composable
fun TabChipText(
    hue: TabHue,
    label: String,
    modifier: Modifier = Modifier,
) {
    val type = HealthTrail.type
    val underline = hue.base

    Box(
        modifier = modifier
            .clearAndSetSemantics { }
            .clip(Radius.tabChip)
            .background(hue.wash)
            .drawBehind {
                // The underline is drawn rather than added as a border, because a
                // border would round with the clip and follow the top corners.
                // This is a tab sitting on a line, so the line is straight and
                // spans the full width at the bottom edge.
                val stroke = 2.dp.toPx()
                drawLine(
                    color = underline,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(TabChipPadding),
    ) {
        // **The body face, not the mono one.** D173. The chip was the third
        // typeface on screens that already carry a display heading and body
        // text, and the owner's note was that the fonts do not mix well and
        // should not be artsy. A typewriter face on an index tab is a costume:
        // the tab's job is to say which section you are in, quietly, and the
        // hue and the underline already do the identifying. Mono stays where it
        // earns its place, which is figures that line up in a column.
        Text(text = label, style = type.bodyS, color = hue.ink)
    }
}

/**
 * Tight, because a tab is small by design and its job is to be recognized
 * rather than read at length. The bottom value is smaller so the label sits
 * clear of its own underline without the tab looking bottom heavy.
 */
private val TabChipPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 3.dp)

/**
 * A tab hue for a section that is not one of the six, used by the whole-app
 * surfaces.
 *
 * Today, the trail, filing, projects, search, and onboarding belong to no
 * section and use gold with the base ladder, per `DESIGN.md` 4.3. This exists
 * so those screens carry a tab in the same shape rather than growing a second
 * treatment, and so nobody reaches for a section hue to fill the gap.
 */
@Composable
fun wholeAppHue(): TabHue = HealthTrail.colors.let {
    TabHue(base = it.gold, ink = it.goldInk, wash = it.goldWash)
}

/** A neutral tab, for a surface that is neither a section nor gold. */
@Composable
fun neutralHue(): TabHue = HealthTrail.colors.let {
    TabHue(base = it.ink3, ink = it.ink2, wash = it.sand)
}
