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
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object ChoosePaperTags {
    const val NAME = "choose_paper"
    const val NEW = "choose_paper_new"
    fun row(id: String) = "choose_paper_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_choose_paper"
}

/**
 * Which paper goes in an empty place on a project. #379. Rewritten onto
 * `ui/v4`, #386.
 *
 * **The template suggests places and nothing could ever be put in one.** The
 * owner's words: "I can't add a document to one of the existing spots that a
 * project template recommended." `fillProjectPaper` had been in the repository
 * without a caller, so the writer existed and only the door was missing.
 *
 * **The papers already kept lead**, because filing something photographed last
 * week is the common case, and a picker that opened the camera first would make
 * the common case the long way round.
 *
 * **Photographing a new one is the other door**, and it lands in the same place:
 * the ordinary capture form, so what comes out is an ordinary document on the
 * trail rather than a special kind only this screen knows about.
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
    backLabelKey: String = "section.back.project_papers",
) {
    val strings = LocalStrings.current
    val hue = hueFor(Repository.Section.DOCUMENTS)

    Page(
        eyebrow = strings["notebook.section.documents"],
        eyebrowColor = hue.ink,
        title = strings["project.papers.fill.title"],
        // **The place is named under the title**, so somebody who opened this
        // from a slot called "The decision letter" is told what they are filling
        // rather than having to remember.
        subtitle = Bidi.isolate(placeName),
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(ChoosePaperTags.ROOT),
    ) {
        item {
            Action(
                label = strings["project.papers.fill.new"],
                onClick = onPhotograph,
                mark = Symbols.add,
                modifier = Modifier.testTag(ChoosePaperTags.NEW),
            )
        }

        if (documents.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about having no papers
                    // saved yet.
                    Body(
                        text = strings["project.papers.fill.none"],
                        color = HealthTrail.colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        labeledBlock(
            label = null,
            rows = documents.map { document ->
                {
                    ListRow(
                        title = Bidi.isolate(document.title),
                        support = document.receivedEdtf
                            ?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        mark = Symbols.documents,
                        markHue = hue,
                        isDoor = true,
                        onClick = { onChoose(document.id) },
                        clickLabel = strings["project.papers.fill"],
                        modifier = Modifier.testTag(ChoosePaperTags.row(document.id)),
                    )
                }
            },
        )

        item { Spacer(Modifier.height(Space.withinGroup)) }
    }
}
