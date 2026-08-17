package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The sheet, written from scratch. #386, and the last of step 2's surfaces.
 *
 * **Eighteen call sites each repeated the same five arguments**, and each was
 * one edit from a sheet that looked slightly different from the other
 * seventeen. The container, the corner, the scrim and the missing handle are
 * the language's business rather than each screen's, `docs/V4.md` 3: a shared
 * surface reaches every screen at once.
 *
 * **No drag handle**, and it is removed rather than labeled. D42: with the
 * sheet fully expanded the handle has no state to toggle, so it is a control
 * that does nothing. `ScreenReaderTest` also caught it carrying a click action
 * and announcing nothing. **Labeling it was tried twice**, wrapping it and
 * passing a modifier down, and Material applies its own semantics outside both,
 * which is the fact worth keeping: the only fix is not drawing it. The sheet
 * still closes by tapping outside and by the back gesture.
 *
 * **A scrim that actually dims.** Material's default is a light veil, and
 * against this app's dark surfaces the notebook behind the sheet stayed almost
 * as bright as the sheet and went on competing for the eye: two screens at once
 * rather than one over another. Seen on the phone.
 *
 * **The surface only.** What goes inside is the caller's, and [SheetBody] is
 * the arrangement most of them want.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    state: SheetState = rememberSheet(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HealthTrail.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = SCRIM),
        dragHandle = null,
        modifier = modifier,
        content = content,
    )
}

/**
 * What a sheet holds: the screen margin, the insets, and room at the bottom.
 *
 * **The insets are the sheet's own problem.** It sits over everything, so the
 * navigation bar and the keyboard are underneath it, and a form whose Save goes
 * under the keyboard is the defect every call site was separately remembering
 * to prevent.
 *
 * [scrolls] is true for anything with a form in it, so the actions stay
 * reachable at a raised font scale, and false where the content brings its own
 * scrolling list: a lazy list inside a scrolling column is the one arrangement
 * Compose refuses outright.
 */
@Composable
fun SheetBody(
    modifier: Modifier = Modifier,
    scrolls: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .then(if (scrolls) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = Space.screenHorizontal)
            .padding(bottom = Space.l),
        content = content,
    )
}

/**
 * A sheet's state, hidden until it is asked for.
 *
 * **`rememberModalBottomSheetState` is deprecated in the alpha** and this is
 * what it says to use instead: a hidden initial value, with the half open stop
 * left out of the allowed set, which is what `skipPartiallyExpanded` used to
 * say. A sheet in this app is open or it is gone; a half open one is a state
 * nobody asked for and a target that moves under a finger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSheet(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)

/** How far the screen underneath goes back. */
private const val SCRIM = 0.62f
