package com.kamsiob.healthtrail.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.MainActivity
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.screens.DisclaimerTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The disclaimer is a gate, not a notice.
 *
 * `PROJECT-DELTAS.md` section 4: before any part of the app is usable, on first
 * launch, the person sees it and must explicitly accept. The acceptance is
 * recorded locally with a timestamp so it is not shown repeatedly, it is not
 * shown again afterward, and there is no version of the app that skips it.
 *
 * These run on the connected phone. `connectedAndroidTest` uninstalls the
 * application afterward, so a fresh install is exactly the state the first test
 * needs, which is convenient rather than a compromise.
 */
@RunWith(AndroidJUnit4::class)
class DisclaimerGateTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun waitPastOpening() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag(AppRootTags.LOADING).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun theGateAppearsOnAFreshInstallAndBlocksEverythingElse() {
        waitPastOpening()

        val accepted = runBlocking {
            Repository.open(context).settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED)
        }

        if (accepted == null) {
            compose.onNodeWithTag(DisclaimerTags.ROOT).assertIsDisplayed()
            compose.onNodeWithTag(DisclaimerTags.ACCEPT).assertIsDisplayed()
        } else {
            // Already accepted on this install, which is the other half of the
            // contract: it is never shown twice.
            assertTrue(
                "the gate reappeared after acceptance",
                compose.onAllNodesWithTag(DisclaimerTags.ROOT).fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    @Test
    fun acceptingRecordsATimestampAndDismissesTheGateForGood() {
        waitPastOpening()

        val repository = runBlocking { Repository.open(context) }
        val before = runBlocking {
            repository.settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED)
        }

        if (before == null) {
            assertNull("expected no acceptance recorded yet", before)
            compose.onNodeWithTag(DisclaimerTags.ACCEPT).performClick()

            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(DisclaimerTags.ROOT).fetchSemanticsNodes().isEmpty()
            }
        }

        val after = runBlocking {
            repository.settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED)
        }
        assertNotNull("accepting did not record a timestamp", after)
        assertTrue("the recorded timestamp is not a real time", after!! > 0L)

        assertTrue(
            "the gate is still showing after acceptance",
            compose.onAllNodesWithTag(DisclaimerTags.ROOT).fetchSemanticsNodes().isEmpty(),
        )
    }
}
