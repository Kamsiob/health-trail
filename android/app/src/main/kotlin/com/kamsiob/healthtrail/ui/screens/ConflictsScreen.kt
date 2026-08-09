package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Merge
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.EmptyDrawing
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.Hairline
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.time.EventDateText
import java.time.Instant
import java.time.ZoneId

object ConflictsTags {
    const val NAME = "conflicts"
    const val EMPTY = "conflicts_empty"
    fun row(seq: Long) = "conflicts_row_$seq"
}

/**
 * What a merge decided, and what the version it did not keep said.
 * `contract/DATA-CONTRACT.md` 8.3, issue #211.
 *
 * **This screen is the second half of a promise the merge makes.** 8.3 requires
 * that a merge "writes every resolution to a conflict log the person can
 * actually open and read", and the schema says why in its own comment: a record
 * keeping app that quietly eats an entry has failed at its one job. Storing
 * both versions and giving nobody a way to look at them would have kept the
 * letter of that and lost the point.
 *
 * **It is not the JSON.** Both versions are kept whole in the database so that
 * either can be recovered by hand, and that is a storage promise. Rule 20: any
 * time the interface asks somebody to understand how the app stores something,
 * that is the code failing to absorb its own complexity. So each resolution
 * shows **only the fields that actually differed**, with what was kept and what
 * the other one said, and the bookkeeping columns that change on every write
 * are left out because "the revision number went from 3 to 4" tells nobody
 * anything about their own record.
 *
 * **Nothing here is a failure and the screen never says it is.** Two versions
 * existing is what happens when a person keeps notes on two phones, which is
 * the situation the merge was built for. The tone is a notice, not an alarm.
 *
 * **Not a drawn screen.** Composed from the existing grouped surface, group
 * header and empty drawing, per rule 12, and logged where rule 12 says to log
 * it.
 */
@Composable
fun ConflictsScreen(
    resolutions: List<Repository.Resolution>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = ConflictsTags.NAME,
        title = strings["conflicts.title"],
        subtitle = strings["conflicts.lead"],
        onBack = onBack,
        backLabelKey = "section.back.conflicts",
        modifier = modifier,
    ) {
        if (resolutions.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ConflictsTags.EMPTY)
                        .padding(vertical = Space.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(Space.sectionGap))
                    EmptyDrawing(section = null)
                    Spacer(Modifier.height(Space.l))
                    Text(
                        text = strings["conflicts.empty"],
                        style = HealthTrail.type.bodyL,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = strings["conflicts.empty.detail"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@SectionScaffold
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            GroupHeader(labelKey = "conflicts.group")
            Spacer(Modifier.height(Space.headerGap))
        }

        items(resolutions.size) { index ->
            val resolution = resolutions[index]
            GroupedSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ConflictsTags.row(resolution.seq))
                        .padding(Space.cardPadding),
                ) {
                    // **What was kept and why, before what it said.** The
                    // person's first question is whether the app did the right
                    // thing, not what the words were.
                    Text(
                        text = Bidi.join(
                            listOf(
                                strings[
                                    if (resolution.kept == "local") {
                                        "conflicts.kept.local"
                                    } else {
                                        "conflicts.kept.incoming"
                                    },
                                ],
                                strings[reasonKey(resolution.reason)],
                            ),
                            separator = ", ",
                        ),
                        style = HealthTrail.type.label,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        // The day it was decided, through the app's own date
                        // path so the precision is never invented and the
                        // format is the locale's.
                        text = EventDateText.render(
                            strings,
                            Instant.ofEpochMilli(resolution.resolvedAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                        ),
                        style = HealthTrail.type.mono,
                        color = colors.ink2,
                    )

                    for (difference in resolution.differences) {
                        Spacer(Modifier.height(Space.m))
                        Hairline()
                        Spacer(Modifier.height(Space.m))
                        val removal = difference.column == "deleted_at"
                        Text(
                            // **Every column here has a word**, because
                            // `Repository.nameable` only lets through the ones
                            // the field map renders, plus the deletion. A key
                            // built from a column name would otherwise throw
                            // the first time a conflict landed on a derived
                            // column. docs/TRAPS.md section 3.
                            text = if (removal) {
                                strings["conflicts.field.deleted_at"]
                            } else {
                                strings["archive.field.${difference.column}"]
                            },
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Value(
                            labelKey = "conflicts.kept.value",
                            value = shown(strings, difference.keptValue, removal),
                            emphasis = true,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Value(
                            labelKey = "conflicts.other.value",
                            value = shown(strings, difference.otherValue, removal),
                            emphasis = false,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.m))
        }

        item { Spacer(Modifier.height(Space.l)) }
    }
}

/**
 * A deletion reads as a word, never as the moment it happened.
 *
 * The column holds a timestamp, and "Removed: 1781701200000" is the stored
 * value reaching a person as itself, which is the same defect #328 closed in
 * the archive.
 */
private fun shown(
    strings: com.kamsiob.healthtrail.i18n.Strings,
    value: String?,
    removal: Boolean,
): String? = when {
    !removal -> value
    value.isNullOrBlank() -> strings["conflicts.not_removed"]
    else -> strings["conflicts.removed"]
}

/**
 * One side of a difference.
 *
 * **A value nobody wrote says so** rather than showing a blank, which is the
 * same rule the archive follows: a blank is ambiguous between "nothing was
 * written" and "the app lost it".
 */
@Composable
private fun Value(labelKey: String, value: String?, emphasis: Boolean) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    Text(
        text = Bidi.join(
            listOf(
                strings[labelKey],
                value?.takeIf { it.isNotBlank() } ?: strings["conflicts.nothing.written"],
            ),
            separator = ": ",
        ),
        style = HealthTrail.type.bodyL,
        color = if (emphasis) colors.ink else colors.ink2,
    )
}

/**
 * The stored reason turned into a sentence.
 *
 * The column holds a token because that row travels between phones that may be
 * set to different languages, which is the same argument #328 settled for the
 * archive: a stored value is not display text.
 */
private fun reasonKey(reason: String) = when (reason) {
    Merge.Reason.SAME_TIME -> "conflicts.reason.same_time"
    else -> "conflicts.reason.newer"
}
