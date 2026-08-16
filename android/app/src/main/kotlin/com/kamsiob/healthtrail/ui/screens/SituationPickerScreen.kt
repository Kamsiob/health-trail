package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object SituationPickerTags {
    const val ROOT = "situation_picker_root"
    const val LIST = "situation_picker_list"
    const val SKIP = "situation_picker_skip"
    const val NOT_PERMANENT = "situation_not_permanent"
    fun row(id: String) = "situation_row_$id"
    fun group(id: String) = "situation_group_$id"
}

/**
 * The headings the fourteen settings sit under, in the order they appear.
 *
 * **Grouped by where the care is happening**, because that is the one thing
 * someone standing in a hallway already knows. Fourteen options at one weight
 * is a wall, and this is the first real screen after the disclaimer: it decides
 * whether a person keeps going.
 *
 * **The membership is in the catalog, not here.** `templates/data/situations.json`
 * carries a `group` on every template, so the web app groups identically rather
 * than reimplementing this list and drifting from it. What lives here is only
 * the order the headings appear in, which is a presentation decision.
 *
 * The order is by how many caregivers each covers, which is what the catalog's
 * `phase` marks. Facility and home lead because that is where most people are.
 */
/**
 * The catalog's `phase` value for the settings covering the most caregivers.
 * They lead their group and they are the ones that carry a burden line.
 */
private const val MOST_COMMON = 1

private val GROUP_ORDER = listOf(
    "facility" to "situation.group.facility",
    "home" to "situation.group.home",
    "treatment" to "situation.group.treatment",
    "comfort" to "situation.group.comfort",
)

/**
 * Choosing the care setting, which is what configures the notebook.
 *
 * A situation template sets which notebook sections sit expanded, which care
 * threads are offered, the first days checklist, and the document slots. All of
 * it is editable and deletable afterward, so this is a starting point rather
 * than a commitment.
 *
 * **Choosing is not applying, and nothing is applied by looking.** Tapping a row
 * selects that setting and moves on. There is no preview state here yet, and
 * when the template library gains one it uses the same presentation as this,
 * per `MASTER_SPEC.md` section 4.10.
 *
 * **The posture strings are shown verbatim** at the top, from the catalog, per
 * `templates/SCHEMA.md`. They are what keep this content reading as structure
 * rather than as advice, and paraphrasing them in the interface is forbidden.
 *
 * **Skipping is a real path.** A notebook with no situation template still
 * works: every section exists, none is folded, and nothing is missing. The
 * person can pick one later.
 *
 * Composed from Display L, Body M, the list row idiom already used elsewhere,
 * and one text action. Nothing new was introduced.
 */
