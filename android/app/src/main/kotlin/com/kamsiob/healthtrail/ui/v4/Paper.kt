package com.kamsiob.healthtrail.ui.v4

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.raisedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The person's own paper, held up, written from scratch. #386.
 *
 * **The one white surface in the language, and `m3v4-5` is why.** Every other
 * container in this interface is a flat tonal block on the canvas, `docs/V4.md`
 * 2.1, and the drawing breaks that exactly once: the paper sits on a white card
 * with a soft shadow under it, because the thing being shown is a sheet of
 * paper and a sheet of paper is white and casts a shadow. The corner measured
 * 22dp against the tonal blocks' 18 in the same drawing, so the card is the
 * larger corner in this app's pair as well: [Radius.cardLarge] against the
 * blocks' own. D183.
 *
 * **It shows the paper's own shape, and it does not crop it square.** The
 * gallery cell crops, because a grid of forty different rectangles is not a
 * grid; a screen whose whole subject is one document does not, because the top
 * of a letter is where the letterhead and the date are. A sheet taller than
 * [TALLEST] is shown from its top edge with the rest below the fold, and the
 * action under it opens the whole thing.
 *
 * **A document with no photograph is a real document**, rule 13: it says so in
 * a sentence on a quiet block rather than drawing an empty frame, because
 * knowing the discharge summary exists and lives in the blue folder is worth
 * writing down before there is a picture of it.
 */
@Composable
fun PaperCard(
    /** The attachment's content hash, or null where the record has no photograph. */
    sha256: String?,
    attachments: Attachments?,
    /** What is said in place of the paper when there is no photograph of it. */
    absent: String,
    modifier: Modifier = Modifier,
    /** What a reader is told the picture is, since the picture is the content here. */
    imageLabel: String? = null,
    /** Opens the paper at reading size. Null leaves the picture inert. */
    onOpen: (() -> Unit)? = null,
    /** What sits under the paper: the way to full size, and nothing else. */
    action: (@Composable () -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    val paper = rememberPaper(sha256, attachments)

    if (paper == null && sha256 == null) {
        // **Absent, not broken.** A quiet block carrying the sentence, in the
        // same shape every other group on the screen wears.
        Block(modifier = modifier) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Symbol(symbol = Symbols.documents, contentDescription = null, tint = colors.ink3)
                // bidi-ok: the app's own sentence about a document with no
                // photograph yet, never anything somebody typed.
                Body(text = absent)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .raisedCard(Radius.cardLarge)
            .clip(Radius.cardLarge)
            .background(colors.card),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(paper?.ratio() ?: SQUARE)
                .background(colors.sand)
                // **Edge to edge inside the card.** The paper is the card's
                // whole face and takes the card's own corner; an inset frame
                // would put a second radius inside the first, which is the
                // thing `docs/V4.md` 2.1 calls two separations at once.
                .then(
                    if (onOpen == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = imageLabel,
                            onClick = onOpen,
                        )
                    },
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            paper?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    // **From the top edge.** Where a sheet is too tall to show
                    // whole, the half worth showing is the half with the
                    // letterhead, the title and the date on it.
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // The picture is announced by the tap label around it,
                        // once, rather than by the bitmap and the box both.
                        .clearAndSetSemantics { },
                )
            }
        }
        // **Under the paper rather than over it.** The drawing floats the
        // control on the sheet, which works on an illustration of a document
        // and covers the person's own words on a photograph of one. It also
        // has somewhere to grow when the font scale is 2.0.
        action?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Space.sm),
                horizontalArrangement = Arrangement.End,
            ) { it() }
        }
    }
}

/**
 * The paper, decoded for the width it is drawn at.
 *
 * **Two passes**: the first reads the header for the real dimensions, the
 * second decodes at the smallest power of two that still covers the card. A
 * phone photograph of a letter is several megabytes and this screen is the one
 * place in the app that draws one at full width.
 *
 * **The decode is here rather than borrowed from the old thumbnail**, per the
 * method in `docs/ACCEPTANCE.md`: this package descends from nothing. The old
 * component keeps its own copy until its last gallery caller goes, and goes
 * with it.
 */
@Composable
private fun rememberPaper(sha256: String?, attachments: Attachments?): ImageBitmap? {
    var bitmap by remember(sha256) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sha256, attachments) {
        if (sha256 == null || attachments == null) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = attachments.fileFor(sha256)
                if (!file.exists()) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                while (
                    bounds.outWidth / sample > PAPER_PIXELS ||
                    bounds.outHeight / sample > PAPER_PIXELS
                ) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

/**
 * The shape the card draws the sheet at: its own, down to the preview's floor.
 *
 * A landscape photograph keeps its width and stays short. A portrait one is
 * shown from the top down to [PREVIEW], because **this is a preview and not the
 * paper**: `m3v4-5` draws a card 331dp wide and 300dp tall holding a portrait
 * sheet, which is the top third of it, with the name and the facts below the
 * fold of nothing. A whole letter at this width is 468dp and takes half the
 * phone, which pushed where the paper is off the screen. Measured, D183: 300dp
 * of card, less the action row under it, is a preview at about 1.35.
 */
private fun ImageBitmap.ratio(): Float = (width.toFloat() / height.toFloat()).coerceAtLeast(PREVIEW)

/** Where the card stops growing with the sheet, and the rest waits for full size. */
private const val PREVIEW = 1.35f

/** What an unreadable attachment leaves behind: a square of quiet field. */
private const val SQUARE = 1f

/**
 * The decode ceiling in pixels.
 *
 * Full screen width at density 3 with headroom for a modest pinch out in the
 * viewer that opens from here; the viewer does its own, larger decode. #378:
 * this was 512 and the owner read a stretched thumbnail as the app having
 * thrown his photograph's resolution away.
 */
private const val PAPER_PIXELS = 1440
