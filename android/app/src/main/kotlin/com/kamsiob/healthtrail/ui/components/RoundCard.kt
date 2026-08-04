package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * One round of lab work: the draw, its reason, and every test in it.
 * `DESIGN.md` section 7, drawn as grid screen 20.
 *
 * **Lab work is not a measurement the family takes, it is a result the family
 * receives**, usually several at once, ordered for a reason. That is the whole
 * argument for this component existing rather than tests being rows on the
 * progress screen. A round holds its tests, its reason, its documents, and who
 * ordered it, and those four things are what a person is actually holding in
 * their head when they ring the office to chase one.
 *
 * **Stored once, reachable three ways: by date, by test, and by the thread it
 * belongs to.** This is the by-date face. Every test name inside it is a door
 * to that test's own history across every round, which is the by-test face, and
 * that is why [RoundTestRow] takes an `onOpen` rather than being bare.
 *
 * **The reason is the heading, not the date.** "Ordered by Dr. Raman, after the
 * dizziness" is what a person remembers a round by. The date is data and sits
 * above it in the mono eyebrow, where dates that identify rather than describe
 * belong.
 *
 * **Nothing here says whether a result is good.** A test that came back is
 * "Results in" and a test that has not is "Still out", which are facts about
 * the paperwork rather than about the person. **No value, no range, no flag, no
 * color by result**, per `CLAUDE.md` rule 2, and this is the single most likely
 * place in the app for somebody to think a small colored arrow would help.
 * There is nowhere to put one, per D90.
 */
@Composable
fun RoundCard(
    eyebrow: String,
    reason: String,
    hue: TabHue,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card),
    ) {
        Column(
            modifier = Modifier.padding(
                start = Space.cardPadding,
                end = Space.cardPadding,
                top = Space.sm,
                bottom = Space.s,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = eyebrow, style = type.mono, color = hue.ink)
            Text(
                text = reason,
                style = type.displayS,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
        }
        content()
    }
}

/**
 * One test inside a round.
 *
 * **The name is a door.** Tapping it opens that test's own history across every
 * round the person has ever recorded, which is what turns a pile of lab reports
 * into something a family can actually follow. A test name that did not open
 * anything would be the dead end rule 18 names.
 *
 * @param status what the paperwork says: results in, still out, chased on a
 *   date. **Never what the results mean.**
 */
@Composable
fun RoundTestRow(
    name: String,
    status: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DenseRow(
        title = name,
        subtitle = status,
        chevron = true,
        onClick = onOpen,
        modifier = modifier,
    )
}
