package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.TodayFieldScreen
import com.kamsiob.healthtrail.ui.screens.TodayFieldTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import androidx.compose.ui.unit.LayoutDirection
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Today, as the person arranged it. `DESIGN.md` 21, and issues #292 and #270.
 *
 * **This surface shipped with no test of any kind**, which is how it came to be
 * reachable on every real notebook without a way to search from it. The three
 * promises below are the ones the design makes in writing and the ones a
 * refactor is most likely to break quietly.
 *
 * 1. **The lead is exactly one thing, and it is not a card.** 21.1.
 * 2. **Search keeps its place regardless of layout.** 21.1.
 * 3. **Nothing moves because the data moved.** 21.8, and the trust audit in
 *    `DESIGN.md` 23.2. This is the whole trust model of the surface.
 */
@RunWith(AndroidJUnit4::class)
class TodayFieldScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val today = LocalDate.of(2026, 4, 4)

    private fun card(
        id: String,
        type: String,
        size: String = "small",
        isLead: Boolean = false,
    ) = Repository.TodayCard(
        id = id,
        type = type,
        size = size,
        sortIndex = 0,
        isLead = isLead,
        sourceTable = null,
        sourceId = null,
    )

    /** The default starting hand, which is what most people will ever see. */
    private fun startingHand() = Repository.TodayLayout(
        lead = card("c-digest", "digest", size = "wide", isLead = true),
        field = listOf(
            card("c-next", "next_up"),
            card("c-meds", "medications"),
            card("c-ask", "ask_next_time"),
            card("c-emerg", "emergency_card"),
        ),
    )

    /**
     * The answers the screen is looking at, so a test can change the record
     * underneath a composed screen rather than composing a second one.
     *
     * `setContent` may be called once per rule, and the trust audit is
     * precisely a claim about one screen watching its data change.
     */
    private val liveAnswers = mutableStateOf<Map<String, Repository.TodayAnswer>>(emptyMap())

    private fun show(
        layout: Repository.TodayLayout = startingHand(),
        answers: Map<String, Repository.TodayAnswer> = emptyMap(),
        digest: Digest.Summary = Digest.nothing,
        onSearch: () -> Unit = {},
        onOpen: (Repository.TodayCard) -> Unit = {},
        locale: Locale = Locale.ENGLISH,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        liveAnswers.value = answers
        val strings = Strings.load(context, locale)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalLayoutDirection provides direction,
                ) {
                    TodayFieldScreen(
                        layout = layout,
                        answers = liveAnswers.value,
                        digest = digest,
                        onOpen = onOpen,
                        onSearch = onSearch,
                        today = today,
                    )
                }
            }
        }
    }

    /** The card ids on the field, top to bottom, as the screen actually laid them out. */
    private fun fieldOrder(layout: Repository.TodayLayout): List<String> =
        layout.field
            .map { it.id to compose.onNodeWithTag(TodayFieldTags.card(it.id)) }
            .sortedBy { (_, node) -> node.fetchSemanticsNode().positionInRoot.y }
            .map { (id, _) -> id }

    @Test
    fun thereIsExactlyOneLeadAndNeverTwo() {
        // 21.1. Never zero and never two, and the singularity is by
        // construction: the layout type has nowhere to put the absence and the
        // database refuses the second one.
        show()
        assertEquals(
            1,
            compose.onAllNodesWithTag(TodayFieldTags.LEAD).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun theLeadIsNotAlsoACardOnTheField() {
        // **The defect this replaced.** The lead was rendered as a wide card,
        // which put the most important thing on the screen at exactly the
        // weight of the four things under it. Rule 15 calls that what it is.
        val layout = startingHand()
        show(layout)
        assertTrue(
            "the lead is wearing the card costume",
            compose.onAllNodesWithTag(TodayFieldTags.card(layout.lead.id))
                .fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithTag(TodayFieldTags.LEAD).assertIsDisplayed()
    }

    @Test
    fun theDigestLeadSaysWhatDayItIs() {
        // The eyebrow is the day rather than the word Today, which is already
        // on the tab chip and on the active navigation tab. Composed through
        // the catalog's own pattern, so the shape of the date is part of the
        // translation.
        val strings = Strings.load(context)
        show()
        compose
            .onNodeWithText(
                EventDateText.dayHeading(strings, today).uppercase(strings.locale),
            )
            .assertIsDisplayed()
    }

    @Test
    fun searchIsOnTheSurfaceAndOpens() {
        // **It was missing entirely.** MASTER_SPEC 4.8 puts universal search at
        // the top of Today, and when this surface replaced the previous one it
        // arrived without a way into search at all, on every seeded notebook.
        var opened = false
        show(onSearch = { opened = true })
        compose.onNodeWithTag(TodayFieldTags.SEARCH).assertIsDisplayed()
        compose.onNodeWithTag(TodayFieldTags.SEARCH).performClick()
        assertTrue("the search door does not open search", opened)
    }

    @Test
    fun searchKeepsItsPlaceWhicheverCardLeads() {
        // 21.1: finding and recording are the two acts that must never move. A
        // door that moves with the cards is a door somebody has to find first.
        val promoted = Repository.TodayLayout(
            lead = card("c-meds", "medications", size = "wide", isLead = true),
            field = listOf(
                card("c-digest", "digest", size = "wide"),
                card("c-next", "next_up"),
            ),
        )
        show(promoted)

        val leadBottom = compose.onNodeWithTag(TodayFieldTags.LEAD)
            .fetchSemanticsNode().boundsInRoot.bottom
        val searchTop = compose.onNodeWithTag(TodayFieldTags.SEARCH)
            .fetchSemanticsNode().boundsInRoot.top
        val firstCardTop = compose.onNodeWithTag(TodayFieldTags.card("c-digest"))
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue("search is not under the lead", searchTop >= leadBottom)
        assertTrue("search is not above the field", searchTop < firstCardTop)
    }

    @Test
    fun theLayoutDoesNotMoveWhenTheDataUnderItMoves() {
        // **The trust audit, `DESIGN.md` 23.2, and 21.8.** The difference
        // between a quiet day and the morning of an appointment is data inside
        // a layout the person owns. Nothing is promoted, nothing is injected,
        // and nothing sorts itself by urgency or recency.
        val layout = startingHand()

        val quiet = mapOf(
            "c-next" to Repository.TodayAnswer(),
            "c-meds" to Repository.TodayAnswer(),
            "c-ask" to Repository.TodayAnswer(),
            "c-emerg" to Repository.TodayAnswer(),
        )
        show(layout, answers = quiet)
        val quietOrder = fieldOrder(layout)
        assertEquals(layout.field.map { it.id }, quietOrder)

        // The busiest day the record can produce, including the rung that most
        // looks like something the app should act on. Swapped underneath the
        // same composed screen, which is what the audit actually claims.
        compose.runOnIdle {
            liveAnswers.value = mapOf(
                "c-next" to Repository.TodayAnswer(title = "Dr. Okafor", detail = "Today 10:15"),
                "c-meds" to Repository.TodayAnswer(count = 11),
                "c-ask" to Repository.TodayAnswer(count = 7),
                "c-emerg" to Repository.TodayAnswer(sourceClosed = true),
            )
        }
        compose.waitForIdle()
        assertEquals(
            "the field reordered itself because the data changed",
            quietOrder,
            fieldOrder(layout),
        )
        assertEquals(
            1,
            compose.onAllNodesWithTag(TodayFieldTags.LEAD).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aQuietLeadIsStillTheLead() {
        // 21.4's "none yet" rung, at the top of the screen. Quiet is allowed to
        // be good news and the answer to the question the person opened the app
        // to ask is still the answer when it is a calm one.
        val strings = Strings.load(context)
        show(digest = Digest.nothing)
        compose.onNodeWithTag(TodayFieldTags.LEAD).assertIsDisplayed()
        compose.onNodeWithText(strings["today.card.digest.quiet"]).assertIsDisplayed()
    }

    @Test
    fun everyCardIsADoorIncludingTheLead() {
        // 21.2, and rule 18. A card is a door to where its answer lives, and
        // the lead is a card that happens to be at the top.
        var opened: Repository.TodayCard? = null
        val layout = startingHand()
        show(layout, onOpen = { opened = it })

        compose.onNodeWithTag(TodayFieldTags.LEAD).performClick()
        assertEquals(layout.lead.id, opened?.id)

        compose.onNodeWithTag(TodayFieldTags.card("c-meds")).performClick()
        assertEquals("c-meds", opened?.id)
    }

    @Test
    fun itHoldsUpInTheLongestLanguage() {
        // Spanish is the longest of the four that ship in v1.
        show(locale = Locale("es"))
        compose.onNodeWithTag(TodayFieldTags.LEAD).assertIsDisplayed()
        compose.onNodeWithTag(TodayFieldTags.SEARCH).assertIsDisplayed()
    }

    @Test
    fun itHoldsUpRightToLeft() {
        // Arabic mirrors, so the lead's chevron and the search door's magnifier
        // both turn around and the field lays out from the other edge. Held
        // here rather than left to a device sweep, because almost none of this
        // family of defect is visible in English at font scale 1.0.
        show(locale = Locale("ar"), direction = LayoutDirection.Rtl)
        compose.onNodeWithTag(TodayFieldTags.LEAD).assertIsDisplayed()
        compose.onNodeWithTag(TodayFieldTags.SEARCH).assertIsDisplayed()
        compose.onNodeWithTag(TodayFieldTags.card("c-meds")).assertIsDisplayed()
    }
}
