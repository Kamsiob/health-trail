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
