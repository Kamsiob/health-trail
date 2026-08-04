package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ExportTags {
    const val NAME = "export"
    const val PASSPHRASE = "export_passphrase"
    const val AGAIN = "export_again"
    const val SAVE = "export_save"
    const val STATUS = "export_status"
    const val REVEAL = "export_reveal"
    const val AGAIN_ACTION = "export_again_action"
}

/** What the export screen is doing right now. */
enum class ExportState { READY, WORKING, DONE, FAILED }

/**
 * Exporting the notebook.
 *
 * **This file is the only recovery path from key loss**, per D24, which makes
 * it the most consequential thing the app writes. `contract/export-format.md`
 * section 4 specifies what this screen has to say, and it says it in those
 * words: if the passphrase is lost the file cannot be recovered, there is no
 * server, no recovery code, and no backdoor. **Before the person commits, not
 * after.**
 *
 * **The passphrase is typed twice.** Nothing else in this app asks anyone to
 * repeat themselves, and this does, because a typo here is not a typo: it is a
 * file that looks like a backup and can never be opened. That is the one
 * failure worth an extra field.
 *
 * **There is no unencrypted export, and this screen offers no way to ask for
 * one.** Version 1 offered it plainly, and the reasoning was right for what
 * version 1 wrote: the payload was the device keyed SQLCipher file, so a plain
 * container still held bytes no other machine could read. Making the export
 * portable changed what a plain one is. It is now a fully readable copy of the
 * whole record, and this screen is the only place it could have been asked
 * for, so this is where it stops. D67.
 *
 * **Nothing here reports success until the bytes are written.** "Saved" appears
 * after the file exists at the chosen place, never when the export began, and
 * a failure says plainly that nothing was saved and the notebook is untouched.
 */
@Composable
fun ExportScreen(
    state: ExportState,
    onExport: (passphrase: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Puts the screen back to its resting state so another copy can be saved. */
    onAgain: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var passphrase by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }

    val mismatch = again.isNotEmpty() && passphrase != again

    // **A space at either end is invisible and permanent.**
    //
    // Found by walking the screen on the phone: a passphrase can end up with a
    // trailing space without the person ever seeing one, because the field is
    // masked and a soft keyboard appends one after a completion or a swipe.
    // Both fields then look identical while differing, and the screen can only
    // say they do not match.
    //
    // The worse version is months later on another phone, where the same
    // invisible space means a correct passphrase is reported as wrong on the
    // one file that is the only way back. D24 makes this the most consequential
    // thing the app writes.
    //
    // **It says so rather than trimming.** Trimming would quietly change
    // somebody's secret, and a space someone chose on purpose is theirs. Naming
    // it turns an invisible failure into a visible one and leaves the decision
    // where it belongs.
    val edgeSpace = passphrase.isNotEmpty() && passphrase != passphrase.trim()
    val canEncrypt = passphrase.isNotEmpty() && passphrase == again
    val busy = state == ExportState.WORKING
    val done = state == ExportState.DONE

    // **Forgotten the moment the file exists.** Holding the passphrase in
    // composition after the export is finished keeps the app's most consequential
    // secret alive for no remaining purpose, and leaves it on screen behind
    // whatever the person does next.
    LaunchedEffect(done) {
        if (done) {
            passphrase = ""
            again = ""
            revealed = false
        }
    }

    SectionScaffold(
        name = ExportTags.NAME,
        title = strings["export.title"],
        subtitle = strings["export.lead"],
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
        // **The result replaces the form rather than sitting under it.** Left
        // in place, the screen said "Saved" in body text below two live buttons
        // and a passphrase still on display, which buries the one thing that
        // just happened beneath the machinery that did it and invites a second
        // file nobody asked for.
        if (done) {
            item {
                Text(
                    text = strings["export.done.title"],
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                    modifier = Modifier.testTag(ExportTags.STATUS),
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["export.done.where"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
                QuietButton(
                    label = strings["export.done.again"],
                    onClick = { onAgain() },
                    modifier = Modifier.fillMaxWidth().testTag(ExportTags.AGAIN_ACTION),
                )
                Spacer(Modifier.height(Space.l))
            }
            return@SectionScaffold
        }

        item {
            Text(
                text = strings["export.why"],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.sectionGap))

            GroupHeader(labelKey = "export.passphrase")
            Spacer(Modifier.height(Space.headerGap))

            HealthTrailTextField(
                label = strings["export.passphrase"],
                value = passphrase,
                onValueChange = { passphrase = it },
                hint = strings["export.passphrase.hint"],
                note = if (edgeSpace) strings["export.passphrase.edges"] else null,
                enabled = !busy,
                keyboardType = KeyboardType.Password,
                masked = !revealed,
                fieldTestTag = ExportTags.PASSPHRASE,
            )
            Spacer(Modifier.height(Space.m))

            // **No hint inside this one, deliberately.** D37 puts the
            // microcopy in the empty box because a bare label over a blank
            // field is an interrogation. Here the label is already the
            // instruction, and "Type it again" repeated inside the box would
            // be the same words in two slots, which section 1 bans by name.
            HealthTrailTextField(
                label = strings["export.passphrase.again"],
                value = again,
                onValueChange = { again = it },
                // Said only when the two actually differ, so it is a
                // correction rather than a warning hanging over an empty field.
                note = if (mismatch) strings["export.mismatch"] else null,
                enabled = !busy,
                keyboardType = KeyboardType.Password,
                masked = !revealed,
                imeAction = ImeAction.Done,
                fieldTestTag = ExportTags.AGAIN,
            )

            // **Hidden by default, with the person free to look.** Typing a
            // passphrase blind, twice, and being told only that the two do not
            // match is a trap for anybody tired, and this screen is used by
            // people who are. Concealing it is the safe default; refusing to
            // ever show it would just move the failure.
            Spacer(Modifier.height(Space.s))
            TextAction(
                label = if (revealed) strings["export.conceal"] else strings["export.reveal"],
                onClick = { revealed = !revealed },
                enabled = !busy,
                modifier = Modifier.testTag(ExportTags.REVEAL),
            )

            Spacer(Modifier.height(Space.m))
            // The sentence the format requires, in the words it requires,
            // before the person commits rather than after.
            Text(
                text = strings["export.warning"],
                style = HealthTrail.type.bodyM,
                color = colors.alertInk,
            )

            Spacer(Modifier.height(Space.l))
            FilledButton(
                label = strings["export.save"],
                onClick = { onExport(passphrase) },
                enabled = canEncrypt && !busy,
                modifier = Modifier.fillMaxWidth().testTag(ExportTags.SAVE),
            )

            // One line, and only when there is something true to say. DONE
            // never reaches here: it replaces the form above.
            val status = when (state) {
                ExportState.WORKING -> strings["export.working"]
                ExportState.FAILED -> strings["export.failed"]
                ExportState.DONE, ExportState.READY -> null
            }
            if (status != null) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = status,
                    style = HealthTrail.type.bodyL,
                    color = if (state == ExportState.FAILED) colors.alertInk else colors.ink,
                    modifier = Modifier.testTag(ExportTags.STATUS),
                )
            }

            Spacer(Modifier.height(Space.l))
        }
    }
}
