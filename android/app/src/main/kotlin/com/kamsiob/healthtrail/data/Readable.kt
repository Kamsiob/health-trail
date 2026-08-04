package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText

/**
 * Turning part of the notebook into something a person can read.
 *
 * `MASTER_SPEC.md` 4.9: everything shareable is generated locally and handed to
 * the system share sheet. No account, no server, no link.
 *
 * **The governing sentence is the last one of 4.9:** every export must be
 * legible standalone to a reader who has never seen the app. That reader is
 * usually a sibling in another state, sometimes a case manager, and
 * occasionally a lawyer. They will not be told what a "thread" is, they do not
 * know the app has sections, and they are reading on a phone in a hurry.
 *
 * So this writes sentences and dates, not a data dump. Nothing here is an
 * identifier, nothing is a field name, and nothing needs a legend.
 *
 * **Pure, and tested as vectors.** Strings and rows in, one string out. No
 * Android, no file system, no clock: the timestamp comes from the caller. That
 * is the shape `Digest` uses and the reason both can be checked exhaustively
 * without a device.
 *
 * **It never concludes.** Rule 2. It states what was recorded and when, and it
 * says plainly at the end that it is somebody's own notes rather than a
 * clinical record, because a document that looks official and is not is worse
 * than one that says what it is.
 */
object Readable {

    /**
     * An incident, from first report to wherever it has got to.
     *
     * The last criterion of the incident journey in `MASTER_SPEC.md` 4.7, "its
     * own export", and the first thing 4.9 lists after the family update.
     */
    fun incident(
        strings: Strings,
        subjectName: String?,
        incident: Repository.Incident,
        entries: List<Repository.TrailEntry>,
    ): String = buildString {
        appendLine(incident.title.ifBlank { strings["incidents.untitled"] })
        appendLine("=".repeat(incident.title.ifBlank { strings["incidents.untitled"] }.length))
        appendLine()

        // **Who this is about, said once at the top.** A reader who has never
        // seen the app has no context at all, and a document about somebody's
        // mother that never names her is a document about nobody.
        subjectName?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.about", "name" to it))
        }

