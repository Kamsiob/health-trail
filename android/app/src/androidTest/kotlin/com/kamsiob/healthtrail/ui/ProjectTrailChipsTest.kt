package com.kamsiob.healthtrail.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.Strings
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every kind of thing that can land on a project's trail has a chip label.
 *
 * **The chip label is looked up by a computed key**, `project.trail.filter.$kind`,
 * which is exactly the shape `check_string_keys.py` cannot see: it holds the
 * string literals in the code against the catalogs, and a key built at runtime
 * is not a literal. `Strings.resolve` throws rather than falling back, which is
 * correct and means a missing one crashes the app on opening the screen.
 *
 * **The list here is `entry.kind`'s own CHECK constraint from
 * `contract/schema.sql`, plus the two synthetic kinds.** A kind added to the
 * schema without a label fails this rather than crashing somebody's phone.
 */
@RunWith(AndroidJUnit4::class)
class ProjectTrailChipsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** What `entry.kind` allows, verbatim, plus the road and the dates. */
    private val everyKind = listOf(
        "call", "visit", "incident", "measurement",
        "question", "document", "note", "transfer", "milestone",
        "stages", "dates",
    )

    @Test
    fun everyKindOnTheTrailHasAChipLabelInEveryLocale() {
        for (locale in listOf("en", "es", "zh", "ar")) {
            val strings = Strings.load(context, Locale.forLanguageTag(locale))
            for (kind in everyKind) {
                // transfer and milestone fold into notes, the same way
                // kindNameKey folds them, so those two are covered by it.
                val key = when (kind) {
                    "transfer", "milestone" -> "project.trail.filter.note"
                    else -> "project.trail.filter.$kind"
                }
                val label = strings[key]
                assert(label.isNotBlank()) { "$locale has no label for $key" }
            }
        }
    }

    @Test
    fun theAllChipCarriesItsCount() {
        val strings = Strings.load(context, Locale.ENGLISH)
        val label = strings("project.trail.filter.all", "count" to 7)
        assert(label.contains("7")) { "the All chip lost its count: $label" }
    }
}
