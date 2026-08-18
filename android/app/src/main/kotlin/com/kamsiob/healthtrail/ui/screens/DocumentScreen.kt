package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.PaperCard
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object OneDocTags {
    const val SAVE = "document_save"
    const val NAME = "document"
    const val IMAGE = "document_image"
    const val EDIT = "document_edit"
    const val REMOVE = "document_remove"
    const val CHAPTER = "document_chapter"
    const val FOLDER = "document_folder"
    fun project(id: String) = "document_project_$id"

    /**
     * The screen itself.
     *
     * **The tag the old scaffold produced**, written out rather than borrowed,
     * so a journey that waits for this screen still finds it after the rewrite.
     * `PageTags.BACK` keeps the way back the same way, #386.
     */
    const val ROOT = "section_root_document"
}

/**
 * One document: the paper itself, large. Rewritten onto `ui/v4`, #386.
 *
 * **This is `m3v4-5` measured rather than remembered.** The drawing leads with
 * the paper on a white card, puts the document's name under it as the caption,
 * raises where the paper original is into a gold block with its own mark, and
 * files everything the record links to under quiet labels below. The two marks
 * opposite the back arrow are what to do with the paper: save it, or change
 * what is written down about it.
 *
 * **Nothing here descends from the old set**, per `docs/ACCEPTANCE.md`: no
 * `SectionScaffold`, no `Block`, no `DenseRow`, no `GroupHeader`, no
 * `Thumbnail`, no `QuietButton`.
 *
 * **Where the paper is matters more than the photograph does.** The schema says
 * so in its own comment and it is the reason this section exists: the digital
 * copy is rarely the one a clerk will accept, so "signed and handed back to the
 * ward clerk, copy in the blue folder" is the line that saves a phone call six
 * weeks later. It is the one raised block on the screen, `docs/V4.md` 2.1.
 *
 * **A document with no photograph is a real document** and says so plainly
 * rather than showing an empty frame, rule 13. Knowing the discharge summary
 * exists and lives in the blue folder is worth writing down before there is a
 * picture.
 */
