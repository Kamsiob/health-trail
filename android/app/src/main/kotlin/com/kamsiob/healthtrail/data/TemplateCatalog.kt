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
        /**
         * The Today this situation ships, as pairs of card type and size.
         *
         * **Nobody ever sees a blank Today**, `DESIGN.md` 21.5, and this is law
         * 5 doing that work. The first card takes the lead.
         *
         * **A starting hand and nothing more.** It is editable from the first
         * minute without penalty, and the app never rearranges it afterward.
         */
        val startingHand: List<Pair<String, String>> = emptyList(),
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
                        startingHand = item.optJSONArray("starting_hand").toCards(),
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

    /**
     * One thing a family can ask a facility to do, as the catalog ships it.
     *
     * **The tag is the load bearing field and it is never optional.** It says
     * whether federal nursing home rules back the request or whether it is
     * something reasonable to ask for that nobody is required to agree to.
     * Showing an instruction without it would let the app imply a right that
     * may not exist, which is the sharpest way this project could break rule 2.
     */
    data class Instruction(
        val id: String,
        val name: String,
        val wording: String,
        /** Either `federal` or `request`. The schema constrains it to those two. */
        val tag: String,
        /** Why the rules back it, in the catalog's words. Empty for a request. */
        val basis: String,
        /** How to actually ask for it. Practical, never legal advice. */
        val askFor: String,
    )

    /** What a tag means, in words the person reads rather than a code. */
    data class InstructionTag(val label: String, val explainer: String)

    /**
     * The eleven starters and the two tags, read together because an
     * instruction without its tag's wording cannot be rendered safely.
     */
    data class Instructions(
        val starters: List<Instruction>,
        val tags: Map<String, InstructionTag>,
    )

    suspend fun instructions(context: Context): Instructions = withContext(Dispatchers.IO) {
        val root = JSONObject(
            context.assets.open("templates/progress-and-instructions.json")
                .bufferedReader().use { it.readText() }
        )
        val tagsObject = root.getJSONObject("standing_instruction_tags")
        val tags = tagsObject.keys().asSequence().associateWith { key ->
            val item = tagsObject.getJSONObject(key)
            InstructionTag(
                label = item.getString("label"),
                explainer = item.getString("explainer"),
            )
        }
        val array = root.getJSONArray("standing_instructions")
        Instructions(
            starters = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Instruction(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    wording = item.getString("wording"),
                    tag = item.getString("tag"),
                    basis = item.optString("basis"),
                    askFor = item.optString("ask_for"),
                )
            },
            tags = tags,
        )
    }

    /**
     * A bureaucratic process the family has to run, as the catalog ships it.
     *
     * **These are the app's reason for existing at the sharp end.** A Medicaid
     * application, an appeal against a discharge, a records request. Each is a
     * long process that stalls on other people, and the value of the template
     * is the ordered steps, which somebody would otherwise have to discover one
     * phone call at a time.
     *
     * `stateVariance` marks a process whose rules differ by state. It is a
     * build instruction rather than a label: the steps stay general and the app
     * never states a rule for a particular state, because it does not know one.
     */
    data class ProjectTemplate(
        val id: String,
        val name: String,
        val subtitle: String,
        /**
         * What the person is trying to do, which is how the picker groups the
         * sixteen. One of `paying`, `challenge`, `moving`, `papers`, held to
         * that set by `check_templates.py` and labeled per locale under
         * `projects.category.*`.
         *
         * **Not `phase`**, which is the order these were built in and never
         * reaches a screen. Grouping by that would be the app organizing
         * somebody's options around its own history, which is rule 20.
         */
        val category: String,
        val stateVariance: Boolean,
        val roles: List<String>,
        /**
         * The starting steps, the third of the five template defaults.
         *
         * **Suggestions of structure, not instructions to act**, `DESIGN.md`
         * 20.4: every one can be deleted and the app never says do this now.
         */
        val steps: List<String>,
        /**
         * Which of the three answers a project from this template opens with,
         * and therefore which of the three shapes it takes. One of `standing`,
         * `date`, `steps`, held to that set by `check_templates.py` and refused
         * by the schema's own CHECK otherwise.
         *
         * **A default and never a cage**, 20.3: it is one control on the
         * project's setup screen and changes with no penalty.
         */
        // **The defaults on these four match the reader's own fallbacks**, so a
        // template built in a test reads the same as one built from a body that
        // predates the field. They do not make the shape optional in the data:
        // check_templates.py requires all three of lead, stages and date_kinds
        // on every one of the sixteen, and the schema's CHECK refuses an unknown
        // lead outright.
        val lead: String = "standing",
        /** The named stretches of the road, drawn as the road strip. 20.4. */
        val stages: List<String> = emptyList(),
        /** The kinds of date this situation tends to have, offered as chips. */
        val dateKinds: List<String> = emptyList(),
        /**
         * The usual papers, as named placeholders, empty until filled.
         *
         * **An empty placeholder reads "not yet", never as an error.** This is
         * the `documents` list in the data, which was already the usual papers
         * before the grid named it one of the five defaults, so there is one
         * list rather than two that drift.
         */
        val papers: List<String> = emptyList(),
    )

    suspend fun projects(context: Context): List<ProjectTemplate> =
        withContext(Dispatchers.IO) {
            val root = JSONObject(
                context.assets.open("templates/projects.json")
                    .bufferedReader().use { it.readText() }
            )
            val array = root.getJSONArray("templates")
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ProjectTemplate(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    subtitle = item.optString("subtitle"),
                    category = item.optString("category"),
                    stateVariance = item.optBoolean("state_variance", false),
                    roles = item.optJSONArray("roles").toLabels(),
                    steps = item.optJSONArray("steps").toStrings(),
                    lead = item.optString("lead", "standing"),
                    stages = item.optJSONArray("stages").toStrings(),
                    dateKinds = item.optJSONArray("date_kinds").toStrings(),
                    papers = item.optJSONArray("documents").toStrings(),
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

    private fun org.json.JSONArray?.toCards(): List<Pair<String, String>> {
        if (this == null) return emptyList()
        return (0 until length()).map {
            val card = getJSONObject(it)
            card.getString("type") to card.optString("size", "small")
        }
    }

    private fun org.json.JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
