package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.SearchDoor
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider
import com.kamsiob.healthtrail.ui.ShellTags
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.alertHue
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.TabHue

object MoreTags {
    const val PEOPLE = "more_people"
    const val COMING = "more_coming"
    const val ABOUT = "more_about"
    const val SEARCH = "more_search"
    const val LIBRARY = "more_library"
    const val EXPORT = "more_export"
    const val RESTORE = "more_restore"
    const val CONFLICTS = "more_conflicts"
    /** What you took out, and the way to put it back. #405. */
    const val BIN = "more_bin"
    const val SITUATION = "more_situation"
    const val SUBJECT = "more_subject"
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
    /** Opens the correction for who this notebook is about. #371. */
    onSubject: () -> Unit = {},
    /** Who this notebook is about, and anyone else it holds. #379. */
    onPeople: () -> Unit = {},
    onConflicts: () -> Unit = {},
    /**
     * How many merge resolutions the person has not looked at.
     *
     * **The door appears only after a merge has happened**, because a notebook
     * that has never been merged has nothing behind it, and a permanent row
     * reading "nothing to look at" is a screen teaching somebody to ignore a
     * row that will one day matter. Rule 13's other half: an unfilled slot
     * reads as "not yet", and this one is genuinely "not applicable".
     */
    conflicts: Int = 0,
    /** Opens what was taken out. #405. */
    onBin: () -> Unit = {},
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
        // The More page says what it is for, like every other page. #379.
        tipsKey = "more",
        subtitleKey = "more.subtitle",
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
                onSubject = onSubject,
                onPeople = onPeople,
                onConflicts = onConflicts,
                conflicts = conflicts,
                onBin = onBin,
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
    onSubject: () -> Unit,
    onPeople: () -> Unit,
    onConflicts: () -> Unit,
    conflicts: Int,
    /** Opens what was taken out. #405. */
    onBin: () -> Unit = {},
) {
    val strings = LocalStrings.current

    // **Destinations, so rows with a mark and a chevron rather than pills.**
    //
    // These were five full width outlined buttons and every one of them opens a
    // screen. A container with a chevron is what this app gives a door; the
    // action shape belongs to a verb that does something now.
    //
    // **Grouped by what somebody came for**, owner, 2026-08-17: eight doors in
    // one block is a list to read rather than a place to look. Finding things,
    // the notebook itself, keeping a copy, and what this app is. Four short
    // groups, each with its own quiet label, which is rule 15 applied to a
    // screen that is nothing but destinations.
    //
    // **Search leads its group**, because `MASTER_SPEC.md` 4.8 puts it here and
    // at the top of Today, and of these it is the one reached weekly rather
    // than once.
    // Gold is what `hueFor` gives a surface belonging to no section.
    val gold = goldHue()
    val groups = listOfNotNull(
        strings["more.group.find"] to listOf(
            Destination(strings["more.library"], onLibrary, MoreTags.LIBRARY, Symbols.notebook, gold),
        ),
        // **How the notebook is set up, which had no door at all.** The
        // situation picker ran once during setup and was then unreachable
        // forever, so a family whose care moved could not tell the app. Law 5
        // promises this is "all of it changeable afterward from one screen,
        // without penalty". This is that screen's door.
        //
        // **Who the notebook is about, which had no door either.** #371: the
        // name was typed once at setup, appears on no screen inside the app,
        // and is printed on everything shared out of it, so a typo was
        // invisible until a clinician was holding it. Beside the situation,
        // because both are how this notebook was set up, and **more than one
        // person**, #379, because that is the same question one step on.
        strings["more.group.notebook"] to listOf(
            Destination(
                strings["more.situation"],
                onSituation,
                MoreTags.SITUATION,
                Symbols.standingInstructions,
                hueFor(Repository.Section.STANDING_INSTRUCTIONS),
            ),
            Destination(
                strings["more.subject"],
                onSubject,
                MoreTags.SUBJECT,
                Symbols.careTeam,
                hueFor(Repository.Section.CARE_TEAM),
            ),
            Destination(
                strings["people.open"],
                onPeople,
                MoreTags.PEOPLE,
                Symbols.addPerson,
                hueFor(Repository.Section.CARE_TEAM),
            ),
        ),
        strings["more.group.copy"] to (
            listOf(
                Destination(
                    strings["more.export"],
                    onExport,
                    MoreTags.EXPORT,
                    Symbols.download,
                    hueFor(Repository.Section.DOCUMENTS),
                ),
                Destination(
                    strings["more.restore"],
                    onRestore,
                    MoreTags.RESTORE,
                    Symbols.documents,
                    hueFor(Repository.Section.DOCUMENTS),
                ),
                // **The bin is always here, unlike the conflict door.** The
                // two look alike and the reasoning is opposite: a resolution
                // notice appears only when a merge decided something, because a
                // permanent "nothing to look at" teaches somebody to ignore a
                // row that will one day matter. **A bin is different: its value
                // is knowing it exists before you need it.** Somebody who has
                // just removed the wrong thing has to be able to find their way
                // back without having read a manual, and a row that appears only
                // after the mistake is a row they have never seen.
                Destination(
                    strings["bin.open"],
                    onBin,
                    MoreTags.BIN,
                    Symbols.bin,
                    hueFor(Repository.Section.TRAIL),
                ),
            ) + if (conflicts > 0) {
                // **Beside restore, because that is where it came from.** Rule
                // 18: if the merge points at this, this belongs where the merge
                // lives.
                listOf(
                    Destination(
                        strings["more.conflicts"],
                        onConflicts,
                        MoreTags.CONFLICTS,
                        Symbols.incidents,
                        alertHue(),
                    ),
                )
            } else {
                emptyList()
            }
            ),
        strings["more.group.app"] to listOf(
            Destination(strings["more.about"], onAbout, MoreTags.ABOUT, Symbols.tips, gold),
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Spacer(Modifier.height(Space.s))
        // **Search is the door rather than a row.** #388 finding 3: this screen
        // opened with a title and then rows of equal weight, and nothing on it
        // led. `SearchDoor` is the component `m3v4-1` draws directly under a
        // title, it was built for exactly this and had no caller anywhere in
        // the app, and search is what `MASTER_SPEC.md` 4.8 puts first here.
        //
        // **It keeps `MoreTags.SEARCH`**, so the tag that names the way into
        // search on this screen names it in its new shape rather than moving.
        SearchDoor(
            label = strings["more.search"],
            onOpen = onSearch,
            modifier = Modifier.testTag(MoreTags.SEARCH),
        )
        groups.forEach { (label, destinations) ->
            Eyebrow(text = label)
            Block(padding = Space.none) {
                destinations.forEachIndexed { index, destination ->
                    ListRow(
                        // bidi-ok: a catalog label, in the app's own words
                        // rather than the person's.
                        title = destination.label,
                        mark = destination.mark,
                        // **The disc every mark in this app wears.** D198: a
                        // mark is `TabHue.base` with `onBase` on top. These ten
                        // rows passed a bare drawable, so More was the one list
                        // in the notebook drawn in gray while every other list
                        // carried its kind's color.
                        markHue = destination.hue,
                        isDoor = true,
                        onClick = destination.onOpen,
                        // **A reader is told the tap opens the place**, rather
                        // than "double tap to activate", which names no act at
                        // all. The row already says where it goes, so the label
                        // is the verb and not the destination said twice.
                        clickLabel = strings["open.action"],
                        modifier = Modifier.testTag(destination.testTag),
                    )
                    if (index != destinations.lastIndex) RowDivider()
                }
            }
        }
    }
}

/** One place More can take you. */
/**
 * One door on the More screen, and the color the door's mark wears.
 *
 * **The hue is the hue of what it leads to**, taken from `hueFor`, which D198
 * calls the owner's mapping and forbids re-deriving. A row that leads to no
 * section takes gold, which is what `hueFor` gives every whole-app surface.
 */
private data class Destination(
    val label: String,
    val onOpen: () -> Unit,
    val testTag: String,
    @androidx.annotation.DrawableRes val mark: Int,
    val hue: TabHue,
)

@Composable
private fun ComingHere() {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MoreTags.COMING)
            .testTag(ShellTags.NOT_BUILT),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Spacer(Modifier.height(Space.sectionGap))
        Eyebrow(text = strings["more.coming.group"])
        Block {
            // bidi-ok: the app's own sentence about what it has not built yet.
            Body(text = strings["more.coming.body"])
        }
    }
}
