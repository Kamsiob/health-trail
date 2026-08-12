package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.ApptTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Appointments has two views, which is what grid screen 22 draws. #357.
 *
 * **`MonthGrid` was finished and nothing composed it.** A built component with
 * no caller is the state nobody reviews, and the deferral that left it there
 * was waiting on the `app_meta` work, which is blocked on the owner. The
 * documents screen has carried a view toggle without any of that for weeks.
 *
 * **No calendar is read, ever.** The month is drawn from appointments recorded
 * in this app, the component has no way to reach a provider, and the manifest
 * asks for no calendar permission. D90.
 */
@RunWith(AndroidJUnit4::class)
class AppointmentsMonthTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val zone: ZoneId = ZoneId.systemDefault()

    private val today = LocalDate.of(2026, 8, 12)

    private val todayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()

    private val soon = Repository.Appointment(
        id = "a1",
        title = "Doctor, follow up",
        scheduledEdtf = "2026-08-15",
        scheduledStart = today.plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli(),
        locationNote = "Suite 210",
        notes = null,
    )

    private fun show(appointments: List<Repository.Appointment>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    AppointmentsScreen(
                        appointments = appointments,
                        todayMillis = todayMillis,
                        onOpen = {},
                        onAdd = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun theMonthIsOneTapFromTheList() {
        show(listOf(soon))

        compose.onNodeWithTag(ApptTags.TOGGLE).assertIsDisplayed()
        compose.onNodeWithText("Month", substring = true).performClick()

        compose.onNodeWithTag(ApptTags.MONTH).assertIsDisplayed()
        compose.onNodeWithText("August 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun theMonthCanBeSteppedThrough() {
        show(listOf(soon))
        compose.onNodeWithText("Month", substring = true).performClick()

        compose.onNodeWithTag(ApptTags.MONTH_NEXT).performClick()
        compose.onNodeWithText("September 2026", substring = true).assertIsDisplayed()

        compose.onNodeWithTag(ApptTags.MONTH_BACK).performClick()
        compose.onNodeWithTag(ApptTags.MONTH_BACK).performClick()
        compose.onNodeWithText("July 2026", substring = true).assertIsDisplayed()
    }

    /**
     * **A choice of two views over an empty notebook is furniture**, which is
     * the same call the documents screen makes.
     */
    @Test
    fun anEmptyNotebookIsOfferedNoChoiceOfView() {
        show(emptyList())

        compose.onNodeWithTag(ApptTags.TOGGLE).assertDoesNotExist()
    }
}
