package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.i18n.formatMoney

/**
 * The archive's vocabulary, read out of the shared catalogs.
 *
 * **This exists so that [ReadableArchive] does not have to.** That object is
 * pure by contract and the regeneration test in `contract/DATA-CONTRACT.md` 8.5
 * rests on it, so it takes its words as an argument rather than looking any of
 * them up. Something has to do the looking up, and this is the seam: catalog on
 * one side, a plain value on the other.
 *
 * **The label set is derived, never listed.** Which tables and columns need a
 * word is decided in `contract/readable-fields.json`, the same file that decides
 * what gets rendered at all, so a column added tomorrow appears here the moment
 * it is given a rendering decision. A hand-written list beside that file would
 * be a second declaration and it would drift, which is D16's whole argument.
 *
 * **What holds the catalogs to it is `check_readable_labels.py`.** These keys
 * are built from a variable, and `docs/TRAPS.md` section 3 is explicit that a
 * key built that way is checked by nothing: `check_string_keys.py` skips it by
 * design. So the whole set is held some other way, which is that check, run in
 * `run_all.py` beside the other seventeen. Without it a column added to the
 * schema would render with an English fallback in an Arabic archive and nothing
 * anywhere would say so, which is exactly the failure #327 was.
 */
internal object ReadableWords {

    /**
     * @param strings the catalog the person is actually running, which is not
     *   the same question as `Locale.getDefault()`. `Strings.load` walks the
     *   per-app language first, then the device preference list, because Android
     *   does not reliably put the requested app language at the front of the
     *   configuration. The export used to ask the default locale and could
     *   therefore stamp `lang="en"` on an archive whose app was set to Chinese.
     * @param fieldMap which tables and columns are rendered, so the label set is
     *   exactly the set that reaches a page.
     */
    fun from(
        strings: Strings,
        fieldMap: Map<String, ReadableArchive.TableFields> = ReadableFieldMap.tables,
    ): ReadableArchive.Words {
        val tables = fieldMap.keys
            .filter { fieldMap.getValue(it).rendered.isNotEmpty() }
            .associateWith { strings["archive.table.$it"] }

        val columns = fieldMap.values
            .flatMap { it.rendered }
            // A zone is rendered as part of its own date and never on its own,
            // and a currency inside its own amount, so neither shows a label
            // and neither needs one.
            .filterNot { it.render in RENDERED_INSIDE_ANOTHER }
            .map { it.column }
            .distinct()
            .associateWith { strings["archive.field.$it"] }

        // **Only the vocabularies something actually renders.** Derived from
        // the field map the same way the labels are, so a column that stops
        // being an enum stops needing words on the same day.
        val vocabularies = fieldMap.values
            .flatMap { it.rendered }
            .mapNotNull { it.vocabulary }
            .distinct()
            .associateWith { vocabulary ->
                ReadableFieldMap.vocabularies[vocabulary].orEmpty().associateWith { value ->
                    strings["archive.vocabulary.$vocabulary.$value"]
                }
            }

        return ReadableArchive.Words(
            lang = languageTag(strings),
            dir = if (strings.isRtl) "rtl" else "ltr",
            tables = tables,
            columns = columns,
            vocabularies = vocabularies,
            money = { minor, code -> formatMoney(strings, minor, code) },
            subjectFallback = strings["archive.page.subject.fallback"],
            about = strings["archive.page.about"],
            datedHeading = strings["archive.page.dated.heading"],
            wholeHeading = strings["archive.page.whole.heading"],
            howToHeading = strings["archive.page.howto.heading"],
            howToBody = strings["archive.page.howto.body"],
            back = strings["archive.page.back"],
            undated = strings["archive.page.year.undated"],
            notRecorded = strings["archive.value.not_recorded"],
            yes = strings["archive.value.yes"],
            no = strings["archive.value.no"],
            covers = { from, to ->
                if (from == to) {
                    strings("archive.page.covers.one", "year" to from)
                } else {
                    strings("archive.page.covers.range", "from" to from, "to" to to)
                }
            },
            yearTitle = { section, year ->
                strings("archive.page.year.title", "section" to section, "year" to year)
            },
            records = { count -> strings("archive.page.records", "count" to count) },
        )
    }

    /**
     * What goes in `<html lang="...">`.
     *
     * **`zh-Hans` rather than a bare `zh`**, per D52. A bare tag carries no
     * script, and the catalog this app ships is Simplified. The tag is read by
     * screen readers and by whatever opens this file in ten years, so being
     * specific costs nothing and being vague costs a reader.
     */
    private fun languageTag(strings: Strings): String =
        when (strings.locale.language) {
            "zh" -> "zh-Hans"
            else -> strings.locale.toLanguageTag()
        }

    /**
     * The decisions whose column is drawn inside another field rather than on
     * its own, so neither ever shows a label of its own.
     */
    private val RENDERED_INSIDE_ANOTHER = setOf("dateZone", "moneyCurrency")
}
