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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

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
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by rememberSaveable { mutableStateOf("") }

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
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["threads.new"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["threads.new.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))
                // **The field's label is not the heading again**, which is the
                // defect #341 took out of four screens. The heading asks what
                // keeps coming up; the field says what to type.
                HealthTrailTextField(
                    label = strings["threads.new.name"],
                    value = name,
                    onValueChange = { name = it },
                    hint = strings["threads.new.hint"],
                    fieldTestTag = AddThreadTags.NAME,
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["threads.new.start"],
                onClick = { onStart(name.trim()) },
                // **A name is the one thing this cannot do without**, because a
                // thread with no name is a spine with nothing written on it and
                // the schema requires the label. Nothing else is asked at all.
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddThreadTags.START),
            )

            Spacer(Modifier.height(Space.sm))

            TextAction(
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
