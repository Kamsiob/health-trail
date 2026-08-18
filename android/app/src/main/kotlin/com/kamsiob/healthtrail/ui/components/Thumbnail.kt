package com.kamsiob.healthtrail.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.raisedSlightly
import com.kamsiob.healthtrail.ui.theme.Radius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kamsiob.healthtrail.ui.v4.IconTile

/**
 * A picture of the person's own paper, per `DESIGN.md` section 11.7.
 *
 * **The app stores photographs of documents and bills and showed none of them.**
 * A documents screen carrying actual images of somebody's own letters is
 * transformed by one change, and it is real content rather than decoration.
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
 * **A thumbnail is never the only thing naming its item**, which is 5.12's rule
 * applied here: the caption always sits beside it, so this is decorative for a
 * reader and marked so.
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
    val colors = HealthTrail.colors
    var bitmap by remember(sha256, targetPixels) { mutableStateOf<ImageBitmap?>(null) }
    // Null until the read finishes, so the loading state is the `sand` field
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

    Box(
        modifier = modifier
            // A gallery cell sets its own width and asks for a square; every
            // other use names a size.
            .then(if (size == FILL) Modifier.aspectRatio(1f) else Modifier.size(size))
            // 4.7's small variant rather than the card treatment: a
            // thumbnail lifts without joining the cards' conversation.
            .raisedSlightly(Radius.thumbnail)
            .clip(Radius.thumbnail)
            .background(colors.sand)
            // Decorative: the caption always names the item beside it, and a
            // reader announcing "image" on every cell of a gallery is noise
            // rather than access.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // **Loading is the `sand` field alone, with no spinner.** Twelve
            // spinners is noise where twelve quiet squares is a grid still
            // filling in.
            !settled -> Unit
            // **Not an image, or unreadable.** The kind drawing rather than a
            // broken image glyph, which keeps the whole grid in one idiom.
            else -> IconTile(
                section = section,
                tint = colors.ink3,
                background = Color.Transparent,
                tileSize = size / 2,
                iconSize = size / 3,
            )
        }
    }
}

/** In a dense row, per 11.7. */
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
