package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FILL
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.Thumbnail
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object OneDocTags {
    const val NAME = "document"
    const val IMAGE = "document_image"
    const val EDIT = "document_edit"
    const val CHAPTER = "document_chapter"
    fun project(id: String) = "document_project_$id"
}

/**
 * One document: the paper itself, large.
 *
 * **Tapping a document used to open the form that edits it**, the same defect
 * one bill had. A section whose whole point is that the app holds the person's
 * own paper had no screen that showed a piece of it at a size anybody could
 * read.
 *
 * **The photograph is the one thing and it gets the width.** Everything the
 * record knows about it is underneath, in the order somebody needs it: where
 * the paper original actually is, when it arrived, and what it came out of.
 *
 * **Where the paper is matters more than the photograph does.** The schema says
 * so in its own comment and it is the reason this section exists: the digital
 * copy is rarely the one a clerk will accept, so "signed and handed back to the
 * ward clerk, copy in the blue folder" is the line that saves a phone call six
 * weeks later.
 *
 * **A document with no photograph is a real document** and says so plainly
 * rather than showing an empty frame. Knowing the discharge summary exists and
 * lives in the blue folder is worth writing down before there is a picture.
 */
@Composable
fun DocumentScreen(
    document: Repository.Document,
    onEdit: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the project this is filed as a paper of. Rule 18, #286. */
    onOpenProject: (String) -> Unit = {},
    /** Where this document is filed among the projects' papers. */
    filings: List<Repository.DocumentFiling> = emptyList(),
    backLabelKey: String = "section.back.documents",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val context = LocalContext.current
    val attachments = remember(context) { Attachments.open(context) }

    SectionScaffold(
        name = OneDocTags.NAME,
        title = strings["notebook.section.documents"],
        heading = Bidi.isolate(document.title),
        section = Repository.Section.DOCUMENTS,
        subtitle = document.receivedEdtf?.takeIf { it.isNotBlank() }
            ?.let { EventDateText.render(strings, it) }
            ?: strings["date.unknown"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            if (document.sha256 != null) {
                Thumbnail(
                    sha256 = document.sha256,
                    attachments = attachments,
                    section = Repository.Section.DOCUMENTS,
                    size = FILL,
                    modifier = Modifier.fillMaxWidth().testTag(OneDocTags.IMAGE),
                )
            } else {
                Text(
                    text = strings["document.nophoto"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(OneDocTags.IMAGE),
                )
            }
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **Where the paper is, at reading weight rather than in a row.** It is
        // the sentence somebody came here for, and a row would put it at the
        // same weight as a date.
        document.originalLocation?.takeIf { it.isNotBlank() }?.let { where ->
            item {
                Text(
                    text = Bidi.isolate(where),
                    style = HealthTrail.type.bodyL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        document.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                Text(
                    text = Bidi.isolate(notes),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // Both ways, per rule 18: a chapter lists its paperwork, and now a
        // piece of paperwork names its chapter.
        document.chapterId?.let { chapterId ->
            val name = document.chapterName?.takeIf { it.isNotBlank() } ?: return@let
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "document.where")
                Spacer(Modifier.height(Space.headerGap))
                GroupedSurface {
                    DenseRow(
                        title = Bidi.isolate(name),
                        leading = { WaypointDot(color = colors.gold, state = Waypoint.MILESTONE) },
                        chevron = true,
                        divider = false,
                        onClick = { onOpenChapter(chapterId) },
                        modifier = Modifier.testTag(OneDocTags.CHAPTER),
                    )
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **The other half of screen 13's link.** A project's papers open the
        // document; without this the document said nothing about the process it
        // belongs to, which is what somebody opening it from Documents months
        // later most wants to know. Rule 18.
        if (filings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "document.filed_as")
                Spacer(Modifier.height(Space.headerGap))
                GroupedSurface {
                    filings.forEachIndexed { index, filing ->
                        DenseRow(
                            title = Bidi.isolate(filing.projectName),
                            // **The place, not just the project.** "The award
                            // letter" is what the person called the slot, and
                            // it is the half that says why this paper is there.
                            subtitle = Bidi.isolate(filing.paperName),
                            leading = { WaypointDot(color = colors.gold) },
                            chevron = true,
                            divider = index < filings.lastIndex,
                            onClick = { onOpenProject(filing.projectId) },
                            modifier = Modifier.testTag(OneDocTags.project(filing.projectId)),
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["document.edit"],
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().testTag(OneDocTags.EDIT),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
