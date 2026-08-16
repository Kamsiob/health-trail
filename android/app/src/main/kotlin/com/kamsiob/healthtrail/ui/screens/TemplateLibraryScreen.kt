package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.ZoneId
import java.time.Instant

object LibraryTags {
    const val NAME = "template_library"
    const val EMPTY_OWN = "library_empty_own"
    fun shipped(id: String) = "library_shipped_$id"
    fun own(id: String) = "library_own_$id"
    fun project(id: String) = "library_project_$id"
    fun category(key: String) = "library_category_$key"
}

/**
 * The order the four kinds are offered in, matching the picker exactly.
 *
 * Two screens showing the same sixteen things in two orders is a defect, per
 * 13.2's rule about a pattern appearing twice in two forms.
 */
private val CATEGORY_ORDER = listOf("paying", "challenge", "moving", "papers")

/**
 * What a template is, and what it has actually produced.
 *
 * **A menu of sixteen processes says nothing about a person's notebook.** The
 * owner's word for what this had to become is state: which templates are in use,
 * when they were applied, and what each one created, with links to every item. A
 * list that only offers is a catalog; a list that also reports is a library.
 *
 * **The one thing, law 1: what has actually produced something.** It was
 * sixteen shipped cards under however many of the person's own, every one the
 * same weight, and in a real notebook fifteen of them say "nothing started from
 * this yet". A wall of sixteen identical cards fifteen of which report nothing
 * is the shape rule 15 calls uniform weight: it pushes the whole job of sorting
 * onto somebody who is already tired.
 *
 * **So in-use leads, and everything else folds by the same four kinds the
 * picker uses.** A template that has produced a project is a card, because it
 * carries the name, where it came from, what it made, and the projects
 * themselves, which is the three-or-more lines rule 22 asks a card for. One that
 * has produced nothing is a name and a line, which is a row.
 *
 * **Their own templates come first among the in-use ones.** A person who has
 * made one has learned something the catalog did not know.
 *
 * **A shipped template is never edited in place.** Changing one makes the
 * person's own copy with `derived_from_id` pointing back, so a catalog update in
 * a later version cannot overwrite what they wrote, and the copy still knows
 * what it grew out of. That is the schema's own design and it had no writer
 * until 2026-08-03.
 */
