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
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.Page

object ExportTags {
    const val NAME = "export"
    const val PASSPHRASE = "export_passphrase"
    const val AGAIN = "export_again"
    const val HINT = "export_hint"
    const val SAVE = "export_save"
    const val STATUS = "export_status"
    const val REVEAL = "export_reveal"
    const val LAST_SAVED = "export_last_saved"
    const val AGAIN_ACTION = "export_again_action"
    const val MISSING = "export_missing"
}

/** What the export screen is doing right now. */
enum class ExportState { READY, WORKING, DONE, FAILED }

/**
 * Exporting the notebook.
 *
 * **This file is the only recovery path from key loss**, per D24, which makes
 * it the most consequential thing the app writes. `contract/EXPORT-FORMAT.md`
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
 *
 * **And "Saved" is not always the whole of it.** An export can succeed and still
 * be short of something, which is what [missingAttachments] carries. Saying only
 * "Saved" in that case is the defect #332 is about, moved from the code into the
 * copy: the person finds out on the new phone, with the old one gone.
 */
@Composable
fun ExportScreen(
    state: ExportState,
    onExport: (passphrase: String, hint: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Puts the screen back to its resting state so another copy can be saved. */
    onAgain: () -> Unit = {},
    /**
     * How many attached files the export could not find on this phone.
     *
     * **Zero on every archive this project has made**, and zero whenever
     * nothing has gone wrong, so this is a state that has never yet been seen
     * rather than one somebody meets routinely. It ships built anyway, per rule
     * 11, because the day it is not zero is the day somebody's storage failed
     * and the archive is the thing standing between them and the record.
     */
    missingAttachments: Int = 0,
    /**
     * When an export last finished and was read back, or null. #413.
     *
     * **A fact about the file, and rule 13 is why it is only that.** No score,
     * no percentage, no meter, no prompt to do better, and no color that reads
     * as a warning. Somebody who has not saved a copy in a year is told when
     * they last did, not that they have been careless.
     */
    lastExportAt: Long? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val lastSaved = lastExportAt?.let { millis ->
        val day = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        strings(
            "export.lastsaved",
            "date" to com.kamsiob.healthtrail.time.EventDateText.render(
                strings, com.kamsiob.healthtrail.time.Edtf.day(day).canonical,
            ),
        )
    }

    var passphrase by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }

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
            hint = ""
            revealed = false
        }
    }

    Page(
        title = strings["export.title"],
        onBack = onBack,
        backLabel = strings["section.back.more"],
        modifier = modifier.testTag(SectionTags.root(ExportTags.NAME)),
        eyebrow = strings["nav.more"],
        subtitle = strings["export.lead"],
    ) {
        // **When a copy was last saved, stated as a fact and nothing more.**
        // #413. Rule 13 forbids the nag, not the date: no score, no
        // percentage, no meter, no prompt to do better, and deliberately the
        // scheme's quiet ink rather than anything that reads as a warning.
        // Somebody who has not saved a copy in a year is told when they last
        // did, not that they have been careless about it.
        //
        // **Absent rather than "never" when there is none.** A first export is
        // not a lapse, and an empty slot reads as "not yet".
        if (lastSaved != null && !done) {
            item {
                Text(
                    text = lastSaved,
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(ExportTags.LAST_SAVED),
                )
                Spacer(Modifier.height(Space.m))
            }
        }

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

                // **What the export could not find, said here rather than
                // discovered at restore.** `Attachments.all` lists files rather
                // than rows, so a live attachment row whose bytes are gone
                // shipped as a row with no file and the archive could not be
                // opened again, while this screen said "Saved" and nothing
                // else. #332.
                //
                // **It sits under "Saved" rather than replacing it**, because
                // the file was written and the rest of the notebook is in it.
                // Turning a successful export into a failure would be as
                // dishonest in the other direction, and it would leave somebody
                // with no archive at all.
                if (missingAttachments > 0) {
                    Spacer(Modifier.height(Space.sectionGap))
                    Eyebrow(text = strings["export.done.missing.header"])
                    Spacer(Modifier.height(Space.headerGap))
                    Text(
                        text = strings("export.done.missing", "count" to missingAttachments),
                        style = HealthTrail.type.bodyL,
                        color = colors.alertInk,
                        modifier = Modifier.testTag(ExportTags.MISSING),
                    )
                    Spacer(Modifier.height(Space.s))
                    // **The consequence, plainly, and it changed on
                    // 2026-08-10.** Until then an archive naming a file it did
                    // not carry was one this app refused to open, and the copy
                    // said to keep an earlier one. The manifest carries the
                    // missing list now, so the archive restores and the entries
                    // arrive with their names and dates. The sentence has to
                    // say that instead, because copy that overstates a loss is
                    // its own defect. #332.
                    Text(
                        text = strings["export.done.missing.keep"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                }

                Spacer(Modifier.height(Space.l))
                Action(
                    label = strings["export.done.again"],
                    onClick = { onAgain() },
                    modifier = Modifier.testTag(ExportTags.AGAIN_ACTION),
                )
            }
            return@Page
        }

        item {
            Text(
                text = strings["export.why"],
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.sectionGap))

            Eyebrow(text = strings["export.passphrase"])
            Spacer(Modifier.height(Space.headerGap))

            DictatableField(
                label = strings["export.passphrase"],
                value = passphrase,
                onValueChange = { passphrase = it },
                // **The warning takes the line from the example**, rather than
                // stacking under it: a field carries one supporting line, and
                // where a space at either end could cost somebody their archive
                // that is the line worth having. `docs/V4.md` 2.1.
                support = if (edgeSpace) {
                    strings["export.passphrase.edges"]
                } else {
                    strings["export.passphrase.hint"]
                },
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
            DictatableField(
                label = strings["export.passphrase.again"],
                value = again,
                onValueChange = { again = it },
                enabled = !busy,
                keyboardType = KeyboardType.Password,
                masked = !revealed,
                imeAction = ImeAction.Done,
                fieldTestTag = ExportTags.AGAIN,
                support = if (mismatch) strings["export.mismatch"] else null,
            )

            // **Hidden by default, with the person free to look.** Typing a
            // passphrase blind, twice, and being told only that the two do not
            // match is a trap for anybody tired, and this screen is used by
            // people who are. Concealing it is the safe default; refusing to
            // ever show it would just move the failure.
            Spacer(Modifier.height(Space.s))
            Action(
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

            Spacer(Modifier.height(Space.sectionGap))

            // **The reminder, and what it costs, in that order.**
            //
            // `contract/DATA-CONTRACT.md` 8.1 requires it and requires the app
            // to say plainly that anyone holding the file can read it. It sits
            // in the outer manifest in the clear, which is the only place a
            // reminder can sit and still be a reminder: one inside the
            // encryption would need the passphrase to read.
            //
            // **The field says what to write in it rather than what not to.**
            // "Where the passphrase is written down, not the passphrase" is the
            // whole instruction, and it teaches the safe answer instead of
            // leaving somebody to invent one and then be told off.
            DictatableField(
                label = strings["export.hint.label"],
                value = hint,
                onValueChange = { hint = it },
                // Said when it is true, which is the one case worth interrupting
                // for: a reminder that contains the passphrase turns an
                // encrypted archive into an unencrypted one for anybody who
                // opens the outer layer. It does not block saving, per rule 13,
                // because this is the person's own record and their call. It
                // takes the line from the example while it applies.
                support = if (hint.isNotBlank() && passphrase.isNotBlank() &&
                    hint.contains(passphrase, ignoreCase = true)
                ) {
                    strings["export.hint.contains"]
                } else {
                    strings["export.hint.hint"]
                },
                enabled = !busy,
                imeAction = ImeAction.Done,
                fieldTestTag = ExportTags.HINT,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["export.hint.warning"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["export.save"],
                onClick = { onExport(passphrase, hint.trim().takeIf { it.isNotEmpty() }) },
                enabled = canEncrypt && !busy,
                modifier = Modifier.fillMaxWidth().testTag(ExportTags.SAVE), emphasis = ActionEmphasis.Main,
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

        }
    }
}
