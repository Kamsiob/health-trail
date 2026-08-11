package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object AcknowledgeTags {
    const val SHEET = "acknowledge_sheet"
    const val FIELD = "acknowledge_field"
    const val SAVE = "acknowledge_save"
    const val CANCEL = "acknowledge_cancel"
    const val REMOVE = "acknowledge_remove"
}

/**
 * Recording how a facility answered a standing instruction.
 *
 * **This is the half that gets pointed back to.** `MASTER_SPEC.md` section 4.3
 * asks for what was asked, of whom, when, and how it was acknowledged. The
 * first three were held and the fourth was not, and the fourth is the one that
 * settles an argument: "I asked, and the charge nurse put it in the care plan
 * on the 4th."
 *
 * **It records and never assesses.** The app does not decide whether an answer
 * was adequate, whether a facility is complying, or what to do about it. It
 * writes down what the person was told, which is the only thing it can know.
 *
 * The date is stamped when this is saved rather than asked for, because the
 * person is writing it down as it happens. It stays editable like every date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgeSheet(
    instruction: Repository.StandingInstruction,
    onSave: (String) -> Unit,
    /**
     * Taking the request off the list, per #218.
     *
     * **This is the request's own place**, reached by tapping it, and it is
     * here because the list row used to carry removal on a long press: the one
     * path a sighted person cannot find and a thumb can hit by accident.
     */
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var how by remember(instruction.id) {
        mutableStateOf(instruction.acknowledgedHow.orEmpty())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = null,
        modifier = Modifier.testTag(AcknowledgeTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["instructions.ack.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))
            // The instruction shown back, so nobody records an answer against
            // the wrong request.
            Text(
                text = instruction.name,
                style = HealthTrail.type.bodyL,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["instructions.ack.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            DictatableField(
                label = strings["instructions.ack.title"],
                value = how,
                onValueChange = { how = it },
                hint = strings["instructions.ack.hint"],
                singleLine = false,
                imeAction = ImeAction.Done,
                fieldTestTag = AcknowledgeTags.FIELD,
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["questions.answer.save"],
                onClick = { onSave(how) },
                modifier = Modifier.fillMaxWidth().testTag(AcknowledgeTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(AcknowledgeTags.CANCEL),
            )

            // Full width because a sheet has no way back at its foot for this
            // to compete with, which is the distinction D118 draws.
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(AcknowledgeTags.REMOVE),
            )
        }
    }
}
