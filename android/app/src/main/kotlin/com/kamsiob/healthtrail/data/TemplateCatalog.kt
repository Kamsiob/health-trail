package com.kamsiob.healthtrail.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The bundled template catalog, read from the same JSON both platforms read.
 *
 * The app keeps no copy of this any more than it keeps a copy of the schema. The
 * build copies the `templates/data` JSON files into assets and this reads them there.
 *
 * **The posture strings are displayed verbatim.** `templates/SCHEMA.md` says so
 * explicitly: they are not paraphrased in the interface, because they are the
 * sentences that keep this content from reading as advice.
 */
object TemplateCatalog {

    /** A care setting: where the person is, and what that shape of care needs. */
    data class Situation(
        val id: String,
        val name: String,
        val subtitle: String,
        /** One sentence naming what is hard about this setting. */
        val burden: String,
        /**
         * Build priority, and also the order in the setup picker, so the
         * settings covering the most caregivers are the first ones read.
         */
        val phase: Int,
        /**
         * Which heading this sits under in the setup picker: `facility`,
         * `home`, `treatment`, or `comfort`. In the data rather than in one
         * platform's code, so both group identically.
         */
        val group: String,
        /** Care threads to offer as toggles. The person picks which are running. */
        val threads: List<Thread>,
        /** Contact roles to offer when adding a person. Suggestions, not a fixed list. */
        val roles: List<String>,
        /** First days checklist. Administrative actions only. */
        val checklist: List<String>,
        /** Document slots, each expecting a photo and a note on where the original lives. */
        val documents: List<String>,
        /**
         * Notebook sections this setting puts forward, by their id in the
         * template data. Emphasis only: the table of contents renders these
         * fullest and does not move them.
         */
        val forward: List<String>,
        /**
         * Notebook sections this setting folds. **Folded is collapsed and one
         * tap away, never hidden and never reordered.** A setting where money
         * rarely comes up quiets that row rather than removing it, because the
         * one month it does come up the person must find it where it always was.
         */
        val folded: List<String>,
        /** Handle copy with extra care. Currently only comfort focused care. */
        val sensitive: Boolean,
    )

    data class Thread(val id: String, val label: String)

    /** Strings the interface shows exactly as written. */
    data class Posture(val generalGuide: String, val recordOnly: String)

    data class Situations(val posture: Posture, val all: List<Situation>)

    suspend fun situations(context: Context): Situations = withContext(Dispatchers.IO) {
        val root = JSONObject(
            context.assets.open("templates/situations.json")
                .bufferedReader().use { it.readText() }
        )

        val postureJson = root.getJSONObject("posture")
        val posture = Posture(
            generalGuide = postureJson.optString("general_guide"),
            recordOnly = postureJson.optString("record_only"),
        )

        val array = root.getJSONArray("templates")
        val all = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Situation(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        subtitle = item.optString("subtitle"),
                        burden = item.optString("burden"),
                        phase = item.optInt("phase", LAST_PHASE),
                        group = item.optString("group"),
                        threads = item.optJSONArray("threads").toThreads(),
                        roles = item.optJSONArray("roles").toLabels(),
                        checklist = item.optJSONArray("checklist").toStrings(),
                        documents = item.optJSONArray("documents").toStrings(),
                        forward = item.optJSONArray("forward").toStrings(),
                        folded = item.optJSONArray("folded").toStrings(),
                        sensitive = item.optBoolean("sensitive", false),
                    )
                )
            }
        }
        Situations(posture, all)
    }

    /**
     * A template with no phase sorts last rather than first. An unknown
     * priority is not a high one, and a catalog edit that forgot the field
     * must not quietly push a rare setting to the top of the first screen
     * after the disclaimer.
     */
    private const val LAST_PHASE = Int.MAX_VALUE

    /**
     * One thing a family can choose to track over time.
     *
     * **`adviceRisk` is carried rather than acted on here.** It is what the
     * rendering layer reads to keep the content rules, and it never becomes a
     * warning to the person: this app records a value and stops, so a preset
     * marked high risk is one the app must be more careful about how it
     * *displays*, never one it cautions anyone about.
     */
    data class Preset(
        val id: String,
        val name: String,
        /** Empty where the thing being tracked is not a number. */
        val unitOptions: List<String>,
        /** How often a family typically records it. Shown as plain guidance, never as a schedule. */
        val cadence: String,
        val style: String,
        val gapTolerance: String,
        val adviceRisk: String,
        val medicationMarkers: Boolean,
    ) {
        /** True where the value is words rather than a number, per the preset's own style. */
        val isText: Boolean get() = unitOptions.isEmpty()
    }

    suspend fun presets(context: Context): List<Preset> = withContext(Dispatchers.IO) {
        val root = JSONObject(
            context.assets.open("templates/progress-and-instructions.json")
                .bufferedReader().use { it.readText() }
        )
        val array = root.getJSONArray("progress_presets")
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Preset(
                id = item.getString("id"),
                name = item.getString("name"),
                unitOptions = item.optJSONArray("unit_options").toStrings(),
                cadence = item.optString("cadence"),
                style = item.optString("style", "continuous"),
                gapTolerance = item.optString("gap_tolerance", "moderate"),
                adviceRisk = item.optString("advice_risk", "low"),
                medicationMarkers = item.optBoolean("medication_markers", false),
            )
        }
    }

    private fun org.json.JSONArray?.toThreads(): List<Thread> {
        if (this == null) return emptyList()
        return (0 until length()).map {
            val o = getJSONObject(it)
            Thread(o.getString("id"), o.getString("label"))
        }
    }

    private fun org.json.JSONArray?.toLabels(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getJSONObject(it).getString("label") }
    }

    private fun org.json.JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