        incident.reportedEdtf?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.reported", "date" to EventDateText.render(strings, it)))
        }
        incident.chapterName?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.where", "place" to it))
        }
        appendLine(
            if (incident.isOpen) strings["readable.state.open"]
            else strings["readable.state.answered"],
        )

        incident.description?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(it)
        }

        if (entries.isNotEmpty()) {
            appendLine()
            appendLine(strings["readable.what.happened"])
            appendLine("-".repeat(strings["readable.what.happened"].length))
            entries.forEach { entry ->
                appendLine()
                val date = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                    ?: strings["date.unknown"]
                appendLine(date)
                entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                entry.body?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            }
        }

        incident.resolutionNote?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(strings["incident.resolution"])
            appendLine("-".repeat(strings["incident.resolution"].length))
            appendLine(it)
        }

        appendLine()
        appendLine(footer(strings))
    }

    /**
     * A prep sheet, as something to hold in a meeting or hand to a sibling.
     *
     * **The one document here somebody reads while a professional is waiting.**
     * So the questions come first, numbered, because that is the order they
     * will be asked in and because a numbered list survives being read aloud
     * from a phone in a room where somebody else is talking.
     */
    fun prep(
        strings: Strings,
        subjectName: String?,
        prep: Repository.Prep,
    ): String = buildString {
        val title = prep.appointment.title.ifBlank { strings["prep.untitled"] }
        appendLine(title)
        appendLine("=".repeat(title.length))
        appendLine()

        subjectName?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.about", "name" to it))
        }
        prep.appointment.scheduledEdtf?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.when", "date" to EventDateText.render(strings, it)))
        }
        prep.appointment.locationNote?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.where", "place" to it))
        }

        appendLine()
        appendLine(strings["prep.questions"])
        appendLine("-".repeat(strings["prep.questions"].length))
        if (prep.questions.isEmpty()) {
            // **Not the screen's words.** The screen tells the person where
            // questions come from, which is a sentence about this app. This
            // document is read by a sibling three states away who does not have
            // it, so it says the plain fact and nothing about where to type.
            appendLine(strings["readable.questions.none"])
        } else {
            prep.questions.forEachIndexed { index, question ->
                val who = question.roleLabel?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                appendLine("${index + 1}. ${question.text}$who")
            }
        }

        appendLine()
        appendLine(strings["prep.changes"])
        appendLine("-".repeat(strings["prep.changes"].length))
        appendLine(
            prep.sinceEdtf?.takeIf { it.isNotBlank() }
                ?.let {
                    strings("prep.changes.since", "date" to EventDateText.render(strings, it))
                }
                ?: strings["prep.changes.all"],
        )
        if (prep.changes.isEmpty()) {
            appendLine()
            appendLine(strings["prep.changes.empty"])
        } else {
            prep.changes.forEach { entry ->
                appendLine()
                val date = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                    ?: strings["date.unknown"]
                appendLine(date)
                entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                entry.body?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            }
        }

        appendLine()
        appendLine(footer(strings))
    }

    /**
     * What every shared document ends with.
     *
     * **This is not boilerplate and it is not a disclaimer for the app's
     * benefit.** A reader holding a tidy dated document will assume it is an
     * official record unless it says otherwise, and the person who wrote it may
     * be handing it to somebody who will act on it. Saying whose notes these
     * are is the honest thing and it is also what keeps the app inside rule 2.
     */
    private fun footer(strings: Strings): String = strings["readable.footer"]

    /**
     * A file name a person can find again on a computer six months later.
     *
     * **No identifiers, no timestamps to the millisecond, and no slashes.** A
     * name is a thing somebody reads in a downloads folder, so it is the words
     * they already know plus the date. Punctuation a file system dislikes is
     * removed rather than escaped, because an escaped file name is a file name
     * nobody recognizes.
     */
    /**
     * The emergency card, as something to hand to a stranger.
     *
     * **This is the one document in the app most likely to be read by somebody
     * who has never heard of it**, in a corridor, on a phone that is not this
     * one. So it is ordered the way it would be needed rather than the way it is
     * stored: who to call, then what they take, then the things that change what
     * somebody does in the next ten minutes.
     *
     * **It carries no advice and draws no conclusion.** It repeats what the
     * person wrote down, which for allergies and conditions is the whole point
     * and for anything else would be the app overstepping.
     */
    fun emergencyCard(
        strings: Strings,
        subjectName: String?,
        card: Repository.EmergencyCard?,
        contacts: List<Repository.EmergencyContact>,
        medications: List<Repository.Medication>,
    ): String = buildString {
        val title = strings["readable.card.title"]
        appendLine(title)
        appendLine("=".repeat(title.length))
        appendLine()

        subjectName?.takeIf { it.isNotBlank() }?.let {
            appendLine(strings("readable.about", "name" to it))
            appendLine()
        }

        if (contacts.isNotEmpty()) {
            appendLine(strings["emergency.group.who"])
            appendLine("-".repeat(strings["emergency.group.who"].length))
            contacts.forEach { contact ->
                appendLine()
                appendLine(contact.displayName)
                contact.relationship?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                contact.phone?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            }
            appendLine()
        }

        if (medications.isNotEmpty()) {
            appendLine(strings["emergency.group.meds"])
            appendLine("-".repeat(strings["emergency.group.meds"].length))
            medications.forEach { medication ->
                appendLine()
                appendLine(medication.name)
                listOfNotNull(
                    medication.doseText?.takeIf { it.isNotBlank() },
                    medication.purposeText?.takeIf { it.isNotBlank() },
                ).takeIf { it.isNotEmpty() }?.let { appendLine(it.joinToString(", ")) }
            }
            appendLine()
        }

        val hurry = listOfNotNull(
            card?.allergies?.takeIf { it.isNotBlank() }
                ?.let { strings["emergency.allergies"] to it },
            card?.bloodType?.takeIf { it.isNotBlank() }
                ?.let { strings["emergency.blood_type"] to it },
            card?.conditions?.takeIf { it.isNotBlank() }
                ?.let { strings["emergency.conditions"] to it },
        )
        if (hurry.isNotEmpty()) {
            appendLine(strings["emergency.group.medical"])
            appendLine("-".repeat(strings["emergency.group.medical"].length))
            hurry.forEach { (label, value) ->
                appendLine()
                appendLine(label)
                appendLine(value)
            }
            appendLine()
        }

        // The same sentence the app says on every screen it hands somebody.
        // A stranger reading this has no way to know it is one family's notes
        // unless it says so.
        appendLine()
        appendLine(strings["readable.footer"])
    }

    fun fileName(title: String, isoDate: String, fallback: String): String {
        val cleaned = title.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .take(60)
            .trim()
        val base = cleaned.ifBlank { fallback }
        return "$base $isoDate.txt"
    }
}
