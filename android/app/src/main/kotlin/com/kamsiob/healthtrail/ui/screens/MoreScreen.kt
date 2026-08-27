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
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kamsiob.healthtrail.ui.v4.Action
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

    /**
     * The donate link at the foot of the scroll.
     *
     * `MASTER_SPEC.md` 2 puts it in three places: the gate, the bottom of
     * Settings, and About. It was in two, and this is the third. #467.
     */
    const val SUPPORT = "more_support"
}

/**
 * More: everything that is not a record.
 *
 * **Five groups, ordered by how often somebody needs them**, #467, and the
 * order is the argument. Search leads the page because it is the one thing here
 * reached weekly. Then the notebook and the people in it, because switching
 * profiles is the only other row a two person household touches often. Then
 * keeping a copy, which is monthly at most but is what somebody comes here in a
 * hurry for. Then the theme, changed once. Then what this app is, read once.
 *
 * **This is arrangement rather than redesign.** Every row that was here is
 * still here and still opens the same place. What moved is which rows sit
 * together and in what order, and the one thing added is the support link the
 * specification has always placed at the bottom of Settings and that this
 * screen never carried.
 *
 * **It still borrows `AppearanceScreen`'s frame**, because the theme choice is
 * a group on this page rather than a destination off it, and that screen owns
 * the control. The groups above the theme arrive as its header and the two
 * below it as its footer, which is what puts the theme fourth.
 *
 * Carries `ShellTags.NOT_BUILT` on the note about what is still coming, so it
 * stays greppable and cannot survive to release once those things exist.
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
    /** Profiles: who this notebook is about, and anyone else it holds. #379, #468. */
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
        // **What this app is comes last, and the offer is the last thing on
        // it.** D59 and D93: the support link sits after the sentence saying
        // the app asks for nothing, is never the filled weight, and is the end
        // of the scroll rather than something somebody has to pass.
        footer = {
            ComingHere()
            ThisApp(onAbout = onAbout)
        },
    )
}

/**
 * What this app is, and the one offer it makes.
 *
 * **The specification has put a support link at the bottom of Settings since
 * `MASTER_SPEC.md` 2 was written, and this screen has never had one.** D93 says
 * it "already sits at the bottom of Settings and About", which was true of
 * About and of the gate and was not true here. One destination, one label, in
 * three places, is what D59 asked for; this is the third.
 */
@Composable
private fun ThisApp(onAbout: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Spacer(Modifier.height(Space.sectionGap))
        Eyebrow(text = strings["more.group.app"])
        Block(padding = Space.none) {
            ListRow(
                // bidi-ok: a catalog label, in the app's own words.
                title = strings["more.about"],
                mark = Symbols.tips,
                markHue = goldHue(),
                isDoor = true,
                onClick = onAbout,
                clickLabel = strings["open.action"],
                modifier = Modifier.testTag(MoreTags.ABOUT),
            )
        }
        // **The quiet weight, never the filled one**, and it leaves the app,
        // which is what makes it the only outbound link on this screen.
        // Nothing is sent and nothing is recorded about the tap.
        Action(
            label = strings["disclaimer.support"],
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()))
            },
            modifier = Modifier.testTag(MoreTags.SUPPORT),
        )
    }
}

/**
 * The three groups above the theme, in the order somebody needs them. #467.
 *
 * **Grouped by what somebody came for**, owner, 2026-08-17: eight doors in one
 * block is a list to read rather than a place to look. **Ordered by how often
 * they are needed**, owner, 2026-08-27, which is what moved: finding things led
 * because search led, and search is not in a group at all. It is the door under
 * the title, `m3v4-1`, and `MASTER_SPEC.md` 4.8 puts it there and at the top of
 * Today.
 *
 * So the notebook and its people lead the groups. **Profiles first inside it**:
 * a household with two people in one notebook switches often, and the other two
 * rows are set once and corrected rarely. Then finding things. Then keeping a
 * copy, which somebody reaches for either monthly or in a hurry, and which
 * holds the way back from a mistake.
 */
@Composable
private fun MoreDestinations(
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSituation: () -> Unit,
    onSubject: () -> Unit,
    onPeople: () -> Unit,
    onConflicts: () -> Unit,
    conflicts: Int,
    /** Opens Deleted Items. #405, renamed by the owner in #465. */
    onBin: () -> Unit = {},
) {
    val strings = LocalStrings.current

    // Gold is what `hueFor` gives a surface belonging to no section.
    val gold = goldHue()
    val groups = listOfNotNull(
        // **1. The notebook and the people in it.**
        //
        // **Profiles had no door at all until #379**, and the situation picker
        // had none until #371: it ran once during setup and was then
        // unreachable forever, so a family whose care moved could not tell the
        // app. Law 5 promises this is "all of it changeable afterward from one
        // screen, without penalty". **Who the notebook is about** had no door
        // either: the name was typed once at setup, appears on no screen inside
        // the app, and is printed on everything shared out of it, so a typo was
        // invisible until a clinician was holding it.
        strings["more.group.notebook"] to listOf(
            Destination(
                strings["people.open"],
                onPeople,
                MoreTags.PEOPLE,
                Symbols.addPerson,
                hueFor(Repository.Section.CARE_TEAM),
            ),
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
        ),
        // **2. Finding things**, under the door that is the other half of it.
        strings["more.group.find"] to listOf(
            Destination(strings["more.library"], onLibrary, MoreTags.LIBRARY, Symbols.notebook, gold),
        ),
        // **3. Keeping a copy**, which is export, restore and the way back from
        // a mistake.
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
                // **Deleted Items is always here, unlike the conflict door.**
                // The two look alike and the reasoning is opposite: a
                // resolution notice appears only when a merge decided
                // something, because a permanent "nothing to look at" teaches
                // somebody to ignore a row that will one day matter. **This one
                // is different: its value is knowing it exists before you need
                // it.** Somebody who has just removed the wrong thing has to be
                // able to find their way back without having read a manual, and
                // a row that appears only after the mistake is a row they have
                // never seen.
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
