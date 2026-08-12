package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The phone remembers that somebody has been through "Before you start". #307, D146.
 *
 * **Restore replaces the notebook and the acceptance went with it**, so putting
 * your own backup on a phone handed you the first run screen the next time the
 * process started, which on this app is as ordinary as changing the font scale.
 *
 * **This cannot make the disclaimer skippable on a phone that has never seen
 * it**, which is the half worth testing: the flag lives in a preferences file
 * that is created with the app and cleared with its data.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeSeenTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun forget() {
        WelcomeSeen(context).forgetForTest()
    }

    @After
    fun forgetAgain() {
        WelcomeSeen(context).forgetForTest()
    }

    @Test
    fun aPhoneThatHasNeverSeenTheWelcomeSaysSo() {
        assertFalse(WelcomeSeen(context).seen())
    }

    @Test
    fun onceItIsRememberedItStaysRememberedAcrossInstances() {
        WelcomeSeen(context).remember()
        // A second instance, which is what the next process start builds.
        assertTrue(WelcomeSeen(context).seen())
    }

    /**
     * **The notebook's own record is untouched by this.** The timestamp stays
     * where it was, travels into the archive, and answers the other question:
     * when the person accepted, rather than whether this phone has asked.
     */
    @Test
    fun theNotebookKeepsItsOwnRecord() = kotlinx.coroutines.runBlocking {
        Repository.closeForTest()
        val repository = Repository.open(context)
        repository.putSettingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED, 1_723_000_000_000L)

        WelcomeSeen(context).remember()

        assertTrue(
            repository.settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED) == 1_723_000_000_000L,
        )
    }
}
