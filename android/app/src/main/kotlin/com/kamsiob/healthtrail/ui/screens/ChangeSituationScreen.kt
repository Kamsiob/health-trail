package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Eyebrow

object ChangeSituationTags {
    const val NAME = "change_situation"
    const val CURRENT = "change_situation_current"
    const val ACTION = "change_situation_action"
    const val CHOSEN = "change_situation_chosen"
    const val CHAPTER = "change_situation_chapter"
    const val DONE = "change_situation_done"
    const val SKIP = "change_situation_skip"
}

/**
 * How this notebook is set up, and changing it.
 *
 * `DESIGN.md` section 14 maps this to screen 19, chapters, and the reason is
 * the whole point of the screen: **a change of situation is a chapter
 * boundary.** Somebody changes this because care moved, and the app's own unit
 * for "where they were then" is a chapter.
 *
 * **The one thing, law 1: what this notebook is set up for right now.** Not the
 * fourteen options, which are one tap away and are the picker's job. Somebody
 * opening this is usually checking rather than changing, and the answer to
 * "which one did I pick" is the reason they came.
 *
 * **Nothing is destroyed and the screen says so in its own words**, which #202
 * requires. `applySituation` only sets a column and adds care threads: it has
 * never deleted anything, and it never will. **Saying that plainly is not
 * reassurance, it is the fact**, and it is the fact somebody hesitating over
 * this button needs, because "change how the notebook is set up" sounds like it
 * might throw away six months of notes.
 *
 * **The new chapter is offered, never made for them.** The app files nothing on
 * its own anywhere else and this is no exception: a chapter created because the
 * person changed a setting would be the app asserting they moved, which is a
 * small false statement about somebody's own record. So it is a field they may
 * leave empty, and leaving it empty is a complete answer, per rule 13.
 *
 * **One filled action**, and it is the one that ends the flow. Choosing a
 * different setting is a smaller move that opens the picker, so it wears the
 * outlined costume, and it is a verb.
 */
@Composable
fun ChangeSituationScreen(
    /** What the notebook is set up for now, or null when nobody chose one. */
    current: TemplateCatalog.Situation?,
    /** What they picked this time, or null while they have not opened the picker. */
    chosen: TemplateCatalog.Situation?,
    onOpenPicker: () -> Unit,
    /** Applies [chosen], and starts a chapter with this name when there is one. */
    onApply: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var chapterName by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ChangeSituationTags.NAME,
        title = strings["nav.more"],
        headingKey = "situation.change.title",
        subtitle = strings["situation.change.lead"],
        section = null,
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
        item(key = "current") {
            // **"Right now" rather than "set up for".** The heading already
            // says "How this notebook is set up", and an eyebrow repeating it
            // is a label carrying nothing, which is what section 5.13 warns
            // the heading itself against. This one adds the fact that matters:
            // it is the current answer and it is meant to change.
            Eyebrow(text = strings["situation.change.current"])
            Spacer(Modifier.height(Space.headerGap))
            if (current == null) {
                // **Not an empty state and not a prompt to finish setting up.**
                // A notebook with no setting is a working notebook with every
                // section in it, which is what setup already promised when it
                // offered to be skipped. Rule 13.
                Text(
                    text = strings["situation.change.none"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(ChangeSituationTags.CURRENT),
                )
            } else {
                Text(
                    text = Bidi.isolate(current.name),
                    style = HealthTrail.type.hero,
                    color = colors.ink,
                    modifier = Modifier.testTag(ChangeSituationTags.CURRENT),
                )
                current.subtitle.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = Bidi.isolate(it),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }
            }
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **What it will become, under its own heading, and only once they have
        // picked one.** It read "Right now: Assisted living" the moment the
        // picker closed, while the notebook was still on the old setting and
        // nothing had been written. That is the screen stating something untrue
        // about the record, which is the one thing this app must never do, and
        // it is exactly the kind of thing that reads as correct in a demo.
        chosen?.let { next ->
            item(key = "chosen") {
                Eyebrow(text = strings["situation.change.to"])
                Spacer(Modifier.height(Space.headerGap))
                Text(
                    text = Bidi.isolate(next.name),
                    // **What it is now leads and what it will become supports
                    // it**, which is what this screen's own prose says it does.
                    // Both were at hero, so in the ordinary state, immediately
                    // after the picker closes, the screen showed three lines
                    // within a point of each other before any body text. Law 1.
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                    modifier = Modifier.testTag(ChangeSituationTags.CHOSEN),
                )
                next.subtitle.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = Bidi.isolate(it),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **The boundary, stated before the button rather than after it.** This
        // is the sentence #202 asks for, and it is where somebody hesitating is
        // looking: immediately above the thing they are hesitating over.
        item(key = "boundary") {
            Eyebrow(text = strings["situation.change.boundary"])
            Spacer(Modifier.height(Space.headerGap))
            Text(
                text = strings["situation.change.boundary.body"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **The chapter is offered only once a different setting is chosen**,
        // because until then there is nothing to be a boundary of. Offering it
        // first would ask somebody checking which setting they picked to
        // account for where the person is living.
        if (chosen != null) {
            item(key = "chapter") {
                // **No mono header over it.** The field's own label is
                // "Where they are now" and a header saying the same three words
                // directly above it is 5.13's heading that carries nothing.
                // Every other form in the app labels its fields and stops.
                HealthTrailTextField(
                    label = strings["situation.change.chapter"],
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    hint = strings["situation.change.chapter.hint"],
                    fieldTestTag = ChangeSituationTags.CHAPTER,
                )
                Spacer(Modifier.height(Space.xs))
                // Said plainly rather than left to be inferred from an empty
                // field, so somebody whose person has not moved knows the
                // field is not waiting on them.
                Text(
                    text = strings["situation.change.chapter.skip"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        item(key = "actions") {
            if (chosen == null) {
                QuietButton(
                    label = strings["situation.change.action"],
                    onClick = onOpenPicker,
                    modifier = Modifier.testTag(ChangeSituationTags.ACTION),
                )
            } else {
                FilledButton(
                    label = strings["situation.change.done"],
                    onClick = { onApply(chapterName.trim().takeIf { it.isNotBlank() }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ChangeSituationTags.DONE),
                )
                Spacer(Modifier.height(Space.cardGap))
                QuietButton(
                    label = strings["situation.change.action"],
                    onClick = onOpenPicker,
                    modifier = Modifier.testTag(ChangeSituationTags.SKIP),
                )
            }
            Spacer(Modifier.height(Space.l))
        }
    }
}

/** What the shell needs to know to finish a change of situation. */
data class SituationChange(
    val situation: TemplateCatalog.Situation,
    /** The chapter to start, or null because they have not moved. */
    val chapterName: String?,
)
