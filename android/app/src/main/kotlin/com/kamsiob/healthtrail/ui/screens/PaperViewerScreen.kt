package com.kamsiob.healthtrail.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaperViewerTags {
    const val ROOT = "paper_viewer"
    const val IMAGE = "paper_viewer_image"
    const val BACK = "paper_viewer_back"
}

/**
 * The person's own paper, at reading size. #378.
 *
 * **The stored file was always full resolution and no screen ever showed it.**
 * The document screen drew its photograph through the thumbnail component,
 * which decodes at 512 pixels for grid cells, so a full width letter rendered
 * soft and nothing responded to a tap. The owner read that as the app having
 * thrown away his picture. It had not, and the difference between those two
 * facts is this screen.
 *
 * **Decoded from the original at up to [READING_EDGE] on the long edge**,
 * sampled by powers of two like every decode in the app. That is a hard
 * ceiling on memory (one bitmap around 26 MB at the ceiling) while being four
 * to five times the linear resolution of the old rendering: the smallest line
 * on a dense page is readable at pinch.
 *
 * **Pinch, drag, and double tap, because a photo viewer is the one interface
 * every phone owner already knows.** Zoom runs 1x to [MAX_ZOOM]; pan is
 * clamped so the paper can not be flung off screen; double tap toggles
 * between fit and [DOUBLE_TAP_ZOOM]. No animation is involved anywhere, so
 * reduced motion has nothing to reduce: the paper tracks the fingers.
 *
 * **Black behind the paper**, not the theme's own dark: paper photographed in
 * a kitchen has every color in it, and a neutral true black is the one
 * background that flatters none of them and reads as "viewer" in both themes.
 *
 * **The way back is D137's bar**, floated over the bottom edge. A viewer with
 * a bespoke close affordance would be the only screen in the app with one.
 */
@Composable
fun PaperViewerScreen(
    sha256: String,
    /** What the paper is called, for the reader; the image is the content. */
    title: String,
    attachments: Attachments?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var bitmap by remember(sha256) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(sha256) { mutableStateOf(false) }

    LaunchedEffect(sha256, attachments) {
        if (attachments == null) {
            failed = true
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
                    bounds.outWidth / sample > READING_EDGE ||
                    bounds.outHeight / sample > READING_EDGE
                ) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
        failed = bitmap == null
    }

    var zoom by remember(sha256) { mutableStateOf(1f) }
    var offset by remember(sha256) { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(Offset.Zero) }

    // The pan limit: at zoom z, the image overflows the box by (z - 1) of the
    // box on each axis, and half of that is available in each direction. The
    // image fits the box at zoom 1, so the clamp collapses to zero and the
    // paper centers itself.
    fun clamp(candidate: Offset, atZoom: Float): Offset {
        val maxX = (box.x * (atZoom - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (box.y * (atZoom - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(PaperViewerTags.ROOT),
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image,
                // **The title is the announcement**, because "image" tells a
                // reader nothing and the paper's name is why they opened it.
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { box = Offset(it.width.toFloat(), it.height.toFloat()) }
                    .semantics { contentDescription = Bidi.isolate(title) }
                    .testTag(PaperViewerTags.IMAGE)
                    .pointerInput(sha256) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
                            // Zoom toward the fingers rather than the center:
                            // the point under the centroid stays under it.
                            val center = box / 2f
                            val focus = centroid - center - offset
                            val grown = focus * (newZoom / zoom) - focus
                            zoom = newZoom
                            offset = clamp(offset + pan - grown, newZoom)
                        }
                    }
                    .pointerInput(sha256) {
                        detectTapGestures(
                            onDoubleTap = { tap ->
                                if (zoom > 1f) {
                                    zoom = 1f
                                    offset = Offset.Zero
                                } else {
                                    val center = box / 2f
                                    zoom = DOUBLE_TAP_ZOOM
                                    offset = clamp((center - tap) * (DOUBLE_TAP_ZOOM - 1f), zoom)
                                }
                            },
                        )
                    }
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }

        if (failed) {
            // The one honest failure: the record names a file the store does
            // not hold, after a restore from a partial archive. Never blank.
            Text(
                text = strings["document.nophoto"],
                style = HealthTrail.type.bodyM,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        TextAction(
            label = strings["paper.viewer.back"],
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.s)
                .testTag(PaperViewerTags.BACK),
        )
    }
}

/**
 * The long edge ceiling for the reading decode.
 *
 * 2560 holds one bitmap near 26 MB worst case, which one screen may spend and
 * a grid may not, and it is five times the linear resolution the document
 * screen used to hand this photograph.
 */
private const val READING_EDGE = 2560

private const val MAX_ZOOM = 6f
private const val DOUBLE_TAP_ZOOM = 2.5f
