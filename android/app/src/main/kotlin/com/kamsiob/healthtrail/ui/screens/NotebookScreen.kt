package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object NotebookTags {
    const val ROOT = "notebook_root"
    fun section(section: Repository.Section) = "notebook_section_${section.name.lowercase()}"
    fun count(section: Repository.Section) = "notebook_count_${section.name.lowercase()}"
}

/** One row of the table of contents: a section, and how much is in it. */
data class SectionCount(val section: Repository.Section, val count: Int)

/**
 * The table of contents, with live counts.
 *
 * **The sections never move.** Their order is fixed, and none is ever hidden,
 * because the whole value of a table of contents is that a person who learned
 * where something was finds it in the same place next month. Which sections sit
 * expanded rather than folded comes from the situation template, and that is a
 * matter of emphasis rather than availability: a folded section is one tap away,
 * never absent.
 *
 * **A count of zero is shown as words, not as a zero.** "Nothing yet" invites,
 * where a column of zeros reads as a scorecard of what the person has failed to
 * fill in. That is the same reason there are no progress rings here and no
 * percentage anywhere: the app never keeps score of someone's diligence.
 *
 * Composed from Display L, Display S, the Mono count style, and the list row
 * idiom. Nothing new was introduced.
 */
@Composable
fun NotebookScreen(
    sections: List<SectionCount>,
    onOpen: (Repository.Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(NotebookTags.ROOT),
        color = colors.paper,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.screenHorizontal),
        ) {
            item {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["notebook.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.l))
            }

            items(sections, key = { it.section.name }) { row ->
                SectionRow(row = row, onClick = { onOpen(row.section) })
                Spacer(Modifier.height(Space.cardGap))
            }

            // Clearance for the capture button, which overlaps the navigation
            // bar's top edge by 16dp and would otherwise sit on top of the last
            // card. 56dp button, 16dp overhang, plus a gap so the last row is
            // readable rather than merely uncovered.
            item { Spacer(Modifier.height(Space.xxl + Space.l)) }
        }
    }
}

@Composable
private fun SectionRow(row: SectionCount, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(NotebookTags.section(row.section))
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings[labelKey(row.section)],
                style = HealthTrail.type.displayS,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                // Composed from a message template so the plural is the
                // catalog's problem rather than a branch in this code, and so
                // zero reads as words rather than as a digit.
                text = strings("notebook.count", "count" to row.count),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
                modifier = Modifier.testTag(NotebookTags.count(row.section)),
            )
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
