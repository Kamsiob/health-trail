package com.kamsiob.healthtrail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import java.time.LocalDate
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.Destination
import androidx.compose.foundation.background
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureSheet
import com.kamsiob.healthtrail.ui.screens.edtf
import com.kamsiob.healthtrail.ui.screens.entryKind
import com.kamsiob.healthtrail.ui.screens.usesTheSharedForm
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.emphasisFrom
import com.kamsiob.healthtrail.ui.screens.MeasurementDraft
import com.kamsiob.healthtrail.ui.screens.MeasurementScreen
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.UnfiledTrayScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ShellTags {
    const val ROOT = "shell_root"
    const val NOT_BUILT = "shell_not_built"
    const val ERROR = "shell_error"
}

/**
 * The notebook itself: four destinations and the capture button, on every screen.
 *
 * **The section order is fixed here, in one place.** It is the order in
 * `MASTER_SPEC.md` section 4.4, and it never varies by situation template,
 * because a table of contents whose order shifts is not a table of contents. The
 * template decides emphasis, not availability.
 *
 * Counts are read through the repository, which means through the `live_*`
 * views, so anything the person deleted is already excluded rather than filtered
 * out here.
 */
@Composable
fun NotebookShell(repository: Repository) {
    val strings = LocalStrings.current
    var destination by remember { mutableStateOf(Destination.NOTEBOOK) }
    var counts by remember { mutableStateOf<List<SectionCount>?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf<CaptureKind?>(null) }
    // Bumped after every write, which is what makes the counts refresh without
    // the screen having to know what changed.
    var revision by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf<CaptureDraft?>(null) }
    // The entry being filed out of the tray, and where it is going. Null means
    // nothing is in flight.
    var filing by remember { mutableStateOf<Pair<String, String?>?>(null) }
    // What this notebook already tracks, and the catalog of things it could.
    var measures by remember { mutableStateOf<List<Repository.Measure>>(emptyList()) }
    var presets by remember { mutableStateOf<List<TemplateCatalog.Preset>>(emptyList()) }
    var recording by remember { mutableStateOf<MeasurementDraft?>(null) }
    // The threads this notebook carries, which the capture form offers as chips.
    // Empty is a real state: a notebook with no situation template has none, and
    // the form drops that question rather than showing an answerless one.
    var threads by remember { mutableStateOf<List<Repository.CareThread>>(emptyList()) }
    // The counts could not be read. Distinct from not having read them yet,
    // which is what a null `counts` means.
    var failed by remember { mutableStateOf(false) }
    // Everything the person saved without saying where it belonged. The capture
    // form already promises them this tray exists.
    var unfiled by remember { mutableStateOf<List<Repository.UnfiledEntry>>(emptyList()) }
    var trayOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Recounted whenever the tab changes, so returning to the notebook after
    // writing something shows the new number rather than a stale one.
    //
    // Wrapped, because a read can fail and a table of contents that throws
    // takes the whole app down. The screen says so and offers the read again,
    // per DESIGN.md section 10.3, which requires an error state on every screen
    // rather than only on the ones where a failure was imagined.
    LaunchedEffect(destination, revision) {
        failed = false
        try {
            val subject = repository.activeSubject()
            val emphasis = emphasisFor(context, subject?.situationTemplateId)
            // Counts belong to a subject. With no subject there is no notebook
            // to count, and showing zeros would be inventing a notebook that
            // does not exist yet.
            counts = subject?.let { active ->
                SECTION_ORDER.map {
                    SectionCount(
                        it,
                        repository.count(it, active.id),
                        emphasis[it] ?: Emphasis.STANDING,
                    )
                }
            } ?: SECTION_ORDER.map { SectionCount(it, 0, Emphasis.STANDING) }
            threads = subject?.let { repository.threads(it.id) }.orEmpty()
            unfiled = subject?.let { repository.unfiled(it.id) }.orEmpty()
            measures = subject?.let { repository.measures(it.id) }.orEmpty()
            presets = TemplateCatalog.presets(context)
        } catch (t: Throwable) {
            failed = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ShellTags.ROOT),
        color = HealthTrail.colors.paper,
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Box(modifier = Modifier.weight(1f)) {
                when (destination) {
                    Destination.NOTEBOOK -> {
                        val loaded = counts
                        when {
                            failed -> CouldNotRead(onRetry = { revision += 1 })
                            loaded == null -> Loading()
                            else -> NotebookScreen(
                                sections = loaded,
                                onOpen = { },
                                waiting = unfiled.size,
                                onOpenUnfiled = { trayOpen = true },
                            )
                        }
                    }
                    // These arrive in their own increments. Until then each
                    // says plainly what it is rather than showing an empty
                    // frame, because a blank area reads as broken.
                    // Today's empty state is a finished screen, per #78 and
                    // persona P1. What is not built is the digest, and the
                    // screen says so itself rather than standing in for it.
                    Destination.TODAY -> TodayScreen(
                        isEmpty = (counts?.sumOf { it.count } ?: 0) == 0,
                    )
                    Destination.PROJECTS -> NotBuiltYet(strings["nav.projects"])
                    Destination.MORE -> NotBuiltYet(strings["nav.more"])
                }
            }

            BottomNav(
                current = destination,
                onSelect = { destination = it },
                onCapture = { sheetOpen = true },
                labels = {
                    when (it) {
                        Destination.TODAY -> strings["nav.today"]
                        Destination.NOTEBOOK -> strings["nav.notebook"]
                        Destination.PROJECTS -> strings["nav.projects"]
                        Destination.MORE -> strings["nav.more"]
                    }
                },
                captureDescription = strings["capture.button.description"],
            )
        }

        if (trayOpen) {
            UnfiledTrayScreen(
                entries = unfiled,
                threads = threads,
                onFile = { entryId, threadId ->
                    filing = entryId to threadId
                },
                onClose = { trayOpen = false },
            )
        }

        val toFile = filing
        if (toFile != null) {
            LaunchedEffect(toFile) {
                repository.fileEntry(toFile.first, toFile.second)
                filing = null
                // Recount, which also refreshes the tray and closes it when the
                // last waiting entry has been filed.
                revision += 1
            }
        }

        if (sheetOpen) {
            CaptureSheet(
                onChoose = { kind ->
                    sheetOpen = false
                    capturing = kind
                },
                onDismiss = { sheetOpen = false },
            )
        }

        // Four kinds, one form. They differ in wording rather than in shape, so
        // they share a screen rather than appearing as four near copies. Which
        // four is declared once, next to the form, rather than repeated here.
        //
        // Measurement and document arrive in their own increments. Until then
        // choosing one closes the sheet and does nothing rather than opening an
        // empty screen, which is the lesser of two bad interim states.
        val kind = capturing
        if (kind != null && kind.usesTheSharedForm) {
            CaptureFormScreen(
                kind = kind,
                threads = threads,
                onSave = { draft ->
                    capturing = null
                    saving = draft
                },
                onCancel = { capturing = null },
            )
        }

        // **Document says so rather than doing nothing.** The sheet offers six
        // ways in and this one is not built, so choosing it used to close the
        // sheet and silently do nothing, which is the app appearing to lose
        // what someone tried to save. It now says plainly that it is not built
        // and why, which is the same honesty the not-yet-built destinations
        // carry, and it is greppable through ShellTags.NOT_BUILT so it cannot
        // survive to release. Issue #57.
        if (kind == CaptureKind.DOCUMENT) {
            NotBuiltYet(
                name = strings["capture.document"],
                detail = strings["capture.not_built"],
                onClose = { capturing = null },
            )
        }

        // Measurement has its own screen because it does not fit the shared
        // form: a value needs to know what is being measured before anything
        // else on the screen means anything.
        if (kind == CaptureKind.MEASUREMENT) {
            MeasurementScreen(
                measures = measures,
                presets = presets,
                onSave = { draft ->
                    capturing = null
                    recording = draft
                },
                onCancel = { capturing = null },
            )
        }

        val measurement = recording
        if (measurement != null) {
            LaunchedEffect(measurement) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // A measure is created the first time the person records
                    // one, never at setup. A notebook that arrives with sixteen
                    // empty charts is a list of things somebody has not done.
                    val measureId = measurement.measureId ?: repository.createMeasure(
                        subjectId = subject.id,
                        preset = measurement.preset!!,
                        unit = measurement.unit,
                        sortIndex = measures.size,
                    )
                    repository.recordMeasurement(
                        measureId = measureId,
                        number = measurement.number,
                        text = measurement.text,
                        unit = measurement.unit,
                        occurred = measurement.occurred,
                        note = measurement.note,
                    )
                }
                recording = null
                revision += 1
                destination = Destination.NOTEBOOK
            }
        }

        val draft = saving
        if (draft != null) {
            LaunchedEffect(draft) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    val entryId = repository.createEntry(
                        subjectId = subject.id,
                        kind = draft.kind.entryKind(),
                        title = draft.who,
                        body = draft.note,
                        occurred = draft.occurred,
                        // An entry nobody could place goes to the Unfiled tray
                        // rather than being filed by the app. Only marked when
                        // there were threads to choose from: with none offered,
                        // not choosing one is not the person declining to say.
                        isUnfiled = threads.isNotEmpty() && draft.threadId == null,
                    )
                    draft.threadId?.let { repository.linkEntryToThread(entryId, it) }
                    // A call carries an extra row for whether anyone picked up.
                    // The other three are fully described by the entry itself.
                    if (draft.kind == CaptureKind.CALL) {
                        repository.addCallDetail(entryId = entryId)
                    }
                }
                saving = null
                revision += 1
                destination = Destination.NOTEBOOK
            }
        }
    }
}

