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
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.TabChip
import com.kamsiob.healthtrail.ui.components.GroupedRows
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.ThemeChoice

object MoreTags {
    const val COMING = "more_coming"
    const val ABOUT = "more_about"
    const val SEARCH = "more_search"
    const val LIBRARY = "more_library"
    const val EXPORT = "more_export"
    const val RESTORE = "more_restore"
    const val SITUATION = "more_situation"
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
    onRestore: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onLibrary: () -> Unit = {},
    onSituation: () -> Unit = {},
) {
    AppearanceScreen(
        choice = choice,
        onChoose = onChoose,
        modifier = modifier,
        // **More is titled More.** It used to render Appearance directly,
        // because Appearance was the only thing in it and a list with one entry
        // is a tap the person pays for nothing. There are five destinations in
        // it now, so a person tapping "More" and landing on a screen headed
        // "Appearance" is the app showing its own structure, which is rule 20.
        titleKey = "nav.more",
        subtitleKey = "more.subtitle",
        tab = { TabChip(hue = wholeAppHue(), labelKey = "nav.more") },
        // **Above the theme, because these are what somebody opens More to
        // reach.** The theme is changed once; search and export are not.
        header = {
            MoreDestinations(
                onAbout = onAbout,
                onExport = onExport,
                onRestore = onRestore,
                onSearch = onSearch,
                onLibrary = onLibrary,
                onSituation = onSituation,
            )
        },
        footer = { ComingHere() },
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
private fun MoreDestinations(
    onAbout: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSituation: () -> Unit,
) {
    val strings = LocalStrings.current

    // **Destinations, so rows with chevrons rather than outlined pills.**
    //
    // These were five full width outlined buttons, and every one of them opens
    // a screen. Law 2 gives that costume to a row ending in a chevron, and
    // gives the outlined pill to a smaller action that does something now,
    // "always a verb or a dialable number". Five pills that all navigate taught
    // the person the wrong thing about what a pill does, on the one screen
    // whose whole content is places to go.
    //
    // `DESIGN.md` section 14 says More follows the notebook, and this is what
    // that means: a grouped surface of doors.
    //
    // **The order is by how often somebody reaches for it**, which is why
    // search leads: `MASTER_SPEC.md` 4.8 puts it here and at the top of Today,
    // and of these it is the one reached weekly rather than once.
    val destinations = listOf(
        Destination(strings["more.search"], onSearch, MoreTags.SEARCH),
        Destination(strings["more.library"], onLibrary, MoreTags.LIBRARY),
        // **How the notebook is set up, which had no door at all.** The
        // situation picker ran once during setup and was then unreachable
        // forever, so a family whose care moved could not tell the app, and
        // could not even see which setting they had chosen. 13.5 calls a
        // capability only its author can find not finished, and law 5 promises
        // this is "all of it changeable afterward from one screen, without
        // penalty". This is that screen's door.
        Destination(strings["more.situation"], onSituation, MoreTags.SITUATION),
        Destination(strings["more.export"], onExport, MoreTags.EXPORT),
        Destination(strings["more.restore"], onRestore, MoreTags.RESTORE),
        Destination(strings["more.about"], onAbout, MoreTags.ABOUT),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Space.sectionGap))
        GroupedRows(items = destinations) { destination, isLast ->
            DenseRow(
                title = destination.label,
                chevron = true,
                divider = !isLast,
                onClick = destination.onOpen,
                modifier = Modifier.testTag(destination.testTag),
            )
        }
    }
}

/** One place More can take you. */
private data class Destination(
    val label: String,
    val onOpen: () -> Unit,
    val testTag: String,
)

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
