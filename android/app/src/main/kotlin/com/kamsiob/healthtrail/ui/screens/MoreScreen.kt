package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.ShellTags
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.ThemeChoice

object MoreTags {
    const val COMING = "more_coming"
    const val ABOUT = "more_about"
    const val EXPORT = "more_export"
}

/**
 * More.
 *
 * **Appearance is the only thing in it, so it shows Appearance directly** rather
 * than a menu of one item pointing at it. A list with a single entry is a tap
 * the person pays for nothing, and rule 18 counts taps. It becomes a real list
 * the moment there is a second thing, which is a change to make then rather
 * than a structure to build in advance of it.
 *
 * **It says what else is coming, and that is not filler.** Three cards and then
 * two thirds of an empty screen reads as unfinished, which rule 14 forbids, and
 * the honest fix is not to invent a fourth setting. It is to say plainly what
 * this destination is going to hold. D44: an interface may offer something it
 * has not built, and it may not go quiet about it.
 *
 * Carries `ShellTags.NOT_BUILT` so the note stays greppable and cannot survive
 * to release once the things it names exist.
 */
@Composable
fun MoreScreen(
    choice: ThemeChoice,
    onChoose: (ThemeChoice) -> Unit,
    onAbout: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppearanceScreen(
        choice = choice,
        onChoose = onChoose,
        modifier = modifier,
        footer = { MoreBelowAppearance(onAbout = onAbout, onExport = onExport) },
    )
}

/**
 * What sits under Appearance in More.
 *
 * **About is a real destination now, so it is offered rather than promised.**
 * The note about what is still coming shrank to what is actually still coming,
 * which is the point of D44: the interface may say it has not built something,
 * and it may not keep saying so once it has.
 */
@Composable
private fun MoreBelowAppearance(onAbout: () -> Unit, onExport: () -> Unit) {
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Space.sectionGap))
        QuietButton(
            label = strings["more.export"],
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().testTag(MoreTags.EXPORT),
        )
        Spacer(Modifier.height(Space.cardGap))
        QuietButton(
            label = strings["more.about"],
            onClick = onAbout,
            modifier = Modifier.fillMaxWidth().testTag(MoreTags.ABOUT),
        )
        ComingHere()
    }
}

@Composable
private fun ComingHere() {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MoreTags.COMING)
            .testTag(ShellTags.NOT_BUILT),
    ) {
        Spacer(Modifier.height(Space.sectionGap))
        GroupHeader(labelKey = "more.coming.group")
        Spacer(Modifier.height(Space.m))
        Text(
            text = strings["more.coming.body"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}
