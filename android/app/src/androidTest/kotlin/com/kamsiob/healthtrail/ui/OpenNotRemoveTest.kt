package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.PersonTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A card that opens something says "open", and offers no removal. #231.
 *
 * **`removableByLongPress` was the only modifier the app had for a tappable
 * card**, so every screen that needed a card to open something reached for it
 * and passed `onLongPress = {}` to switch the removal off. The gesture went
 * quiet and the words did not: a reader announced the **tap** as "remove" on a
 * card that opens an entry, and listed a **long press** called "remove" that
 * ran an empty function.
 *
 * **This asserts the click label rather than the drawing**, because nothing
 * about it is visible: a screenshot of the fixed screen and the broken one are
 * the same image, which is why it survived six screens and a design review.
 */
@RunWith(AndroidJUnit4::class)
class OpenNotRemoveTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val person = Repository.Person(
        id = "p1",
        displayName = "Denise Alvarado",
        roleLabel = "Intake caseworker",
        phone = null,
        email = null,
        notes = null,
    )

    private val entry = Repository.TrailEntry(
        id = "e1",
        kind = "call",
        title = null,
        body = "They have the application.",
        occurredEdtf = "2026-03-28",
        occurredStart = 1L,
        createdAt = 1L,
        isUnfiled = false,
        threads = emptyList(),
        pinnedAt = null,
    )

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    PersonScreen(
                        person = person,
                        entries = listOf(entry),
                        onCall = {},
                        onEdit = {},
                        onOpenEntry = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun theCardSaysTheTapOpensTheEntry() {
        show()
        val node = compose.onNodeWithTag(PersonTags.entry(entry.id)).fetchSemanticsNode()
        val label = node.config.getOrNull(SemanticsActions.OnClick)?.label
        assertEquals(strings["prep.change.open"], label)
    }

    /**
     * And it announces no removal at all.
     *
     * The long press was the half that ran an empty function, so a reader
     * offered an action that did nothing. `openableByTap` declares one action
     * and it is the one that happens.
     */
    @Test
    fun theCardOffersNoLongPressThatDoesNothing() {
        show()
        val node = compose.onNodeWithTag(PersonTags.entry(entry.id)).fetchSemanticsNode()
        assertNull(
            "the card still declares a long press, which used to do nothing",
            node.config.getOrNull(SemanticsActions.OnLongClick),
        )
    }
}