/**
 * The fixed order of the table of contents, from `MASTER_SPEC.md` section 4.4.
 * Projects sit outside the notebook and have their own destination, so they are
 * not in this list.
 */
/**
 * What weight each section carries, read from the active situation template.
 *
 * **The template decides emphasis and nothing else.** It cannot add a section,
 * cannot remove one, and cannot move one, so this returns a weight per section
 * and never an order. A subject with no template, which is what "Not sure yet"
 * on the situation picker leaves behind, gets an empty map and therefore a
 * notebook where every section stands at the same weight. That is a complete
 * notebook, not a degraded one.
 *
 * A template id that is not in the catalog, which is what an export from a
 * later version of the app would carry, falls through to the same empty map
 * rather than failing.
 */
private suspend fun emphasisFor(
    context: android.content.Context,
    templateId: String?,
): Map<Repository.Section, Emphasis> {
    if (templateId == null) return emptyMap()
    val situation = TemplateCatalog.situations(context).all.firstOrNull { it.id == templateId }
        ?: return emptyMap()
    return emphasisFrom(forward = situation.forward, folded = situation.folded)
}

private val SECTION_ORDER = listOf(
    Repository.Section.CARE_TEAM,
    Repository.Section.MEDICATIONS,
    Repository.Section.APPOINTMENTS,
    Repository.Section.CHAPTERS,
    Repository.Section.THREADS,
    Repository.Section.TRAIL,
    Repository.Section.PROGRESS,
    Repository.Section.DOCUMENTS,
    Repository.Section.MONEY,
    Repository.Section.STANDING_INSTRUCTIONS,
    Repository.Section.ASK_NEXT_TIME,
    Repository.Section.EMERGENCY_CARD,
)

