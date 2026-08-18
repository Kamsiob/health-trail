package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor

/**
 * What is on the list, written fresh on Material 3 Expressive. D196.
 *
 * **This screen is not a conversion of the old one and nothing was carried
 * across but its contract.** The owner, 2026-08-17: "you're still just trying
 * to change the existing UI code instead of getting rid of it and building
 * fresh." That is the difference between this file and everything before it
 * today: the old screen was deleted rather than edited, and this was written
 * from Material's components outward.
 *
 * **Everything visible here is Material's own.** `Scaffold`,
 * `LargeFlexibleTopAppBar` with its own collapse, `Card` at the container
 * roles, `ListItem` for every row, `ExtendedFloatingActionButton` for the one
 * action, `Icon` over Material Symbols. Nothing measures a mockup and nothing
 * paints a hex: the colors are `MaterialTheme.colorScheme` and the identity is
 * `hueFor`, which is the one place a section's color lives.
 *
 * **What this app adds on top**, and it is the only part worth hand writing:
 *
 * - **The list somebody came for is in the section's own container**, so the
 *   screen has a colored thing and it means "this is medications" rather than
 *   grading anything. One per screen, `docs/V4.md` 2.1.
 * - **A dose is never parsed.** The schema keeps the sentence the person was
 *   told, in the words they were told it in, and this shows exactly that.
 *   Misparsing a dose is worse than not parsing one.
 * - **A stopped medication stays and is not hidden.** "She was on this until
 *   March" is the answer to a question somebody eventually gets asked. It sits
 *   under its own label, which says finished without asking for a tap. D185.
 * - **Nothing here reminds, alerts, or judges.** Rule 2. The screen counts and
 *   stops.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    medications: List<Repository.Medication>,
    onOpen: (Repository.Medication) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * How many questions are still waiting on each medication, by id.
     *
     * A medication with none is absent from the map rather than present with a
     * zero, so a row cannot say "0 questions" by accident.
     */
    openQuestions: Map<String, Int> = emptyMap(),
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val hue = hueFor(Repository.Section.MEDICATIONS)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val current = medications.filterNot { it.isStopped }
    val stopped = medications.filter { it.isStopped }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.background,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = strings["meds.heading"]) },
                subtitle = {
                    Text(
                        text = strings["notebook.section.medications"],
                        color = hue.ink,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(PageTagsBack)) {
                        Icon(
                            painter = painterResource(Symbols.back),
                            contentDescription = strings[backLabelKey],
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    scrolledContainerColor = scheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            // **Material's extended button for the one thing this screen does.**
            // It says the verb rather than leaving a bare plus to carry it, and
            // it is the only action here, so it is the only filled thing.
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = {
                    Icon(painter = painterResource(Symbols.add), contentDescription = null)
                },
                text = { Text(text = strings["meds.add"]) },
                // **The sentence sits on the button's own node.** The words are
                // in a `Text` two levels down, inside the row Material builds
                // for an extended button, so the node that carries the tap
                // carried no label of its own and the reader sweep listed it as
                // a touchable with nothing to say. Rule 19 is a gate, and this
                // is the one screen that had already been rebuilt.
                modifier = Modifier
                    .testTag(MedsTags.ADD)
                    .semantics { contentDescription = strings["meds.add"] },
            )
        },
    ) { inset ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(MedsTags.ROOT),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = inset.calculateTopPadding(),
                // **Clear of the floating button**, which sits over the list
                // rather than in it: without this the last medication is under
                // the one control on the screen. Seen on the phone, rule 21.
                bottom = inset.calculateBottomPadding() + Space.fabScrollClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.betweenGroups),
        ) {
            item {
                Text(
                    text = strings["meds.subtitle"],
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (medications.isEmpty()) {
                item {
                    SectionEmpty(
                        name = MedsTags.NAME,
                        text = strings["meds.empty"],
                        section = Repository.Section.MEDICATIONS,
                    )
                }
            }

            if (current.isNotEmpty()) {
                item {
                    // **The surface stays neutral and the color is a mark.**
                    // A first attempt put the section's wash on the whole list
                    // and the owner's reply was that it had become a green
                    // wall, which it had: a container that tall is not an
                    // accent, it is the screen. Material keeps surfaces neutral
                    // and spends color in small places that mean something, and
                    // so does every approved drawing.
                    MedicationGroup(
                        container = scheme.surfaceContainerLow,
                        medications = current,
                        openQuestions = openQuestions,
                        hue = hue,
                        onOpen = onOpen,
                    )
                }
            }

            if (stopped.isNotEmpty()) {
                item {
                    Text(
                        text = strings["meds.stopped"],
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
                item {
                    // Quiet, because what is current is what the screen leads
                    // with. Present, because it is still part of the record.
                    MedicationGroup(
                        container = scheme.surfaceContainerLow,
                        medications = stopped,
                        openQuestions = openQuestions,
                        hue = hue,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

/** One run of medications in a card, divided the way Material divides a list. */
@Composable
private fun MedicationGroup(
    container: Color,
    medications: List<Repository.Medication>,
    openQuestions: Map<String, Int>,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onOpen: (Repository.Medication) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
    ) {
        medications.forEachIndexed { index, medication ->
            MedicationRow(
                medication = medication,
                waiting = openQuestions[medication.id] ?: 0,
                hue = hue,
                onOpen = onOpen,
            )
            if (index != medications.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = Space.markTile + Space.ml),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * One medication: what it is called, what it is for, and what they take.
 *
 * **The dose is the last line rather than a column at the end.** A dose is a
 * sentence somebody was told, and sentences of different lengths in a trailing
 * column leave the name wrapping around whatever is left.
 *
 * **The emergency card marker is words, never a color alone.** `DESIGN.md` 12.
 */
@Composable
private fun MedicationRow(
    medication: Repository.Medication,
    waiting: Int,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onOpen: (Repository.Medication) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme

    val detail = listOfNotNull(
        strings("meds.questions.waiting", "count" to waiting).takeIf { waiting > 0 },
        medication.purposeText?.takeIf { it.isNotBlank() },
        strings["meds.on_card.badge"].takeIf { medication.showsOnEmergencyCard },
    ).let { Bidi.join(it) }.takeIf { it.isNotBlank() }

    val dose = medication.doseText?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }

    ListItem(
        headlineContent = { Text(text = Bidi.isolate(medication.name)) },
        // **The tap, the tag and the reader's sentence on one node**, which is
        // `docs/TRAPS.md`'s first entry and what this row was missing entirely:
        // it took an `onOpen` and never called it, and drew a chevron promising
        // a door that did nothing. Rule 16, and a list whose rows do not open
        // is the medication screen with no way into any medication.
        modifier = Modifier
            .clickable(
                onClickLabel = strings["meds.open"],
                onClick = { onOpen(medication) },
            )
            .semantics(mergeDescendants = true) { }
            .testTag(MedsTags.row(medication.id)),
        supportingContent = if (detail == null && dose == null) {
            null
        } else {
            {
                Column {
                    detail?.let { Text(text = it) }
                    dose?.let {
                        Text(text = it, color = scheme.onSurface)
                    }
                }
            }
        },
        leadingContent = {
            // **This is where the section's color belongs**: one mark per row,
            // which says which part of the notebook this is and leaves the
            // reading surface alone. Saturated, D198: the pale version left a
            // page of rows reading as one color.
            HueMark(
                hue = hue,
                mark = Symbols.of(Repository.Section.MEDICATIONS),
                size = Space.markTile,
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(Symbols.forward),
                contentDescription = null,
                tint = scheme.outline,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** The tag the page frame uses for its way back, so journeys still find it. */
private const val PageTagsBack = "section_back"

object MedsTags {
    const val NAME = "medications"
    const val ADD = "medications_add"
    fun row(id: String) = "medication_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_medications"
}
