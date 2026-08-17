package com.kamsiob.healthtrail.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.kamsiob.healthtrail.R
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.screens.CaptureKind

/**
 * The icon vocabulary. Material Symbols Rounded, filled, at 24dp. D182.
 *
 * **The app drew its own icons and the approved mockups do not.** The owner,
 * on the first captures off the rebuilt theme: "the style and the font and the
 * icons are different". Every mark in `docs/screenshots/m3v4-*.png` is a
 * Material Symbol: the filled calendar, the open book, the flag, the three
 * dots. `SectionIcon.kt` drew hand-authored strokes on a 24 unit grid, which
 * were good drawings of the wrong alphabet.
 *
 * **These are vector drawables rather than an icon font**, which keeps the
 * reason [SectionIcon] gave for not using one: a font can fall back to a box
 * glyph on a device that does not have it, and a path draws the same
 * everywhere. Fetched from `google/material-design-icons`, Apache License 2.0,
 * on 2026-08-16, and the license line travels in every file.
 *
 * **Named by role rather than by drawing.** A screen asks for
 * [Symbols.medications], not for a pill, so the day a section changes its mark
 * there is one line to change rather than eleven call sites to find.
 */
object Symbols {
    // The four destinations, in nav bar order.
    @DrawableRes val today = R.drawable.ic_calendar_month
    @DrawableRes val notebook = R.drawable.ic_menu_book
    @DrawableRes val projects = R.drawable.ic_flag
    @DrawableRes val more = R.drawable.ic_more_horiz

    // The notebook's sections.
    @DrawableRes val careTeam = R.drawable.ic_groups
    @DrawableRes val medications = R.drawable.ic_medication
    @DrawableRes val appointments = R.drawable.ic_event
    // A place, which is what a chapter is: `m3v4-1` draws the pin.
    @DrawableRes val chapters = R.drawable.ic_location_on
    @DrawableRes val careThreads = R.drawable.ic_route
    @DrawableRes val trail = R.drawable.ic_show_chart
    @DrawableRes val progress = R.drawable.ic_monitoring
    @DrawableRes val documents = R.drawable.ic_description
    @DrawableRes val money = R.drawable.ic_payments
    @DrawableRes val standingInstructions = R.drawable.ic_rule
    @DrawableRes val askNextTime = R.drawable.ic_quiz
    @DrawableRes val emergencyCard = R.drawable.ic_emergency
    @DrawableRes val incidents = R.drawable.ic_medical_services

    // Actions and marks.
    @DrawableRes val search = R.drawable.ic_search
    @DrawableRes val add = R.drawable.ic_add
    @DrawableRes val edit = R.drawable.ic_edit
    @DrawableRes val close = R.drawable.ic_close
    @DrawableRes val back = R.drawable.ic_arrow_back
    @DrawableRes val forward = R.drawable.ic_chevron_right
    @DrawableRes val expand = R.drawable.ic_expand_more
    @DrawableRes val check = R.drawable.ic_check
    @DrawableRes val call = R.drawable.ic_call
    @DrawableRes val mail = R.drawable.ic_mail
    @DrawableRes val addPerson = R.drawable.ic_person_add
    @DrawableRes val download = R.drawable.ic_download
    @DrawableRes val share = R.drawable.ic_share
    @DrawableRes val overflow = R.drawable.ic_more_vert
    @DrawableRes val tips = R.drawable.ic_lightbulb
    @DrawableRes val whereThePaperIs = R.drawable.ic_inventory_2
    @DrawableRes val fullSize = R.drawable.ic_zoom_in

    /**
     * Put something on the calendar, and save a question to ask.
     *
     * **Google's own symbols rather than a plus beside an existing mark.** The
     * owner asked for the official Material 3 Expressive assets where they
     * exist, and both of these do: `calendar_add_on` and `add_comment` are
     * drawn on the same grid as every other mark here, so the pair reads as
     * part of the alphabet instead of as two marks stuck together. #386.
     */
    @DrawableRes val addToCalendar = R.drawable.ic_calendar_add_on
    @DrawableRes val addQuestion = R.drawable.ic_add_comment

    /**
     * The mark for one notebook section, so a screen never maps this itself.
     *
     * **Exhaustive with no `else`**, so a section added to the repository stops
     * the compiler here rather than shipping with whatever the fallback drew.
     */
    @DrawableRes
    fun of(section: Repository.Section): Int = when (section) {
        Repository.Section.CARE_TEAM -> careTeam
        Repository.Section.MEDICATIONS -> medications
        Repository.Section.APPOINTMENTS -> appointments
        Repository.Section.CHAPTERS -> chapters
        Repository.Section.THREADS -> careThreads
        Repository.Section.TRAIL -> trail
        Repository.Section.PROGRESS -> progress
        Repository.Section.DOCUMENTS -> documents
        Repository.Section.MONEY -> money
        Repository.Section.STANDING_INSTRUCTIONS -> standingInstructions
        Repository.Section.ASK_NEXT_TIME -> askNextTime
        Repository.Section.EMERGENCY_CARD -> emergencyCard
        Repository.Section.PROJECTS -> projects
    }

    /**
     * The mark for one thing a person can capture.
     *
     * **The same alphabet as everything else**, which is the point of doing the
     * capture sheet in the same pass as the notebook: `docs/V4.md` 3 says the
     * build never carries two design languages at once, and hand-stroked marks
     * in one sheet beside Material Symbols everywhere else is exactly that.
     */
    @DrawableRes
    fun of(kind: CaptureKind): Int = when (kind) {
        CaptureKind.CALL -> call
        CaptureKind.VISIT -> stethoscope
        CaptureKind.INCIDENT -> incidents
        CaptureKind.MEASUREMENT -> monitorWeight
        CaptureKind.QUESTION -> askNextTime
        CaptureKind.DOCUMENT -> documents
    }

    @DrawableRes val stethoscope = R.drawable.ic_stethoscope
    @DrawableRes val monitorWeight = R.drawable.ic_monitor_weight
    @DrawableRes val noteStack = R.drawable.ic_note_stack

    /** The mark for one destination. */
    @DrawableRes
    fun of(destination: Destination): Int = when (destination) {
        Destination.TODAY -> today
        Destination.NOTEBOOK -> notebook
        Destination.PROJECTS -> projects
        Destination.MORE -> more
    }
}

/**
 * One symbol, tinted.
 *
 * **The content description is the caller's and it is not optional to think
 * about.** A mark beside a word that already says the same thing takes null,
 * because a reader that says "Medications, Medications" is worse than one that
 * says it once. A mark that is the only thing naming its control takes a word.
 */
@Composable
fun Symbol(
    @DrawableRes symbol: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    // **`LocalContentColor`, not `Color.Unspecified`, and the difference is a
    // white icon on a white surface.** `Icon` treats an unspecified tint as
    // "draw the painter as authored", and every symbol here is authored in
    // white so it can be tinted. The first build of the navigation bar drew
    // four invisible glyphs for exactly this reason. Seen on the phone.
    //
    // Taking the content color also means a Material component that sets it,
    // which is every one of them, colors the mark inside it without the call
    // site repeating the role.
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(symbol),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
