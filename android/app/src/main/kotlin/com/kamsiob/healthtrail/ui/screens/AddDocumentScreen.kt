package com.kamsiob.healthtrail.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.Disclosure
import com.kamsiob.healthtrail.ui.components.CARD_SIZE
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.ROW_SIZE
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.StageDots
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.raisedSlightly
import com.kamsiob.healthtrail.ui.theme.Space

object AddDocTags {
    const val ROOT = "add_doc_root"
    const val PICK = "add_doc_pick"
    const val SAVE = "add_doc_save"
    const val CANCEL = "add_doc_cancel"
    const val FOLDERS = "add_doc_folders"
    const val PICK_DATE = "add_doc_pick_date"
    const val STAGE_DOTS = "add_doc_stage_dots"
    const val NEXT = "add_doc_next"
    const val BACK = "add_doc_back"
    const val MORE = "add_doc_more"
    fun field(key: String) = "add_doc_$key"
}

/** How many questions the form asks before it runs out of them. */
private const val DOC_STAGES = 3

/**
 * The empty sheet's shape, which is a sheet of paper rather than a photograph.
 *
 * Portrait, because what somebody photographs is a letter, a bill, or a
 * discharge summary, and a landscape frame would read as a picture of a view.
 * **Squarer than a real sheet of paper on purpose**: at A4's ratio the size
 * limit underneath it fell off the bottom of a Pixel 8, and a limit stated
 * below the fold is a limit somebody meets by being refused.
 */
private const val PaperAspect = 0.84f

/**
 * The chosen photograph's shape when it is carried into a later question.
 *
 * A band rather than the sheet: on those screens it is context for what is
 * being typed, and a full sheet would push the question it belongs to below the
 * fold. **A ratio rather than a height**, so it stays a band on any width and
 * so this screen adds no measurement of its own. D142.
 */
private const val CarriedAspect = 2.6f

/** What the person chose and typed about a document. */
data class DocumentDraft(
    val title: String = "",
    val originalLocation: String = "",
    /**
     * When the paper is from, at whatever precision the person gave.
     *
     * **Null until they say**, and null saves as unknown rather than as today.
     * This form had no date at all and the shell stamped `LocalDate.now()` into
     * every document, so a letter from three weeks ago was recorded as arriving
     * on the day somebody photographed it, and nothing could correct it. #339,
     * rule 17, and `DESIGN.md` 9.2.
     */
    val received: Edtf.Date? = null,
    val notes: String = "",
    val picked: Uri? = null,
    /**
     * The person's own word for the pile this belongs in. Blank means none.
     *
     * **Their word rather than a fixed set.** The documents screen folds by
     * this, and a fixed vocabulary would be the app deciding what kinds of
     * paper exist in somebody's life. Rule 20. #221.
     */
    val category: String = "",
) {
    companion object {
        /**
         * Saved the same flat way [CaptureFormState] is, and for the same
         * reason: the bundle is the one place this app's data leaves the
         * encrypted database, so what goes into it stays short and legible.
         *
         * **#371 item 7: the stage survived process death and the draft did
         * not**, so somebody who was interrupted came back to question three
         * of an empty form, with two answered questions gone and the form
         * insisting they were on the last one.
         *
         * The picked image is deliberately not carried. It is a content uri
         * whose permission grant does not outlive the process, so restoring
         * the string would restore a thumbnail that cannot be read.
         */
        val Saver: androidx.compose.runtime.saveable.Saver<DocumentDraft, Any> =
            androidx.compose.runtime.saveable.listSaver(
                save = {
                    listOf(
                        it.title,
                        it.originalLocation,
                        it.received?.canonical ?: "",
                        it.notes,
                        it.category,
                    )
                },
                restore = {
                    DocumentDraft(
                        title = it[0] as String,
                        originalLocation = it[1] as String,
                        received = (it[2] as String).takeIf(String::isNotEmpty)
                            ?.let(Edtf::parse),
                        notes = it[3] as String,
                        category = it[4] as String,
                    )
                },
            )
    }
}

