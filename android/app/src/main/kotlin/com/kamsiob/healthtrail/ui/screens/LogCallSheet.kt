package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object LogCallTags {
    const val SHEET = "log-call-sheet"
    const val WHO = "log-call-who"
    const val WORDS = "log-call-words"
    const val SAVE = "log-call-save"
}

/**
 * Writing down a call from inside the project it was about.
 *
 * `DESIGN.md` 20.5, screen 9. **Pre-answered with the project**, which is the
 * whole reason it lives here: somebody who has just put the phone down should
 * not have to say which of five processes the call was about, and a capture
 * that asks them to is a capture they will do later and then not do.
 *
 * **The words are the point.** What the office said, as close to their words as
 * the person can get it, because that sentence is what they read back at the
 * start of the next call. It is stored exactly as typed and never summarized.
 *
 * **Who they spoke to is separate and optional.** A name and a role is what
 * makes the record usable months later, and not having one is ordinary: plenty
 * of calls are answered by whoever picked up.
 *
 * **The date is today and is not asked for**, like the standing sheet, and it
 * stays editable from the entry itself, rule 17.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCallSheet(
    /** The project this call was about, shown so nobody logs it on the wrong one. */
    projectName: String,
    onSave: (who: String, words: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()

    // **A call logged into a sheet that dies with the process is a call
    // nobody wrote down.** #371 item 7.
    var who by rememberSaveable { mutableStateOf("") }
    var words by rememberSaveable { mutableStateOf("") }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(LogCallTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // **Every sheet that carries a form scrolls**, or its own
                // action is unreachable on a short screen. The standing sheet
                // proved it on 2026-08-12: its Save sat below the fold with no
                // way to reach it, and the old test device was tall enough to
                // hide that for the life of the screen. #129.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.log_call.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))
            // **The project shown back**, so somebody logging three calls in an
            // afternoon never logs one against the wrong process. The same
            // reason the answer sheet shows the question it is answering.
            Text(
                text = Bidi.isolate(projectName),
                style = HealthTrail.type.bodyL,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.log_call.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["project.log_call.title"],
                value = words,
                onValueChange = { words = it },
                hint = strings["project.log_call.words.hint"],
                singleLine = false,
                imeAction = ImeAction.Default,
                fieldTestTag = LogCallTags.WORDS,
            )

            Spacer(Modifier.height(Space.m))

            DictatableField(
                label = strings["project.log_call.who"],
                value = who,
                onValueChange = { who = it },
                hint = strings["project.log_call.who.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = LogCallTags.WHO,
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["project.log_call.save"],
                // Their words are the one thing this cannot be saved without.
                // Everything else about a call can be filled in later or never.
                enabled = words.isNotBlank(),
                onClick = { onSave(who.trim(), words.trim()) },
                modifier = Modifier.fillMaxWidth().testTag(LogCallTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