@Composable
fun TemplateLibraryScreen(
    shipped: List<TemplateCatalog.ProjectTemplate>,
    own: List<Repository.OwnTemplate>,
    /** Every project in the notebook, so each template can say what it produced. */
    projects: List<Repository.Project>,
    onOpenProject: (Repository.Project) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var openCategories by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var unusedOwnOpen by rememberSaveable { mutableStateOf(false) }

    fun startedFrom(templateId: String) = projects.filter { it.templateId == templateId }

    val ownUsed = own.filter { startedFrom(it.id).isNotEmpty() }
    val ownUnused = own.filter { startedFrom(it.id).isEmpty() }
    val shippedUsed = shipped.filter { startedFrom(it.id).isNotEmpty() }
    val shippedUnused = shipped.filter { startedFrom(it.id).isEmpty() }

    SectionScaffold(
        name = LibraryTags.NAME,
        // The chip says where you are, the heading says what you came for.
        title = strings["nav.more"],
        headingKey = "library.title",
        subtitle = strings["library.subtitle"],
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
        // **What is in use, at the top, as cards.** This is the answer to why
        // somebody opens a library rather than the picker: not "what could I
        // start" but "what did I start, and where did it go".
        if (ownUsed.isNotEmpty() || shippedUsed.isNotEmpty()) {
            item(key = "used_head") {
                GroupHeader(labelKey = "library.used")
                Spacer(Modifier.height(Space.headerGap))
            }
            ownUsed.forEach { template ->
                item(key = "u_${template.id}") {
                    TemplateCard(
                        title = Bidi.isolate(template.name),
                        // **Lineage in words.** A copy of a shipped template
                        // says so, and one built from nothing says that
                        // instead. Both are facts about where the person's own
                        // work came from.
                        provenance = ownProvenance(template, strings),
                        steps = template.steps.size,
                        started = startedFrom(template.id),
                        onOpenProject = onOpenProject,
                        modifier = Modifier.testTag(LibraryTags.own(template.id)),
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            shippedUsed.forEach { template ->
                item(key = "u_${template.id}") {
                    TemplateCard(
                        title = Bidi.isolate(template.name),
                        provenance = Bidi.isolate(template.subtitle),
                        steps = template.steps.size,
                        started = startedFrom(template.id),
                        onOpenProject = onOpenProject,
                        modifier = Modifier.testTag(LibraryTags.shipped(template.id)),
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item(key = "used_gap") { Spacer(Modifier.height(Space.sectionGap)) }
        }

        // **Their own, when they have made none or have made one nothing has
        // used yet.** With every one of theirs already in the group above, this
        // section says nothing that is not already on screen and is left out
        // rather than headed and empty.
        //
        // **The heading and its line stay when they have made none**, because
        // that line is how somebody learns making one is possible at all, which
        // is 13.5. It is the one place in the app that says so.
        if (own.isEmpty()) {
            item(key = "own_empty") {
                GroupHeader(labelKey = "library.own")
                Spacer(Modifier.height(Space.headerGap))
                Text(
                    text = strings["library.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(LibraryTags.EMPTY_OWN),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else if (ownUnused.isNotEmpty()) {
            item(key = "own_unused") {
                GroupHeader(labelKey = "library.own")
                Spacer(Modifier.height(Space.headerGap))
                FoldRowText(
                    label = strings["library.unused"],
                    expanded = unusedOwnOpen,
                    onToggle = { unusedOwnOpen = !unusedOwnOpen },
                    count = ownUnused.size.toString(),
                    modifier = Modifier.testTag(LibraryTags.category("own")),
                )
                Spacer(Modifier.height(Space.cardGap))
                if (unusedOwnOpen) {
                    GroupedSurface {
                        ownUnused.forEachIndexed { index, template ->
                            DenseRow(
                                title = Bidi.isolate(template.name),
                                subtitle = ownProvenance(template, strings),
                                trailing = strings(
                                    "projects.step_count",
                                    "count" to template.steps.size,
                                ),
                                divider = index < ownUnused.lastIndex,
                                modifier = Modifier.testTag(LibraryTags.own(template.id)),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **The sixteen, folded by the same four kinds the picker uses**, each
        // named and counted. Nothing is hidden and nothing is a wall.
        item(key = "shipped_head") {
            GroupHeader(labelKey = "library.shipped")
            Spacer(Modifier.height(Space.headerGap))
        }

        CATEGORY_ORDER.forEach { key ->
            val inCategory = shippedUnused.filter { it.category == key }
            if (inCategory.isEmpty()) return@forEach
            item(key = "cat_$key") {
                FoldRowText(
                    label = strings["projects.category.$key"],
                    expanded = key in openCategories,
                    onToggle = {
                        openCategories = if (key in openCategories) {
                            openCategories - key
                        } else {
                            openCategories + key
                        }
                    },
                    count = inCategory.size.toString(),
                    modifier = Modifier.testTag(LibraryTags.category(key)),
                )
                Spacer(Modifier.height(Space.cardGap))
                if (key in openCategories) {
                    GroupedSurface {
                        inCategory.forEachIndexed { index, template ->
                            // **No chevron and no handler.** These have started
                            // nothing, and there is no template detail screen:
                            // a chevron here would point at nothing, which is
                            // the dead end rule 18 forbids. Starting one is the
                            // picker's job, and it is one tap from Projects.
                            DenseRow(
                                // English until #62, so an opposite-direction
                                // run in an Arabic layout. Section 15.
                                title = Bidi.isolate(template.name),
                                subtitle = template.subtitle.takeIf { it.isNotBlank() }
                                    ?.let { Bidi.isolate(it) },
                                // **Uncapped, because this subtitle is a sentence somebody
                                // reads to choose.** At one line every row ended mid-sentence,
                                // and at two they did it again the moment the system font
                                // reached 2.0. Any fixed cap truncates at some size.
                                subtitleMaxLines = Int.MAX_VALUE,
                                trailing = strings(
                                    "projects.step_count",
                                    "count" to template.steps.size,
                                ),
                                divider = index < inCategory.lastIndex,
                                modifier = Modifier.testTag(LibraryTags.shipped(template.id)),
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item(key = "tail") { Spacer(Modifier.height(Space.l)) }
    }
}

/**
 * One template that has produced something, and what it produced.
 *
 * **A card rather than a row**, per rule 22, because it carries three or more
 * lines somebody reads: the step count, the name, where it came from, how many
 * it has started, and the projects themselves. The projects inside it are dense
 * rows, which is a link that shows what it points at and one tap cheaper than a
 * name that does not.
 */
@Composable
private fun TemplateCard(
    title: String,
    provenance: String,
    steps: Int,
    started: List<Repository.Project>,
    onOpenProject: (Repository.Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.card)
            .padding(Space.cardPadding),
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
            Text(
                text = strings("projects.step_count", "count" to steps),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            // bidi-ok: every caller isolates before handing it here.
            Text(text = title, style = HealthTrail.type.displayS, color = colors.ink)

            if (provenance.isNotBlank()) {
                Spacer(Modifier.height(Space.xs))
                // bidi-ok: every caller isolates before handing it here.
                Text(text = provenance, style = HealthTrail.type.bodyM, color = colors.ink2)
            }

            Spacer(Modifier.height(Space.sm))
            Text(
                text = strings("library.in_use", "count" to started.size),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }

        started.forEachIndexed { index, project ->
            DenseRow(
                title = Bidi.isolate(project.name),
                subtitle = strings["projects.status.${project.status}"],
                chevron = true,
                divider = index < started.lastIndex,
                onClick = { onOpenProject(project) },
                modifier = Modifier.testTag(LibraryTags.project(project.id)),
            )
        }
    }
}

/**
 * Where one of the person's own templates came from, and when they saved it.
 *
 * **The date is what tells two saves of one project apart.** #315: saving a
 * project as a template twice makes two rows with the same name, and that is
 * not itself wrong, because the second save is a different shape: the road has
 * moved, steps have been added, papers have been named. Somebody may want both.
 * **What was wrong is that the library gave them no way to tell which is
 * which.** D124, and it is rule 23 picking the option that costs the person
 * least: nothing is discarded and nothing is asked of them at the moment of
 * saving.
 *
 * **The date it was saved, at the precision a save has**, which is a day.
 */
@Composable
private fun ownProvenance(
    template: Repository.OwnTemplate,
    strings: Strings,
): String = Bidi.join(
    strings[if (template.derivedFromId != null) "library.derived" else "library.scratch"],
    strings(
        "library.saved_on",
        "date" to EventDateText.render(
            strings,
            Instant.ofEpochMilli(template.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString(),
        ),
    ),
)
