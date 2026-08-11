package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * One stage of a project's road.
 *
 * [reached] is whether the project has been here, which is a fact drawn from the
 * record. **It is never a completion count and never a percentage**, per rule 13
 * and `DESIGN.md` 20.7: a road says where something stands, not how much of it
 * somebody has managed to get through.
 */
data class RoadStage(val name: String, val reached: Boolean)

/** How much room the road takes. `DESIGN.md` 20.6. */
enum class RoadSize {
    /** On the project home, under the title. Labels shown. */
    FULL,

    /** On a project card in a list. Smaller, and the caller decides on labels. */
    MINI,
}

/**
 * A project's stages as a horizontal stretch of the trail. `DESIGN.md` 20.6.
 *
 * **This is the project surface's visual signature**: waypoints on a dashed
 * gold rail, the same gold the trail uses everywhere else, because a project is
 * a stretch of the same road rather than a different kind of thing.
 *
 * **Costume: bare.** It is information and does nothing on touch. Stages are
 * renamed, added and removed from the project's setup screen, which is where
 * law 5 says that control belongs. Nothing here takes a press state, because a
 * control that responds and then does nothing reads as broken, D42.
 *
 * **Where it stands is marked by shape and weight, never by color alone**, per
 * section 9. The current waypoint is larger and ringed in ink; the ones behind
 * it are filled. Turn the screen grayscale and it still reads, which is the test
 * that matters for the people using this.
 *
 * **It is one node to a screen reader, not five.** A reader stopping on each
 * waypoint would say nothing useful five times. The caller passes [description],
 * already composed as a sentence, for the same reason [StageDots] does.
 *
 * **When to use it.** On a project, at the top of its home screen and on its
 * card in a list. Nowhere else.
 *
 * **When not to use it.** On anything that is not a project with named stages.
 * It is not a progress bar and it is not a staged flow the person walks through:
 * that is [StageDots], and the difference is that a road is where a slow process
 * has got to on its own, while dots are where somebody is in a conversation they
 * are having right now. **A road with one stage is not a road** and is refused,
 * because a single waypoint on a dashed line says nothing at all.
 *
 * **It mirrors fully in right to left**, per 23.2. The first stage sits on the
 * start edge in both directions, so the road runs the way the language reads.
 */
@Composable
fun RoadStrip(
    stages: List<RoadStage>,
    /** Said by a reader in place of the road, already composed by the caller. */
    description: String,
    modifier: Modifier = Modifier,
    size: RoadSize = RoadSize.FULL,
    showLabels: Boolean = size == RoadSize.FULL,
) {
    require(stages.size >= 2) {
        "A road needs at least two stages, not ${stages.size}. One waypoint on a " +
            "dashed line says nothing. DESIGN.md 20.6."
    }

    val colors = HealthTrail.colors
    val type = HealthTrail.type

    val waypoint: Dp = if (size == RoadSize.FULL) 13.dp else 9.dp
    val here: Dp = if (size == RoadSize.FULL) 15.dp else 11.dp
    val rail: Dp = if (size == RoadSize.FULL) 2.dp else 1.5.dp

    // The last stage that has been reached is where the project stands now.
    // Computed rather than passed, so a caller cannot say a project is at a
    // stage it has not reached, which would be the screen asserting something
    // the record does not.
    val current = stages.indexOfLast { it.reached }
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            // One node, one sentence.
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xs)
                .height(here),
            contentAlignment = Alignment.CenterStart,
        ) {
            // The dashed rail, drawn behind the waypoints and inset by half a
            // waypoint at each end so it starts and ends under one rather than
            // sticking out past the road.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(here)
                    .drawBehind {
                        val y = this.size.height / 2f
                        val inset = here.toPx() / 2f
                        drawLine(
                            color = colors.gold.copy(alpha = 0.55f),
                            start = Offset(inset, y),
                            end = Offset(this.size.width - inset, y),
                            strokeWidth = rail.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(rail.toPx() * 3f, rail.toPx() * 3f),
                            ),
                        )
                    },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stages.forEachIndexed { index, stage ->
                    Waypoint(
                        reached = stage.reached,
                        isHere = index == current,
                        diameter = if (index == current) here else waypoint,
                    )
                }
            }
        }

        // **Labels under the waypoints only while a stage name fits under
        // one.** At font scale 2.0 with four stages each label gets about a
        // quarter of the width, which is narrower than the word itself, and
        // Compose then breaks inside the word: "Gathering" came out as
        // "Gatherin" over "g". A road that spells its own stages wrong is
        // worse than a road that lists them.
        //
        // **Below that width the names run as one line under the strip
        // instead.** Nothing is dropped and nothing is abbreviated: the road
        // still shows where the project is, and the names still say what the
        // stages are called, which is what somebody at that font scale needs
        // most. Section 9, and rule 14: the project home draws the same road
        // and had the same defect.
        if (showLabels) {
            LabelsOrList(stages = stages, current = current, mirrored = mirrored)
        }
    }
}

