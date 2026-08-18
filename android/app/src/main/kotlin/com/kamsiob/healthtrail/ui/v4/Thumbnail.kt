package com.kamsiob.healthtrail.ui.v4

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A picture of the person's own paper, in a row or a grid cell. #387, D196.
 *
 * **The app stores photographs of documents and bills and showed none of them.**
 * A documents screen carrying actual images of somebody's own letters is
 * transformed by one change, and it is real content rather than decoration.
 *
 * **Written fresh on Material's `Surface`, the old file deleted.** What it
 * replaced was a `Box` with a `clip`, a `background`, a hand rolled drop shadow
 * that branched on the theme, and a corner named from a second radius ladder.
 * `Surface` is that container: it owns the shape, the color and the clipping,
 * and `MaterialTheme.shapes.small` is the same 16dp corner the old token was,
 * now stated once for the whole app.
 *
 * **Flat, and that is the change worth naming.** The old thumbnail lifted off
 * the page on a shadow. `docs/V4.md` 2.1 puts every container in this interface
 * on a flat tonal block and spends the one shadow in the language on the paper
 * held up at reading size, [PaperCard]. A wall of forty lifted cells was two
 * separations doing one job.
 *
 * **It decodes at the size it will be drawn**, using `inSampleSize`, because a
 * gallery of forty phone photographs decoded at full resolution is forty
 * multi-megabyte bitmaps for a grid of 112dp cells, and the person's notebook
 * is the one place this app must not run out of memory.
 *
 * **Nothing is cached outside the app's own storage** and nothing leaves the
 * device. This is a local-first app and its thumbnails are held to the same
 * rule as its data.
 *
 * **A thumbnail is never the only thing naming its item**, so it is decorative
 * for a reader and marked so: the caption always sits beside it.
 */
@Composable
fun Thumbnail(
    /** The attachment's content hash, or null where the record has no photograph. */
    sha256: String?,
    attachments: Attachments?,
    /** The drawing shown when there is no image to show. */
    section: Repository.Section,
    modifier: Modifier = Modifier,
    size: Dp = ROW_SIZE,
    /**
     * The decode ceiling in pixels. The default fits grid cells; a screen
     * presenting the paper itself at full width passes a higher one, because
     * 512 pixels stretched across the screen is what the owner read as the
     * app having dropped his photograph's resolution. #378.
     */
    targetPixels: Int = TARGET_PIXELS,
) {
    val scheme = MaterialTheme.colorScheme
    var bitmap by remember(sha256, targetPixels) { mutableStateOf<ImageBitmap?>(null) }
    // Null until the read finishes, so the loading state is the empty field
    // rather than a flash of the fallback drawing.
    var settled by remember(sha256) { mutableStateOf(sha256 == null) }

    LaunchedEffect(sha256, attachments, targetPixels) {
        if (sha256 == null || attachments == null) {
            settled = true
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = attachments.fileFor(sha256)
                if (!file.exists()) return@runCatching null
                // Two passes: the first only reads the header for the real
                // dimensions, the second decodes at the smallest power of two
                // that still covers the cell.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val target = targetPixels
                var sample = 1
                while (
                    bounds.outWidth / sample > target || bounds.outHeight / sample > target
                ) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
        settled = true
    }

    Surface(
        modifier = modifier
            // A gallery cell sets its own width and asks for a square; every
            // other use names a size.
            .then(if (size == FILL) Modifier.aspectRatio(1f) else Modifier.size(size))
            // Decorative: the caption always names the item beside it, and a
            // reader announcing "image" on every cell of a gallery is noise
            // rather than access.
            .clearAndSetSemantics { },
        shape = MaterialTheme.shapes.small,
        // **The highest container step, not the canvas.** This scheme's low
        // step *is* the page in light, so a cell drawn on it would be invisible
        // until its photograph arrived. `docs/TRAPS.md`.
        color = scheme.surfaceContainerHighest,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val image = bitmap
            when {
                image != null -> Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // **Loading is the empty field alone, with no spinner.** Twelve
                // spinners is noise where twelve quiet squares is a grid still
                // filling in.
                !settled -> Unit
                // **Not an image, or unreadable.** The kind drawing rather than
                // a broken image glyph, which keeps the whole grid in one
                // idiom, and it sits on the cell's own ground rather than
                // putting a second surface inside the first.
                else -> IconTile(
                    section = section,
                    tint = scheme.outline,
                    background = Color.Transparent,
                    tileSize = size / 2,
                    iconSize = size / 3,
                )
            }
        }
    }
}

/** In a dense row. */
val ROW_SIZE = 40.dp

/** Inside a card. */
val CARD_SIZE = 64.dp

/** Fills its cell and stays square, which is what a gallery gives it. */
val FILL = 0.dp

/**
 * How many pixels a thumbnail is decoded to on its longest edge.
 *
 * The gallery cell is about a third of a 360dp screen at density 3, which is
 * roughly 360 pixels. Decoding to 512 covers that and every smaller use with
 * one number, and halving again would show on the largest cell.
 */
private const val TARGET_PIXELS = 512
