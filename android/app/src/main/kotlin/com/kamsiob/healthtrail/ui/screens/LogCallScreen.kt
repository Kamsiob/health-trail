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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object LogCallTags {
    const val ROOT = "log_call_root"
    const val WHO = "log_call_who"
    const val NOTE = "log_call_note"
    const val SAVE = "log_call_save"
    const val CANCEL = "log_call_cancel"
}

/** What a logged call carries. Both fields can be blank. */
data class CallDraft(val who: String, val note: String)

/**
 * Logging a call, which is the most common thing this app is used for.
 *
 * **Every field is optional and saving a blank call is allowed.** A person who
 * hangs up and taps this has already done the useful thing, which is recording
 * that the call happened and when. What was said can be added later or never.
 * Partial is a finished state.
 *
 * The note grows with what is typed rather than sitting at a fixed height,
 * because a fixed height silently teaches people to write less.
 *
 * Composed from Display L, the text field from section 5.9, one filled button,
 * and one text action. Nothing new was introduced.
 */
@Composable
fun LogCallScreen(
    onSave: (CallDraft) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var who by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(LogCallTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = strings["capture.call.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["entry.optional"],
                    style = HealthTrail.type.mono,
                    color = colors.ink3Text,
                )

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["capture.call.who"],
                    hint = strings["capture.call.who.hint"],
                    value = who,
                    onValueChange = { who = it },
                    fieldTestTag = LogCallTags.WHO,
                )

                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["capture.call.note"],
                    hint = strings["capture.call.note.hint"],
                    value = note,
                    onValueChange = { note = it },
                    singleLine = false,
                    imeAction = ImeAction.Default,
                    fieldTestTag = LogCallTags.NOTE,
                )

                Spacer(Modifier.height(Space.l))
            }

            FilledButton(
                label = strings["common.save"],
                onClick = { onSave(CallDraft(who.trim(), note.trim())) },
                modifier = Modifier.fillMaxWidth().testTag(LogCallTags.SAVE),
            )

            Spacer(Modifier.height(Space.sm))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testTag(LogCallTags.CANCEL),
            )
        }
    }
}
