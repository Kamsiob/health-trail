package com.kamsiob.healthtrail.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.RoadStrip
import com.kamsiob.healthtrail.ui.components.stageNamesLine
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The road, and the one line it falls back to. `DESIGN.md` 20.6.
 *
 * **The fallback line read backwards in Arabic.** The waypoints above it mirror
 * because Compose lays them out; the names were concatenated by hand with a
 * middle dot, so a run of Latin names stayed left to right inside an RTL
 * paragraph. The road ran one way and its own names ran the other, which put
 * the first stage at opposite ends of the two. Every template name the app
 * ships is Latin, #62, so that was the normal case in Arabic rather than an
 * edge one, and it was found by looking at the screen in Arabic.
 *
 * **The assertions are on `stageNamesLine` rather than on the rendered text**,
 * because `RoadStrip` clears its descendants' semantics so a reader hears one
 * sentence instead of five waypoints. That makes the strip's own text
 * unreachable from a test, which is worth knowing before writing another one.
 */
@RunWith(AndroidJUnit4::class)
class RoadStripTest {

    @get:Rule
    val compose = createComposeRule()

    private val stages = listOf(
        RoadStage("Decision received", reached = true),
        RoadStage("Preparing", reached = false),
        RoadStage("Submitted", reached = false),
        RoadStage("Answered", reached = false),
    )

    /**
     * Each name is isolated, so the run lays out in the paragraph's direction
     * rather than staying left to right inside an RTL screen.
     *
     * **Asserting the isolate marks and not just the words** is the whole
     * point: the bare concatenation and this contain the same letters in the
     * same order and lay out differently, so a test comparing the words alone
     * would have passed against the defect.
     */
    @Test
    fun theOneLineFallbackIsolatesEachName() {
        assertEquals(Bidi.join(stages.map { it.name }), stageNamesLine(stages))
    }

    /** The bare concatenation this used to draw is gone, not merely alongside. */
    @Test
    fun theBareConcatenationIsNotWhatIsDrawn() {
        val bare = stages.joinToString(" · ") { it.name }
        assertTrue(
            "the names are still concatenated raw, so they read backwards in Arabic",
            stageNamesLine(stages) != bare,
        )
    }

    /** The order is the road's order and nothing here reverses it by hand. */
    @Test
    fun theNamesStayInTheRoadsOwnOrder() {
        val line = stageNamesLine(stages)
        val positions = stages.map { line.indexOf(it.name) }
        assertTrue("a name is missing from the line", positions.none { it < 0 })
        assertEquals(
            "the line reorders the stages, which the layout is supposed to do",
            positions.sorted(),
            positions,
        )
    }

    /**
     * A road with one stage is refused. `DESIGN.md` 20.6: a single waypoint on
     * a dashed line says nothing at all, and the component says so rather than
     * drawing something meaningless.
     */
    @Test(expected = IllegalArgumentException::class)
    fun oneStageIsNotARoad() {
        compose.setContent {
            HealthTrailTheme {
                RoadStrip(
                    stages = listOf(RoadStage("Applied", reached = true)),
                    description = "a road",
                )
            }
        }
    }
}
