package com.kamsiob.healthtrail.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DocTags {
    const val NAME = "documents"
    const val ADD = "documents_add"
    fun row(id: String) = "document_$id"
}

/**
 * Documents: photographs of the paperwork, and where each original is.
 *
 * **Where the paper physically is matters more than the photograph does.** The
 * schema says so in its own comment and it is the reason this section earns its
 * place: the digital copy is rarely the one a clerk will accept, so "signed and
 * handed back to the ward clerk, copy in the blue folder" is the line that
 * saves a phone call six weeks later.
 *
 * **A document with no photograph is a real document.** Knowing the discharge
 * summary exists and lives in the blue folder is worth writing down before
 * there is a picture of it.
 *
 * The thumbnail is decoded from the content addressed file on disk, at a size
 * that fits the card rather than at full resolution, because a list of full
 * size photographs is how a scrolling list runs out of memory.
 */
@Composable
fun DocumentsScreen(
    documents: List<Repository.Document>,
    onRemove: (Repository.Document) -> Unit,
    onEdit: (Repository.Document) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = DocTags.NAME,
        title = strings["notebook.section.documents"],
        subtitle = strings["docs.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (documents.isEmpty()) {
            item {
                SectionEmpty(name = DocTags.NAME, text = strings["docs.empty"], section = Repository.Section.DOCUMENTS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        for (document in documents) {
            item(key = document.id) {
                DocumentRow(
                    document = document,
                    onRemove = { onRemove(document) },
                    onEdit = { onEdit(document) },
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["docs.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(DocTags.ADD),
            )
        }
    }
}

@Composable
private fun DocumentRow(
    document: Repository.Document,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .removableByLongPress(strings["edit.hint"], onRemove, onEdit)
            .testTag(DocTags.row(document.id))
            .padding(Space.cardPadding),
    ) {
        document.receivedEdtf?.takeIf { it.isNotBlank() }?.let { received ->
            Text(
                text = EventDateText.render(strings, received),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = document.title,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        val thumbnail = rememberThumbnail(document.sha256)
        if (thumbnail != null) {
            Spacer(Modifier.height(Space.sm))
            Image(
                bitmap = thumbnail,
                // The photograph is of the document already named above it, so
                // describing it again would make a reader hear the same thing
                // twice.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(Radius.thumbnail),
            )
        } else if (document.sha256 == null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings["docs.no_photo"],
                style = HealthTrail.type.bodyS,
                color = colors.ink3Text,
            )
        }

        document.originalLocation?.takeIf { it.isNotBlank() }?.let { where ->
            Spacer(Modifier.height(Space.sm))
            Text(
                text = strings["docs.original"],
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
            Spacer(Modifier.height(Space.xs))
            Text(text = where, style = HealthTrail.type.bodyL, color = colors.ink)
        }

        document.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.sm))
            Text(text = notes, style = HealthTrail.type.bodyM, color = colors.ink2)
        }
    }
}

/**
 * The photograph, decoded small enough to sit in a list.
 *
 * **Subsampled rather than decoded whole.** A phone camera photograph is
 * several thousand pixels on a side and a handful of them at full size is how a
 * scrolling list runs out of memory. It is decoded off the main thread, and a
 * file that will not decode returns null rather than throwing, because a
 * document whose image is unreadable is still a document with a title and a
 * location worth showing.
 */
@Composable
private fun rememberThumbnail(sha256: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(sha256) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(sha256) {
        if (sha256 == null) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = Attachments.open(context).fileFor(sha256)
                if (!file.exists()) return@runCatching null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > TARGET_WIDTH_PX) sample *= 2

                BitmapFactory
                    .decodeFile(
                        file.path,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    return bitmap
}

/** Wider than any phone in dp, so a thumbnail never looks soft. */
private const val TARGET_WIDTH_PX = 1080
