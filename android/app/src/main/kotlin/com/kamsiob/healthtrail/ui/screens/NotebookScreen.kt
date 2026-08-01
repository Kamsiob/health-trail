package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object NotebookTags {
    const val ROOT = "notebook_root"
    fun section(section: Repository.Section) = "notebook_section_${section.name.lowercase()}"
    fun count(section: Repository.Section) = "notebook_count_${section.name.lowercase()}"
    fun group(group: NotebookGroup) = "notebook_group_${group.name.lowercase()}"
}

/**
 * How much weight a section carries in this notebook, which the active
 * situation template decides.
 *
 * **This is emphasis, never availability.** All three are present, all three
 * are one tap from the table of contents, and none of them changes where a
 * section sits. A notebook with no situation template renders every section
 * [STANDING], because "Not sure yet" is a real answer to the situation picker
 * and it must not cost the person a working notebook.
 */
enum class Emphasis {
    /** Named in the template's `forward` array. The fullest row. */
    FORWARD,

    /** Named in neither array, or no template chosen. */
    STANDING,

    /** Named in the template's `folded` array. Collapsed to one line, never hidden. */
    FOLDED,
}

/** One row of the table of contents: a section, how much is in it, and its weight. */
data class SectionCount(
    val section: Repository.Section,
    val count: Int,
    val emphasis: Emphasis = Emphasis.STANDING,
)

/**
 * The weights a situation template asks for, turned into sections.
 *
 * Pure, and separate from the screen and from the catalog, because this is the
 * rule that has to be right rather than the pixels: an app that quietly buried
 * a section a person needed would be a worse failure than one that drew the row
 * badly.
 *
 * The template's ids are the section names in lower case, which is what
 * `templates/data/situations.json` already carries. **An id that names no
 * section is skipped rather than failing**, because an export written by a
 * later version of this app will carry sections this one has never heard of and
 * it must still open.
 *
 * Folded is applied first and forward second, so a hand edited template naming
 * the same section in both arrays resolves to the louder of the two. Nothing in
 * the bundled data does that. This is here so that it could never quietly bury
 * a section that was also meant to be put forward.
 */
fun emphasisFrom(
    forward: List<String>,
    folded: List<String>,
): Map<Repository.Section, Emphasis> {
    fun section(id: String) =
        Repository.Section.entries.firstOrNull { it.name.lowercase() == id }

    return buildMap {
        folded.forEach { id -> section(id)?.let { put(it, Emphasis.FOLDED) } }
        forward.forEach { id -> section(id)?.let { put(it, Emphasis.FORWARD) } }
    }
}

/**
 * The four groups of the table of contents, and which sections sit in each.
 *
 * **The grouping is fixed and the order inside it is the order in
 * `MASTER_SPEC.md` section 4.4, unchanged.** Grouping was added to give the
 * screen a hierarchy, and it would have cost more than it gave if it had also
 * reordered the sections: a person who learned where documents were would have
 * had to learn it again. So the twelve sections are read in their existing
 * order and a header is placed at each of the three points where the subject
 * changes. Nothing moved.
 */
enum class NotebookGroup(val labelKey: String, val sections: List<Repository.Section>) {
    PEOPLE_AND_CARE(
        "notebook.group.people_and_care",
        listOf(
            Repository.Section.CARE_TEAM,
            Repository.Section.MEDICATIONS,
            Repository.Section.APPOINTMENTS,
        ),
    ),
    THE_RECORD(
        "notebook.group.the_record",
        listOf(
            Repository.Section.CHAPTERS,
            Repository.Section.THREADS,
            Repository.Section.TRAIL,
            Repository.Section.PROGRESS,
        ),
    ),
    PAPERWORK(
        "notebook.group.paperwork",
        listOf(
            Repository.Section.DOCUMENTS,
            Repository.Section.MONEY,
            Repository.Section.STANDING_INSTRUCTIONS,
        ),
    ),
    KEEP_AT_HAND(
        "notebook.group.keep_at_hand",
        listOf(
            Repository.Section.ASK_NEXT_TIME,
            Repository.Section.EMERGENCY_CARD,
        ),
    ),
}

