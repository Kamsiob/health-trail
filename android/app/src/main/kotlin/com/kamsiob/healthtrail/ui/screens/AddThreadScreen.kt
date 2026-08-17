package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Field

object AddThreadTags {
    const val STAGE = "add_thread"
    const val NAME = "add_thread_name"
    const val START = "add_thread_start"
    const val CANCEL = "add_thread_cancel"
}

/**
 * Starting a care thread from nothing. #349, D145.
 *
 * **Applying a situation was the only way a thread had ever come into being**,
 * so a person dealing with a landlord, a school, or an employer's leave
 * department had a real recurring situation that none of the fourteen
 * situations ever heard of, and it lived in the trail with no spine of its own.
 * The owner ruled that a person whose situation the templates do not cover must
 * not be locked out of a section of their own notebook. Threads a situation
 * creates are unchanged.
 *
 * **A name is the only thing asked for**, per rule 13 and the same shape
 * `OwnProject` and `NameSomethingElse` already have. A thread carries a start
 * date, an end, an end note and notes of its own, and every one of them is
 * either taken from today or left for later on the thread's own screen.
 *
 * **One question at a time**, per law 3, so this screen asks one and stops.
 * The lead line says what a thread is for in the words somebody would use out
 * loud, because rule 20 puts the app's own organizing scheme on the code rather
 * than on the person.
 */
@Composable
fun AddThreadScreen(
    onStart: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The thread being renamed, or null when this one is new. #371.
     *
     * **A thread could be started and never corrected.** Its label titles its
     * own screen, is a chip on the capture form, a filing target in the unfiled
     * tray and a line in the trail, so a thread named wrong was named wrong in
     * five places forever and the only escape was removing it and losing
     * everything filed under it. The same screen does both, because it asks the
     * same one question.
     */
    existing: Repository.CareThread? = null,
    /**
     * The words, so one screen can ask "what is this called" for more than one
     * kind of thing. #371.
     *
     * **One screen rather than a second near identical one.** A chapter's
     * rename asks exactly this question with exactly this shape, and two files
     * asking one question is how two screens drift apart, which is the argument
     * `SectionScaffold` and the `Card` inside `ChapterScreen` both already make.
     */
    titleKey: String? = null,
    labelKey: String = "threads.new.name",
    hintKey: String? = "threads.new.hint",
    saveKey: String? = null,
    /**
     * The lead line under the title, so a screen asking about something other
     * than a thread does not explain what a care thread is.
     */
    leadKey: String = "threads.new.lead",
    /**
     * The section whose tab chip this wears, per D151.
     *
     * **A rename belongs to the thing being renamed.** A chapter's rename wears
     * the chapters chip, because that is where somebody came from and where
     * they are going back to. Defaults to threads, which is what every caller
     * before #374 wanted.
     */
    section: Repository.Section = Repository.Section.THREADS,
    /**
     * What the field starts with when there is no [existing] thread to read it
     * from. #374.
     *
     * **Because this screen now renames things that are not threads.**
     * `renameChapter` and `renameProject` have sat in the repository with no
     * caller since the day they were written, waiting on a full screen surface
     * that `NotebookShell` had no room for. It has room now.
     */
    initialName: String? = null,
    /**
     * Whether the field is one line.
     *
     * **False where the thing being named is a sentence rather than a name.** A
     * question's own words are what somebody reads out in an appointment, and
     * they do not fit on one line at any font size worth using. #374.
     */
    singleLine: Boolean = true,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by rememberSaveable(existing?.id, initialName) {
        mutableStateOf(existing?.label ?: initialName.orEmpty())
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(AddThreadTags.STAGE),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                FormHeader(
                    title = strings[
                        titleKey ?: if (existing == null) "threads.new" else "threads.rename",
                    ],
                    // The lead is an Aside now, on the section's wash with
                    // its own icon, rather than the smallest gray line under the
                    // title. D172, and the approved medication mockup.
                    lead = null,
                    section = section,
                )
                    Spacer(Modifier.height(Space.m))
                    FactBlock(
                        label = null,
                        text = strings[leadKey],
                        tone = BlockTone.Section,
                        mark = Symbols.of(section),
                        hue = hueFor(section),
                    )

                Spacer(Modifier.height(Space.l))
                // **The field's label is not the heading again**, which is the
                // defect #341 took out of four screens. The heading asks what
                // keeps coming up; the field says what to type.
                Field(
                    label = strings[labelKey],
                    value = name,
                    onValueChange = { name = it },
                    singleLine = singleLine,
                    fieldTestTag = AddThreadTags.NAME,
                    support = hintKey?.let { strings[it] },
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            Action(
                label = strings[
                    saveKey
                        ?: if (existing == null) "threads.new.start" else "threads.rename.save",
                ],
                onClick = { onStart(name.trim()) },
                // **A name is the one thing this cannot do without**, because a
                // thread with no name is a spine with nothing written on it and
                // the schema requires the label. Nothing else is asked at all.
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddThreadTags.START), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.sm))

            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddThreadTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}
