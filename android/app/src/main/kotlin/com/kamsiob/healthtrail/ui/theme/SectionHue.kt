package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Composable
import com.kamsiob.healthtrail.data.Repository

/**
 * Which tab hue each section wears. `DESIGN.md` section 4.3.
 *
 * **This mapping is an owner decision and is not to be re-derived.** Five hues
 * come from the grid file and `stone` was added in v4 because the grid draws no
 * standing instructions screen and the section needs an identity, `DECISIONS.md`
 * D79. A section added later inherits the hue of the section it most resembles
 * in kind, and that choice is recorded in `DESIGN.md` with its reasoning rather
 * than made here and left for somebody to find.
 *
 * **Tabs are identity, never state.** A hue says which section you are in. It
 * never reports that something is open, overdue, or unread.
 *
 * **Whole-app surfaces get no section hue.** Today, the trail, filing, projects,
 * search, and onboarding belong to no section and use gold with the base ladder.
 * The two of those that appear in [Repository.Section] are handled here so that
 * a caller cannot accidentally give one a section identity it does not have.
 */
@Composable
fun hueFor(section: Repository.Section): TabHue {
    val colors = HealthTrail.colors
    return when (section) {
        // People and chapters. A chapter is a place a person was, which is the
        // same kind of thing as the people who were there.
        Repository.Section.CARE_TEAM,
        Repository.Section.CHAPTERS,
        -> colors.rose

        // Medications, tests, and questions. All three are things asked of, or
        // received from, the clinical side.
        //
        // **Green, measured off `m3v4-1`**: the drawing's medication tile is
        // `#3B6C48` on `#E2EDE1`, which is `moss`. It was teal here, and teal
        // is what the same drawing gives Progress.
        Repository.Section.MEDICATIONS,
        Repository.Section.ASK_NEXT_TIME,
        -> colors.moss

        Repository.Section.APPOINTMENTS -> colors.slate

        // **Progress is teal, measured off `m3v4-0` and `m3v4-1`**: the
        // tracked measure's card sets its name and its line in `#2D7166`, and
        // the notebook's Progress tile is that ink on `#DEEBE6`.
        Repository.Section.PROGRESS -> colors.teal

        // **Care threads are green with the medications**, which is what
        // `m3v4-1` draws: both tiles are the same `#3B6C48`. A thread is a
        // clinical stream, and the drawing groups it with them rather than
        // with the measure.
        Repository.Section.THREADS -> colors.moss

        // Documents and money. Both are paper.
        Repository.Section.DOCUMENTS,
        Repository.Section.MONEY,
        -> colors.manila

        Repository.Section.STANDING_INSTRUCTIONS -> colors.stone

        // Whole-app surfaces. The trail and projects belong to no section, and
        // the emergency card is the one screen that is neither a section nor
        // gold: it is alert, because that is what it is for, and alert is the
        // only semantic color allowed to act as an identity anywhere in the app.
        Repository.Section.TRAIL,
        Repository.Section.PROJECTS,
        -> TabHue(base = colors.gold, ink = colors.goldInk, wash = colors.goldWash)

        Repository.Section.EMERGENCY_CARD ->
            TabHue(base = colors.alert, ink = colors.alertInk, wash = colors.alertWash)
    }
}

/**
 * The three identities that belong to no section, as packs. D198.
 *
 * **Because a mark takes a pack and not two loose colors.** Eighteen call sites
 * passed an ink and a wash by hand, which is how the same mark came to be drawn
 * three different ways, and none of them could be made saturated in one place.
 */
@Composable
fun goldHue(): TabHue = HealthTrail.colors.let {
    TabHue(base = it.gold, ink = it.goldInk, wash = it.goldWash)
}

/** The emergency card, an open incident. Never a measurement, rule 2. */
@Composable
fun alertHue(): TabHue = HealthTrail.colors.let {
    TabHue(base = it.alert, ink = it.alertInk, wash = it.alertWash)
}

/** The single accent. Every action, and only actions. */
@Composable
fun accentHue(): TabHue = HealthTrail.colors.let {
    TabHue(base = it.blue, ink = it.blueDeep, wash = it.blueWash)
}
