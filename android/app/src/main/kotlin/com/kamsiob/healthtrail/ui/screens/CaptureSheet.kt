package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object CaptureTags {
    const val SHEET = "capture_sheet"
    fun option(kind: CaptureKind) = "capture_option_${kind.name.lowercase()}"
}

/**
 * The six things a person can write down.
 *
 * The order is the order in `MASTER_SPEC.md` section 4.2 and it does not change,
 * because someone reaching for this while a nurse is still on the phone is
 * reaching by position rather than by reading.
 */
enum class CaptureKind { CALL, VISIT, INCIDENT, MEASUREMENT, QUESTION, DOCUMENT }

/**
 * The capture sheet, which is the only way data enters this app.
 *
 * **The person chooses what happened, never where it goes.** Each of the six
 * files itself into the right section. Anything that cannot be categorized lands
 * in the Unfiled tray, where the app suggests a home by plain word matching and
 * the person confirms with one tap. The app never files anything on its own.
 *
 * A real bottom sheet rather than the dimmed composite the mockups show, per
 * `DESIGN.md` section 3 item 7: a static image cannot show a transition, so the
 * mockup drew it that way and the built thing is a sheet.
 *
 * It opens with the expressive spring, which is one of only three moments in the
 * whole app allowed to overshoot. The other two are a milestone joining the arc
 * and an incident being marked resolved. Three, because each is a small piece of
 * relief in an app used during hard times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    onChoose: (CaptureKind) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        modifier = Modifier.testTag(CaptureTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Text(
                text = strings["capture.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.l))

            CaptureKind.entries.forEach { kind ->
                CaptureOption(
                    label = strings[labelKey(kind)],
                    onClick = { onChoose(kind) },
                    modifier = Modifier.testTag(CaptureTags.option(kind)),
                )
                Spacer(Modifier.height(Space.s))
            }
        }
    }
}

/**
 * One choice.
 *
 * A `sand` inset row rather than a card, because these are actions inside a
 * sheet rather than records. No icons: six icons would be six small pictures of
 * what a call or an incident looks like, and the words are already the shortest
 * unambiguous form.
 */
@Composable
private fun CaptureOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(colors.sand)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
        )
    }
}

private fun labelKey(kind: CaptureKind): String = when (kind) {
    CaptureKind.CALL -> "capture.call"
    CaptureKind.VISIT -> "capture.visit"
    CaptureKind.INCIDENT -> "capture.incident"
    CaptureKind.MEASUREMENT -> "capture.measurement"
    CaptureKind.QUESTION -> "capture.question"
    CaptureKind.DOCUMENT -> "capture.document"
}
