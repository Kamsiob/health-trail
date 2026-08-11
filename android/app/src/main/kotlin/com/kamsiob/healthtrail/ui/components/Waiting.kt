package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail

/**
 * The app is reading something and has nothing to show yet.
 *
 * **One word, quiet, centered, and that is the whole of it.** No spinner, no
 * progress bar, no skeleton rows. A progress bar would be claiming to know how
 * long a database read takes, which it does not, and skeleton rows draw a
 * shape the real answer may not have: an empty section would flash three gray
 * rows and then show nothing, which reads as something having been lost.
 *
 * **It is deliberately not designed up.** Rule 14 says visually plain is not
 * done, and it means the screens somebody sits with. This one is on screen for
 * a few hundred milliseconds and its whole job is to not be a moment. Anything
 * more here would be the app performing effort at somebody who is waiting.
 *
 * **It exists as one composable because there were two.** `AppRoot` had one
 * while the database opened and `NotebookShell` had another while the counts
 * loaded, six identical lines apart, which is how an app ends up with two
 * loading treatments that drift. Rule 22: one treatment for the whole app.
 */
@Composable
fun Waiting(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = strings["common.loading"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
    }
}