@Composable
fun DocumentScreen(
    document: Repository.Document,
    onEdit: () -> Unit,
    /** Taking the document out of the notebook, per #218. Opens the confirmation. */
    onRemove: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the project this is filed as a paper of. Rule 18, #286. */
    onOpenProject: (String) -> Unit = {},
    /** Where this document is filed among the projects' papers. */
    filings: List<Repository.DocumentFiling> = emptyList(),
    backLabelKey: String = "section.back.documents",
    /**
     * Opens the paper at reading size. #378: the photograph rendered at grid
     * resolution and answered no tap, which read as the picture having been
     * thrown away. Null keeps the image inert for a caller with no viewer.
     */
    onOpenPaper: ((String) -> Unit)? = null,
    /**
     * Hands the paper to the system sheet, which is how Android saves a file
     * to the phone. #379: "anywhere there's a document I need to be able to
     * download it to my phone or save it."
     */
    onSavePaper: ((String) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val context = LocalContext.current
    val attachments = remember(context) { Attachments.open(context) }
    // **Unknown is a first class value**, rule 17, and it is not "received on
    // unknown". A date somebody chose not to give says so on its own line;
    // wrapping it in the sentence produced "Received Date not known", which was
    // invisible until the screen was on the phone. Rule 21.
    val date = document.receivedEdtf?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) }
    val received = when {
        date == null || date.precision == Edtf.Precision.UNKNOWN -> strings["date.unknown"]
        else -> strings("document.received", "date" to EventDateText.render(strings, date))
    }

    // The picture opens at reading size where there is one and a viewer to open
    // it in. Both halves are the caller's, so the tap and the action under the
    // card are decided once rather than twice.
    val sha = document.sha256
    val openPaper: (() -> Unit)? = if (sha != null && onOpenPaper != null) {
        { onOpenPaper(sha) }
    } else {
        null
    }

    val paperCard: @Composable () -> Unit = {
        PaperCard(
            sha256 = sha,
            attachments = attachments,
            absent = strings["document.nophoto"],
            imageLabel = strings["document.image.open"],
            onOpen = openPaper,
            action = openPaper?.let {
                {
                    // **Filled, because the card has exactly one action and a
                    // tonal pill on a white card is barely a container.** The
                    // drawing sets it solid for the same reason. It opens
                    // rather than writes, so it does not tick.
                    Action(
                        label = strings["document.image.open"],
                        onClick = it,
                        emphasis = ActionEmphasis.Main,
                        mark = Symbols.fullSize,
                        confirms = false,
                    )
                }
            },
            modifier = Modifier.testTag(OneDocTags.IMAGE),
        )
    }

    Page(
        // The name is the person's own words for their paper.
        title = Bidi.isolate(document.title),
        onBack = onBack,
        backLabel = strings[backLabelKey],
        subtitle = received,
        // The calendar mark the drawing sets the date with. Not announced: it
        // draws what the date beside it already says.
        subtitleMark = Symbols.today,
        // **The paper leads only when there is one.** With no photograph there
        // is no hero, and a gray paragraph standing where the picture would be,
        // above the person's own name for their document, inverts rule 15. The
        // note moves under the facts instead.
        hero = if (sha != null) paperCard else null,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                // **Saving is the system sheet**, which is how a phone puts a
                // file in Downloads, in the gallery, or into a message. The
                // drawing puts a download mark and a share mark side by side;
                // this app has one control for both, because the sheet Android
                // opens is the same sheet.
                if (sha != null && onSavePaper != null) {
                    IconAction(
                        symbol = Symbols.download,
                        label = strings["document.save"],
                        onClick = { onSavePaper(sha) },
                        modifier = Modifier.testTag(OneDocTags.SAVE),
                    )
                }
                IconAction(
                    symbol = Symbols.edit,
                    label = strings["document.edit"],
                    onClick = onEdit,
                    modifier = Modifier.testTag(OneDocTags.EDIT),
                )
            }
        },
        modifier = modifier.testTag(OneDocTags.ROOT),
    ) {
        // **The one raised block on this screen**, and it is not the picture.
        document.originalLocation?.takeIf { it.isNotBlank() }?.let { where ->
            item {
                FactBlock(
                    label = strings["docs.original"],
                    text = Bidi.isolate(where),
                    mark = Symbols.whereThePaperIs,
                )
            }
        }

        // **Said after the facts rather than in place of the picture**, because
        // a document with no photograph is a real document, rule 13, and what
        // is written down about it is the point of the screen either way.
        if (sha == null) {
            item { paperCard() }
        }

        // What the person wrote about it, grouped as a thing rather than left
        // loose on the canvas.
        document.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                Block {
                    Body(text = Bidi.isolate(notes), color = colors.ink)
                }
            }
        }

        // Both ways, per rule 18: a chapter lists its paperwork, and now a
        // piece of paperwork names its chapter.
        document.chapterId?.let { chapterId ->
            val name = document.chapterName?.takeIf { it.isNotBlank() }
            if (name != null) {
                labeledBlock(
                    label = strings["document.where"],
                    rows = listOf {
                        ListRow(
                            title = Bidi.isolate(name),
                            mark = Symbols.chapters,
                            markHue = goldHue(),
                            isDoor = true,
                            onClick = { onOpenChapter(chapterId) },
                            clickLabel = name,
                            modifier = Modifier.testTag(OneDocTags.CHAPTER),
                        )
                    },
                )
            }
        }

        // **Which pile it is in, said on the thing itself.** The documents
        // screen folds by this and until 2026-08-10 nothing could write it, so
        // there was nothing to say. Now that a person can file a document, the
        // document has to be able to say where it was filed, which is rule 18:
        // if the folder shows the document, the document shows the folder. #221.
        document.category?.takeIf { it.isNotBlank() }?.let { folder ->
            labeledBlock(
                label = strings["document.folder"],
                rows = listOf {
                    ListRow(
                        title = Bidi.isolate(folder),
                        mark = Symbols.documents,
                        // **The section's own wash**, not the default white
                        // tile: a white square inside a sand block was the
                        // loudest thing on the screen and it named a fold on a
                        // list. Gold stays on the two rows that lead somewhere.
                        markHue = hueFor(Repository.Section.DOCUMENTS),
                        // **No mark at the end and no tap**, because a folder is
                        // not a screen: it is a fold on the documents list. A row
                        // that looked openable and was not would be the defect
                        // rule 11 names.
                        modifier = Modifier.testTag(OneDocTags.FOLDER),
                    )
                },
            )
        }

        // **The other half of screen 13's link.** A project's papers open the
        // document; without this the document said nothing about the process it
        // belongs to, which is what somebody opening it from Documents months
        // later most wants to know. Rule 18.
        labeledBlock(
            label = strings["document.filed_as"],
            rows = filings.map { filing ->
                {
                    ListRow(
                        title = Bidi.isolate(filing.projectName),
                        // **The place, not just the project.** "The award
                        // letter" is what the person called the slot, and it is
                        // the half that says why this paper is there.
                        support = Bidi.isolate(filing.paperName),
                        mark = Symbols.projects,
                        markHue = goldHue(),
                        isDoor = true,
                        onClick = { onOpenProject(filing.projectId) },
                        clickLabel = filing.projectName,
                        modifier = Modifier.testTag(OneDocTags.project(filing.projectId)),
                    )
                }
            },
        )

        // **Sized to its label**, D118, and quiet, because removing the record
        // is never what this screen is for. **Removing the document removes the
        // record of it**, and the confirmation says so in the same words it
        // says everywhere else.
        item {
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                emphasis = ActionEmphasis.Quiet,
                modifier = Modifier.testTag(OneDocTags.REMOVE),
            )
        }
    }
}