/**
 * The table of contents, with live counts.
 *
 * **The sections never move.** Their order is fixed, none is ever hidden, and
 * none changes group, because the whole value of a table of contents is that a
 * person who learned where something was finds it in the same place next month.
 * What the situation template decides is weight: a section it puts forward gets
 * the fullest row, a section it folds collapses to a single line, and a folded
 * section is still right there in its own place, one tap away.
 *
 * **A count of zero is shown as words, not as a zero.** "Nothing yet" invites,
 * where a column of zeros reads as a scorecard of what the person has failed to
 * fill in. That is the same reason there are no progress rings here and no
 * percentage anywhere: the app never keeps score of someone's diligence. The
 * count is one style in one color at every weight, so the row's emphasis is
 * never mistaken for a judgment about how full the section is.
 *
 * **The empty state is the resting state.** A new notebook is twelve rows each
 * saying "Nothing yet", which is a complete screen rather than a blank one, so
 * there is no separate empty layout to fall into and no way to reach one.
 *
 * Composed from the icon tile 5.12, the group header 5.13, cards 5.3, Display L,
 * Display S, Label, Body M, and the Mono count style. Nothing new was invented.
 */
@Composable
fun NotebookScreen(
    sections: List<SectionCount>,
    onOpen: (Repository.Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val bySection = sections.associateBy { it.section }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.paper,
    ) {
        LazyColumn(
            // The tag sits on the list rather than on the surface around it, so
            // that a caller can ask the list where one of its keyed items is.
            // On the surface the scroll action still merged upward and looked
            // like it worked, while the item index did not, which is how a test
            // came to report the last two rows as missing when they were only
            // further down.
            modifier = Modifier
                .fillMaxSize()
                .testTag(NotebookTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
        ) {
            item {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["notebook.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["notebook.subtitle"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
            }

            NotebookGroup.entries.forEach { group ->
                val rows = group.sections.mapNotNull { bySection[it] }
                if (rows.isEmpty()) return@forEach

                item(key = "group_${group.name}") {
                    Spacer(Modifier.height(Space.sectionGap))
                    GroupHeader(group)
                    Spacer(Modifier.height(Space.headerGap))
                }

                rows.forEach { row ->
                    item(key = row.section.name) {
                        SectionRow(row = row, onClick = { onOpen(row.section) })
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }

            // Clearance for the capture button, which overlaps the navigation
            // bar's top edge by 16dp and would otherwise sit on top of the last
            // card. 56dp button, 16dp overhang, plus a gap so the last row is
            // readable rather than merely uncovered.
            item { Spacer(Modifier.height(Space.xxl + Space.l)) }
        }
    }
}

/**
 * A group header, per `DESIGN.md` section 5.13: a mono eyebrow with a hairline
 * running out to the end edge, which is how the reference file heads a month in
 * the trail.
 *
 * The hairline is decorative in the sense section 2.3 defines: remove it and
 * nothing becomes unreadable, because the words carry the heading on their own.
 * It is `ink3NonText` all the same.
 */
@Composable
private fun GroupHeader(group: NotebookGroup) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NotebookTags.group(group)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // Uppercased against the catalog's own locale rather than the
            // device's, so a Turkish phone showing the English catalog cannot
            // turn an "i" into a dotted capital. In Arabic and Chinese this is
            // a no-op, which is correct: neither script has case, and the
            // eyebrow reads as an eyebrow there through its size and tracking.
            text = strings[group.labelKey].uppercase(strings.locale),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.width(Space.sm))
        // The label carries no weight and the rule carries all of it, so the
        // label takes exactly the width it needs and the rule takes what is
        // left. A label long enough to fill the row, which is what the longest
        // language does, wraps inside the row and the rule shrinks to nothing
        // rather than pushing the words off the end edge.
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(
                color = colors.ink3NonText.copy(alpha = 0.4f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
            )
        }
    }
}

/**
 * One section, at one of three weights.
 *
 * A forward row and a standing row are the same shape at different densities. A
 * folded row is the same row collapsed: the count comes up onto the title's
 * line, the tile loses its fill, and the height drops. It is still a card, still
 * the full width, still tappable, and still exactly where it always was, because
 * folded means quieter rather than gone.
 */
@Composable
private fun SectionRow(row: SectionCount, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val folded = row.emphasis == Emphasis.FOLDED

    // The emergency card keeps the alert tone at every weight. Section 2.2
    // gives `alert` to this one section, and the reference file draws its row
    // that way in screen 04.
    val emergency = row.section == Repository.Section.EMERGENCY_CARD
    val tint = when {
        emergency -> colors.alertText
        row.emphasis == Emphasis.FORWARD -> colors.ink
        folded -> colors.ink3NonText
        else -> colors.ink2
    }
    // **The filled tile is what carries the hierarchy.** It was tried first as a
    // difference in icon ink alone and that failed on the device: at a glance
    // twelve rows still read as twelve identical rows, which is exactly the
    // defect this rebuild exists to fix. A filled tile against an unfilled one
    // is visible without reading anything, and it is a fill rather than a hue,
    // so it costs the app no second accent and survives a grayscale screenshot.
    val tile = when {
        folded -> Color.Transparent
        emergency -> colors.alertSoft
        row.emphasis == Emphasis.FORWARD -> colors.sand
        else -> Color.Transparent
    }
    val verticalPadding = when (row.emphasis) {
        Emphasis.FORWARD -> Space.m
        Emphasis.STANDING -> Space.sm
        Emphasis.FOLDED -> Space.s
    }

    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                // The row's own surface is the press feedback, per section
                // 5.14. A ripple on top of it would be a second, louder answer
                // to the same touch.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(NotebookTags.section(row.section))
            .padding(horizontal = Space.cardPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(section = row.section, tint = tint, background = tile)
        Spacer(Modifier.width(Space.sm))

        // Composed from a message template so the plural is the catalog's
        // problem rather than a branch in this code, and so zero reads as words
        // rather than as a digit. One style, one color, at all three weights.
        val count = @Composable {
            Text(
                text = strings("notebook.count", "count" to row.count),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
                modifier = Modifier.testTag(NotebookTags.count(row.section)),
            )
        }

        if (folded) {
            Text(
                text = strings[labelKey(row.section)],
                style = HealthTrail.type.label,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Space.s))
            count()
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings[labelKey(row.section)],
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.xs))
                count()
            }
        }

        Spacer(Modifier.width(Space.sm))
        Chevron()
    }
}

