package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Search inside one list, sitting at the top of that list. `DESIGN.md` section
 * 7, and the third of law 4's four tools.
 *
 * **It is scoped, and the scope is the point.** This is not the app's search
 * screen and it never looks outside the list it sits on. Somebody standing in
 * the trail looking for the word "catheter" wants the trail, not eleven other
 * sections arriving to help. A field that quietly widens its own scope is the
 * app answering a question nobody asked.
 *
 * **The hint says how much it is searching**, which is the one number that makes
 * a search field feel like a tool rather than a formality. "Search 1,630
 * entries" tells the person both what the field does and how large the thing
 * under it has become, and it is the honest reason the field exists at all.
 *
 * **Nothing is submitted.** It filters as the person types and there is no
 * search button, because a field that needs a second tap to do the thing it
 * obviously does is a tap spent on the app's convenience.
 *
 * **When not to use it.** On a list that fits in a screenful or two, which is
 * the same rule the scrubber has. A search field over six rows is furniture, and
 * on a small list it costs a person more to read than to scan what is under it.
 *
 * @param count how many things are in the scope, already formatted for the
 *   catalog, so the hint can say what it is searching.
 */
@Composable
fun ScopedSearch(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(surface)
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.pill)
            .padding(horizontal = Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative, and it says so. The hint beside it is what names the
        // field, so a reader that announced the glyph too would say it twice.
        MagnifierMark()
        Spacer(Modifier.width(Space.s))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(text = hint, style = type.bodyM, color = colors.ink2)
            }
            CompositionLocalProvider(LocalTextStyle provides type.bodyM) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = type.bodyM.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.blue),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        // **Search, not Next.** There is nothing after this
                        // field to move to, and a Next key that goes nowhere is
                        // the keyboard promising something the screen has not
                        // got. The list is already filtering by then, so the key
                        // only closes the keyboard.
                        imeAction = ImeAction.Search,
                    ),
                    interactionSource = interaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
                        .semantics { contentDescription = hint },
                )
            }
        }

        // **Only once there is something to clear.** An always-present clear
        // button on an empty field is a control that does nothing, which rule 16
        // says reads as broken.
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(Space.xs))
            val clearInteraction = remember { MutableInteractionSource() }
            val clearSurface by pressedSurface(clearInteraction, colors.sand)
            Box(
                modifier = Modifier
                    .size(Space.touchTarget)
                    .clip(Radius.pill)
                    .background(clearSurface)
                    .clickable(
                        interactionSource = clearInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { onValueChange("") },
                    )
                    .semantics { contentDescription = clearLabel },
                contentAlignment = Alignment.Center,
            ) {
                CrossMark()
            }
        }
    }
}

/**
 * The magnifier, drawn rather than set in a font, for the same reason the
 * chevron is: an icon font falls back to a box in some script somewhere, and
 * this one would fall back on the screen where somebody is already lost.
 *
 * **Its handle mirrors.** A magnifier is a held object and it points the way a
 * right handed person holds it, which is the other way around in a right to
 * left layout.
 *
 * **One magnifier, drawn once.** Two in one app are two that drift the moment
 * either is touched, and the mirroring rule above is exactly the sort of thing
 * the second copy would be missing.
 */
@Composable
internal fun MagnifierMark(modifier: Modifier = Modifier, size: Dp = 15.dp) {
    val colors = HealthTrail.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(modifier = modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val radius = this.size.minDimension * 0.32f
        val cx = if (rtl) this.size.width - radius - stroke else radius + stroke
        val cy = radius + stroke
        drawCircle(
            color = colors.ink3,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
        val diagonal = radius * 0.72f
        val dx = if (rtl) -diagonal else diagonal
        drawLine(
            color = colors.ink3,
            start = Offset(cx + dx, cy + diagonal),
            end = Offset(
                if (rtl) stroke else this.size.width - stroke,
                this.size.height - stroke,
            ),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** The clear mark. Non-text, so it sits at the 3:1 boundary rather than a text ratio. */
@Composable
private fun CrossMark(size: Dp = 13.dp) {
    val colors = HealthTrail.colors
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = colors.ink3,
            start = Offset(0f, 0f),
            end = Offset(this.size.width, this.size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.ink3,
            start = Offset(this.size.width, 0f),
            end = Offset(0f, this.size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
