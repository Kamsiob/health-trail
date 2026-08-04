package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.items
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object LibraryTags {
    const val NAME = "template_library"
    const val EMPTY_OWN = "library_empty_own"
    fun shipped(id: String) = "library_shipped_$id"
    fun own(id: String) = "library_own_$id"
    fun project(id: String) = "library_project_$id"
}

/**
 * What a template is, and what it has actually produced.
 *
 * **A menu of sixteen processes says nothing about a person's notebook.** The
 * owner's word for what this had to become is state: which templates are in
 * use, when they were applied, and what each one created, with links to every
 * item. A list that only offers is a catalog; a list that also reports is a
 * library.
 *
 * **Their own templates come first.** A person who has made one has learned
 * something the catalog did not know, and putting the sixteen shipped ones
 * above it would say the opposite.
 *
 * **A shipped template is never edited in place.** Changing one makes the
 * person's own copy with `derived_from_id` pointing back, so a catalog update
 * in a later version cannot overwrite what they wrote, and the copy still knows
 * what it grew out of. That is the schema's own design and it had no writer
 * until 2026-08-03.
 *
 * Composed from Display L, Body M, the group header 5.13, the dense row 11.3,
 * cards 5.3 for the template itself, and the Mono style. Nothing new.
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

    SectionScaffold(
        name = LibraryTags.NAME,
        title = strings["library.title"],
        subtitle = strings["library.subtitle"],
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
        item {
            GroupHeader(labelKey = "library.own")
            Spacer(Modifier.height(Space.headerGap))
        }

        if (own.isEmpty()) {
            item {
                Text(
                    text = strings["library.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(LibraryTags.EMPTY_OWN),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else {
            items(own, key = { it.id }) { template ->
                TemplateCard(
                    title = template.name,
                    // **Lineage in words.** A copy of a shipped template says
                    // so, and one built from nothing says that instead. Both
                    // are facts about where the person's own work came from.
                    provenance = if (template.derivedFromId != null) {
                        strings["library.derived"]
                    } else {
                        strings["library.scratch"]
                    },
                    steps = template.steps.size,
                    // Own templates carry their own id on a project only once
                    // a project has been started from one, which nothing does
                    // yet: the start screen offers them and `startProject`
                    // records the id it was given.
                    started = projects.filter { it.templateId == template.id },
                    onOpenProject = onOpenProject,
                    modifier = Modifier.testTag(LibraryTags.own(template.id)),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            item { Spacer(Modifier.height(Space.sectionGap)) }
        }

        item {
            GroupHeader(labelKey = "library.shipped")
            Spacer(Modifier.height(Space.headerGap))
        }

        items(shipped, key = { it.id }) { template ->
            TemplateCard(
                title = template.name,
                provenance = template.subtitle,
                steps = template.steps.size,
                started = projects.filter { it.templateId == template.id },
                onOpenProject = onOpenProject,
                modifier = Modifier.testTag(LibraryTags.shipped(template.id)),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        item { Spacer(Modifier.height(Space.l)) }
    }
}

/**
 * One template, and what it has produced.
 *
 * **A card rather than a dense row**, per 11.4, because it carries three or
 * more lines the person reads: the name, where it came from, what it has made,
 * and the projects themselves. The projects inside it are dense rows, which is
 * 11.10's inline preview: a link that shows what it points at, and one tap
 * cheaper than a name that does not.
 *
 * **Nothing started from it says so.** A blank there would be the absence of a
 * line carrying the meaning, which is D75.
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
            .clip(Radius.card)
            .background(colors.card)
            .padding(Space.cardPadding),
    ) {
        Text(
            text = strings("projects.step_count", "count" to steps),
            style = HealthTrail.type.mono,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.xs))
        Text(text = title, style = HealthTrail.type.displayS, color = colors.ink)

        if (provenance.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            Text(text = provenance, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        Spacer(Modifier.height(Space.sm))
        Text(
            text = if (started.isEmpty()) {
                strings["library.never_used"]
            } else {
                strings("library.in_use", "count" to started.size)
            },
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
        )

        started.forEachIndexed { index, project ->
            DenseRow(
                title = project.name,
                subtitle = strings["projects.status.${project.status}"],
                chevron = true,
                divider = index < started.lastIndex,
                onClick = { onOpenProject(project) },
                modifier = Modifier.testTag(LibraryTags.project(project.id)),
            )
        }
    }
}
