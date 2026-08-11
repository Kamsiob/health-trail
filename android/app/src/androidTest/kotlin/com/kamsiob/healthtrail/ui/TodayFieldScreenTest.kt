package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.CardOptionsTags
import com.kamsiob.healthtrail.ui.screens.TodayFieldScreen
import com.kamsiob.healthtrail.ui.screens.TodayFieldTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import androidx.compose.ui.unit.Density
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
        sourceTable: String? = null,
        sourceId: String? = null,
    ) = Repository.TodayCard(
        id = id,
        type = type,
        size = size,
        sortIndex = 0,
        isLead = isLead,
        sourceTable = sourceTable,
        sourceId = sourceId,
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
        onDial: (String) -> Unit = {},
        people: List<Repository.Person> = emptyList(),
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
                        onDial = onDial,
                        people = people,
                        today = today,
                    )
                }
            }
        }
    }

    /** Somebody on the care team, for the card that points at one person. */
    private fun person(
        id: String,
        name: String,
        role: String? = null,
        phone: String? = null,
    ) = Repository.Person(
        id = id,
        displayName = name,
        roleLabel = role,
        phone = phone,
        email = null,
        notes = null,
    )

    /** The card ids on the field, top to bottom, as the screen actually laid them out. */
    private fun fieldOrder(layout: Repository.TodayLayout): List<String> =
        layout.field
            .map { it.id to compose.onNodeWithTag(TodayFieldTags.card(it.id)) }
            .sortedBy { (_, node) -> node.fetchSemanticsNode().positionInRoot.y }
            .map { (id, _) -> id }

    /**
     * What a card says, as a reader hears it, with the isolate marks taken out.
     *
     * **A Today card cannot be tested through its rendered text and does not say
     * so.** `TodayCard` and `TodayLead` both `clearAndSetSemantics`, on purpose:
     * 21.2 wants a card to be one stop for a reader rather than four, so every
     * descendant is dropped from the semantics tree and `onNodeWithText` finds
     * nothing inside one. A test written against the text fails looking exactly
     * like the defect it was meant to catch, which is what fourteen of these did
     * the first time they ran on a phone. `HANDOFF.md` section 7 names the trap
     * and `RoadStrip` hit it first.
     *
     * **So the assertion is on the sentence the card composes**, which is the
     * thing that has to say what the screen says, per section 9. That it also
     * *looks* right is what the device pass is for, and a test cannot do it.
     *
     * `Bidi.join` wraps every part in isolate characters, so a raw substring
     * never matches. They are stripped here rather than being written into every
     * expectation.
     */
    private fun spoken(tag: String): String =
        compose.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString(" ")
            .orEmpty()
            .filterNot { it == '\u2068' || it == '\u2069' }

    /** The same, for the card whose id is given. */
    private fun spokenByCard(id: String): String = spoken(TodayFieldTags.card(id))

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
        assertTrue(
            "the lead does not name the day: ${spoken(TodayFieldTags.LEAD)}",
            EventDateText.dayHeading(strings, today) in spoken(TodayFieldTags.LEAD),
        )
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
    fun theNextDatedThingShowsWhenItIs() {
        // **The defect this test exists for.** `whenEdtf` was read from the
        // record for Next up, Milestones and a measure, carried through
        // `TodayAnswer`, and rendered by nothing, so the card whose whole
        // question is "what is the next dated thing" showed a name and no date.
        val strings = Strings.load(context)
        show(
            answers = mapOf(
                "c-next" to Repository.TodayAnswer(
                    title = "Dr. Okafor",
                    whenEdtf = "2026-04-09",
                ),
            ),
        )
        assertTrue(
            "the card does not say when: ${spokenByCard("c-next")}",
            EventDateText.render(strings, "2026-04-09") in spokenByCard("c-next"),
        )
    }

    @Test
    fun aCoarseDateOnACardIsNotMadePrecise() {
        // Rule 17 and 9.2, on this surface too. "Sometime in April 2026" is
        // honest and "April 1, 2026" for the same stored value is a
        // fabrication, and a card is not exempt from that because it is small.
        val strings = Strings.load(context)
        show(
            answers = mapOf(
                "c-next" to Repository.TodayAnswer(title = "Dr. Okafor", whenEdtf = "2026-04"),
            ),
        )
        val rendered = EventDateText.render(strings, "2026-04")
        assertTrue(
            "the card does not say when: ${spokenByCard("c-next")}",
            rendered in spokenByCard("c-next"),
        )
        assertTrue(
            "a month precise date rendered as a day: $rendered",
            rendered == strings("date.month", "date" to "April 2026"),
        )
    }

    @Test
    fun aCountCarriesTheWordForWhatItCounted() {
        // 21.3: every size carries one answer and one line of context, and a
        // card reading "6" alone makes the person supply the noun themselves.
        val strings = Strings.load(context)
        show(answers = mapOf("c-meds" to Repository.TodayAnswer(count = 6)))
        val said = spokenByCard("c-meds")
        assertTrue("the card does not say 6: $said", "6" in said)
        assertTrue(
            "the count has no noun: $said",
            strings["today.card.medications.count"] in said,
        )
    }

    @Test
    fun growingTheMedicationsCardRevealsTheListAndSaysWhatIsNotOnIt() {
        // The example 21.3 is written around: at small a count, at wide the
        // list. And the count stays the true total, so a card showing three of
        // eleven says so rather than cropping and letting the person believe
        // they are looking at all of it.
        val strings = Strings.load(context)
        val wide = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-meds", "medications", size = "wide")),
        )
        show(
            wide,
            answers = mapOf(
                "c-meds" to Repository.TodayAnswer(
                    count = 11,
                    items = listOf(
                        Repository.TodayItem("Metformin", "500 mg twice a day"),
                        Repository.TodayItem("Lisinopril", "10 mg in the morning"),
                        Repository.TodayItem("Atorvastatin"),
                    ),
                ),
            ),
        )
        val said = spokenByCard("c-meds")
        assertTrue("the list is not there: $said", "Metformin · 500 mg twice a day" in said)
        assertTrue("a dose-less medication is missing: $said", "Atorvastatin" in said)
        assertTrue(
            "the card crops eight medications without saying so: $said",
            strings("today.card.more", "count" to 8) in said,
        )
    }

    @Test
    fun noStoredDateReachesTheScreenAsTheStringItIsStoredAs() {
        // **Rule 17 and 9.2: the person never sees EDTF.** This nearly shipped:
        // the trail card's list put `occurred_edtf` straight into the line, so
        // the front screen of the app would have shown somebody "2026-04" as
        // though that were a date anybody writes.
        val strings = Strings.load(context)
        val wide = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-trail", "trail_lately", size = "wide")),
        )
        show(
            wide,
            answers = mapOf(
                "c-trail" to Repository.TodayAnswer(
                    count = 2,
                    title = "Called the ward",
                    whenEdtf = "2026-04-09",
                    items = listOf(
                        Repository.TodayItem("Called the ward", noteEdtf = "2026-04-09"),
                        Repository.TodayItem("Letter arrived", noteEdtf = "2026-04"),
                    ),
                ),
            ),
        )
        val said = spokenByCard("c-trail")
        for (raw in listOf("2026-04-09", "2026-04")) {
            assertTrue(
                "the stored form $raw reaches the person, which rule 17 rules out: $said",
                raw !in said,
            )
        }
        assertTrue(
            "the item's date is not rendered: $said",
            "Letter arrived · " + EventDateText.render(strings, "2026-04") in said,
        )
    }

    @Test
    fun aMeasureCardAnswersWithTheValueAndNamesItOnTheTab() {
        // **The card was upside down.** 21.7 asks "what is the latest value",
        // and it put the measure's name at display size with the reading in the
        // quiet line under it. The name belongs on the tab, which is where 21.2
        // puts identity and what lets two measure cards be told apart.
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-weight", "measure")),
        )
        show(
            layout,
            answers = mapOf(
                "c-weight" to Repository.TodayAnswer(
                    sourceName = "Weight",
                    title = "148.5 lb",
                    whenEdtf = "2026-04-02",
                ),
            ),
        )
        val said = spokenByCard("c-weight")
        assertTrue("the tab does not name the measure: $said", "Weight" in said)
        assertTrue("the value is not the answer: $said", "148.5 lb" in said)
    }

    @Test
    fun aProjectDateCountsDownAndSaysPassedInPlainWords() {
        // 21.7 and 21.4. A date that has gone by reads "passed 6 days ago", in
        // plain words, with no urgency color and no alarm.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-due", "project_date"), card("c-gone", "project_date")),
        )
        show(
            layout,
            answers = mapOf(
                // today is April 4th.
                "c-due" to Repository.TodayAnswer(
                    sourceName = "Waiver",
                    title = "Window closes",
                    whenEdtf = "2026-04-22",
                ),
                "c-gone" to Repository.TodayAnswer(
                    sourceName = "Waiver",
                    title = "Form was due",
                    whenEdtf = "2026-03-29",
                ),
            ),
        )
        assertTrue(
            "no countdown: ${spokenByCard("c-due")}",
            strings("project.countdown.days", "count" to 18) in spokenByCard("c-due"),
        )
        assertTrue(
            "a date that has gone by does not say so: ${spokenByCard("c-gone")}",
            strings("project.countdown.passed", "count" to 6) in spokenByCard("c-gone"),
        )
    }

    @Test
    fun aCoarseProjectDateIsNeverTurnedIntoANumberOfDays() {
        // Rule 17. "Sometime in April" is not a number of days away, and
        // counting from a month the person gave loosely would invent exactly
        // the precision the date model exists to protect.
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-due", "project_date")),
        )
        show(
            layout,
            answers = mapOf(
                "c-due" to Repository.TodayAnswer(
                    sourceName = "Waiver",
                    title = "Window closes",
                    whenEdtf = "2026-06",
                ),
            ),
        )
        val said = spokenByCard("c-due")
        assertTrue("the kind is not shown: $said", "Window closes" in said)
        assertTrue("a month precise date was counted down to days: $said", "days" !in said)
    }

    @Test
    fun aProjectCardLeadsWithWhoItIsWaitingOn() {
        // 21.7: whose hands, since when. The person waiting on the county wants
        // the county, not the word Waiting.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-proj", "project_standing", size = "wide")),
        )
        show(
            layout,
            answers = mapOf(
                "c-proj" to Repository.TodayAnswer(
                    sourceName = "Waiver",
                    title = "The county office",
                    detail = "waiting",
                ),
            ),
        )
        val said = spokenByCard("c-proj")
        assertTrue("who it waits on is not the answer: $said", "The county office" in said)
        assertTrue(
            "the status is not worded: $said",
            strings["projects.status.waiting"] in said,
        )
        assertTrue(
            "the stored status value reaches the person: $said",
            " waiting " !in " $said ",
        )
    }

    @Test
    fun aMeasureDrawsItsShapeAtTallAndNowhereSmaller() {
        // 21.7 asks the measure card for the latest value **and its recent
        // shape**, and 21.3 puts a chart at tall only: a line across half a
        // screen width is a decoration rather than a shape anybody can read.
        val strings = Strings.load(context)
        val series = (0..5).map {
            Repository.Reading(
                id = "r$it",
                measureId = "m1",
                number = 148.0 + it,
                text = null,
                unit = "lb",
                occurredEdtf = "2026-03-0${it + 1}",
                occurredStart = 1772000000000L + it * 86_400_000L,
                note = null,
                source = null,
            )
        }
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(
                card("c-tall", "measure", size = "tall"),
                card("c-small", "measure", size = "small"),
            ),
        )
        show(
            layout,
            answers = mapOf(
                "c-tall" to Repository.TodayAnswer(
                    sourceName = "Weight",
                    title = "153 lb",
                    series = series,
                ),
                "c-small" to Repository.TodayAnswer(
                    sourceName = "Weight",
                    title = "153 lb",
                    series = series,
                ),
            ),
        )

        // The reader hears the chart as one sentence, never as coordinates.
        fun spoken(id: String) = compose.onNodeWithTag(TodayFieldTags.card(id))
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString(" ")
            .orEmpty()

        val readings = strings("progress.readings", "count" to series.size)
        assertTrue(
            "the tall card does not say how many readings its chart draws",
            readings in spoken("c-tall"),
        )
        assertTrue(
            "the small card announces a chart it does not draw",
            readings !in spoken("c-small"),
        )

        // The chart itself takes height the small card does not have.
        val tall = compose.onNodeWithTag(TodayFieldTags.card("c-tall"))
            .fetchSemanticsNode().size.height
        val small = compose.onNodeWithTag(TodayFieldTags.card("c-small"))
            .fetchSemanticsNode().size.height
        assertTrue("the tall card is not taller than the small one", tall > small)
    }

    @Test
    fun standingInstructionsSaysWhetherAnythingWasNotFollowed() {
        // 21.7 asks two things: how many are active, **and are any issues
        // noted**. The second half is the part a family actually needs in a
        // room, and the card never answered it.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-si", "standing_instructions", size = "wide")),
        )
        show(
            layout,
            answers = mapOf(
                "c-si" to Repository.TodayAnswer(
                    count = 4,
                    detailKey = "instruction.violations.count",
                    detailCount = 3,
                ),
            ),
        )
        assertTrue(
            "issues noted are not said: ${spokenByCard("c-si")}",
            strings("instruction.violations.count", "count" to 3) in spokenByCard("c-si"),
        )
    }

    @Test
    fun noIssuesNotedIsSaidByNotSayingIt() {
        // **Zero is not a line.** "Not followed 0 times" introduces a worry to
        // somebody who does not have one, and 21.4 lets quiet be good news
        // without announcing itself. Rule 2 as well: the app counts, and a
        // count of nothing is not a finding.
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-si", "standing_instructions", size = "wide")),
        )
        show(
            layout,
            answers = mapOf("c-si" to Repository.TodayAnswer(count = 4)),
        )
        assertTrue(
            "the card announces that nothing has gone wrong, which nobody asked: " +
                spokenByCard("c-si"),
            "followed" !in spokenByCard("c-si"),
        )
    }

    @Test
    fun theNextDatedThingSaysTodayAndTomorrowInWords() {
        // The grid draws "Tomorrow 10:15" and the card rendered "April 5, 2026
        // at 10:15 AM", which is correct and makes the person do arithmetic in
        // a kitchen. Anything further out keeps its full date.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(
                card("c-soon", "next_up", size = "wide"),
                card("c-later", "next_up", size = "wide"),
            ),
        )
        show(
            layout,
            answers = mapOf(
                // today is April 4th.
                "c-soon" to Repository.TodayAnswer(
                    title = "Dr. Okafor",
                    whenEdtf = "2026-04-05T10:15",
                ),
                "c-later" to Repository.TodayAnswer(
                    title = "Home nurse",
                    whenEdtf = "2026-05-20",
                ),
            ),
        )
        assertTrue(
            "tomorrow is not said in words: ${spokenByCard("c-soon")}",
            strings["date.tomorrow"] in spokenByCard("c-soon"),
        )
        assertTrue(
            "a date further out lost its date: ${spokenByCard("c-later")}",
            EventDateText.render(strings, "2026-05-20") in spokenByCard("c-later"),
        )
    }

    @Test
    fun aCoarseDateIsNeverCalledTodayOrTomorrow() {
        // Rule 17. A month is not a day, so it can be neither, and saying so
        // would be the fabrication the date model exists to prevent.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-vague", "next_up", size = "wide")),
        )
        show(
            layout,
            answers = mapOf(
                "c-vague" to Repository.TodayAnswer(
                    title = "The review meeting",
                    whenEdtf = "2026-04",
                ),
            ),
        )
        val said = spokenByCard("c-vague")
        for (word in listOf(strings["date.today"], strings["date.tomorrow"])) {
            assertTrue("a month precise date was called $word: $said", word !in said)
        }
        assertTrue(
            "the month is not rendered: $said",
            EventDateText.render(strings, "2026-04") in said,
        )
    }

    @Test
    fun aBrandNewNotebookIsNotToldWhatChangedSinceAVisitItNeverHad() {
        // **The first screen anybody sees after onboarding.** It read "Nothing
        // new since you were last here" on a notebook made thirty seconds ago.
        // The previous Today showed no digest at all on a first run and was
        // right to; this surface cannot, because 21.1 says the lead is never
        // empty, so it says what is true on day one instead.
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    TodayFieldScreen(
                        layout = startingHand(),
                        answers = emptyMap(),
                        onOpen = {},
                        today = today,
                        hasAnything = false,
                    )
                }
            }
        }
        val said = spoken(TodayFieldTags.LEAD)
        assertTrue(
            "a new notebook is told about a visit that never happened: $said",
            strings["today.card.digest.quiet"] !in said,
        )
        assertTrue(
            "the first run says nothing of its own: $said",
            strings["today.card.digest.first"] in said,
        )
        // **And it does not turn into a task list.** Rule 13: an unfilled slot
        // reads as not yet, never as something the person has failed to do.
        for (banned in listOf("should", "need to", "must", "finish", "complete")) {
            assertTrue(
                "the first run nags: $banned is in \"$said\"",
                banned !in said.lowercase(),
            )
        }
    }

    @Test
    fun twoAppointmentsOnOneDayBecomeTwoLinesAndNextWeekDoesNot() {
        // 21.7. A card listing next week as well has stopped answering "what is
        // the next dated thing" and started being an agenda, which the app
        // already has a screen for.
        val strings = Strings.load(context)
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-next", "next_up", size = "wide")),
        )
        show(
            layout,
            answers = mapOf(
                "c-next" to Repository.TodayAnswer(
                    title = "Dr. Okafor",
                    whenEdtf = "2026-04-09T10:15",
                    items = listOf(
                        Repository.TodayItem("Blood draw", noteEdtf = "2026-04-09T14:00"),
                        Repository.TodayItem("Care plan meeting", noteEdtf = "2026-04-16"),
                    ),
                ),
            ),
        )
        val said = spokenByCard("c-next")
        assertTrue("the second thing that day is missing: $said", "Blood draw" in said)
        assertTrue("a later day is being listed: $said", "Care plan meeting" !in said)
    }

    @Test
    fun aCoarseDateSharesItsDayWithNothing() {
        // Rule 17. "Sometime in April" is not on any particular day, so it
        // cannot be the same day as anything, and saying it is would invent the
        // precision the date model exists to protect.
        val layout = Repository.TodayLayout(
            lead = card("c-digest", "digest", size = "wide", isLead = true),
            field = listOf(card("c-next", "next_up", size = "wide")),
        )
        show(
            layout,
            answers = mapOf(
                "c-next" to Repository.TodayAnswer(
                    title = "The review meeting",
                    whenEdtf = "2026-04",
                    items = listOf(Repository.TodayItem("Blood draw", noteEdtf = "2026-04")),
                ),
            ),
        )
        assertTrue(
            "two month precise dates were treated as the same day",
            "Blood draw" !in spokenByCard("c-next"),
        )
    }

    @Test
    fun theFieldReflowsToOneColumnAtLargeTypeAndKeepsItsOrder() {
        // 21.6 screen 8. **Designed, not endured**: at scale 2.0 a half width
        // cell is a card barely wider than the words in it, so the field goes
        // to one column and every card renders full width. **Layout order is
        // preserved exactly**, which is the part a reflow is most likely to
        // get wrong and the part the person would notice.
        val layout = startingHand()
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 2.0f,
                    ),
                ) {
                    TodayFieldScreen(
                        layout = layout,
                        answers = emptyMap(),
                        onOpen = {},
                        today = today,
                    )
                }
            }
        }

        assertEquals(
            "the field did not keep its order when it reflowed",
            layout.field.map { it.id },
            fieldOrder(layout),
        )

        // One column means every card starts at the same edge.
        val lefts = layout.field.map {
            compose.onNodeWithTag(TodayFieldTags.card(it.id))
                .fetchSemanticsNode().boundsInRoot.left
        }
        assertEquals(
            "cards are still sharing a row at the largest type size: $lefts",
            1,
            lefts.distinct().size,
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
        assertTrue(
            "a quiet day does not say so: ${spoken(TodayFieldTags.LEAD)}",
            strings["today.card.digest.quiet"] in spoken(TodayFieldTags.LEAD),
        )
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

    // -- edit mode, #271 -----------------------------------------------------

    @Test
    fun draggingACardMovesItAndDoneSavesWhereItLanded() {
        // 21.6 screen 5 gives edit mode a drag handle, and 23.2 keeps Move up
        // and Move down beside it: the drag is the shortcut and the named
        // controls are the path. **Nothing saves until Done**, here as
        // everywhere else on this surface.
        var saved: List<Repository.TodayCard>? = null
        val layout = startingHand()
        liveAnswers.value = emptyMap()
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides Strings.load(context)) {
                    TodayFieldScreen(
                        layout = layout,
                        answers = liveAnswers.value,
                        onOpen = {},
                        onSave = { saved = it },
                        today = today,
                    )
                }
            }
        }

        compose.onNodeWithTag(TodayFieldTags.EDIT).performClick()

        val before = fieldOrder(layout)
        val first = compose.onNodeWithTag(TodayFieldTags.card(before.first()))
            .fetchSemanticsNode().size.height.toFloat()

        compose.onNodeWithTag(TodayFieldTags.drag(before.first()), useUnmergedTree = true)
            .performTouchInput {
                down(center)
                // Past the slop, then far enough to clear the card below it.
                // One move would be a fling; the steps are what a finger does.
                moveBy(Offset(0f, first * 0.4f))
                moveBy(Offset(0f, first * 0.4f))
                moveBy(Offset(0f, first * 0.4f))
                up()
            }

        assertTrue(
            "the dragged card did not move: ${fieldOrder(layout)}",
            fieldOrder(layout).first() != before.first(),
        )
        assertEquals("nothing should have been saved before Done", null, saved)

        compose.onNodeWithTag(TodayFieldTags.DONE).performClick()
        assertTrue(
            "Done did not save where the card landed",
            saved?.map { it.id }?.drop(1)?.first() != before.first(),
        )
        assertEquals(
            "a card was lost or duplicated by the drag",
            layout.all.map { it.id }.sorted(),
            saved?.map { it.id }?.sorted(),
        )
    }

    @Test
    fun everyCardInEditModeCarriesAGripAndOpensItsOptions() {
        // Grid screen 05: in edit mode a card carries a remove dot and a drag
        // handle, and everything else is in its options, screen 07. Inline it
        // was three chips and four actions on every card, which on a twenty
        // card Today is the wall rule 15 describes.
        val layout = startingHand()
        show(layout)
        compose.onNodeWithTag(TodayFieldTags.EDIT).performClick()

        for (card in layout.field) {
            compose.onNodeWithTag(TodayFieldTags.drag(card.id), useUnmergedTree = true)
                .assertIsDisplayed()
        }
        // The card is the door to its options, so the size chips are not on it.
        assertTrue(
            "a size chip is still on the card's face",
            compose.onAllNodesWithText(Strings.load(context)["today.edit.size.small"])
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aCardsOptionsHoldTheNamedReorderPath() {
        // 23.2. Move up and Move down work one handed, with a reader on, and
        // with switch access; the drag works with none of those, so the named
        // path is the mechanism and the drag is the shortcut.
        val layout = startingHand()
        show(layout)
        compose.onNodeWithTag(TodayFieldTags.EDIT).performClick()
        compose.onNodeWithTag(TodayFieldTags.card("c-meds")).performClick()

        compose.onNodeWithTag(CardOptionsTags.SHEET).assertIsDisplayed()
        compose.onNodeWithTag(CardOptionsTags.UP).assertIsDisplayed()
        compose.onNodeWithTag(CardOptionsTags.DOWN).assertIsDisplayed()
        compose.onNodeWithTag(CardOptionsTags.LEAD).assertIsDisplayed()
        compose.onNodeWithTag(CardOptionsTags.REMOVE).assertIsDisplayed()
        compose.onNodeWithTag(CardOptionsTags.size("wide")).assertIsDisplayed()
    }

    // -- the trail card's mini spine, #259 ----------------------------------
    //
    // `DESIGN.md` 21.7 asks this card for a three-entry mini spine at tall,
    // gap markers included, and 5.2.4 is why the markers are the point: two
    // calls a week apart read as a week of calls, and the same two rows four
    // months apart read as somebody left alone until something happened. A list
    // shows the same rows either way.
    //
    // **The marker is here rather than only on the phone because no seed
    // produces it.** It needs a fortnight of silence at the head of the trail,
    // and every fixture ends its history on the day it is generated, so the
    // newest entries are always days apart. Said out loud on the issue too.

    /** A day, as the instant the record stores for it. */
    private fun at(year: Int, month: Int, day: Int): Long =
        java.time.LocalDate.of(year, month, day)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** One entry on the card, newest first, as the query hands them over. */
    private fun entry(
        label: String,
        edtf: String,
        kind: String = "call",
        start: Long? = null,
    ) = Repository.TodayItem(
        label = label,
        noteEdtf = edtf,
        kind = kind,
        noteStart = start,
    )

    private fun trailCard(size: String = "tall") = Repository.TodayLayout(
        lead = card("c-digest", "digest", size = "wide", isLead = true),
        field = listOf(card("c-trail", "trail_lately", size = size)),
    )

    @Test
    fun theTrailCardDrawsItsLastFewAsASpineAndSaysEachOnce() {
        // The spine's first waypoint is the newest entry, which is the same
        // thing the smaller sizes say at display scale, so the title is not
        // repeated over the top of it. This card has had that defect once.
        show(
            layout = trailCard(),
            answers = mapOf(
                "c-trail" to Repository.TodayAnswer(
                    title = "Care plan meeting",
                    whenEdtf = "2026-06-29",
                    items = listOf(
                        entry("Care plan meeting", "2026-06-29", start = at(2026, 6, 29)),
                        entry("Called about the bill", "2026-06-28", start = at(2026, 6, 28)),
                        entry("Visited in the afternoon", "2026-06-27", "visit", at(2026, 6, 27)),
                    ),
                ),
            ),
        )

        val said = spokenByCard("c-trail")
        assertEquals(
            "the newest entry is said twice: $said",
            1,
            Regex(Regex.escape("Care plan meeting")).findAll(said).count(),
        )
        assertTrue("the second entry is missing: $said", "Called about the bill" in said)
        assertTrue("the third entry is missing: $said", "Visited in the afternoon" in said)
    }

    @Test
    fun theSpineSaysHowLongThePauseWas() {
        // 5.2.4, and the reason this card is drawn rather than listed. Three
        // weeks of silence is the difference between a week of calls and
        // somebody left alone, and the rows look identical without it.
        val strings = Strings.load(context)
        show(
            layout = trailCard(),
            answers = mapOf(
                "c-trail" to Repository.TodayAnswer(
                    title = "Called the ward",
                    whenEdtf = "2026-06-29",
                    items = listOf(
                        entry("Called the ward", "2026-06-29", start = at(2026, 6, 29)),
                        entry("Nothing since the review", "2026-06-08", start = at(2026, 6, 8)),
                    ),
                ),
            ),
        )
        assertTrue(
            "the spine says nothing about the three weeks between them: ${spokenByCard("c-trail")}",
            strings("trail.gap.weeks", "count" to 3) in spokenByCard("c-trail"),
        )
    }

    @Test
    fun aCoarselyGivenDateNeverProducesAGapMarker() {
        // Rule 17. The distance between "sometime in April" and a day in June
        // is not a number anybody gave, and a confident "two months earlier"
        // derived from it is exactly the invented precision the date model
        // exists to prevent.
        val strings = Strings.load(context)
        show(
            layout = trailCard(),
            answers = mapOf(
                "c-trail" to Repository.TodayAnswer(
                    title = "Called the ward",
                    whenEdtf = "2026-06-29",
                    items = listOf(
                        entry("Called the ward", "2026-06-29", start = at(2026, 6, 29)),
                        entry("Sometime in the spring", "2026-04", start = at(2026, 4, 1)),
                    ),
                ),
            ),
        )
        val said = spokenByCard("c-trail")
        assertTrue(
            "a coarse date was measured against a precise one: $said",
            listOf("trail.gap.days", "trail.gap.weeks", "trail.gap.months", "trail.gap.years")
                .none { key -> (1..12).any { strings(key, "count" to it) in said } },
        )
    }

    @Test
    fun atWideTheTrailCardListsFromTheSecondEntry() {
        // The newest entry is the answer at display scale there, and listing it
        // under itself made the card say "Care plan meeting" twice. The drop is
        // the renderer's, because at tall all three are waypoints and a query
        // that had skipped one would leave the spine without its head.
        show(
            layout = trailCard(size = "wide"),
            answers = mapOf(
                "c-trail" to Repository.TodayAnswer(
                    title = "Care plan meeting",
                    whenEdtf = "2026-06-29",
                    items = listOf(
                        entry("Care plan meeting", "2026-06-29", start = at(2026, 6, 29)),
                        entry("Called about the bill", "2026-06-28", start = at(2026, 6, 28)),
                    ),
                ),
            ),
        )
        val said = spokenByCard("c-trail")
        assertEquals(
            "the newest entry is said twice at wide: $said",
            1,
            Regex(Regex.escape("Care plan meeting")).findAll(said).count(),
        )
        assertTrue("the older entry is missing at wide: $said", "Called about the bill" in said)
    }

    // -- the care team card, #258 -------------------------------------------
    //
    // `DESIGN.md` 21.7 draws this card two ways and the difference between them
    // is whether it points at a person. Its states ladder is here rather than
    // only on the phone because two of its rungs, the sparse one and the one
    // where nobody recorded a number, are not reachable from any seed: the
    // fixture's people include every office contact a project ever involved, so
    // its count is never small. `report_today_rungs.py` says so out loud.

    /** A layout holding one care team card, so each rung composes on its own. */
    private fun careTeam(
        size: String = "wide",
        sourceId: String? = null,
    ) = Repository.TodayLayout(
        lead = card("c-digest", "digest", size = "wide", isLead = true),
        field = listOf(
            card(
                "c-team",
                "care_team",
                size = size,
                sourceTable = sourceId?.let { "person" },
                sourceId = sourceId,
            ),
        ),
    )

    @Test
    fun aCareTeamCardPointingAtOnePersonOffersTheirNumber() {
        // 21.7: "how do I reach them, now". 21.3 allows one inline outlined
        // action at wide, "a verb or a dialable number", and this is it.
        var dialed: String? = null
        show(
            layout = careTeam(sourceId = "p-1"),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    title = "Angela Reyes",
                    detail = "Charge nurse, day shift",
                    phone = "555 0142",
                ),
            ),
            onDial = { dialed = it },
        )

        compose.onNodeWithTag(TodayFieldTags.dial("c-team")).assertIsDisplayed()
        compose.onNodeWithTag(TodayFieldTags.dial("c-team")).performClick()
        assertEquals(
            "the pill did not hand the number over exactly as it was recorded",
            "555 0142",
            dialed,
        )
    }

    @Test
    fun theCardIsOneStopAndTheNumberIsTheSecond() {
        // 21.2: a card that announced its tab, its number, its line and its
        // chevron as four stops would make somebody listen to four things to
        // learn one. 21.3 gives wide two touch targets, so a reader gets
        // exactly two: the card, and the number.
        //
        // **Measured through the merged tree, which is what a reader walks.**
        // The device dump shows the unmerged one and cannot answer this: it
        // prints every text inside a card whether or not anybody can reach it.
        show(
            layout = careTeam(sourceId = "p-1"),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    title = "Angela Reyes",
                    detail = "Charge nurse, day shift",
                    phone = "555 0142",
                ),
            ),
        )

        val card = compose.onNodeWithTag(TodayFieldTags.card("c-team"))
        card.assertHasClickAction()
        assertTrue(
            "the card's own sentence is not on the card's own node",
            card.fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
                .isNotEmpty(),
        )
        assertEquals(
            "a reader has more than the number to step through inside the card",
            1,
            card.fetchSemanticsNode().children.size,
        )
        assertEquals(
            "the one stop inside the card is not the number",
            listOf("Call 555 0142"),
            card.fetchSemanticsNode().children.single()
                .config.getOrNull(SemanticsProperties.ContentDescription),
        )
    }

    @Test
    fun theNumberIsNotOfferedAtSmall() {
        // 21.3's touch-target budget: small carries one target and it is the
        // whole card. A second one there is the rule broken, not a bonus.
        show(
            layout = careTeam(size = "small", sourceId = "p-1"),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    title = "Angela Reyes",
                    detail = "Charge nurse, day shift",
                    phone = "555 0142",
                ),
            ),
        )
        assertTrue(
            "a small card is offering a second touch target",
            compose.onAllNodesWithTag(TodayFieldTags.dial("c-team"))
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aPersonWithNoNumberSaysSoRatherThanShowingAnEmptyPill() {
        // Rule 13: an unfilled slot reads as not yet, never as an error. The
        // card is still a door to the person, which is where a number goes.
        val strings = Strings.load(context)
        show(
            layout = careTeam(sourceId = "p-2"),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    title = "Tonya K.",
                    detail = "Aide, evenings",
                ),
            ),
        )
        assertTrue(
            "a card with no number is showing a pill anyway",
            compose.onAllNodesWithTag(TodayFieldTags.dial("c-team"))
                .fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "the card does not say there is no number: ${spokenByCard("c-team")}",
            strings["careteam.no_phone"] in spokenByCard("c-team"),
        )
    }

    @Test
    fun aCardPointingAtSomebodyWhoLeftSaysSoAndKeepsWorkingAsADoor() {
        // 21.4's source-closed rung. Archiving is the person saying somebody is
        // no longer involved, and the card is removed only by their own hand.
        // **No number is offered**, because suggesting a call they have already
        // decided against is the app having a view.
        val strings = Strings.load(context)
        var opened: Repository.TodayCard? = null
        show(
            layout = careTeam(sourceId = "p-3"),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    title = "Nadine Cross",
                    sourceClosed = true,
                ),
            ),
            onOpen = { opened = it },
        )

        assertTrue(
            "the closed card does not say so: ${spokenByCard("c-team")}",
            strings["today.card.source_closed"] in spokenByCard("c-team"),
        )
        assertTrue(
            "somebody who left is still being offered for a call",
            compose.onAllNodesWithTag(TodayFieldTags.dial("c-team"))
                .fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithTag(TodayFieldTags.card("c-team")).performClick()
        assertEquals("c-team", opened?.id)
    }

    @Test
    fun theRowOfEveryoneSaysHowManyItIsNotDrawing() {
        // The full rung of the other variant. Three faces out of nine is three
        // faces and a sentence, never a quiet crop: cropping would be the app
        // deciding which three of somebody's people matter.
        val strings = Strings.load(context)
        show(
            layout = careTeam(),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    count = 9,
                    items = listOf(
                        Repository.TodayItem(label = "Angela Reyes", note = "Charge nurse"),
                        Repository.TodayItem(label = "Marcus Bell", note = "Social worker"),
                        Repository.TodayItem(label = "Dr. Priya Raman", note = "Attending"),
                    ),
                ),
            ),
        )
        assertTrue(
            "the card does not say who is not drawn: ${spokenByCard("c-team")}",
            strings("today.card.more", "count" to 6) in spokenByCard("c-team"),
        )
        assertTrue(
            "the names behind the faces are not read aloud: ${spokenByCard("c-team")}",
            "Angela Reyes" in spokenByCard("c-team"),
        )
    }

    @Test
    fun aSparseRosterDrawsEverybodyAndClaimsNoMore() {
        // 21.4's "few". Two people is two faces and no overflow, and the card
        // never says there are more than the record holds.
        val strings = Strings.load(context)
        show(
            layout = careTeam(),
            answers = mapOf(
                "c-team" to Repository.TodayAnswer(
                    count = 2,
                    items = listOf(
                        Repository.TodayItem(label = "Angela Reyes", note = "Charge nurse"),
                        Repository.TodayItem(label = "Marcus Bell", note = "Social worker"),
                    ),
                ),
            ),
        )
        assertTrue(
            "a sparse roster claims people it does not have: ${spokenByCard("c-team")}",
            (1..9).none { strings("today.card.more", "count" to it) in spokenByCard("c-team") },
        )
        assertTrue(
            "the card is not answering at all: ${spokenByCard("c-team")}",
            "Marcus Bell" in spokenByCard("c-team"),
        )
    }

    @Test
    fun anEmptyRosterIsCalmAndNeverAnError() {
        // 21.4's "none yet". Quiet is allowed to be good news, and this rung is
        // the whole of a notebook nobody has written a name into yet.
        val strings = Strings.load(context)
        show(
            layout = careTeam(),
            answers = mapOf("c-team" to Repository.TodayAnswer(count = 0)),
        )
        assertTrue(
            "the empty card does not name its own nothing: ${spokenByCard("c-team")}",
            strings["today.card.care_team.none"] in spokenByCard("c-team"),
        )
    }

    @Test
    fun choosingWhoTheCardShowsRewordsItStraightAway() {
        // 21.6 screen 7, and the reason the pick is not left to Done: a control
        // that appears to do nothing reads as broken. Nothing is saved until
        // Done all the same, which is what `onSave` not being called says.
        var saved: List<Repository.TodayCard>? = null
        val layout = careTeam()
        liveAnswers.value = mapOf("c-team" to Repository.TodayAnswer(count = 2))
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides Strings.load(context)) {
                    TodayFieldScreen(
                        layout = layout,
                        answers = liveAnswers.value,
                        onOpen = {},
                        onSave = { saved = it },
                        today = today,
                        people = listOf(
                            person("p-1", "Angela Reyes", "Charge nurse", "555 0142"),
                            person("p-2", "Marcus Bell", "Social worker", "555 0187"),
                        ),
                    )
                }
            }
        }

        compose.onNodeWithTag(TodayFieldTags.EDIT).performClick()
        // Through the card's own options, per grid screen 07, which is where
        // the source picker lives with everything else about the card.
        compose.onNodeWithTag(TodayFieldTags.card("c-team")).performClick()
        compose.onNodeWithTag(CardOptionsTags.WHO).performClick()
        // **Substring, because the name arrives wrapped.** A person's own name
        // renders inside isolate marks so that a Latin name in an Arabic list
        // keeps its own direction, which means no exact match ever finds it.
        compose.onNodeWithText("Marcus Bell", substring = true).performClick()

        assertTrue(
            "the card did not take the pick: ${spokenByCard("c-team")}",
            "Marcus Bell" in spokenByCard("c-team"),
        )
        assertEquals("nothing should have been saved before Done", null, saved)

        compose.onNodeWithTag(TodayFieldTags.DONE).performClick()
        assertEquals(
            "the saved card does not point at the person who was picked",
            "p-2",
            saved?.firstOrNull { it.id == "c-team" }?.sourceId,
        )
        assertTrue(
            "the source table was not written with the source",
            saved?.firstOrNull { it.id == "c-team" }?.sourceTable == "person",
        )
    }
}
