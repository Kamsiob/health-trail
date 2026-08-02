package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ExportTags {
    const val NAME = "export"
    const val PASSPHRASE = "export_passphrase"
    const val AGAIN = "export_again"
    const val SAVE = "export_save"
    const val PLAIN = "export_plain"
    const val STATUS = "export_status"
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
 * **An unencrypted export is offered plainly rather than hidden.** The format
 * says so and the reason is right: it is their data and wanting to read it is
 * reasonable. It carries a warning rather than a scolding.
 *
 * **Nothing here reports success until the bytes are written.** "Saved" appears
 * after the file exists at the chosen place, never when the export began, and
 * a failure says plainly that nothing was saved and the notebook is untouched.
 */
@Composable
fun ExportScreen(
    state: ExportState,
    onExport: (passphrase: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var passphrase by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }

    val mismatch = again.isNotEmpty() && passphrase != again
    val canEncrypt = passphrase.isNotEmpty() && passphrase == again
    val busy = state == ExportState.WORKING

    SectionScaffold(
        name = ExportTags.NAME,
        title = strings["export.title"],
        subtitle = strings["export.lead"],
        onBack = onBack,
        backLabelKey = "section.back.more",
        modifier = modifier,
    ) {
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
                enabled = !busy,
                keyboardType = KeyboardType.Password,
                fieldTestTag = ExportTags.PASSPHRASE,
            )
            Spacer(Modifier.height(Space.m))

            HealthTrailTextField(
                label = strings["export.passphrase.again"],
                value = again,
                onValueChange = { again = it },
                // Said only when the two actually differ, so it is a
                // correction rather than a warning hanging over an empty field.
                note = if (mismatch) strings["export.mismatch"] else null,
                enabled = !busy,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                fieldTestTag = ExportTags.AGAIN,
            )

            Spacer(Modifier.height(Space.m))
            // The sentence the format requires, in the words it requires,
            // before the person commits rather than after.
            Text(
                text = strings["export.warning"],
                style = HealthTrail.type.bodyM,
                color = colors.alertText,
            )

            Spacer(Modifier.height(Space.l))
            FilledButton(
                label = strings["export.save"],
                onClick = { onExport(passphrase) },
                enabled = canEncrypt && !busy,
                modifier = Modifier.fillMaxWidth().testTag(ExportTags.SAVE),
            )

            Spacer(Modifier.height(Space.sectionGap))
            GroupHeader(labelKey = "export.plain")
            Spacer(Modifier.height(Space.headerGap))
            Text(
                text = strings["export.plain.warning"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.m))
            QuietButton(
                label = strings["export.plain"],
                onClick = { onExport(null) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(ExportTags.PLAIN),
            )

            // One line, and only when there is something true to say.
            val status = when (state) {
                ExportState.WORKING -> strings["export.working"]
                ExportState.DONE -> strings["export.done"]
                ExportState.FAILED -> strings["export.failed"]
                ExportState.READY -> null
            }
            if (status != null) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = status,
                    style = HealthTrail.type.bodyL,
                    color = if (state == ExportState.FAILED) colors.alertText else colors.ink,
                    modifier = Modifier.testTag(ExportTags.STATUS),
                )
            }

            Spacer(Modifier.height(Space.l))
        }
    }
}