/**
 * Saving a document.
 *
 * **One question at a time, per law 3, and it was a stack of five fields until
 * 2026-08-12.** The owner's words on #361 were that the app looks like a data
 * entry app, and he named this form first. Every field had a label above it down
 * one scroll, which predates law 3 being enforced and is the shape the capture
 * form was rebuilt away from. It now asks three questions: the photograph, what
 * it is and when it is from, then where the paper original went, with the folder
 * and the note behind the same "Add more" the capture form uses.
 *
 * **Save is live from the first question**, so photographing a letter and
 * tapping save is still the whole interaction. Rule 13: partial is a finished
 * state, and none of the three stages has to be reached.
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
    /** The record being corrected, or null when this one is new. */
    existing: Repository.Document? = null,
    error: String? = null,
    /**
     * The folders this notebook already has, offered as suggestions.
     *
     * **Empty on a fresh notebook, and that is the honest resting state.** The
     * field is still there and still typeable; there is simply nothing to
     * suggest yet. Offering an invented starter set would be the app deciding
     * what kinds of paper a person's life contains.
     */
    folders: List<String> = emptyList(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by rememberSaveable(existing?.id, stateSaver = DocumentDraft.Saver) {
        mutableStateOf(
            DocumentDraft(
                // bidi-ok: the value inside a field being edited. Isolate marks here would become characters the person has to delete.
                title = existing?.title.orEmpty(),
                originalLocation = existing?.originalLocation.orEmpty(),
                received = existing?.receivedEdtf?.let { Edtf.parse(it) },
                notes = existing?.notes.orEmpty(),
                category = existing?.category.orEmpty(),
            ),
        )
    }

    var picking by remember { mutableStateOf(false) }

    /**
     * Which of the three questions is on screen.
     *
     * **Correcting a document is not staged, and that is deliberate.** Somebody
     * who opens a saved document to change it arrives knowing which line is
     * wrong, and walking them through three questions to reach it would be the
     * form asking them to start a conversation they came to end. A new document
     * is the conversation; a correction is one field. D147, and rule 23: among
     * the defensible answers, the one that is easiest for the person.
     */
    var stage by rememberSaveable(existing?.id) { mutableIntStateOf(0) }
    val staged = existing == null

    /** True when this group is on screen, which on a correction is all of them. */
    fun showing(which: Int) = !staged || stage == which

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
                FormHeader(
                    title = if (existing == null) strings["docs.add"] else strings["docs.edit.title"],
                    lead = strings["docs.add.lead"],
                    section = Repository.Section.DOCUMENTS,
                )

                // Said only when it happened, and it says plainly that nothing
                // was saved, because the worst version of this is somebody
                // believing a document went in when it did not. **Above the
                // stages rather than inside one**: save is live from the first
                // question, so the refusal has to be visible from wherever the
                // person was standing when they tapped it.
                if (error != null) {
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = error,
                        style = HealthTrail.type.bodyM,
                        color = colors.alertInk,
                    )
                }

                Spacer(Modifier.height(Space.l))

                val preview = rememberPickedPreview(draft.picked)

                // **The paper stays in front of the person while they describe
                // it**, on every question after the one that chose it. Rule 18:
                // carry the context forward rather than asking somebody to
                // remember which letter they photographed thirty seconds ago.
                // It is also what fills these two screens, which without it are
                // two short questions and a great deal of nothing.
                if (staged && stage > 0) {
                    preview?.let { image ->
                        Image(
                            bitmap = image,
                            // Decorative: the questions beside it are what name
                            // this paper, and a reader hearing "image" before
                            // every question would be told nothing twice.
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(CarriedAspect)
                                .raisedSlightly(Radius.thumbnail)
                                .clip(Radius.thumbnail),
                        )
                        Spacer(Modifier.height(Space.l))
                    }
                }

                if (showing(0)) {

                // **The paper is the screen, and it used to be a button on an
                // empty field.** #361, 2026-08-12, found by looking rather than
                // by reading: the first question was a title, one outlined
                // button, one small line, and then two thirds of the phone
                // doing nothing, which rule 11 rules out as plainly as it rules
                // out a placeholder.
                //
                // **Rule 22 already named the component**: a thumbnail is where
                // the app holds the person's own paper. This is that at the
                // size of the question, an empty sheet waiting for a
                // photograph, in the same `sand` field and at the same corner
                // as every other thumbnail in the app.
                //
                // **The sheet is the control**, so the obvious tap does the
                // obvious thing. It is one stop for a reader, named by what the
                // tap does rather than by what is drawn inside it.
                val pickLabel = if (draft.picked == null) {
                    strings["docs.pick"]
                } else {
                    strings["docs.replace"]
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(PaperAspect)
                        .raisedSlightly(Radius.thumbnail)
                        .clip(Radius.thumbnail)
                        .openableByTap(
                            label = pickLabel,
                            onTap = {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            resting = colors.sand,
                            shape = Radius.thumbnail,
                        )
                        .semantics { contentDescription = pickLabel }
                        .testTag(AddDocTags.PICK),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            // The sheet around it already says what the tap
                            // does, and the fields on the next question name
                            // the paper, so describing the picture too would
                            // make a reader hear the same thing twice.
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // The sheet is one stop and says the whole thing.
                            modifier = Modifier.clearAndSetSemantics { },
                        ) {
                            // **Sized from the thumbnail vocabulary rather
                            // than from two numbers typed here**, per D142: the
                            // mark on an empty sheet is the same mark the
                            // documents list draws when it has no picture, one
                            // step up.
                            IconTile(
                                section = Repository.Section.DOCUMENTS,
                                tint = colors.ink3,
                                background = Color.Transparent,
                                tileSize = CARD_SIZE,
                                iconSize = ROW_SIZE,
                            )
                            Spacer(Modifier.height(Space.s))
                            Text(
                                text = pickLabel,
                                style = HealthTrail.type.bodyL,
                                color = colors.blue,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["docs.limit_note"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )

                }

                if (showing(1)) {

                if (!staged) Spacer(Modifier.height(Space.sectionGap))

                HealthTrailTextField(
                    label = strings["docs.title"],
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    hint = strings["docs.title.hint"],
                    fieldTestTag = AddDocTags.field("title"),
                )
                Spacer(Modifier.height(Space.m))

                // **When the paper is from, and the person says or does not.**
                // This form had no date and the shell stamped today into every
                // document, so a letter from three weeks ago was recorded as
                // arriving on the day it was photographed and nothing could
                // correct it. #339.
                //
                // **Not sure is a real answer that saves**, per rule 17, and
                // leaving both chips alone saves as unknown rather than as
                // today: the app never quietly fills a date in.
                ChoiceChipGroup(label = strings["docs.received"]) {
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = draft.received != null &&
                            draft.received?.precision != Edtf.Precision.UNKNOWN,
                        onClick = { picking = true },
                        modifier = Modifier.testTag(AddDocTags.PICK_DATE),
                    )
                    ChoiceChip(
                        label = strings["date.pick.clear"],
                        selected = draft.received?.precision == Edtf.Precision.UNKNOWN,
                        onClick = { draft = draft.copy(received = Edtf.unknown()) },
                    )
                }

                // **Shown back at exactly the precision it was given**, which
                // is the whole point: "sometime in March" stays a month.
                draft.received?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }

                }

                if (showing(2)) {

                if (!staged) Spacer(Modifier.height(Space.sectionGap))

                // **The field this screen exists for**, per the schema's own
                // comment: the digital copy is rarely the one a clerk will
                // accept. It leads its stage rather than sitting behind the
                // disclosure with the folder and the notes.
                HealthTrailTextField(
                    label = strings["docs.original"],
                    value = draft.originalLocation,
                    onValueChange = { draft = draft.copy(originalLocation = it) },
                    hint = strings["docs.original.hint"],
                    fieldTestTag = AddDocTags.field("original"),
                )

                Spacer(Modifier.height(Space.sectionGap))

                // **The last two are behind one control nobody has to touch**,
                // per 10.8 and the disclosure in `Disclosure.kt`, which is what
                // the capture form's third stage already does. A folder and a
                // note are what somebody adds when they are sitting down, and
                // this form is used standing up with a letter in one hand.
                Disclosure(
                    testTag = AddDocTags.MORE,
                    // A correction shows what is already written rather than
                    // folding the person's own note behind "Add more".
                    startOpen = existing != null,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                // **The folder, which the screen has always folded by and no
                // form ever wrote.** Every document a person saved landed in
                // "Everything else", and the folds were visible only because
                // the fixture invented categories. #221.
                //
                // **A field with suggestions rather than a picker**, because
                // the folder is the person's own word for a pile of paper. The
                // chips are what this notebook already has, so the second
                // insurance letter goes where the first one went with one tap
                // and the first one was free to be called anything.
                //
                // **Tapping the chip that is already chosen clears it**, which
                // is how a document comes back out of a folder without the
                // person having to select the text and delete it.
                HealthTrailTextField(
                    label = strings["docs.folder"],
                    value = draft.category,
                    onValueChange = { draft = draft.copy(category = it) },
                    hint = strings["docs.folder.hint"],
                    fieldTestTag = AddDocTags.field("folder"),
                )
                if (folders.isNotEmpty()) {
                    Spacer(Modifier.height(Space.s))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                        modifier = Modifier.fillMaxWidth().testTag(AddDocTags.FOLDERS),
                    ) {
                        folders.forEach { folder ->
                            val chosen = draft.category.trim().equals(folder, ignoreCase = true)
                            ChoiceChip(
                                label = folder,
                                selected = chosen,
                                onClick = {
                                    draft = draft.copy(category = if (chosen) "" else folder)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.m))

                DictatableField(
                    label = strings["appts.notes"],
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    hint = strings["appts.notes.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AddDocTags.field("notes"),
                )

                    }
                }

                }

                Spacer(Modifier.height(Space.xl))
            }

            // The gap the pinned action footer requires, per DESIGN.md 5.15.
            Spacer(Modifier.height(Space.m))

            // **Where you are, and the way on, on one line**, exactly as the
            // capture form draws it. The dots say where somebody is and never
            // how much is left, per rule 13, and the way on is worded as
            // skipping while the question is untouched because none of these
            // is required.
            if (staged) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.screenHorizontal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StageDots(
                        count = DOC_STAGES,
                        current = stage,
                        description = strings(
                            "capture.stage",
                            "current" to stage + 1,
                            "total" to DOC_STAGES,
                        ),
                        modifier = Modifier.testTag(AddDocTags.STAGE_DOTS),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (stage > 0) {
                            TextAction(
                                label = strings["capture.back"],
                                onClick = { stage -= 1 },
                                modifier = Modifier.testTag(AddDocTags.BACK),
                            )
                            Spacer(Modifier.width(Space.m))
                        }
                        if (stage < DOC_STAGES - 1) {
                            val filled = when (stage) {
                                0 -> draft.picked != null
                                else -> draft.title.isNotBlank() || draft.received != null
                            }
                            TextAction(
                                label = strings[
                                    if (filled) "capture.next" else "capture.skip",
                                ],
                                onClick = { stage += 1 },
                                modifier = Modifier.testTag(AddDocTags.NEXT),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.s))
            }

            // **Live from the first question.** Somebody who photographs a
            // letter and taps save never sees the other two, and what they
            // have is saved from wherever they are standing.
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
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddDocTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = draft.received,
            onPick = {
                draft = draft.copy(received = it)
                picking = false
            },
            onDismiss = { picking = false },
        )
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