/**
 * The chevron, drawn with a Canvas so it needs no icon font and cannot fall
 * back to a box in any language.
 *
 * **It mirrors.** Directional icons flip in a right to left layout, per
 * DESIGN.md section 4.4, and a chevron still pointing right in Arabic is the
 * single most likely direction defect in an app like this. The layout direction
 * is read at draw time rather than assumed.
 *
 * Non-text, so it is held to the 3:1 boundary rather than a text ratio, and it
 * carries no content description because the row it sits in is already the
 * button and already says where it goes.
 */
@Composable
private fun Chevron() {
    val colors = HealthTrail.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(modifier = Modifier.size(width = 8.dp, height = 14.dp)) {
        val strokeWidth = 2.dp.toPx()
        val midY = size.height / 2f
        // Points toward the end edge, whichever edge that is.
        val tipX = if (rtl) 0f else size.width
        val baseX = if (rtl) size.width else 0f

        drawLine(
            color = colors.ink3NonText,
            start = Offset(baseX, 0f),
            end = Offset(tipX, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.ink3NonText,
            start = Offset(tipX, midY),
            end = Offset(baseX, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun labelKey(section: Repository.Section): String = when (section) {
    Repository.Section.CARE_TEAM -> "notebook.section.care_team"
    Repository.Section.MEDICATIONS -> "notebook.section.medications"
    Repository.Section.APPOINTMENTS -> "notebook.section.appointments"
    Repository.Section.CHAPTERS -> "notebook.section.chapters"
    Repository.Section.THREADS -> "notebook.section.threads"
    Repository.Section.TRAIL -> "notebook.section.trail"
    Repository.Section.PROGRESS -> "notebook.section.progress"
    Repository.Section.DOCUMENTS -> "notebook.section.documents"
    Repository.Section.MONEY -> "notebook.section.money"
    Repository.Section.STANDING_INSTRUCTIONS -> "notebook.section.standing_instructions"
    Repository.Section.ASK_NEXT_TIME -> "notebook.section.ask_next_time"
    Repository.Section.EMERGENCY_CARD -> "notebook.section.emergency_card"
    Repository.Section.PROJECTS -> "notebook.section.projects"
}
