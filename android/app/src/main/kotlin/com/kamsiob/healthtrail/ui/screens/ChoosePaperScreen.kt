package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.Thumbnail
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.time.EventDateText

object ChoosePaperTags {
    const val NAME = "choose_paper"
    const val NEW = "choose_paper_new"
    fun row(id: String) = "choose_paper_$id"
}

/**
 * Which paper goes in an empty place on a project. #379.
 *
 * **The template suggests places and nothing could ever be put in one.** The
 * owner's words: "I can't add a document to one of the existing spots that a
 * project template recommended." `fillProjectPaper` had been in the repository
 * without a caller, so the writer existed and only the door was missing, which
 * is the same shape as the chapters dead end on #377.
 *
 * **The papers already kept lead**, because filing something you photographed
 * last week is the common case, and a picker that opened the camera first
 * would make the common case the long way round.
 *
 * **Photographing a new one is the other door**, and it lands in the same
 * place: the ordinary capture form, so what comes out is an ordinary document
 * on the trail rather than a special kind only this screen knows about.
 */
@Composable
fun ChoosePaperScreen(
    placeName: String,
    documents: List<Repository.Document>,
    attachments: Attachments?,
    onChoose: (String) -> Unit,
    onPhotograph: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = ChoosePaperTags.NAME,
        title = strings["notebook.section.documents"],
        heading = strings["project.papers.fill.title"],
        // **The place is named in the subtitle**, so somebody who opened this
        // from a slot called "The decision letter" is told what they are
        // filling rather than having to remember.
        subtitle = Bidi.isolate(placeName),
        section = Repository.Section.DOCUMENTS,
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            QuietButton(
                label = strings["project.papers.fill.new"],
                onClick = onPhotograph,
                modifier = Modifier.testTag(ChoosePaperTags.NEW),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        if (documents.isEmpty()) {
            item {
                SectionEmpty(
                    name = ChoosePaperTags.NAME,
                    text = strings["project.papers.fill.none"],
                    section = Repository.Section.DOCUMENTS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
            }
            return@SectionScaffold
        }

        item {
            GroupedSurface {
                documents.forEachIndexed { index, document ->
                    DenseRow(
                        title = Bidi.isolate(document.title),
                        subtitle = document.receivedEdtf
                            ?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        leading = {
                            Thumbnail(
                                sha256 = document.sha256,
                                attachments = attachments,
                                section = Repository.Section.DOCUMENTS,
                            )
                        },
                        chevron = true,
                        divider = index < documents.lastIndex,
                        onClick = { onChoose(document.id) },
                        clickLabel = strings["project.papers.fill"],
                        modifier = Modifier.testTag(ChoosePaperTags.row(document.id)),
                    )
                }
            }
            Spacer(Modifier.height(Space.l))
        }
    }
}
