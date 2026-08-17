package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.ui.components.Aside
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.ScopedSearch
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.kamsiob.healthtrail.ui.theme.raisedCard
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider

object StartProjectTags {
    const val OWN = "start_project_own"
    fun ownTemplate(id: String) = "start_project_template_$id"
    const val OWN_NAME = "start_project_own_name"
    const val OWN_START = "start_project_own_start"
    const val ROOT = "start_project_root"
    const val CANCEL = "start_project_cancel"
    const val SEARCH = "start_project_search"
    fun template(id: String) = "start_project_$id"
    fun category(key: String) = "start_project_category_$key"
}

/**
 * The order the four kinds are offered in, and it never changes.
 *
 * **Fixed rather than sorted by size.** Ordering by how many templates each
 * holds would move the groups around the first time one gained a template, and
 * law 1's whole promise is that the places do not move. This order is roughly
 * how often a family meets them, and it is a decision rather than an accident.
 */
private val CATEGORY_ORDER = listOf("paying", "challenge", "moving", "papers")

/**
 * Choosing a process to take on.
 *
 * **The one thing, law 1: the template the person is looking for.** Sixteen
 * months-long bureaucratic processes were a flat wall of sixteen cards, which is
 * the shape rule 22 names: a card is for three or more lines somebody reads, and
 * a template is a name, a line about it, and a step count, which is a row in a
 * list somebody scans.
 *
 * **Grouped, per screen 23, so nothing hides below the fold unannounced.** The
 * grid's picker opens with the relevant few, folds the rest by kind with each
 * fold named and counted, and puts make-your-own at the bottom. This is that
 * composition with the person's own templates as the open group, because they
 * are the ones this person has already decided are worth keeping.
 *
 * **The category is what the person is trying to do**, not what kind of office
 * it involves. Somebody looking for a process is thinking "they cut her off and
 * I want to fight it", not "this is a Medicare matter". `templates/SCHEMA.md`,
 * and the four are held to a closed set by `check_templates.py` so a category
 * with no label can never reach a screen.
 *
 * **With no templates of their own, the first group opens instead**, so the
 * screen has a body rather than four sand rows and nothing else. First rather
 * than largest, for the same reason the order is fixed.
 *
 * **The search flattens, like the trail's.** Somebody who has typed a word wants
 * the handful that match, and folding those back under headings would hide the
 * answer behind the structure the search exists to escape.
 *
 * **Each row says how many steps it carries before it is chosen.** These are
 * months of work, and somebody deciding whether to start a Medicaid application
 * tonight deserves to know it is eleven steps rather than three before they
 * commit, not after.
 *
 * **A process whose rules vary by state says so, plainly.** The catalog marks
 * those, and the app says the steps stay general rather than inventing a rule
 * for a state it knows nothing about. That is rule 2 at its most load bearing: a
 * confidently wrong step in a Medicaid application costs somebody real money.
 */
