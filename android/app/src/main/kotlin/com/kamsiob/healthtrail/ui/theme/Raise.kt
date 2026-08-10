package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * The one place a surface is lifted off the page. `DESIGN.md` section 4.7.
 *
 * **`Elevation` was specified in full and read by nothing.** Both shadow
 * layers, the small variant and the print hairline were in `Dimens.kt` with a
 * comment explaining each, and a grep for `Elevation.` across the sources
 * returned exactly one hit: the line that defines the object. There was no
 * `.shadow(` anywhere. **Every card and every group in the app was a flat
 * rectangle.** #324.
 *
 * **It survived because light theme hides it.** `paper` is `#F6F1E6` and `card`
 * is `#FFFFFF`, so a card is legible against the page by warmth alone. What was
 * lost is the layering: five cards on Today read as five painted areas rather
 * than five things sitting on a desk, and the desk is the whole metaphor.
 * Section 17 bans a border as a card's only definition, and fill as the only
 * definition is the same argument.
 *
 * **Dark theme is correct flat and stays flat.** 4.7 is explicit: no shadow in
 * dark. Elevation there is `paper` to `card` to `sand` plus an optional
 * hairline, because a shadow on a dark surface reads as dirt rather than depth.
 * So this is theme aware and is not "add a shadow".
 *
 * **One place, so it cannot be done four slightly different ways.** That was the
 * first item on #324 and it is the reason this file exists rather than a
 * `.shadow()` call inside each component.
 */

/**
 * The warm tint both layers are drawn in, from 4.7's `rgba(60,54,38,...)`.
 *
 * **Warm rather than black**, which is the whole character of the shadow in this
 * direction. A neutral shadow on a cream page reads as gray and makes the paper
 * look dirty; this one is the page's own darker relative.
 *
 * **Alpha lives in the color rather than in a parameter**, because that is where
 * Compose reads it: `ambientColor` and `spotColor` carry their own alpha and
 * there is no separate opacity on `Modifier.shadow`.
 */
private val WARM = Color(red = 60, green = 54, blue = 38, alpha = 255)

/** 4.7's outer layer, `rgba(60,54,38,.10)`. */
private val OUTER = WARM.copy(alpha = 0.10f)

/** 4.7's tight layer, `rgba(60,54,38,.06)`. */
private val TIGHT = WARM.copy(alpha = 0.06f)

/** 4.7's row and thumbnail layer, `rgba(60,54,38,.08)`. */
private val SMALL = WARM.copy(alpha = 0.08f)

/**
 * A card or a group, lifted. Two layers, per 4.7.
 *
 * **Both layers, and the tight one is not decoration.** The wide soft layer
 * alone reads as a halo with nothing holding the edge down; the 2dp layer is
 * what makes the edge sit on the page rather than float above it. That is why
 * the direction specifies two and why this applies two.
 *
 * **`clip = false` on both.** Clipping here would round the content twice, once
 * for the shadow and once for whoever set the real radius, and a component that
 * clips its own children is a component that can crop them. The caller still
 * owns its `clip`.
 *
 * **The shape has to match what the caller clips to**, or the shadow is drawn
 * for a rectangle behind a rounded surface and shows at the corners. It is a
 * required argument for that reason rather than defaulted to something plausible.
 */
@Composable
fun Modifier.raisedCard(shape: Shape): Modifier = composed {
    if (HealthTrail.colors.isDark) {
        this
    } else {
        this
            .shadow(
                elevation = Elevation.cardOffsetY,
                shape = shape,
                clip = false,
                ambientColor = OUTER,
                spotColor = OUTER,
            )
            .shadow(
                elevation = Elevation.cardOffsetYTight,
                shape = shape,
                clip = false,
                ambientColor = TIGHT,
                spotColor = TIGHT,
            )
    }
}

/**
 * A row, a thumbnail, or anything raised only slightly. One layer, per 4.7.
 *
 * **Deliberately not the card treatment at a smaller number.** A row that
 * carried the two layer shadow would compete with the cards around it, and the
 * point of the small variant is that it lifts without joining that conversation.
 */
@Composable
fun Modifier.raisedSlightly(shape: Shape): Modifier = composed {
    if (HealthTrail.colors.isDark) {
        this
    } else {
        this.shadow(
            elevation = Elevation.smallOffsetY,
            shape = shape,
            clip = false,
            ambientColor = SMALL,
            spotColor = SMALL,
        )
    }
}
