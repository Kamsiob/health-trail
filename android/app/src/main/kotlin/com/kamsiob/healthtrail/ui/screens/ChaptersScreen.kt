package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ChapterTags {
    const val NAME = "chapters"
    fun row(id: String) = "chapter_$id"
}

/**
 * Chapters: the places they have been, in order.
 *
 * **A chapter is a place, and the record is organized around them** because
 * that is how the person actually remembers it. "When she was at Maplewood" is
 * how somebody recalls a period, not a date range, and `MASTER_SPEC.md` section
 * 4.5 builds the journey view on exactly that.
 *
 * **Where they are now is separated from where they have been.** It is the one
 * a person needs most often, it is the answer to a question somebody gets asked
 * on the phone, and it is identified by having no end date rather than by a
 * flag that could disagree with the dates.
 *
 * **A place with no dates at all is a complete row.** Setup asks where they are
 * and asks for nothing else, so the very first chapter in most notebooks is a
 * name and nothing more. That is not a partial record, it is what the person
 * knew at the time.
 *
 * The chapter journey of section 4.5, which pulls a chapter's incidents,
 * documents, and archived care team together, is not built. This lists the
 * places and says when. It is the layer that has to exist first.
 */
@Composable
fun ChaptersScreen(
    chapters: List<Repository.Chapter>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val current = chapters.filter { it.isCurrent }
    val earlier = chapters.filterNot { it.isCurrent }

    SectionScaffold(
        name = ChapterTags.NAME,
        title = strings["notebook.section.chapters"],
        subtitle = strings["chapters.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (chapters.isEmpty()) {
            item { SectionEmpty(name = ChapterTags.NAME, text = strings["chapters.empty"]) }
        }

        if (current.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "chapters.current")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (chapter in current) {
                item(key = chapter.id) {
                    ChapterRow(chapter)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        if (earlier.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "chapters.earlier")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (chapter in earlier) {
                item(key = chapter.id) {
                    ChapterRow(chapter)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(chapter: Repository.Chapter) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(ChapterTags.row(chapter.id))
            .padding(Space.cardPadding),
    ) {
        // The dates are the eyebrow, and they are absent entirely when nobody
        // gave any. An empty date line would be worse than none.
        val started = chapter.startedEdtf?.takeIf { it.isNotBlank() }
        if (started != null) {
            val rendered = EventDateText.render(strings, started)
            Text(
                text = if (chapter.isCurrent) {
                    strings("chapters.since", "date" to rendered)
                } else {
                    rendered
                },
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = chapter.name,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        chapter.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Spacer(Modifier.height(Space.xs))
            Text(text = reason, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        chapter.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = notes, style = HealthTrail.type.bodyS, color = colors.ink3Text)
        }
    }
}
