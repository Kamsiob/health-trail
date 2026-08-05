package com.kamsiob.healthtrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.kamsiob.healthtrail.ui.screens.DisclaimerTags
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.contract.ContractAssets
import com.kamsiob.healthtrail.ui.AppRootTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Phase 0 smoke test.
 *
 * It proves three things on real hardware, none of which the unit suite can
 * prove:
 *
 * The app launches and renders its first screen.
 *
 * The shared contract reached the device. The build copies `contract/schema.sql`
 * and the template catalog into assets, and if that wiring breaks, the app is
 * running against nothing.
 *
 * SQLite on this device accepts the schema. A schema that parses in the Python
 * checks and a schema this device will execute are not the same claim, and the
 * difference has already bitten once: Android's execSQL refuses any statement
 * that returns rows, which `PRAGMA journal_mode` does.
 */
@RunWith(AndroidJUnit4::class)
class FoundationSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theAppLaunchesAndReachesAFirstScreen() {
        // Either the gate or the notebook, depending on whether the disclaimer
        // has been accepted on this install. What matters here is that it gets
        // past opening rather than sitting on the loading state.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(DisclaimerTags.ROOT).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag(AppRootTags.LOADING).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun theSharedContractReachedTheDevice() {
        assertTrue(
            "the schema, the export format, or the template catalog is missing " +
                "from assets, which means the build wiring in app/build.gradle.kts " +
                "is not copying them in",
            ContractAssets.isComplete(context),
        )
    }

    @Test
    fun theSchemaInAssetsIsTheContractFileUnchanged() {
        val schema = ContractAssets.readSchema(context)

        // Spot checks on the parts that carry the guarantees, rather than a
        // hash, which would fail on every legitimate schema edit.
        assertTrue("change_log is missing", schema.contains("CREATE TABLE IF NOT EXISTS change_log"))
        assertTrue("conflict_log is missing", schema.contains("CREATE TABLE IF NOT EXISTS conflict_log"))
        assertTrue(
            "the tombstone retention window is not stated in the schema comments",
            schema.contains("TOMBSTONE RETENTION WINDOW"),
        )
    }

    @Test
    fun sqliteOnThisDeviceExecutesTheSchema() {
        val facts = ContractAssets.inspectSchema(context)

        assertNull("the schema failed to execute: ${facts.error}", facts.error)

        // 40 user data tables, plus app_meta, device, change_log, conflict_log,
        // and schema_migration, plus android_metadata which the platform adds to
        // every database it creates.
        //
        // **Counted from contract/schema.sql rather than written down here.**
        // These were 40, 34 and 68 and are now 46, 40 and 80, and a number in
        // this file means every table added to the contract fails a smoke test
        // for a reason that has nothing to do with whether SQLite on this phone
        // can execute the schema, which is what this test is about. That every
        // table has its view and both triggers is check_schema.py's job, and it
        // holds the whole contract to it rather than counting.
        val schema = ContractAssets.readSchema(context)
        fun declared(what: String) =
            Regex(what, RegexOption.IGNORE_CASE).findAll(schema).count()

        val localTables = 5
        val platformTables = 1
        assertEquals(
            "unexpected table count",
            declared("CREATE TABLE") + platformTables,
            facts.tables,
        )
        assertEquals("unexpected view count", declared("CREATE VIEW"), facts.views)
        assertEquals("unexpected trigger count", declared("CREATE TRIGGER"), facts.triggers)

        // And the shape those numbers are meant to express, which a count alone
        // does not: one live view per user data table, and two triggers each.
        val userTables = facts.tables - localTables - platformTables
        assertEquals("a table is missing its live view", userTables, facts.views)
        assertEquals("a table is missing a change log trigger", userTables * 2, facts.triggers)
    }

    @Test
    fun theTemplateCatalogReachedTheDevice() {
        assertEquals(
            "expected 57 templates: 14 situations, 16 projects, 16 progress presets, " +
                "and 11 standing instructions",
            57,
            ContractAssets.countTemplates(context),
        )
    }


}
