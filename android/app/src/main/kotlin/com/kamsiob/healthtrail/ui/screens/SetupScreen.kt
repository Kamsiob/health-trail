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
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object SetupTags {
    const val ROOT = "setup_root"
    const val NAME = "setup_name"
    const val WHERE = "setup_where"
    const val PHONE = "setup_phone"
    const val CONTINUE = "setup_continue"
    const val SKIP = "setup_skip"
}

/** What setup collected. Every field can be blank. */
data class SetupAnswers(
    val name: String,
    val relationship: String,
    val where: String,
    val phoneName: String,
    val phoneNumber: String,
)

/**
 * Essentials first, and everything else waits.
 *
 * The situation this is built for is persona P1: a parent has just been
 * admitted after a fall, the person is standing in a corridor holding the phone
 * in one hand, and has about four minutes. So it asks three things, per
 * `MASTER_SPEC.md` section 4.1: who you are looking after, where they are, and
 * one phone number you would need in an emergency.
 *
 * **Every field is optional, including the name, and continuing with all of them
 * blank is allowed.** Partial is a finished state. There is no required field
 * marker, no validation, no error state, and no progress indicator, because a
 * progress indicator on setup frames an unfinished form as a deficiency.
 *
 * **One screen rather than a three step wizard.** A wizard means three taps of
 * Next before anything is written down, and it hides how little is being asked.
 * One short scrolling screen shows the whole ask at once, which is what makes it
 * possible to see that it is nearly nothing.
 *
 * **The word Optional appears once,** at the top, rather than beside each field.
 * Repeating it on every field turns a reassurance into noise.
 *
 * **Section headings and field labels are never the same words.** Built with
 * them shared, the screen showed "Where are they right now" twice in a row, once
 * as a heading and once as the label beneath it. It read as a bug, and a screen
 * reader announced it twice. Found by looking at the built screen on a device.
 *
 * Composed from Display L, Body M, the text field from section 5.9, and one
 * filled button. Nothing new was introduced.
 */
@Composable
fun SetupScreen(
    onContinue: (SetupAnswers) -> Unit,
    onSkip: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var where by remember { mutableStateOf("") }
    var phoneName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SetupTags.ROOT),
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
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = strings["setup.title"],
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
                    label = strings["setup.name.label"],
                    hint = strings["setup.name.hint"],
                    value = name,
                    onValueChange = { name = it },
                    fieldTestTag = SetupTags.NAME,
                )

                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["setup.relationship.label"],
                    value = relationship,
                    onValueChange = { relationship = it },
                )

                Spacer(Modifier.height(Space.sectionGap))

                Text(
                    text = strings["setup.where.title"],
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                )

                Spacer(Modifier.height(Space.sm))

                HealthTrailTextField(
                    label = strings["setup.where.label"],
                    value = where,
                    onValueChange = { where = it },
                    fieldTestTag = SetupTags.WHERE,
                )

                Spacer(Modifier.height(Space.sectionGap))

                Text(
                    text = strings["setup.phone.title"],
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                )

                Spacer(Modifier.height(Space.s))

                Text(
                    text = strings["setup.phone.hint"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.sm))

                HealthTrailTextField(
                    label = strings["setup.phone.person.label"],
                    value = phoneName,
                    onValueChange = { phoneName = it },
                )

                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["setup.phone.number.label"],
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    fieldTestTag = SetupTags.PHONE,
                )

                Spacer(Modifier.height(Space.l))
            }

            FilledButton(
                label = strings["setup.continue"],
                onClick = {
                    onContinue(
                        SetupAnswers(
                            name = name.trim(),
                            relationship = relationship.trim(),
                            where = where.trim(),
                            phoneName = phoneName.trim(),
                            phoneNumber = phoneNumber.trim(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SetupTags.CONTINUE),
            )

            Spacer(Modifier.height(Space.sm))

            // Skipping is a real path, not a discouraged one. It sits below the
            // primary action at equal reach, with no styling that makes it feel
            // like giving up.
            TextAction(
                label = strings["setup.skip"],
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SetupTags.SKIP),
            )
        }
    }
}
