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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.FieldBlock

object SetupTags {
    const val ROOT = "setup_root"
    const val NAME = "setup_name"
    const val WHERE = "setup_where"
    const val PHONE = "setup_phone"
    const val REASSURE = "setup_reassure"
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
 * **The reassurance is one warm sentence at the top**, rather than the word
 * Optional beside each field. Repeating it on every field turns a reassurance
 * into noise, and the word on its own was the vocabulary of a form being
 * administered. The sentence also carries the other half a label could not:
 * that nothing here is permanent.
 *
 * **The three things are grouped and headed.** Five labeled boxes in a row read
 * as a form. Three short groups read as three questions, which is what this
 * actually is, and it is the same group header the notebook and the situation
 * picker use, so a person arriving from the disclaimer meets one app.
 *
 * **Every field carries a hint that is genuine guidance**, per section 5.9, not
 * a repeat of its label. Four of the five were bare gray boxes, which is the
 * thing that made the screen feel like paperwork more than anything else on it.
 *
 * **Group headings and field labels are never the same words.** Built with
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

    // **Setup is the one form somebody fills in while doing something
    // else**, and losing it means the app greeting them as a stranger for
    // the second time. #371 item 7.
    var name by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf("") }
    var where by rememberSaveable { mutableStateOf("") }
    var phoneName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

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
            // The questions scroll and the actions do not, which is what keeps
            // Continue in the lower half where a thumb reaches it on a large
            // phone, per section 9, whatever the font size or translation
            // length does to the questions above it.
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

                // **The reassurance, in words, once.** This replaced a mono
                // "Optional" that sat directly under the title. The word is
                // accurate and it is the vocabulary of a form being
                // administered, which is the last thing this screen should be
                // in front of someone in a corridor. It also does the second
                // job the old label could not: saying plainly that nothing here
                // is permanent.
                Text(
                    text = strings["setup.reassure"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(SetupTags.REASSURE),
                )

                Spacer(Modifier.height(Space.l))

                // **Three containers rather than fifteen loose rows.** D174.
                // The page was a label, a box and a gap repeated for as far as
                // it scrolled, with a hairline and a word standing in for
                // structure. Grouping the fields into objects lets somebody
                // finish one part and scroll past a thing they have completed.
                FieldBlock(label = strings["setup.group.who"]) {
                    Field(
                        label = strings["setup.name.label"],
                        value = name,
                        onValueChange = { name = it },
                        fieldTestTag = SetupTags.NAME,
                        support = strings["setup.name.hint"],
                    )

                    Spacer(Modifier.height(Space.m))

                    Field(
                        label = strings["setup.relationship.label"],
                        value = relationship,
                        onValueChange = { relationship = it },
                        support = strings["setup.relationship.hint"],
                    )
                }

                Spacer(Modifier.height(Space.betweenGroups))

                FieldBlock(label = strings["setup.group.where"]) {
                    Field(
                        label = strings["setup.where.label"],
                        value = where,
                        onValueChange = { where = it },
                        fieldTestTag = SetupTags.WHERE,
                        support = strings["setup.where.hint"],
                    )
                }

                Spacer(Modifier.height(Space.betweenGroups))

                FieldBlock(label = strings["setup.group.reach"]) {
                    Field(
                        label = strings["setup.phone.person.label"],
                        value = phoneName,
                        onValueChange = { phoneName = it },
                        support = strings["setup.phone.person.hint"],
                    )

                    Spacer(Modifier.height(Space.m))

                    Field(
                        label = strings["setup.phone.number.label"],
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                        fieldTestTag = SetupTags.PHONE,
                        support = strings["setup.phone.number.hint"],
                    )
                }

                Spacer(Modifier.height(Space.l))
            }

            // **A real gap between the last question and the action.** With
            // the keyboard up the scrolling area shrinks until the field at its
            // edge sits directly against the button, and on the phone that read
            // as the button overlapping the field rather than as content
            // scrolling behind it. Invisible in the resting screenshot and
            // obvious in a hand.
            //
            // **And the actions stand on their own tonal band**, which is what
            // `m3v4-4` draws under Save: the questions scroll behind it and the
            // band says where the screen stops asking and starts offering. The
            // owner, 2026-08-17: it needs to be more polished, and the content
            // used to be sliced mid field by the edge of the viewport with
            // nothing marking it.
            Spacer(Modifier.height(Space.m))

            Block {
            Action(
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
                    .testTag(SetupTags.CONTINUE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.sm))

            // Skipping is a real path, not a discouraged one. It sits below the
            // primary action at equal reach, with no styling that makes it feel
            // like giving up. **Sized to its label since #371 item 5**, which
            // is not a demotion: D137 gives the full width outlined bar to the
            // way back alone, and every form in the app now reads the same.
            Action(
                label = strings["setup.skip"],
                onClick = onSkip,
                modifier = Modifier.testTag(SetupTags.SKIP),
            )
            }
        }
    }
}
