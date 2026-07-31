package com.kamsiob.healthtrail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import com.kamsiob.healthtrail.R
import com.kamsiob.healthtrail.contract.ContractAssets
import com.kamsiob.healthtrail.contract.SchemaFacts
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import androidx.compose.ui.graphics.ColorFilter

/**
 * Phase 0 only.
 *
 * This screen exists to prove two things on real hardware: that the app
 * launches, and that the shared contract genuinely reached the device and
 * executes there. It is replaced by the disclaimer gate and the Today screen in
 * Phase 1, and nothing here is a pattern to copy.
 */
object FoundationTags {
    const val ROOT = "foundation_root"
    const val SCHEMA_TABLES = "foundation_schema_tables"
    const val TEMPLATE_COUNT = "foundation_template_count"
}

@Composable
fun FoundationScreen() {
    val context = LocalContext.current
    val facts by produceState<SchemaFacts?>(initialValue = null) {
        value = ContractAssets.inspectSchema(context)
    }
    val templateCount by produceState(initialValue = -1) {
        value = runCatching { ContractAssets.countTemplates(context) }.getOrDefault(-1)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(FoundationTags.ROOT),
        color = HealthTrail.colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Mark()

            Spacer(Modifier.height(Space.l))

            Text(
                text = stringResource(R.string.foundation_title),
                style = HealthTrail.type.displayL,
                color = HealthTrail.colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = stringResource(R.string.foundation_subtitle),
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink2,
            )

            Spacer(Modifier.height(Space.sectionGap))

            Card {
                Text(
                    text = stringResource(R.string.foundation_contract_heading).uppercase(),
                    style = HealthTrail.type.mono,
                    color = HealthTrail.colors.ink3Text,
                )
                Spacer(Modifier.height(Space.sm))

                val current = facts
                if (current == null || !current.isUsable) {
                    Text(
                        text = stringResource(R.string.foundation_contract_missing),
                        style = HealthTrail.type.bodyM,
                        color = HealthTrail.colors.ink,
                    )
                    val detail = current?.error
                    if (detail != null) {
                        Spacer(Modifier.height(Space.s))
                        // A technical value rather than copy, which is why it is
                        // not translated. It is what the database said.
                        Text(
                            text = detail,
                            style = HealthTrail.type.mono,
                            color = HealthTrail.colors.ink2,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.foundation_schema_tables, current.tables),
                        style = HealthTrail.type.bodyL,
                        color = HealthTrail.colors.ink,
                        modifier = Modifier.testTag(FoundationTags.SCHEMA_TABLES),
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = stringResource(R.string.foundation_schema_views, current.views),
                        style = HealthTrail.type.bodyL,
                        color = HealthTrail.colors.ink,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = stringResource(R.string.foundation_templates, templateCount),
                        style = HealthTrail.type.bodyL,
                        color = HealthTrail.colors.ink,
                        modifier = Modifier.testTag(FoundationTags.TEMPLATE_COUNT),
                    )
                }
            }

            Spacer(Modifier.height(Space.cardGap))

            Text(
                text = stringResource(R.string.foundation_note),
                style = HealthTrail.type.bodyS,
                color = HealthTrail.colors.ink2,
            )
        }
    }
}

/**
 * The card, per DESIGN.md section 5.3. Card surface, 20dp radius, 16dp padding,
 * no border and no colored edge.
 */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(HealthTrail.colors.card)
            .padding(Space.cardPadding),
        verticalArrangement = Arrangement.Top,
    ) {
        content()
    }
}

/**
 * The mark at 44dp, which is its size on the launch and About screens.
 *
 * It carries no content description because it is decorative here: the screen
 * title directly beneath it already names where the person is, and a screen
 * reader announcing the logo before every title is noise.
 */
@Composable
private fun Mark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_mark),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp())
                .clearAndSetSemantics { },
            colorFilter = ColorFilter.tint(HealthTrail.colors.blaze),
        )
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())

@Preview(name = "Light", showBackground = true)
@Composable
private fun FoundationPreviewLight() {
    HealthTrailTheme(darkTheme = false) { FoundationScreen() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun FoundationPreviewDark() {
    HealthTrailTheme(darkTheme = true) { FoundationScreen() }
}
