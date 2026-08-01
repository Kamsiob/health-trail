package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.ShellTags
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object TodayTags {
    const val ROOT = "today_root"
    const val EMPTY = "today_empty"
    const val INTERIM = "today_interim"
    fun step(number: Int) = "today_step_$number"
}

/**
 * Today.
 *
 * **Built because persona P1 requires it and was not getting it.** P1 is the
 * person standing in a corridor on the day of an admission, and one of the five
 * things that must be true for them is that the empty Today coaches rather than
 * sitting blank, with the Emergency Card first. It was the not-built screen,
 * which is exactly the wrong thing to hand someone at the moment they are most
 * likely to put the phone away.
 *
 * **The empty state is the finished screen, and the rest is honest about
 * itself.** The digest, the open counts, and the next appointment all need the
 * deterministic engine and screens that do not exist. A notebook with something
 * in it therefore says plainly that the summary is still being built, and says
 * in the same breath that nothing the person writes is waiting on it. That is
 * D44: an interface may offer something it has not built, and it may not go
 * quiet about it.
 *
 * **The three steps are guidance, not controls.** Two of the three lead to
 * screens that do not exist yet, and offering them as buttons would be exactly
 * the dead end D44 removed from the capture sheet. The one thing a person can
 * act on right now, capture, is already the gold button on every screen, so the
 * list reads as what to do next rather than as three disabled offers.
 *
 * **The numbering is allowed here** because this is genuinely a sequence, which
 * is the condition section 1's ban on numbered markers attaches to. Filling in
 * the emergency card first is the whole point of the list.
 *
 * Composed from Display L, Display S, Body L, Body M, the Mono style, and
 * cards 5.3. Nothing new was introduced.
 */
@Composable
fun TodayScreen(
    /** True when the notebook has nothing in it yet, which is P1's case. */
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = modifier.fillMaxSize().testTag(TodayTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal),
        ) {
            Spacer(Modifier.height(Space.l))
            Text(
                text = strings["today.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.l))

            if (isEmpty) {
                CoachedStart()
            } else {
                InterimDigest()
            }

            // Clearance for the capture button, which overlaps the navigation
            // bar and would otherwise sit on the last line.
            Spacer(Modifier.height(Space.xxl + Space.l))
        }
    }
}

/**
 * What to do first, written as an invitation rather than an absence.
 *
 * Section 5.10: every list has an empty state and it is written as an
 * invitation. **The first item is always the Emergency Card**, because it is
 * the highest value two minutes a new person can spend, and because it is the
 * one thing in this app that is useful to somebody else in a hurry.
 */
@Composable
private fun CoachedStart() {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = Modifier.fillMaxWidth().testTag(TodayTags.EMPTY)) {
        Text(
            text = strings["today.empty.title"],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.m))

        listOf(
            strings["today.empty.step.1"],
            strings["today.empty.step.2"],
            strings["today.empty.step.3"],
        ).forEachIndexed { index, step ->
            Step(number = index + 1, text = step)
            if (index < 2) Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = HealthTrail.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(TodayTags.step(number))
            .padding(Space.cardPadding),
    ) {
        // A numbered disc rather than a bullet, because the order is the
        // advice: the emergency card first is the point of the list.
        Row(
            modifier = Modifier
                .size(Space.l)
                .clip(CircleShape)
                .background(colors.sand),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            text = text,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A notebook with something in it, before the digest engine exists.
 *
 * **Says what is missing and says nothing is waiting on it.** The second half
 * matters more than the first: a person who reads that a summary is being built
 * needs to know immediately that their records are being kept regardless, or
 * the sentence reads as a reason to stop writing things down.
 *
 * Carries `ShellTags.NOT_BUILT` so it stays greppable and cannot survive to
 * release.
 */
@Composable
private fun InterimDigest() {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TodayTags.INTERIM)
            .testTag(ShellTags.NOT_BUILT),
    ) {
        Text(
            text = strings["today.digest.heading"],
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["today.digest.not_built"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}
