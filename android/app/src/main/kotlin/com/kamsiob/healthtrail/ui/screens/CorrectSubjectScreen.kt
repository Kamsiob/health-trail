package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Field

object CorrectSubjectTags {
    const val ROOT = "correct_subject"
    const val NAME = "correct_subject_name"
    const val RELATIONSHIP = "correct_subject_relationship"
    const val SAVE = "correct_subject_save"
    const val CANCEL = "correct_subject_cancel"
}

/** What the person typed, so the screen holds no repository of its own. */
data class SubjectCorrection(val displayName: String, val relationship: String)

/**
 * Correcting who the notebook is about. #371.
 *
 * **The name was typed once during setup and could never be changed.** There
 * was no updater in the repository at all, and the name appears on no screen
 * inside the app: every read of it sits inside a share block, so it reaches the
 * emergency card, the incident summary, the prep sheet and the month review.
 * **A typo made at two in the morning on install day stayed invisible until a
 * clinician was holding it.**
 *
 * **It asks the two questions setup asked, in the same words**, because a
 * person coming to fix one of them should recognize the screen rather than
 * learn a new one.
 *
 * **The name is not required here either.** Setup allows an empty name and so
 * does this: rule 13, and a notebook whose subject nobody has named is a real
 * state rather than an error.
 */
@Composable
fun CorrectSubjectScreen(
    subject: Repository.Subject,
    onSave: (SubjectCorrection) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // bidi-ok: the values inside fields being edited. Isolate marks here would
    // become characters the person has to delete.
    var name by remember(subject.id) { mutableStateOf(subject.displayName) }
    var relationship by remember(subject.id) { mutableStateOf(subject.relationship.orEmpty()) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(CorrectSubjectTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["more.subject"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["subject.correct.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                // **Fields sit on the canvas, never in a block.** A field is
                // already a container with its own outline and its own label, so a
                // second one around it is two edges on one thing, which is the
                // clutter D183 took out of the forms. `docs/V4.md` 2.1, `m3v4-4`.
                Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
                    DictatableField(
                        label = strings["setup.name.label"],
                        value = name,
                        onValueChange = { name = it },
                        support = strings["setup.name.hint"],
                        imeAction = ImeAction.Next,
                        fieldTestTag = CorrectSubjectTags.NAME,
                    )
                    DictatableField(
                        label = strings["setup.relationship.label"],
                        value = relationship,
                        onValueChange = { relationship = it },
                        support = strings["setup.relationship.hint"],
                        imeAction = ImeAction.Done,
                        fieldTestTag = CorrectSubjectTags.RELATIONSHIP,
                    )
                }

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            Action(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        SubjectCorrection(displayName = name, relationship = relationship),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(CorrectSubjectTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(CorrectSubjectTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}
