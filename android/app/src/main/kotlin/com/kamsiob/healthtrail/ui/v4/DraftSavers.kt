package com.kamsiob.healthtrail.ui.v4

import androidx.compose.runtime.saveable.Saver
import com.kamsiob.healthtrail.time.Edtf

/**
 * What a half filled form is allowed to leave in the bundle, and how.
 *
 * **#371 item 7: only the capture form's draft survived process death.** Every
 * other form in the app held its state in a plain `remember`, so a phone that
 * killed the app while somebody took a call came back to an empty form. The
 * capture form's own comment calls losing a half written note "the worst thing
 * this app could do short of losing the notebook", and ten forms did it.
 *
 * **A date is saved as its canonical string and nothing else.** `Edtf.Date`
 * carries a precision and a qualifier, and both are read from the canonical
 * form rather than stored beside it, so the string is the whole value and
 * parsing it back is exact rather than approximate. Rule 17: the precision the
 * person expressed comes back as they expressed it.
 *
 * **Nothing here ever carries a passphrase.** The export and restore screens
 * keep theirs in a plain `remember` on purpose, so the secret that opens an
 * archive is never written to a bundle the system may persist to disk. Losing a
 * passphrase field to process death costs one retype. The alternative costs the
 * archive. That is said here because this file is where somebody would look for
 * a reason those two screens were skipped.
 */
val EdtfSaver: Saver<Edtf.Date?, Any> = Saver(
    save = { it?.canonical ?: "" },
    restore = { (it as String).takeIf(String::isNotEmpty)?.let(Edtf::parse) },
)
