package com.kamsiob.healthtrail.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.healthtrail.data.Backup
import com.kamsiob.healthtrail.data.ExportContainer
import com.kamsiob.healthtrail.data.MergeApply
import com.kamsiob.healthtrail.i18n.LocalStrings
import java.io.File

/**
 * Choosing a file, opening it, and putting a notebook back.
 *
 * **Lifted out of `NotebookShell` on 2026-08-13**, and it is the shape B6 asks
 * for rather than a tidy up. Every piece of state this needs is used by nothing
 * else: the file, the passphrase entered, the two guards, and the choice
 * between replacing and merging. Held here, the shell loses six state variables
 * and about a hundred and twenty lines from the one composable that is at the
 * JVM's method limit, and gains two.
 *
 * **And it is the reason the unrecoverable screen can offer a restore at all.**
 * #343: that screen runs above the shell, at `AppRoot`, where there is no open
 * repository, so the flow had to be something both can host. Telling somebody
 * to install the app again and restore from their file is the app declining to
 * absorb its own complexity, which is rule 20.
 *
 * **`tools/seed.sh` drives this screen**, so its tags and its steps are load
 * bearing outside the app as well as inside it.
 *
 * @param onApplied called after a restore or a merge succeeds, so a host that
 *   has a notebook on screen can reread it. `AppRoot` uses it to leave the
 *   unrecoverable state.
 */
@Composable
fun RestoreFlow(
    onBack: () -> Unit,
    onApplied: (RestoreHow) -> Unit = {},
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

    var state by remember { mutableStateOf<RestoreState>(RestoreState.Empty) }
    var file by remember { mutableStateOf<File?>(null) }
    var openWith by remember { mutableStateOf<String?>(null) }
    var openNow by remember { mutableStateOf(false) }
    var applyNow by remember { mutableStateOf(false) }
    var how by remember { mutableStateOf(RestoreHow.REPLACE) }

    val chooseFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val staged = File(context.cacheDir, "restore-source.htx")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    staged.outputStream().use { input.copyTo(it) }
                } ?: error("no input stream")
            }.onSuccess {
                file = staged
                openWith = null
                openNow = true
            }.onFailure {
                state = RestoreState.Problem(strings["export.failed"])
            }
        }
    }

    RestoreScreen(
        state = state,
        onChoose = {
            state = RestoreState.Empty
            chooseFile.launch(arrayOf("*/*"))
        },
        onUnlock = { entered -> openWith = entered; openNow = true },
        onRestore = { chosen -> how = chosen; applyNow = true },
        onBack = {
            state = RestoreState.Empty
            file = null
            onBack()
        },
    )

    // **Reading is separate from applying, and this is the reading half.**
    // Nothing about the notebook changes here, whatever the file turns out to
    // be.
    //
    // **The guard is cleared last, never first.** Setting it false at the top
    // of the effect removes the effect from composition and cancels the
    // coroutine before it finishes, which looked exactly like the file being
    // unreadable: the staging directory filled up and the screen never moved
    // off its empty state.
    if (openNow) {
        val source = file
        LaunchedEffect(source, openWith) {
            if (source == null) {
                openNow = false
                return@LaunchedEffect
            }
            val staging = File(context.cacheDir, "restore-staging")
            val result = ExportContainer.open(
                file = source,
                staging = staging,
                passphrase = openWith?.takeIf { it.isNotEmpty() }?.toCharArray(),
                expected = Backup.schema(context),
            )
            state = result.fold(
                onSuccess = { RestoreState.Ready(it.manifest) },
                onFailure = { failure ->
                    val problem = failure as? ExportContainer.ExportProblem
                    when (val p = problem?.problem) {
                        is ExportContainer.Problem.PassphraseNeeded ->
                            RestoreState.NeedsPassphrase()
                        // **A wrong passphrase leaves them here to try again.**
                        // Retyping is the expected case, and sending somebody
                        // back to pick the file a second time punishes a typo.
                        is ExportContainer.Problem.CouldNotDecrypt ->
                            RestoreState.NeedsPassphrase(p.message)
                        null -> RestoreState.Problem(
                            failure.message ?: strings["common.error.generic"],
                        )
                        else -> RestoreState.Problem(p.message)
                    }
                },
            )
            // **A failed open leaves the same plaintext behind.** #417. The
            // successful path keeps it, because Ready means the person is about
            // to choose replace or merge and the apply reads what is staged.
            if (state is RestoreState.Problem) {
                staging.deleteRecursively()
                source.delete()
            }
            openNow = false
        }
    }

    if (applyNow) {
        val source = file
        LaunchedEffect(source, how) {
            if (source == null) {
                applyNow = false
                return@LaunchedEffect
            }
            state = RestoreState.Working
            val staging = File(context.cacheDir, "restore-staging")
            val opened = ExportContainer.open(
                file = source,
                staging = staging,
                passphrase = openWith?.takeIf { it.isNotEmpty() }?.toCharArray(),
                expected = Backup.schema(context),
            )
            state = opened.fold(
                onSuccess = { container ->
                    // **Two promises, two functions.** Replace swaps the whole
                    // file; keeping both merges row by row and writes what it
                    // resolved. 8.3 requires the choice to be the person's
                    // rather than the app's, so it is carried here rather than
                    // decided here.
                    // **The report is carried rather than mapped away.** #454.
                    // This was `.map { 0 }`, which discarded inserted, updated,
                    // conflicts, attachments and skipped, so the screen could
                    // only say "Done. Both notebooks are here now."
                    val applied: Result<Any> = when (how) {
                        RestoreHow.MERGE ->
                            MergeApply.merge(context, container, System.currentTimeMillis())
                        else -> Backup.restore(context, container)
                    }
                    applied.fold(
                        onSuccess = { value ->
                            onApplied(how)
                            if (how == RestoreHow.MERGE) {
                                val report = value as? MergeApply.Report
                                if (report == null) {
                                    RestoreState.Merged()
                                } else {
                                    RestoreState.Merged(
                                        added = report.inserted,
                                        updated = report.updated,
                                        unchanged = report.unchanged,
                                        conflicts = report.conflicts,
                                        attachments = report.attachments,
                                        skipped = report.skipped.size,
                                        counted = true,
                                    )
                                }
                            } else {
                                RestoreState.Done
                            }
                        },
                        onFailure = { failure ->
                            RestoreState.Problem(
                                // A merge that refused says why in its own
                                // words, and those words are the ones the
                                // person needs rather than a generic failure.
                                if (failure is MergeApply.Refused) {
                                    strings["restore.refused"]
                                } else {
                                    failure.message ?: strings["common.error.generic"]
                                },
                            )
                        },
                    )
                },
                onFailure = {
                    RestoreState.Problem(it.message ?: strings["common.error.generic"])
                },
            )

            // **The staged copy is the whole record in the clear.** #417.
            //
            // `ExportContainer.open` removes `payload.enc` and `payload.zip`
            // and leaves `staging/trail.sqlite` and every extracted attachment,
            // and the payload is a plain unencrypted SQLite file by design,
            // contract 8.1. So after any restore, including one that failed,
            // the entire care record sat readable in `cacheDir`, which defeats
            // the encryption at rest guarantee against anyone who can read app
            // storage. `restore-source.htx` is the archive itself and was never
            // removed either.
            //
            // **Here rather than inside `open`**, because `Opened` hands the
            // database and the attachments to the caller and deleting them
            // there would delete what the caller is about to read.
            // `Backup.decryptedCopy` already cleans up on the export side, so
            // the pattern was in the building.
            staging.deleteRecursively()
            source.delete()

            applyNow = false
        }
    }
}
