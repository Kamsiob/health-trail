package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.Space

object HeaderActionTags {
    const val EDIT = "header_edit"
    const val ROW = "header_actions"
    const val SEARCH = "header_search"
}

/**
 * The controls in the top corner of a screen, in one fixed order. D173.
 *
 * **The owner's rule, and it is about learnability rather than looks:** "if
 * there's anything on one page then the user should know where to find it on a
 * different page." A control that moves between screens has to be found again
 * every time, and finding it again is the cost paid by somebody who is tired
 * and holding a phone in a waiting room.
 *
 * **So the corner has one grammar everywhere.** Reading inward from the edge:
 * the edit mark sits furthest right, because it is the one the thumb reaches
 * for most and the corner is the easiest target on the screen; the lamp sits
 * beside it, and search inside that. A screen that has only one of them still
 * puts that one in its own place.
 *
 * **Material's own icon buttons, D196.** These were three hand built boxes with
 * a clip, a background, a hand rolled press scale and, in the pencil's case, a
 * shape drawn on a `Canvas`. `IconButton` owns the 48dp target, the state layer
 * and the ripple; the lamp's gold comes from the scheme through
 * [TipsButton], not from a value typed here.
 */
@Composable
fun HeaderActions(
    modifier: Modifier = Modifier,
    /**
     * Opens search. Null on a screen that is not a way into looking for things.
     *
     * **Furthest from the corner, so the order the rest of the app learned does
     * not move.** Reading inward from the edge the row is pencil, lamp, search.
     *
     * **A mark, because the box was clutter.** The owner, on Today, 2026-08-17:
     * a full width search field "clutters the screen", and search belongs as "a
     * search button in the top right, like an icon that matches". D192.
     */
    onSearch: (() -> Unit)? = null,
    /** Opens what this page is for. Null on a screen with nothing to explain. */
    onTips: (() -> Unit)? = null,
    /** Goes back and changes what is already written here. Null where nothing is editable. */
    onEdit: (() -> Unit)? = null,
    /** What a reader calls the edit action, in this screen's own words. */
    editLabel: String? = null,
    /**
     * The screen's own tag for its edit control.
     *
     * **The tag follows the control rather than the position.** Five screens had
     * their own tag on a button in the body; moving the button to the corner
     * without bringing the tag would have left five instrumented tests looking
     * for a node that no longer exists, and a green suite that had stopped
     * checking the thing it names.
     */
    editTag: String? = null,
) {
    if (onSearch == null && onTips == null && onEdit == null) return
    val strings = LocalStrings.current
    Row(
        modifier = modifier.testTag(HeaderActionTags.ROW),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onSearch?.let { open ->
            IconButton(
                onClick = open,
                modifier = Modifier.testTag(HeaderActionTags.SEARCH),
            ) {
                Icon(
                    painter = painterResource(Symbols.search),
                    contentDescription = strings["today.search"],
                )
            }
        }
        // **The leading slot is held**, because that is what keeps the pencil in
        // the corner rather than letting it slide out when the lamp is absent.
        // The trailing slot is not: the owner, 2026-08-16, asked the corner to
        // end at the screen margin so the two ends of the header agree, and a
        // margin is a stronger alignment than a reserved box.
        if (onTips != null) TipsButton(onOpen = onTips) else if (onEdit != null) HeldSlot()
        if (onEdit != null) {
            EditAction(onClick = onEdit, label = editLabel, tag = editTag)
        }
    }
}

/** One action's width, held open so the other never slides into its place. */
@Composable
private fun HeldSlot() {
    Box(Modifier.size(Space.touchTarget))
}

/**
 * Change what is already written on this screen.
 *
 * **A mark rather than the word.** The owner, looking at Today: the corner
 * should carry an edit icon instead of the word "Arrange". A verb in the corner
 * is a different word on every screen, which means the corner has to be read
 * before it can be used; a pencil is the same mark everywhere and is read once.
 *
 * **Google's pencil rather than one drawn here.** This was a filled outline on a
 * `Canvas`, laid out in fractions of the touch target, with two lines painted in
 * the button's own ground to suggest the ferrule and the cut. It was a good
 * drawing of the wrong alphabet: every other mark in the app is a Material
 * Symbol, and the app authors no glyphs. D182 and D196.
 *
 * **Quieter than the lamp on purpose.** Gold is the app's accent and it is spent
 * on the capture button and on the lamp. Two gold circles side by side would be
 * two accents in one corner and neither would lead, so the pencil takes
 * Material's plain icon button, which is the right weight for a control used
 * often that is never the reason somebody opened the screen.
 *
 * **The spoken label is the screen's own.** A reader should hear "arrange the
 * cards" or "edit these notes" rather than "edit", because the mark is general
 * and what it edits is not.
 */
@Composable
fun EditAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    /**
     * The screen's own tag, replacing the shared one.
     *
     * **A parameter rather than a `testTag` in the caller's modifier.** Two
     * `testTag` calls in one chain do not compose: the later one silently wins,
     * so the component's own tag would have overridden every screen's.
     */
    tag: String? = null,
) {
    val strings = LocalStrings.current
    val spoken = label ?: strings["action.edit"]
    IconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = spoken }
            .testTag(tag ?: HeaderActionTags.EDIT),
    ) {
        Icon(painter = painterResource(Symbols.edit), contentDescription = null)
    }
}
