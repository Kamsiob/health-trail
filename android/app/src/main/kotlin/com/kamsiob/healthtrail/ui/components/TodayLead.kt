package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * The lead slot on Today. `DESIGN.md` 21.1, and the one-thing hero in costume.
 *
 * **Exactly one, never zero and never two, and that is by construction rather
 * than by convention.** A free-form dashboard breaks law 1: if six equal cards
 * can be stacked then no screen has one thing first. The resolution is a fixed
 * structure with free contents, so what fills this slot is the person's choice
 * and the fact that something singular is at the top is not.
 *
 * **It is not a card and must not become one.** It sits directly on `paper`
 * with no surface, no shadow and no border, which is the whole difference
 * between the top of a page and the first item in a list. Wearing the card
 * costume here would put the most important thing on the screen at exactly the
 * weight of the four things under it, which is rule 15's uniform weight and is
 * the defect this component exists to prevent. Same rule as [Hero], and this is
 * that component's shape with an eyebrow the catalog cannot hold.
 *
 * **The eyebrow is data, not copy.** For the digest it is the day, because that
 * is the one fact worth stating above a sentence about today and because the
 * word "Today" is already on the tab chip and on the active navigation tab. For
 * any other card promoted here it is that card's own name, so the person can
 * see what they put at the top. Either way it takes the hue's ink, which is
 * identity and never state: 21.2, the hue does not change with what the answer
 * says.
 *
 * **The answer wraps freely.** A fixed cap is a cap at the smallest type size
 * and a truncation at the largest, per D105, and the sentence here is the one
 * thing the screen exists to say.
 *
 * **It is one node to a reader**, like every card, unless it is holding
 * controls: the eyebrow, the sentence, the quiet line and the chevron read as
 * four stops would make somebody listen to four things to learn one.
 *
 * **When to use it.** At the top of Today, once, and nowhere else. Elsewhere in
 * the app the same job is [Hero]'s.
 *
 * **When not to use it.** For anything that is not the single thing the person
 * came for. Promoting a second block to this treatment does not give the screen
 * two leads, it gives it none.
 */
@Composable
fun TodayLead(
    /** The day, or the promoted card's own name. Already in the person's language. */
    eyebrow: String,
    /** The eyebrow's hue, from the tab pack. Gold for a whole-app surface. */
    hue: TabHue,
    /** What a reader says instead of the whole block, composed by the caller. */
    description: String,
    /** The verb a reader announces for the tap. */
    openLabel: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether the slot speaks as one node.
     *
     * **False while it holds edit controls**, for the same reason [TodayCard]
     * turns it off: `clearAndSetSemantics` clears every descendant, so controls
     * inside would be unreachable by a screen reader and by switch access.
     */
    speaksAsOneNode: Boolean = true,
    /**
     * One inline control, outside everything the lead says.
     *
     * **The same slot [TodayCard] has and for the same reason**: the answer is
     * silenced so the lead is one stop for a reader, and anything inside that
     * silence is silenced with it. A care team card promoted to the lead brings
     * its dialable number with it, and a number nobody can reach with a reader
     * is not a control.
     */
    action: (@Composable () -> Unit)? = null,
    /**
     * Touch and hold, which starts arranging Today.
     *
     * **The lead answers the hold too, and that is the point.** It is the
     * largest thing on the screen and the first thing a thumb lands on, so a
     * hold that works on every card and not on this one teaches somebody that
     * the gesture is unreliable, which is worse than not having it.
     *
     * long-press-twin: the Arrange action in Today's own header, which enters
     * the same mode. D155.
     */
    onLongPress: (() -> Unit)? = null,
    /** What a reader calls the hold. Required whenever [onLongPress] is set. */
    longPressLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val strings = LocalStrings.current

    // **The lead is a block of color with a shape of its own.** D170, and this
    // is the structural half of the redesign: it was transparent, so the one
    // thing the screen leads with looked like a paragraph that happened to be
    // first. It is the section's own wash now, at the hero shape, which is the
    // only place in the app that corner treatment appears. Nothing else on
    // Today can be mistaken for it.
    //
    // **The wash rather than the base**, because the answer inside it is the
    // person's own words and has to stay black on light. A saturated fill
    // would make every lead a white-on-color plaque and cost the readability
    // this app is built around.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.hero)
            .background(hue.wash)
            .openableByTap(
                label = openLabel,
                onTap = onOpen,
                resting = hue.wash,
                shape = Radius.hero,
                // long-press-twin: Today's Arrange action, per the parameter above.
                onLongPress = onLongPress,
                longPressLabel = longPressLabel,
            )
            // The lead's sentence sits on the lead's own node, beside its tap
            // action, so one stop says both.
            .semantics { contentDescription = description }
            .padding(horizontal = Space.cardPadding, vertical = Space.m),
    ) {
        Column(
            // Silenced rather than cleared away, exactly as `TodayCard` does
            // it, and given up in edit mode for the same reason: the controls
            // are in [content] and clearing it would put them out of reach.
            modifier = if (speaksAsOneNode) Modifier.clearAndSetSemantics { } else Modifier,
        ) {
            // **One quiet line above the answer**, D171. Uppercase letterspaced
            // mono was a third typographic voice competing with the display
            // face above it and the body face below, on a screen that already
            // has a masthead. Sentence case in the section's ink says the same
            // thing and lets the answer be the loud one.
            Text(
                text = eyebrow,
                style = HealthTrail.type.bodyS,
                color = hue.ink,
            )
            Spacer(Modifier.height(Space.s))
            Row(
                modifier = Modifier.sizeIn(minHeight = Space.touchTarget),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) { content() }
                Spacer(Modifier.width(Space.sm))
                // The chevron sits on the first line of the sentence rather
                // than centered against a block that can grow to six lines at
                // the largest font size, where centered would leave it floating
                // beside nothing.
                Chevron(modifier = Modifier.padding(top = Space.xs))
            }
        }
        action?.invoke()
    }
}
