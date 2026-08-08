plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

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

    buildTypes {
        debug {
            // No suffix. A suffix produces a second application id, a second
            // launcher icon, and an installation that cannot receive an in
            // place upgrade, which is exactly what the one copy rule forbids.
            isMinifyEnabled = false
        }
        release {
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

        // **The two version currency checks, because they are not checks on
        // this repository.** `NewerVersionAvailable` and
        // `AndroidGradlePluginVersion` fail the build the moment somebody else
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
    description = "Generates ReadableFieldMap.kt from contract/readable-fields.json."

    val source = contractDir.resolve("readable-fields.json")
    val outputDir = layout.buildDirectory.dir("generated/readableFields")
    inputs.file(source)
    outputs.dir(outputDir)

    doLast {
        val root = groovy.json.JsonSlurper().parse(source) as Map<*, *>
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
                            appendLine("                \"$column\" to \"$renderer\",")
                        }
                    }
                    appendLine("            ),")
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
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateReadableFields)
}

// Every variant's assets depend on the copy, including the test variants, so a
// test can never run against assets that were not refreshed.
tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
    dependsOn(copyContractAssets)
}
tasks.named("preBuild") { dependsOn(copyContractAssets, generateReadableFields) }

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