/**
 * The stage names, under their own waypoints where they fit and as one line
 * where they do not.
 *
 * **Measured rather than guessed.** A threshold derived from the font scale
 * looked right on paper and did nothing on the phone, so this lays the longest
 * name out with the same style the labels use and asks how wide it actually
 * came out. That is the only answer that cannot be wrong about a font this
 * component does not choose.
 */
@Composable
private fun LabelsOrList(stages: List<RoadStage>, current: Int, mirrored: Boolean) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val perStage = with(LocalDensity.current) { (maxWidth / stages.size).toPx() }
        val widest = remember(stages, type.mono, perStage) {
            stages.maxOf { measurer.measure(it.name, type.mono).size.width }
        }

        // **A word wider than its column is a word Compose breaks in half.**
        // At font scale 2.0 with four stages, "Gathering" came out as
        // "Gatherin" over "g". A road that spells its own stages wrong is worse
        // than a road that lists them, so below this the names run as one line
        // under the strip: nothing dropped, nothing abbreviated. Rule 14, and
        // the project home draws the same road.
        if (widest <= perStage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                stages.forEachIndexed { index, stage ->
                    Text(
                        // **A family's own words, not the catalog's.** A stage
                        // arrives from a template and `ProjectRoadScreen` lets
                        // anybody rename it, which is what this was annotated
                        // as catalog copy and is not. `RoadStripTest` was
                        // already asserting `Bidi.join` on the same names one
                        // node up, which is the test disagreeing with the
                        // comment and being right.
                        text = Bidi.isolate(stage.name),
                        style = type.mono,
                        // **ink2 and not ink3 for a stage not yet reached.**
                        // The grid draws it in the faintest tone it has, and on
                        // a bright monitor that reads as pleasantly quiet. ink3
                        // is 2.37:1, which is unreadable on a phone in bad light
                        // for exactly this audience, and D92 makes it non-text.
                        // A stage the project has not reached still has to be
                        // legible: it is where this is going.
                        color = when {
                            index == current -> colors.ink
                            stage.reached -> colors.goldInk
                            else -> colors.ink2
                        },
                        // Two lines, because a stage name in the longest
                        // language does not fit on one at this width and a name
                        // cut in half is worse than a road one line taller.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = when {
                            index == 0 -> if (mirrored) TextAlign.End else TextAlign.Start
                            index == stages.lastIndex ->
                                if (mirrored) TextAlign.Start else TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Text(
                text = stageNamesLine(stages),
                style = type.mono,
                color = colors.ink2,
                modifier = Modifier.fillMaxWidth().padding(top = Space.xs),
            )
        }
    }
}

/**
 * The stage names as one line, for when they cannot sit under their waypoints.
 *
 * **`Bidi.join` and not a plain concatenation**, which is what this was and
 * which read backwards in Arabic. The waypoints above mirror because Compose
 * lays them out; a run of Latin names joined by hand with a middle dot does
 * not, so the road ran right to left while its own names ran left to right and
 * the first stage sat at opposite ends of the two. **Every template name the
 * app ships is Latin, #62**, so that was the normal case in Arabic rather than
 * an edge one, and it was found by looking at the screen.
 *
 * **Raw names in**, per `DESIGN.md` section 15: these have not been isolated
 * anywhere else, and joining something already isolated nests the marks.
 *
 * **It is a function rather than an expression inline** because [RoadStrip]
 * clears its descendants' semantics to speak as one node, so nothing in the
 * strip's own text is reachable from a test. This is the only place the
 * concatenation can be held.
 */
internal fun stageNamesLine(stages: List<RoadStage>): String =
    Bidi.join(stages.map { it.name })

/**
 * One point on the road.
 *
 * Three states and every one of them differs in more than color: not reached is
 * an open ring, reached is filled, and here is larger with an ink ring around
 * it. Section 9.
 */
@Composable
private fun Waypoint(reached: Boolean, isHere: Boolean, diameter: Dp) {
    val colors = HealthTrail.colors
    Box(
        modifier = Modifier
            .height(diameter)
            .width(diameter)
            .drawBehind {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                when {
                    isHere -> {
                        drawCircle(colors.paper, radius, center)
                        drawCircle(
                            color = colors.ink,
                            radius = radius - 1.5.dp.toPx(),
                            center = center,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }

                    reached -> {
                        drawCircle(colors.goldWash, radius, center)
                        drawCircle(colors.gold, radius - 2.dp.toPx(), center)
                    }

                    else -> {
                        drawCircle(colors.paper, radius, center)
                        drawCircle(
                            color = colors.gold,
                            radius = radius - 1.25.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                    }
                }
            },
    )
}