@Composable
fun SituationPickerScreen(
    situations: TemplateCatalog.Situations,
    onChoose: (TemplateCatalog.Situation) -> Unit,
    onSkip: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SituationPickerTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Text(
                text = strings["situation.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))

            Text(
                text = strings["situation.subtitle"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            if (situations.posture.generalGuide.isNotBlank()) {
                Spacer(Modifier.height(Space.sm))
                // Verbatim from the catalog. Not paraphrased, not shortened.
                // **Isolated, because the catalog is English until #62 and this
                // is an Arabic layout.** Without it the sentence's own full
                // stop is moved to the front of its last line: ".your own"
                // rather than "your own.". Section 15, and it is the exact
                // failure that rule was written for.
                Text(
                    text = Bidi.isolate(situations.posture.generalGuide),
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }

            Spacer(Modifier.height(Space.l))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(SituationPickerTags.LIST),
            ) {
                GROUP_ORDER.forEach { (id, labelKey) ->
                    // Sorted by how many caregivers each setting covers, so the
                    // common ones are the first read inside every heading.
                    val inGroup = situations.all
                        .filter { it.group == id }
                        .sortedBy { it.phase }
                    if (inGroup.isEmpty()) return@forEach

                    item(key = "group_$id") {
                        Spacer(Modifier.height(Space.sectionGap))
                        GroupHeader(
                            labelKey = labelKey,
                            modifier = Modifier.testTag(SituationPickerTags.group(id)),
                        )
                        Spacer(Modifier.height(Space.headerGap))
                    }
                    // **The likeliest one is a card and the rest are rows.**
                    // Fourteen cards was three and a half screenfuls on the
                    // first screen after the disclaimer, and rule 22 is why: a
                    // name and a line telling two settings apart is a row, and
                    // only the leading one carries a third line worth reading.
                    // The hierarchy inside each group survives, and it is now
                    // carried by shape rather than by length alone.
                    item(key = inGroup.first().id) {
                        SituationCard(
                            situation = inGroup.first(),
                            onClick = { onChoose(inGroup.first()) },
                        )
                        Spacer(Modifier.height(Space.cardGap))
                    }
                    // **One lazy item per setting, and no surface around the
                    // run.** `GroupedSurface`'s own rule: not around a list long
                    // enough to scroll, where the rows should be full bleed with
                    // hairlines so the scroll is not a slab moving under a
                    // window. Fourteen settings is that list.
                    //
                    // **It is also what makes every one of them reachable.** The
                    // test scrolls this list by each setting's own id, which is
                    // the only reliable way to reach a lazy row, and batching a
                    // group into one item took those keys away. The trap is
                    // documented in `HANDOFF.md` and it was walked into anyway.
                    val rest = inGroup.drop(1)
                    rest.forEachIndexed { index, situation ->
                        item(key = situation.id) {
                            SituationRow(
                                situation = situation,
                                divider = index < rest.lastIndex,
                                onClick = { onChoose(situation) },
                            )
                        }
                    }
                    if (rest.isNotEmpty()) {
                        item(key = "rest_gap_$id") {
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }

                // A setting the catalog groups under a heading this version
                // does not know about is still shown, under no heading, rather
                // than disappearing. A person must never be unable to find
                // their own situation because of a data edit.
                val ungrouped = situations.all.filter { s ->
                    GROUP_ORDER.none { it.first == s.group }
                }.sortedBy { it.phase }
                if (ungrouped.isNotEmpty()) {
                    item(key = "ungrouped_gap") { Spacer(Modifier.height(Space.sectionGap)) }
                    ungrouped.forEachIndexed { index, situation ->
                        item(key = situation.id) {
                            SituationRow(
                                situation = situation,
                                divider = index < ungrouped.lastIndex,
                                onClick = { onChoose(situation) },
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(Space.s)) }
            }

            // **A real gap between the list and the pinned footer.** At the
            // largest system font the list fills the screen and the card at its
            // edge ends directly against this sentence, which on the phone read
            // as text overlapping a card rather than as a list scrolling behind
            // one. Found at font scale 2.0 and invisible at 1.0.
            Spacer(Modifier.height(Space.m))

            // **Said once, here, where the decision is being made.** The whole
            // weight of this screen is a person in a corridor worrying they
            // will pick wrong, and this sentence is what removes it. It sits
            // above the skip action rather than buried in the subtitle,
            // because that is where someone hesitating is looking.
            Text(
                text = strings["situation.not_permanent"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
                modifier = Modifier.testTag(SituationPickerTags.NOT_PERMANENT),
            )

            Spacer(Modifier.height(Space.s))

            // **Sized to its label, not the width of the screen.** D137: a
            // full width outlined bar is the way back and nothing else, and
            // under a full width filled action it is a second bar of which
            // only one leaves. #371 item 5, and it is retroactive per rule 14.
            TextAction(
                label = strings["situation.skip"],
                onClick = onSkip,
                modifier = Modifier.testTag(SituationPickerTags.SKIP),
            )
        }
    }
}

/**
 * One setting the group does not lead with, as a row.
 *
 * **A name and the line that tells it apart from its neighbor**, which is two
 * lines somebody scans rather than three they read, so rule 22 makes it a row.
 * The subtitle is uncapped for the reason D105 gives: it is the sentence that
 * distinguishes a nursing home from assisted living, and a cap on it is a
 * truncation at some font size.
 *
 * **No chevron.** A chevron means a screen opens, and tapping here chooses.
 */
@Composable
private fun SituationRow(
    situation: TemplateCatalog.Situation,
    divider: Boolean,
    onClick: () -> Unit,
) {
    DenseRow(
        title = Bidi.isolate(situation.name),
        subtitle = situation.subtitle.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) },
        subtitleMaxLines = Int.MAX_VALUE,
        divider = divider,
        onClick = onClick,
        modifier = Modifier.testTag(SituationPickerTags.row(situation.id)),
    )
}

/**
 * The setting its group leads with, as a card.
 *
 * **Name and subtitle always.** The subtitle exists specifically so two similar
 * settings can be told apart, and a name alone forces a guess between a nursing
 * home and assisted living, which is a guess this audience should not have to
 * make.
 *
 * **The burden line, on the settings that lead their group only.** It is one
 * sentence naming what is hard about a setting, written so the person feels
 * understood rather than processed. Read once it does that. Read fourteen times
 * in a row it is a wall of other people's hardship, on the screen that decides
 * whether someone in a corridor keeps going. So it appears on the settings the
 * catalog's `phase` marks as covering the most caregivers, which are the ones
 * most people are here for and the ones each group leads with. That is
 * `templates/SCHEMA.md`'s "use it as supporting text where it helps", taken at
 * its word.
 *
 * It also gives the list a hierarchy inside each group, the same shape the
 * notebook uses: the likeliest option is the fullest row.
 *
 * No chevron. A chevron implies going somewhere to look at something, and
 * tapping here chooses.
 */
@Composable
private fun SituationCard(
    situation: TemplateCatalog.Situation,
    onClick: () -> Unit,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.cardLarge)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.cardLarge)
            .clickable(
                interactionSource = interaction,
                // The row's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(SituationPickerTags.row(situation.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = Bidi.isolate(situation.name),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        if (situation.subtitle.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Bidi.isolate(situation.subtitle),
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }
        if (situation.burden.isNotBlank() && situation.phase == MOST_COMMON) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = Bidi.isolate(situation.burden),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}