@Composable
private fun Loading() {
    val strings = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = strings["common.loading"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
    }
}

/**
 * The counts could not be read.
 *
 * It says what happened, says plainly that nothing was lost, and offers the one
 * action that might fix it. It does not apologize for the software and it does
 * not explain the exception, because neither helps the person holding the phone.
 */
@Composable
private fun CouldNotRead(onRetry: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.l)
            .testTag(ShellTags.ERROR),
    ) {
        Text(
            text = strings["notebook.title"],
            style = HealthTrail.type.displayL,
            color = HealthTrail.colors.ink,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["common.error.generic"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
        Spacer(Modifier.height(Space.m))
        FilledButton(label = strings["common.retry"], onClick = onRetry)
    }
}

/**
 * A destination that exists in the navigation but is not built yet.
 *
 * This is a real screen rather than a stub: it names itself and says plainly
 * that it is not built, which is honest. What it must never be is a blank area,
 * because a blank area reads as broken and the person cannot tell the difference
 * between "nothing here yet" and "this app is failing".
 *
 * It disappears entirely as each destination lands. If one of these is still
 * here at release, that is a bug.
 */
@Composable
private fun NotBuiltYet(
    name: String,
    detail: String? = null,
    onClose: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthTrail.colors.paper)
            .systemBarsPadding()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.l)
            .testTag(ShellTags.NOT_BUILT),
    ) {
        Text(text = name, style = HealthTrail.type.displayL, color = HealthTrail.colors.ink)
        Spacer(Modifier.height(Space.s))
        Text(
            text = detail ?: strings["shell.not_built"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
        if (onClose != null) {
            Spacer(Modifier.height(Space.l))
            TextAction(label = strings["common.close"], onClick = onClose)
        }
    }
}
