package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object SectionTags {
    const val BACK = "section_back"
    fun root(name: String) = "section_root_$name"
    fun empty(name: String) = "section_empty_$name"
}

/**
 * The shape every section screen shares: a title, a line saying what the section
 * is for, the section's own contents, and one way back.
 *
 * **It exists so that twelve sections cannot become twelve slightly different
 * screens.** They open from one table of contents whose whole promise is that
 * the places never move, and that promise is broken as easily by twelve
 * inconsistent screens as by a shifting order.
 *
 * **The way back is a real control rather than the system gesture alone.** Rule
 * 18 counts taps and forbids dead ends. A screen reachable in one tap that can
 * only be left by a gesture some people do not know is a dead end for exactly
 * the people this app is for.
 *
 * The header renders through [LazyColumn] rather than above it, so a long title
 * at the largest font scale scrolls with the content instead of eating the
 * screen. That was found on the device at font scale 2.0, not in the code.
 */
@Composable
fun SectionScaffold(
    name: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What the way back says.
     *
     * **It names where the person is actually going**, which is the notebook
     * for the twelve sections and somewhere else for anything reached from
     * another screen. A back action that names the wrong destination is a small
     * lie the person only notices by being surprised.
     */
    backLabelKey: String = "section.back",
    content: LazyListScope.() -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        // Its own system bar padding, because a section screen renders over the
        // shell rather than inside it and does not inherit what the four
        // destinations get. The Unfiled tray learned this the same way.
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(SectionTags.root(name))
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                item {
                    Spacer(Modifier.height(Space.l))
                    Text(text = title, style = HealthTrail.type.displayL, color = colors.ink)
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = subtitle,
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.l))
                }

                content()

                item { Spacer(Modifier.height(Space.l)) }
            }

            // The pinned action footer, per DESIGN.md 5.15, with its required gap.
            Spacer(Modifier.height(Space.m))

            TextAction(
                label = strings[backLabelKey],
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(SectionTags.BACK),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * What a section says when it holds nothing.
 *
 * **It reads as "not yet", never as an error and never as a deficiency**, per
 * rule 13. It says what will turn up here and how, so an empty section teaches
 * the person what the section is for at the moment they are most likely to be
 * wondering.
 */
@Composable
fun SectionEmpty(name: String, text: String) {
    Text(
        text = text,
        style = HealthTrail.type.bodyL,
        color = HealthTrail.colors.ink2,
        modifier = Modifier.testTag(SectionTags.empty(name)),
    )
}