@Composable
fun StartProjectScreen(
    templates: List<TemplateCatalog.ProjectTemplate>,
    onChoose: (TemplateCatalog.ProjectTemplate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** Starts a project with no template behind it, given what to call it. */
    onStartOwn: (String) -> Unit = {},
    /**
     * The person's own templates, offered above the sixteen.
     *
     * **A saved template nothing can start from is a saved template that does
     * not exist.** The library showed them and the start screen did not offer
     * them for the first hour, which is a feature with a hole through the
     * middle of it.
     */
    own: List<Repository.OwnTemplate> = emptyList(),
    onChooseOwn: (Repository.OwnTemplate) -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var query by rememberSaveable { mutableStateOf("") }
    var openCategories by rememberSaveable { mutableStateOf(emptySet<String>()) }

    val term = query.trim().lowercase()
    val matching = remember(templates, term) {
        if (term.isEmpty()) {
            templates
        } else {
            templates.filter {
                it.name.lowercase().contains(term) || it.subtitle.lowercase().contains(term)
            }
        }
    }
    val matchingOwn = remember(own, term) {
        if (term.isEmpty()) own else own.filter { it.name.lowercase().contains(term) }
    }

    val byCategory = remember(templates) { templates.groupBy { it.category } }
    // The group that arrives open when the person has none of their own, so the
    // screen is never four sand rows with nothing between them.
    val leadCategory = remember(byCategory) {
        CATEGORY_ORDER.firstOrNull { byCategory[it].orEmpty().isNotEmpty() }
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        // **`imePadding` because this screen has a text field.** Without it the
        // keyboard covers the very control the person has just typed a name
        // for: type, dismiss, scroll, tap. D38, and it is invisible at rest,
        // which is why 16.4 step 1 requires looking at a screen with a field
        // with the keyboard actually up.
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(StartProjectTags.ROOT)
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item(key = "head") {
                    Spacer(Modifier.height(Space.l))
                    FormHeader(
                        title = strings["projects.start.title"],
                        // The lead is an Aside now, on the section's wash with
                        // its own icon, rather than the smallest gray line under the
                        // title. D172, and the approved medication mockup.
                        lead = null,
                        section = Repository.Section.PROJECTS,
                    )
                        Spacer(Modifier.height(Space.m))
                        Aside(text = strings["projects.start.lead"], section = Repository.Section.PROJECTS)
                    Spacer(Modifier.height(Space.xs))
                    // **Where they are in the flow**, 20.5 screens 03 and 04.
                    // The preview said "2 of 2" while this said nothing, so
                    // the second stage announced a first one nobody had been
                    // shown. Law 3: staged, and the stages say so.
                    //
                    // **Under the lead rather than above the title**, since
                    // #371 item 5: the eyebrow slot above now holds the tab
                    // chip, and a mono line stacked over a chip is two
                    // eyebrows arguing about which one says where you are.
                    Text(
                        text = strings["projects.start.stage"],
                        style = HealthTrail.type.eyebrow,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.l))
                    ScopedSearch(
                        value = query,
                        onValueChange = { query = it },
                        hint = strings("projects.search.hint", "count" to templates.size),
                        clearLabel = strings["projects.search.clear"],
                        testTag = StartProjectTags.SEARCH,
                    )
                    Spacer(Modifier.height(Space.m))
                }

                if (term.isNotEmpty()) {
                    // **Flat, and no folds**, the trail's own answer to the same
                    // problem. What matched is the whole point; putting it back
                    // under a heading would be answering a search with a door.
                    if (matchingOwn.isNotEmpty()) {
                        item(key = "found_own") {
                            OwnTemplates(own = matchingOwn, onChoose = onChooseOwn)
                            Spacer(Modifier.height(Space.sectionGap))
                        }
                    }
                    if (matching.isNotEmpty()) {
                        item(key = "found") {
                            Templates(templates = matching, onChoose = onChoose)
                            Spacer(Modifier.height(Space.sectionGap))
                        }
                    }
                    if (matching.isEmpty() && matchingOwn.isEmpty()) {
                        item(key = "found_none") {
                            Text(
                                text = strings["projects.search.none"],
                                style = HealthTrail.type.bodyM,
                                color = colors.ink2,
                                modifier = Modifier.padding(vertical = Space.l),
                            )
                            // **A search that matches nothing is exactly when
                            // starting from nothing is the answer**, and
                            // moving this block to the browse branch took it
                            // away from the one screen state that needs it
                            // most. A test caught it, and the test was right:
                            // somebody whose process is not in the catalog
                            // searches for it first, finds nothing, and must
                            // not be left staring at a sentence. #379.
                            OwnProject(onStart = onStartOwn)
                            Spacer(Modifier.height(Space.l))
                        }
                    }
                } else {
                    // **Starting from nothing leads, on the owner's direction,
                    // #379.** It sat last, on the argument that most people
                    // want one of the sixteen and should not be asked to write
                    // from a blank page first. He watched it and disagreed:
                    // the option was invisible down there, and somebody whose
                    // process is not in the catalog concluded the app could
                    // not hold it. It leads now, and it is raised so it reads
                    // as an offer rather than as a form left open.
                    item(key = "own_project") {
                        OwnProject(onStart = onStartOwn)
                        Spacer(Modifier.height(Space.sectionGap))
                    }

                    // **Theirs first and open.** Somebody who has made one has
                    // learned something the catalog did not know, and putting
                    // the sixteen shipped ones above it would say the opposite.
                    if (own.isNotEmpty()) {
                        item(key = "own") {
                            Eyebrow(text = strings["library.own"])
                            Spacer(Modifier.height(Space.headerGap))
                            OwnTemplates(own = own, onChoose = onChooseOwn)
                            Spacer(Modifier.height(Space.sectionGap))
                        }
                    }

                    CATEGORY_ORDER.forEach { key ->
                        val inCategory = byCategory[key].orEmpty()
                        if (inCategory.isEmpty()) return@forEach
                        val leads = own.isEmpty() && key == leadCategory
                        val open = leads || key in openCategories

                        item(key = "cat_$key") {
                            if (leads) {
                                Eyebrow(text = strings["projects.category.$key"], fixed = false)
                                Spacer(Modifier.height(Space.headerGap))
                            } else {
                                FoldRowText(
                                    label = strings["projects.category.$key"],
                                    expanded = open,
                                    onToggle = {
                                        openCategories = if (key in openCategories) {
                                            openCategories - key
                                        } else {
                                            openCategories + key
                                        }
                                    },
                                    count = inCategory.size.toString(),
                                    modifier = Modifier.testTag(StartProjectTags.category(key)),
                                )
                                Spacer(Modifier.height(Space.cardGap))
                            }

                            if (open) {
                                Templates(templates = inCategory, onChoose = onChoose)
                                Spacer(Modifier.height(Space.sectionGap))
                            }
                        }
                    }
                }

            }

            Spacer(Modifier.height(Space.m))

            // **Sized to its label, not the width of the screen.** D137: a
            // full width outlined bar is the way back and nothing else, and
            // under a full width filled action it is a second bar of which
            // only one leaves. #371 item 5, and it is retroactive per rule 14.
            QuietButton(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(StartProjectTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * A run of shipped templates as rows.
 *
 * **Rows rather than cards**, per rule 22 and section 7: sixteen of these is a
 * list somebody scans for the one they came for, and a card row plus its gap
 * costs nearly twice what a row does. The name is what they are scanning, the
 * line under it says what the process is, and the step count sits in mono at the
 * end because a count is data.
 *
 * **The chevron is honest**: choosing one starts the project and opens it.
 */
@Composable
private fun Templates(
    templates: List<TemplateCatalog.ProjectTemplate>,
    onChoose: (TemplateCatalog.ProjectTemplate) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Block(padding = Space.none) {
        templates.forEachIndexed { index, template ->
            ListRow(
                // **Isolated even though it is catalog copy rather than the
                // person's own words.** The sixteen are English until #62
                // translates them, so in an Arabic layout every one of these is
                // an opposite-direction run, which is the case section 15's
                // rule exists for. A name ending in a bracket or a digit
                // reorders against the layout without it.
                title = Bidi.isolate(template.name),
                support = template.subtitle.takeIf { it.isNotBlank() }
                    ?.let { Bidi.isolate(it) },
                value = template.steps.size
                    .takeIf { it > 0 }
                    ?.let { strings("projects.step_count", "count" to it) },
                // **The count sits beside the first line of the name.** These
                // names wrap to three lines and the subtitle to three more, so
                // a centered trailing floated in the middle of a tall row,
                // reading as part of neither. Money's wrapping amount hit the
                // same collision and this is that fix. #376.
                valueAtTop = true,
                isDoor = true,
                onClick = { onChoose(template) },
                modifier = Modifier.testTag(StartProjectTags.template(template.id)),
            )
            if (index < templates.lastIndex) RowDivider(inset = false)
        }
    }

    // **Said once for the group rather than once per row.** It is the same
    // sentence on every template that carries it, and repeating it under six
    // rows is section 15's exact complaint: three copies of the same two
    // hundred words with what each row actually said buried between them.
    //
    // **It still sits in front of the decision**, which is where rule 2 wants
    // it. A confidently wrong step in a Medicaid application costs somebody
    // real money, so the app says the steps stay general rather than inventing
    // a rule for a state it knows nothing about.
    if (templates.any { it.stateVariance }) {
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["projects.state_varies"],
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
        )
    }
}

/**
 * The person's own templates as rows, in the same shape as the shipped ones.
 *
 * The same shape because it is the same choice. What differs is the line under
 * the name, which says where it came from rather than what the process is: they
 * wrote it, so they already know what it is for.
 */
@Composable
private fun OwnTemplates(
    own: List<Repository.OwnTemplate>,
    onChoose: (Repository.OwnTemplate) -> Unit,
) {
    val strings = LocalStrings.current
    Block(padding = Space.none) {
        own.forEachIndexed { index, template ->
            ListRow(
                title = Bidi.isolate(template.name),
                support = strings[
                    if (template.derivedFromId != null) "library.derived" else "library.scratch"
                ],
                value = template.steps.size
                    .takeIf { it > 0 }
                    ?.let { strings("projects.step_count", "count" to it) },
                // **The count sits beside the first line of the name.** These
                // names wrap to three lines and the subtitle to three more, so
                // a centered trailing floated in the middle of a tall row,
                // reading as part of neither. Money's wrapping amount hit the
                // same collision and this is that fix. #376.
                valueAtTop = true,
                isDoor = true,
                onClick = { onChoose(template) },
                modifier = Modifier.testTag(StartProjectTags.ownTemplate(template.id)),
            )
            if (index < own.lastIndex) RowDivider(inset = false)
        }
    }
}

/**
 * Starting something the catalog does not cover.
 *
 * **The name is the only thing asked for**, because it is the only thing the app
 * needs and everything else is the person's to add as they learn it. A blank
 * project with a name is a working project, which is rule 13's "partial is a
 * finished state" applied to a whole record rather than to a field.
 *
 * **No card around it.** It was one, and a white block holding a heading, a
 * line, a field and a button sat at the bottom of a picker competing with the
 * templates it was an alternative to. Bare, under its own mono header, it reads
 * as the last option rather than as a seventeenth template.
 *
 * The action appears only once there is a name, because saving one with no name
 * would put an untitled row in a list whose whole job is to be scanned. It is
 * the one filled action on this screen, per law 1.
 */
@Composable
private fun OwnProject(onStart: (String) -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var name by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(false) }

    // **Prominent but compact until it is wanted.** #379 moved this to the
    // top because it was invisible at the bottom, and the first attempt put
    // the whole form up there: a text field and a button above everything
    // else, which pushed the later categories out of the list's composition
    // window entirely. A test caught that by failing to reach the papers
    // fold, which is a real person failing to reach it too.
    //
    // Closed it is one raised row that says what it offers. Open it is the
    // form. Either way it leads.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .raisedCard(Radius.cardLarge)
            .clip(Radius.cardLarge)
            .background(colors.card)
            .testTag(StartProjectTags.OWN),
    ) {
        ListRow(
            title = strings["projects.blank"],
            support = strings["projects.blank.aside"],
            isDoor = !open,
            onClick = if (open) null else { { open = true } },
            clickLabel = strings["projects.start"],
        )
        if (open) {
            Column(modifier = Modifier.padding(Space.cardPadding)) {
                HealthTrailTextField(
                    label = strings["projects.name"],
                    value = name,
                    onValueChange = { name = it },
                    hint = strings["projects.name.hint"],
                    fieldTestTag = StartProjectTags.OWN_NAME,
                )
                if (name.isNotBlank()) {
                    Spacer(Modifier.height(Space.m))
                    FilledButton(
                        label = strings["projects.start"],
                        onClick = { onStart(name.trim()) },
                        modifier = Modifier.fillMaxWidth().testTag(StartProjectTags.OWN_START),
                    )
                }
            }
        }
    }
}
