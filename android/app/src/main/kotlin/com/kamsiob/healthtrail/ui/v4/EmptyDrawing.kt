package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.Symbols

/**
 * The drawing on an empty screen. `DESIGN.md` section 5.17.
 *
 * **Empty states were the biggest character opportunity in this app and the
 * thinnest thing in it.** One line of gray text, on the screen a new person sees
 * most, with nothing competing for their attention. Section 1 bans every cheap
 * way to make a screen interesting, which is right, and left nothing in its
 * place, so this is what the app's own vocabulary puts there instead.
 *
 * **The section's own mark, scaled up. Nothing here is invented.** Thirteen
 * freshly drawn illustrations would drift in weight and character however
 * carefully they were made. Reusing the mark also means an empty screen teaches
 * the icon the person will navigate by for the next two years, at the moment
 * they have nothing else to look at.
 *
 * **And it is now the mark they will actually see, D196.** This drew the old
 * hand-authored path off a 24 unit grid, at 1.7 of stroke: the alphabet the app
 * used before D182, kept alive in one component after every other surface had
 * moved to Material Symbols. So the empty screen taught a mark that appears
 * nowhere else in the app, which is the opposite of the argument above. It is
 * `Symbols.of(section)` now, the same file the row, the tile and the card head
 * all draw, and two hundred and thirty lines of path data are deleted with it.
 *
 * **Decorative, in the sense section 2.3 defines.** Remove every one of these
 * and nothing becomes unreadable, because the words carry the screen alone. So
 * they are exempt from the 3:1 ratio, they carry no content description, and
 * they are cleared from the semantics tree entirely: a reader announcing "line
 * drawing of a path" on every empty screen is noise rather than access.
 *
 * Banned here specifically, restating section 1 because this is where
 * illustration usually goes wrong: no 3D, no blobs, no plastic, no stock, no
 * mascot, no character, no scene with a person in it, and no color.
 */
@Composable
fun EmptyDrawing(
    section: Repository.Section?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    // **A place outside the thirteen gets nothing, and honestly nothing.**
    // Reserving the box with no drawing in it would be rule 11's blank area in
    // miniature on every screen that passes null.
    if (section == null) return

    Box(
        modifier = modifier.size(size).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Symbols.of(section)),
            contentDescription = null,
            modifier = Modifier.size(size * MARK_SCALE),
            // **Faint, because it is the quietest thing on a screen that is
            // already quiet.** A filled symbol carries far more ink than the
            // stroked path it replaces, so the mark is smaller and paler than
            // the drawing was and lands at about the same weight on the page.
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = MARK_ALPHA),
        )
    }
}

/** The mark takes a little over half the box, so it has air around it. */
private const val MARK_SCALE = 0.55f

/** Quiet enough to be a texture rather than a thing the eye stops on. */
private const val MARK_ALPHA = 0.14f
