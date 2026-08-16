package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor

/**
 * One yes or no question, as a row on a tinted surface with a switch.
 *
 * **The approved medication mockup asks "On the emergency card" this way and
 * the app asked it with a chip.** A chip is a choice among several; a switch is
 * a state that is on or off, and the difference matters here because the
 * question has exactly two answers and one of them is the consequence of doing
 * nothing. A single chip standing alone also reads as a filter, which is what
 * chips do everywhere else in this app.
 *
 * **The switch is drawn here rather than imported.** This app has its own
 * vocabulary and reaches for almost nothing out of Material: bringing in
 * `material3.Switch` would arrive with the ripple that D175 spent a night
 * removing, its own shape scale, and its own color roles, none of which are
 * this app's. A track, a thumb and a spring are twenty lines.
 *
 * **The whole row is the target, not the switch.** A 52dp switch at the end of
 * a card is a small target at the edge of the screen for a thumb, and the row
 * already says what the switch is for. Rule 18's tap count, and section 12's
 * 48dp floor, which the row clears by a wide margin.
 *
 * **A reader hears a switch, because that is what it is.** `Role.Switch` with a
 * spoken on or off state, so the control announces its state rather than
 * leaving somebody to infer it from a drawing they cannot see.
 *
 * **Never a required question.** Rule 13: off is a finished answer, the same as
 * on. Nothing here marks the off state as incomplete and nothing counts how
 * many of these are unanswered.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    section: Repository.Section,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val strings = LocalStrings.current
    val hue = hueFor(section)
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, hue.wash)
    val scale by pressScale(interaction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(Radius.cardLarge)
            .background(surface)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics {
                stateDescription = if (checked) strings["state.on"] else strings["state.off"]
            }
            .padding(Space.cardPadding)
            .defaultMinSize(minHeight = Space.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        IconTile(section = section, tint = hue.ink, background = Color.Transparent)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = type.rowTitle, color = colors.ink)
            if (subtitle != null) {
                Text(text = subtitle, style = type.bodyS, color = colors.ink2)
            }
        }
        Switch(checked = checked)
    }
}

/**
 * The track and the thumb, and nothing else: the row above owns the gesture and
 * the semantics, so this draws the state and never reports it. A second
 * announcement from in here is how a control ends up read out twice.
 */
@Composable
private fun Switch(checked: Boolean) {
    val colors = HealthTrail.colors
    val motion = LocalMotion.current

    val track by animateColorAsState(
        targetValue = if (checked) colors.blue else colors.sand,
        animationSpec = motion.quick(),
        label = "switchTrack",
    )
    // **The thumb springs and the color fades**, which is the same pair every
    // other control in this app uses: physics for the thing that moves, a
    // quick fade for the thing that changes color. D167.
    // A fraction of the travel rather than two dp values, so the distance is
    // stated once and the resting position is the absence of travel.
    val traveled by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion.springy(),
        label = "switchThumb",
    )

    val thumbTravelPx = with(LocalDensity.current) { thumbTravel.toPx() }

    Box(
        modifier = Modifier
            .width(Space.switchTrackWidth)
            .height(Space.switchTrackHeight)
            .clip(Radius.pill)
            .background(track)
            // **A border in the off state, because sand on a wash is faint.**
            // Section 12 holds a control's own boundary to the non-text 3:1
            // ratio, and the off state is the one that has to survive a
            // grayscale screenshot and every color vision difference. Drawn
            // only when it is off, rather than drawn at zero width, so the
            // filled state carries no invisible edge.
            .then(
                if (checked) {
                    Modifier
                } else {
                    Modifier.border(Space.hairlineWidth, colors.ink3, Radius.pill)
                },
            )
            .padding(Space.switchThumbInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                // **The lambda overload, so the thumb moves without
                // recomposing.** An animated value read here rather than in a
                // lambda re-runs composition on every frame of the spring, on
                // a control whose whole job is to move. Lint names this one by
                // itself, `UseOfNonLambdaOffsetOverload`.
                .offset { IntOffset(x = (thumbTravelPx * traveled).toInt(), y = 0) }
                .size(Space.switchThumb)
                .clip(CircleShape)
                .background(colors.paper),
        )
    }
}

/** How far the thumb travels: the track, less the thumb and both insets. */
private val thumbTravel =
    Space.switchTrackWidth - Space.switchThumb - Space.switchThumbInset * 2
