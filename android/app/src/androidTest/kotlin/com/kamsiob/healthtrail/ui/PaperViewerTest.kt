package com.kamsiob.healthtrail.ui

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.DocumentScreen
import com.kamsiob.healthtrail.ui.screens.OneDocTags
import com.kamsiob.healthtrail.ui.screens.PaperViewerScreen
import com.kamsiob.healthtrail.ui.screens.PaperViewerTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The paper can be reached and read. #378.
 *
 * The owner photographed a document, and what the app gave back was a 512
 * pixel decode stretched across the screen that answered no tap. He read it
 * as the app having thrown his picture away. **The stored file was fine the
 * whole time.** These tests hold the two halves of the repair: the document
 * screen's photograph opens something, and the something is the viewer.
 */
@RunWith(AndroidJUnit4::class)
class PaperViewerTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings get() = Strings.load(context)

    private fun store(): Pair<Attachments, String> {
        val root = File(context.cacheDir, "paper-viewer-test").apply { mkdirs() }
        val attachments = Attachments.openAt(root)
        val bitmap = Bitmap.createBitmap(128, 96, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val sha = runBlocking { attachments.put(bytes.toByteArray()) }
        return attachments to sha
    }

    @Test
    fun theDocumentsPhotographOpensThePaper() {
        // **The tap that answered nothing.** The photograph is the one thing
        // on the document screen, and it was inert.
        val (_, sha) = store()
        var opened: String? = null
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    DocumentScreen(
                        document = Repository.Document(
                            id = "d1",
                            title = "Discharge summary",
                            category = null,
                            originalLocation = "Blue folder",
                            notes = null,
                            receivedEdtf = "2026-08-01",
                            sha256 = sha,
                            byteSize = 1024L,
                        ),
                        onEdit = {},
                        onRemove = {},
                        onOpenChapter = {},
                        onBack = {},
                        onOpenPaper = { opened = it },
                    )
                }
            }
        }

        compose.onNodeWithTag(OneDocTags.IMAGE, useUnmergedTree = true).performClick()

        assertEquals("tapping the photograph did not open the paper", sha, opened)
    }

    @Test
    fun theViewerShowsThePaperAndTheWayBack() {
        val (attachments, sha) = store()
        var back = 0
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    PaperViewerScreen(
                        sha256 = sha,
                        title = "Discharge summary",
                        attachments = attachments,
                        onBack = { back += 1 },
                    )
                }
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(PaperViewerTags.IMAGE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // **The paper announces its own name**, not "image".
        compose.onNodeWithContentDescription(Bidi.isolate("Discharge summary"))
            .assertIsDisplayed()

        compose.onNodeWithTag(PaperViewerTags.BACK).performClick()
        assertEquals("the way back did not fire", 1, back)
    }

    @Test
    fun aPinchSurvivesWithoutThePaperEscaping() {
        // **The clamp, exercised rather than trusted.** A pinch out then a
        // hard fling must leave the image on screen: the gesture math is the
        // one part of the viewer a compile cannot vouch for. The assertion is
        // that the node still exists and is displayed after abuse, which
        // fails if the transform threw or the image left the composition.
        val (attachments, sha) = store()
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    PaperViewerScreen(
                        sha256 = sha,
                        title = "Discharge summary",
                        attachments = attachments,
                        onBack = {},
                    )
                }
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(PaperViewerTags.IMAGE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val image = compose.onNodeWithTag(PaperViewerTags.IMAGE, useUnmergedTree = true)
        image.performTouchInput {
            pinch(
                start0 = center - Offset(50f, 0f),
                end0 = center - Offset(300f, 0f),
                start1 = center + Offset(50f, 0f),
                end1 = center + Offset(300f, 0f),
            )
        }
        image.performTouchInput {
            down(center)
            moveBy(Offset(5_000f, 5_000f))
            up()
        }

        image.assertIsDisplayed()
    }
}
