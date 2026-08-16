import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// **The release key is read from outside the repository and never in it.**
// `keystore.properties` is gitignored and holds a path to a keystore that
// lives under the home directory, so nothing about the app's identity is in
// git and a clone of this repository cannot sign anything.
//
// **Absent means unsigned, not broken.** CI and any other machine build the
// release variant without the key, and get an unsigned APK rather than a
// failure, which is what keeps a missing secret from looking like a bug.
// D160.
val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasSigningKey = signingProps.getProperty("storeFile")?.let { File(it).exists() } == true

// The contract and the template catalog live outside this module, at the root
// of the monorepo, because they are shared with the web platform and neither
// platform owns them.
val contractDir: File = rootProject.projectDir.parentFile.resolve("contract")
val templatesDataDir: File = rootProject.projectDir.parentFile.resolve("templates/data")

android {
    namespace = "com.kamsiob.healthtrail"

    // Compiled against 37, targeting 36. These are separate on purpose:
    // compileSdk decides which APIs the code may call, targetSdk opts the app
    // into new runtime behavior, and minSdk decides which devices can install
    // it. androidx 1.19 and lifecycle 2.11 both require compiling against 37.
    // See DECISIONS.md D15.
    compileSdk = 37

    defaultConfig {
        // One application id. Never a variant suffix, never a second package,
        // because exactly one copy of this app exists on the owner's phone at
        // all times and every install after the first is an in place upgrade.
        applicationId = "com.kamsiob.healthtrail"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Arabic ships in v1, so right to left is on from the first screen
        // rather than being a localization pass at the end.
        resourceConfigurations += setOf("en", "es", "zh", "ar")
    }

    if (hasSigningKey) {
        signingConfigs {
            create("release") {
                storeFile = File(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                // v1 is off because minSdk is well above the API 24 that
                // needed it, and leaving it on ships a weaker signature
                // alongside the strong one for nobody's benefit.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // No suffix. A suffix produces a second application id, a second
            // launcher icon, and an installation that cannot receive an in
            // place upgrade, which is exactly what the one copy rule forbids.
            isMinifyEnabled = false
        }
        release {
            if (hasSigningKey) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        named("main") {
            kotlin.srcDir("src/main/kotlin")
            // The copied contract and template files are added as an asset
            // directory rather than checked in, so there is exactly one copy of
            // each in the repository and it is the one in /contract.
            //
            // Declared as a plain path rather than a Provider: AGP rejects
            // Providers here because Android Studio cannot tell generated from
            // static content, and the task dependency it would have carried is
            // wired explicitly below instead.
            assets.srcDir("build/generated/contractAssets")
        }
        named("test") { kotlin.srcDir("src/test/kotlin") }
        named("androidTest") { kotlin.srcDir("src/androidTest/kotlin") }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // The baseline is deliberately absent. A baseline file is how a project
        // accumulates warnings nobody ever looks at again.
        checkDependencies = true

        // One check disabled, with its reason, rather than a baseline.
        //
        // OldTargetApi fires because targetSdk is 36 while compileSdk is 37. It
        // is arguing for something this project should not do automatically.
        // targetSdk opts the app into a platform version's runtime behavior
        // changes, so raising it is a decision that follows testing rather than
        // one that follows an SDK release. 36 is the level Google Play requires
        // from 31 August 2026, and it moves to 37 deliberately, after the
        // behavior changes have been walked through on a device.
        disable += "OldTargetApi"

        // ObsoleteSdkInt fires on res/mipmap-anydpi-v26, arguing the v26
        // qualifier is redundant because minSdk is already 26. It is right in
        // principle and wrong in practice: dropping the qualifier makes AAPT2
        // skip the folder entirely, so the launcher icon does not link and the
        // build fails with "resource mipmap/ic_launcher not found". Verified by
        // trying it. The qualifier stays.
        disable += "ObsoleteSdkInt"

        // **The three version currency checks, because they are not checks on
        // this repository.** `NewerVersionAvailable`,
        // `AndroidGradlePluginVersion` and `GradleDependency` fail the build the moment somebody else
        // publishes a release, so a tree nobody has touched goes from green to
        // red overnight and the failure names a file nobody edited. It happened
        // twice in one hour on 2026-08-08: Gradle 9.7.0 on the 6th, then Bouncy
        // Castle 1.85.2, each one red on an unchanged commit.
        //
        // **This is not the check being routed around.** Staying current is
        // still owned, and it is owned by the thing built for it: Dependabot
        // watches the Gradle ecosystem and opens a pull request naming the
        // version, which is reviewable, testable and does not break `main` at
        // three in the morning. A build whose greenness depends on the outside
        // world's release schedule is not telling anybody anything about this
        // code. D121.
        disable += "NewerVersionAvailable"
        disable += "AndroidGradlePluginVersion"
        // **`GradleDependency` was the one D121 missed**, and it proved the
        // entry right on 2026-08-12 by turning `main` red on a documentation
        // commit: Compose BOM 2026.08.00 published, and lint failed naming
        // `libs.versions.toml`, a file that had not been touched in weeks. Two
        // of the family were disabled and the third was left, which is the same
        // shape as a check that is only partly a check. Same reason, same owner.
        disable += "GradleDependency"
    }
}

/**
 * Copies the canonical schema and the template catalog into the app's assets.
 *
 * The data contract is explicit that neither platform may keep its own copy of
 * the schema, the export format, the templates, or the message catalog, and
 * that if the build cannot read them it fails loudly rather than falling back
 * to a stale internal copy. That failure is what this task exists to produce.
 *
 * The reason is worth restating where someone will see it: a schema that exists
 * only as platform code makes the second platform a reimplementation rather
 * than a second reader, and the two drift apart within weeks.
 */
// Validated at configuration time rather than inside the task, for two reasons.
// It fails before any work starts, which is the right moment to say the contract
// is missing. And a doFirst block in the Kotlin DSL captures the build script
// instance, which the configuration cache cannot serialize.
run {
    val missing = buildList {
        if (!contractDir.resolve("schema.sql").isFile) add(contractDir.resolve("schema.sql").path)
        if (!contractDir.resolve("EXPORT-FORMAT.md").isFile) add(contractDir.resolve("EXPORT-FORMAT.md").path)
        if (!contractDir.resolve("readable-fields.json").isFile) {
            add(contractDir.resolve("readable-fields.json").path)
        }
        if (!contractDir.resolve("readable-vocabularies.json").isFile) {
            add(contractDir.resolve("readable-vocabularies.json").path)
        }
        if (!contractDir.resolve("readable-money.json").isFile) {
            add(contractDir.resolve("readable-money.json").path)
        }
        if (!templatesDataDir.isDirectory) add(templatesDataDir.path)
    }
    if (missing.isNotEmpty()) {
        throw GradleException(
            buildString {
                appendLine("The build cannot read the shared contract, so it is stopping.")
                appendLine()
                appendLine("Missing:")
                missing.forEach { appendLine("  $it") }
                appendLine()
                appendLine("These files are the contract between the Android app and the web")
                appendLine("platform. The app keeps no copy of any of them, so there is nothing")
                appendLine("to fall back to, and falling back would be worse: a stale internal")
                appendLine("schema is how two platforms drift apart.")
                appendLine()
                appendLine("If you are running Gradle from somewhere other than the android")
                appendLine("directory of the health-trail repository, that is the cause.")
            }
        )
    }
}

/**
 * Copies the canonical schema and the template catalog into the app's assets.
 *
 * The data contract is explicit that neither platform may keep its own copy of
 * the schema, the export format, the templates, or the message catalog, and
 * that if the build cannot read them it fails loudly rather than falling back
 * to a stale internal copy. The check above is that failure.
 */
val copyContractAssets by tasks.registering(Sync::class) {
    group = "build"
    description = "Copies contract/schema.sql and templates/data into assets."

    from(contractDir.resolve("schema.sql")) { into("contract") }
    from(contractDir.resolve("EXPORT-FORMAT.md")) { into("contract") }
    // Which of the schema's columns the archive's readable copy renders, and
    // why each of the rest does not. In /contract because the web version
    // renders the same archive from the same decisions.
    from(contractDir.resolve("readable-fields.json")) { into("contract") }
    // The fixed vocabularies those decisions name, so a stored value reaches a
    // page as a word rather than as itself.
    from(contractDir.resolve("readable-vocabularies.json")) { into("contract") }
    from(templatesDataDir) {
        into("templates")
        include("*.json")
    }
    from(contractDir.resolve("i18n")) {
        into("contract/i18n")
        include("*.json")
    }
    // The golden vectors, so the test suite runs the same files the web app
    // will. A vector kept in the test source tree would be a second opinion
    // about correct, which is exactly what a shared vector exists to prevent.
    from(contractDir.resolve("test-vectors")) {
        into("contract/test-vectors")
        include("*.json")
    }

    into(layout.buildDirectory.dir("generated/contractAssets"))
}

/**
 * Turns `contract/readable-fields.json` into a Kotlin constant.
 *
 * **Generated rather than parsed at runtime, and the reason is determinism.**
 * The archive's readable copy has to be byte identical across runs, because
 * `contract/DATA-CONTRACT.md` 8.5's regeneration test asserts exactly that, and
 * the field order is part of what makes it so. Parsing at runtime would put that
 * order at the mercy of whichever JSON implementation is present, and JSON does
 * not guarantee object key order.
 *
 * Generating it also means the app carries no parser for this file, the order is
 * fixed at build time, and the whole thing is unit testable with no Android and
 * no Robolectric.
 *
 * **The contract stays the single source.** This reads `/contract` and writes a
 * build output. Nothing is hand maintained on this side, so the two cannot
 * drift, which is D16's rule.
 */
val generateReadableFields by tasks.registering {
    group = "build"
    description = "Generates ReadableFieldMap.kt from the contract's readable decisions."

    val source = contractDir.resolve("readable-fields.json")
    val vocabularySource = contractDir.resolve("readable-vocabularies.json")
    val moneySource = contractDir.resolve("readable-money.json")
    val outputDir = layout.buildDirectory.dir("generated/readableFields")
    inputs.file(source)
    inputs.file(vocabularySource)
    inputs.file(moneySource)
    outputs.dir(outputDir)

    doLast {
        val root = groovy.json.JsonSlurper().parse(source) as Map<*, *>
        val vocabularies = groovy.json.JsonSlurper().parse(vocabularySource) as Map<*, *>
        val money = groovy.json.JsonSlurper().parse(moneySource) as Map<*, *>
        val file = outputDir.get().asFile.resolve(
            "com/kamsiob/healthtrail/data/ReadableFieldMap.kt",
        )
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("package com.kamsiob.healthtrail.data")
                appendLine()
                appendLine("// Generated from contract/readable-fields.json by the build.")
                appendLine("// Do not edit. Change the contract file instead.")
                appendLine("//")
                appendLine("// Which of the schema's columns the archive's readable copy renders,")
                appendLine("// in the order it renders them. contract/DATA-CONTRACT.md 8.2 and 8.5.")
                appendLine("internal object ReadableFieldMap {")
                appendLine("    val tables: Map<String, ReadableArchive.TableFields> = mapOf(")
                root.keys.sortedBy { it.toString() }.forEach { table ->
                    val entry = root[table] as Map<*, *>
                    val order = (entry["order"] as? List<*>).orEmpty()
                    val columns = entry["columns"] as? Map<*, *> ?: emptyMap<Any, Any>()
                    appendLine("        \"$table\" to ReadableArchive.TableFields(")
                    appendLine("            listOf(")
                    order.forEach { column ->
                        val decision = columns[column] as? Map<*, *>
                        val renderer = decision?.get("render")?.toString()
                        if (!renderer.isNullOrEmpty()) {
                            // The optional half of a decision, written as named
                            // arguments so the generated file reads as prose
                            // rather than as a row of positional strings.
                            val extras = listOf("vocabulary", "currency", "currencyFrom", "catalog")
                                .mapNotNull { name ->
                                    decision[name]?.toString()?.takeIf { it.isNotEmpty() }
                                        ?.let { "$name = \"$it\"" }
                                }
                            val tail = if (extras.isEmpty()) "" else ", " + extras.joinToString(", ")
                            appendLine(
                                "                ReadableArchive.Field(\"$column\", " +
                                    "\"$renderer\"$tail),",
                            )
                        }
                    }
                    appendLine("            ),")
                    appendLine("        ),")
                }
                appendLine("    )")
                appendLine()
                appendLine("    // The fixed vocabularies those decisions name, in the order")
                appendLine("    // contract/readable-vocabularies.json declares them.")
                appendLine("    val vocabularies: Map<String, List<String>> = mapOf(")
                vocabularies.keys
                    .map { it.toString() }
                    .filterNot { it.startsWith("_") }
                    .sorted()
                    .forEach { name ->
                        val entry = vocabularies[name] as Map<*, *>
                        val values = (entry["values"] as? List<*>).orEmpty()
                        appendLine(
                            "        \"$name\" to listOf(" +
                                values.joinToString(", ") { "\"$it\"" } + "),",
                        )
                    }
                appendLine("    )")
                appendLine("}")
                appendLine()
                appendLine("// The ISO 4217 codes whose minor unit is not two digits.")
                appendLine("// Generated from contract/readable-money.json. Anything absent is 2,")
                appendLine("// which ReadableMoney states rather than leaves silent. #331.")
                appendLine("internal object ReadableCurrency {")
                appendLine("    val exponents: Map<String, Int> = mapOf(")
                (money["exponents"] as Map<*, *>).keys
                    .map { it.toString() }
                    .sorted()
                    .forEach { code ->
                        appendLine("        \"$code\" to ${(money["exponents"] as Map<*, *>)[code]},")
                    }
                appendLine("    )")
                appendLine("}")
            },
        )
    }
}

/**
 * Turns `contract/test-vectors/readable/vector.json` into a Kotlin constant for
 * the **unit** test source set.
 *
 * **This is what lets the archive's regeneration run without a device.** `B4`
 * dropped the emulator on the argument that data survival is proven by the
 * round trip against golden vectors in continuous integration rather than by a
 * long lived phone installation, and until this existed nothing in continuous
 * integration rendered a readable page at all: `RegenerationTest` is
 * instrumented and `DateVectorTest` reads assets, so both need the phone.
 *
 * **Generated rather than parsed, for the same reason the field map is.** The
 * order of a row's columns and of the tables themselves is part of what makes
 * the output byte identical, and JSON does not guarantee object key order. It
 * also means the unit test needs no JSON parser, which matters because
 * `org.json` is an Android stub on the unit test classpath and returns null for
 * everything.
 */
val generateReadableVector by tasks.registering {
    group = "build"
    description = "Generates ReadableVector.kt from contract/test-vectors/readable/vector.json."

    val source = contractDir.resolve("test-vectors/readable/vector.json")
    val outputDir = layout.buildDirectory.dir("generated/readableVector")
    inputs.file(source)
    outputs.dir(outputDir)

    doLast {
        val root = groovy.json.JsonSlurper().parse(source) as Map<*, *>
        val file = outputDir.get().asFile.resolve(
            "com/kamsiob/healthtrail/data/ReadableVector.kt",
        )
        file.parentFile.mkdirs()

        fun quote(value: Any?): String = "\"" + value.toString()
            .replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("$", "\${'$'}").replace("\n", "\\n") + "\""

        fun mapOfStrings(entries: Map<*, *>): String =
            entries.entries.joinToString(", ") { "${quote(it.key)} to ${quote(it.value)}" }

        file.writeText(
            buildString {
                appendLine("package com.kamsiob.healthtrail.data")
                appendLine()
                appendLine("// Generated from contract/test-vectors/readable/vector.json by the")
                appendLine("// build. Do not edit. Change the contract file instead.")
                appendLine("internal object ReadableVector {")
                appendLine("    const val SUBJECT_NAME = ${quote(root["subjectName"])}")
                appendLine()
                appendLine("    val rows: Map<String, List<Map<String, String?>>> = mapOf(")
                (root["tables"] as Map<*, *>).forEach { (table, rowList) ->
                    appendLine("        ${quote(table)} to listOf(")
                    (rowList as List<*>).forEach { row ->
                        val cells = (row as Map<*, *>).entries.joinToString(", ") { (column, value) ->
                            "${quote(column)} to " + if (value == null) "null" else quote(value)
                        }
                        appendLine("            mapOf($cells),")
                    }
                    appendLine("        ),")
                }
                appendLine("    )")
                appendLine()
                appendLine("    val words: Map<String, ReadableArchive.Words> = mapOf(")
                (root["locales"] as Map<*, *>).forEach { (locale, spec) ->
                    val w = spec as Map<*, *>
                    appendLine("        ${quote(locale)} to ReadableArchive.Words(")
                    appendLine("            lang = ${quote(w["lang"])},")
                    appendLine("            dir = ${quote(w["dir"])},")
                    appendLine("            tables = mapOf(${mapOfStrings(w["tables"] as Map<*, *>)}),")
                    appendLine("            columns = mapOf(${mapOfStrings(w["columns"] as Map<*, *>)}),")
                    appendLine("            vocabularies = mapOf(")
                    (w["vocabularies"] as Map<*, *>).forEach { (name, values) ->
                        appendLine(
                            "                ${quote(name)} to " +
                                "mapOf(${mapOfStrings(values as Map<*, *>)}),",
                        )
                    }
                    appendLine("            ),")
                    // **The same names in every locale, deliberately.** A
                    // template's name is content in `templates/data` rather than
                    // a string with four translations, so this is not a gap in
                    // the vector. #329, D130.
                    appendLine("            catalogNames = mapOf(")
                    (w["catalogNames"] as? Map<*, *>).orEmpty().forEach { (name, entries) ->
                        appendLine(
                            "                ${quote(name)} to " +
                                "mapOf(${mapOfStrings(entries as Map<*, *>)}),",
                        )
                    }
                    appendLine("            ),")
                    for (name in listOf("subjectFallback", "about", "datedHeading", "wholeHeading",
                                        "howToHeading", "howToBody", "back", "undated",
                                        "notRecorded", "yes", "no")) {
                        appendLine("            $name = ${quote(w[name])},")
                    }
                    // Plain substitution rather than ICU. These templates carry
                    // no plural and no number, so the two agree, and the unit
                    // test classpath has no ICU to agree with.
                    appendLine(
                        "            covers = { from, to -> if (from == to) " +
                            "${quote(w["coversOne"])}.replace(\"{year}\", from) else " +
                            "${quote(w["coversRange"])}.replace(\"{from}\", from)" +
                            ".replace(\"{to}\", to) },",
                    )
                    appendLine(
                        "            yearTitle = { section, year -> ${quote(w["yearTitle"])}" +
                            ".replace(\"{section}\", section).replace(\"{year}\", year) },",
                    )
                    // **A lookup rather than a format**, because Arabic needs
                    // six plural forms and only ICU can choose between them.
                    // Each of these was read off a real Arabic export.
                    appendLine(
                        "            records = { count -> mapOf(" +
                            mapOfStrings(w["records"] as Map<*, *>) +
                            ").getValue(count.toString()) },",
                    )
                    // **Computed now, where it used to be a lookup.** The
                    // entries were read off a real export because
                    // `java.text.NumberFormat` answered differently on Android
                    // and on the JDK, so computing them here would have locked
                    // the vector to whichever machine ran the build. #331 took
                    // money out of the platform's hands, so the vector calls the
                    // same function the export does and the amount is the same
                    // everywhere. D128 said to do this the day it became
                    // possible.
                    appendLine(
                        "            money = { minor, code -> " +
                            "ReadableMoney.format(minor, code) },",
                    )
                    appendLine("        ),")
                }
                appendLine("    )")
                appendLine("}")
            },
        )
    }
}

/**
 * Turns `contract/test-vectors/digest.json` into a Kotlin constant for the
 * **unit** test source set.
 *
 * **So the digest's cases belong to the contract rather than to one platform's
 * test file.** They lived inside `DigestTest` as Kotlin until 2026-08-10, which
 * made them tests rather than vectors: #15 asks for input fixtures paired with
 * the exact expected output, run by the Kotlin suite **and** by the web
 * scaffold, so that if the two engines disagree on one input continuous
 * integration says so. A vector only one platform can read cannot do that.
 *
 * **Generated rather than parsed at run time**, for the same reason the readable
 * vector is: a unit test JVM has no `org.json`, and the order of the cases and
 * of each expected section list is part of what is being asserted.
 */
val generateDigestVector by tasks.registering {
    group = "build"
    description = "Generates DigestVector.kt from contract/test-vectors/digest.json."

    val source = contractDir.resolve("test-vectors/digest.json")
    val outputDir = layout.buildDirectory.dir("generated/digestVector")
    inputs.file(source)
    outputs.dir(outputDir)

    doLast {
        val root = groovy.json.JsonSlurper().parse(source) as Map<*, *>
        val file = outputDir.get().asFile.resolve(
            "com/kamsiob/healthtrail/data/DigestVector.kt",
        )
        file.parentFile.mkdirs()

        fun quote(value: Any?): String = "\"" + value.toString()
            .replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("$", "\${'$'}").replace("\n", "\\n") + "\""

        file.writeText(
            buildString {
                appendLine("package com.kamsiob.healthtrail.data")
                appendLine()
                appendLine("// Generated from contract/test-vectors/digest.json by the build.")
                appendLine("// Do not edit. Change the contract file instead.")
                appendLine("internal object DigestVector {")
                appendLine()
                appendLine("    data class Case(")
                appendLine("        val name: String,")
                appendLine("        val why: String,")
                appendLine("        val since: Long,")
                appendLine("        val changes: List<Digest.Change>,")
                appendLine("        val added: List<Pair<String, Int>>,")
                appendLine("        val corrected: Int,")
                appendLine("        val removed: Int,")
                appendLine("    )")
                appendLine()
                appendLine("    /** Table name to the section it belongs to, from the contract. */")
                appendLine("    val sections: Map<String, String> = mapOf(")
                (root["sections"] as Map<*, *>).forEach { (table, section) ->
                    appendLine("        ${quote(table)} to ${quote(section)},")
                }
                appendLine("    )")
                appendLine()
                appendLine("    /** Tables the change log writes that no section claims, named rather than absent. */")
                appendLine("    val unmapped: List<String> = listOf(")
                (root["unmapped"] as List<*>).forEach { appendLine("        ${quote(it)},") }
                appendLine("    )")
                appendLine()
                appendLine("    val cases: List<Case> = listOf(")
                (root["cases"] as List<*>).forEach { raw ->
                    val case = raw as Map<*, *>
                    val expect = case["expect"] as Map<*, *>
                    appendLine("        Case(")
                    appendLine("            name = ${quote(case["name"])},")
                    appendLine("            why = ${quote(case["why"])},")
                    appendLine("            since = ${(case["since"] as Number).toLong()}L,")
                    appendLine("            changes = listOf(")
                    (case["changes"] as List<*>).forEach { rawChange ->
                        val change = rawChange as Map<*, *>
                        appendLine(
                            "                Digest.Change(" +
                                "table = ${quote(change["table"])}, " +
                                "rowId = ${quote(change["row"])}, " +
                                "op = ${quote(change["op"])}, " +
                                "changedAt = ${(change["at"] as Number).toLong()}L),",
                        )
                    }
                    appendLine("            ),")
                    appendLine("            added = listOf(")
                    (expect["added"] as List<*>).forEach { rawAdded ->
                        val added = rawAdded as Map<*, *>
                        appendLine(
                            "                ${quote(added["section"])} to " +
                                "${(added["count"] as Number).toInt()},",
                        )
                    }
                    appendLine("            ),")
                    appendLine("            corrected = ${(expect["corrected"] as Number).toInt()},")
                    appendLine("            removed = ${(expect["removed"] as Number).toInt()},")
                    appendLine("        ),")
                }
                appendLine("    )")
                appendLine("}")
            },
        )
    }
}

// A plain path rather than a Provider: the Android source set API rejects a
// Provider because it cannot tell generated files from editable ones.
android.sourceSets.getByName("main").kotlin.srcDir(
    layout.buildDirectory.get().asFile.resolve("generated/readableFields"),
)
android.sourceSets.getByName("test").kotlin.srcDir(
    layout.buildDirectory.get().asFile.resolve("generated/readableVector"),
)
android.sourceSets.getByName("test").kotlin.srcDir(
    layout.buildDirectory.get().asFile.resolve("generated/digestVector"),
)
// The expected pages are read as ordinary files by the unit test rather than
// copied anywhere, so the failure message can name the file somebody has to
// open. The path is handed in as a system property, because a unit test's
// working directory is the module and the contract is two levels above it.
tasks.withType<Test>().configureEach {
    val expected = contractDir.resolve("test-vectors/readable/expected")
    // **Declared as an input, or the test does not re-run when the vector
    // changes.** Reading a path out of a system property is invisible to
    // Gradle's up to date checking, so editing an expected page left
    // `testDebugUnitTest` UP-TO-DATE and the golden vector unchecked. It would
    // still have run in continuous integration, on a fresh checkout, which is
    // the worst version of this: green locally and red only on push.
    inputs.dir(expected)
        .withPropertyName("readableVectorExpected")
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .optional(true)
    systemProperty("healthtrail.vector.expected", expected.path)
    // Forwarded rather than inherited: a Gradle test task runs in its own JVM
    // and does not pass the invocation's system properties down. Without this
    // the regeneration switch silently does nothing and the test reports that
    // the expected pages are missing, which is a confusing way to say that a
    // flag was ignored.
    providers.systemProperty("healthtrail.vector.write").orNull?.let {
        systemProperty("healthtrail.vector.write", it)
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateReadableFields, generateReadableVector, generateDigestVector)
}

// Every variant's assets depend on the copy, including the test variants, so a
// test can never run against assets that were not refreshed.
tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
    dependsOn(copyContractAssets)
}
tasks.named("preBuild") { dependsOn(copyContractAssets, generateReadableFields, generateReadableVector) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.sqlcipher)

    // Argon2id for the export's key derivation, and nothing else. The
    // format names it and PBKDF2 is not an acceptable substitute, because the
    // export file is the only recovery path from key loss. D24 and D51.
    implementation(libs.bouncycastle)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
