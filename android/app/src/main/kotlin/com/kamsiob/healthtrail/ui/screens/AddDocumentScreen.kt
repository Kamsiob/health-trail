package com.kamsiob.healthtrail.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object AddDocTags {
    const val ROOT = "add_doc_root"
    const val PICK = "add_doc_pick"
    const val SAVE = "add_doc_save"
    const val CANCEL = "add_doc_cancel"
    fun field(key: String) = "add_doc_$key"
}

/** What the person chose and typed about a document. */
data class DocumentDraft(
    val title: String = "",
    val originalLocation: String = "",
    val notes: String = "",
    val picked: Uri? = null,
)

/**
 * Saving a document.
 *
 * **The photo picker asks for no permission**, which is the whole reason it is
 * this and not the camera or a storage read. `PickVisualMedia` hands back one
 * image the person chose and nothing else, so the app never gains access to
 * their photographs as a whole. This app asks for no permission it does not
 * need, and it does not need that one.
 *
 * **"Where the paper original is" is the field this screen exists for.** The
 * schema's own comment says the digital copy is rarely the one a clerk will
 * accept. A photograph with no note about where the paper went is half a
 * record.
 *
 * **A document with no photograph saves.** Knowing the discharge summary exists
 * and lives in the blue folder is worth writing down before there is a picture
 * of it, so the picker is an option rather than a gate.
 *
 * **The size limit is stated before the person meets it**, per the schema
 * comment on the 25 MB cap, rather than after they have chosen something and
 * had it refused.
 */
@Composable
fun AddDocumentScreen(
    onSave: (DocumentDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember { mutableStateOf(DocumentDraft()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) draft = draft.copy(picked = uri) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(AddDocTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["docs.add"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["docs.add.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                val preview = rememberPickedPreview(draft.picked)
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        // The person just chose this image and the fields below
                        // name it, so describing the picture itself would make
                        // a reader hear the same thing twice.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(Radius.thumbnail),
                    )
                    Spacer(Modifier.height(Space.sm))
                }

                QuietButton(
                    label = if (draft.picked == null) {
                        strings["docs.pick"]
                    } else {
                        strings["docs.replace"]
                    },
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag(AddDocTags.PICK),
                )

                Spacer(Modifier.height(Space.xs))
                Text(
                    text = strings["docs.limit_note"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink3Text,
                )

                // Said only when it happened, and it says plainly that nothing
                // was saved, because the worst version of this is somebody
                // believing a document went in when it did not.
                if (error != null) {
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = error,
                        style = HealthTrail.type.bodyM,
                        color = colors.alertText,
                    )
                }

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["docs.title"],
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    hint = strings["docs.title.hint"],
                    fieldTestTag = AddDocTags.field("title"),
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["docs.original"],
                    value = draft.originalLocation,
                    onValueChange = { draft = draft.copy(originalLocation = it) },
                    hint = strings["docs.original.hint"],
                    fieldTestTag = AddDocTags.field("original"),
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["appts.notes"],
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    hint = strings["appts.notes.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AddDocTags.field("notes"),
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddDocTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddDocTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * The chosen image, decoded small enough to preview.
 *
 * **Decoded here rather than through an image loading library**, because
 * pulling in a whole dependency to show one preview is weight this app does not
 * need, and it would be the only such dependency in the project.
 *
 * Subsampled and decoded off the main thread, exactly as the documents list
 * does it. A file that will not decode returns null, and the screen simply
 * shows no preview rather than failing: the person can still save the document
 * with its title and its location.
 */
@Composable
private fun rememberPickedPreview(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri == null) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val bounds = android.graphics.BitmapFactory.Options()
                    .apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri).use {
                    android.graphics.BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                while (bounds.outWidth / sample > 1080) sample *= 2

                context.contentResolver.openInputStream(uri).use { stream ->
                    android.graphics.BitmapFactory.decodeStream(
                        stream,
                        null,
                        android.graphics.BitmapFactory.Options()
                            .apply { inSampleSize = sample },
                    )
                }?.asImageBitmap()
            }.getOrNull()
        }
    }

    return bitmap
}
