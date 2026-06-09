// SPDX-License-Identifier: CC0-1.0

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
}

// Release signing is driven by an untracked keystore.properties at the repo root (see .gitignore),
// so the upload keystore and its passwords never enter committed code. When the file is absent — a
// fresh clone, or CI without the secrets — the release build is left unsigned rather than failing,
// so the project still builds and tests run.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val gitVersionCode: Int = try {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        workingDir(rootDir)
    }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

val gitVersionName: String = try {
    providers.exec {
        commandLine("git", "describe", "--tags", "--always", "--dirty")
        workingDir(rootDir)
    }.standardOutput.asText.get().trim().removePrefix("v")
} catch (e: Exception) {
    "dev"
}

android {
    namespace = "com.nudgery.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nudgery.android"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only define the release signing config when the keystore.properties file is present;
        // otherwise the release build stays unsigned (see note above) instead of erroring.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Null when keystore.properties is absent → an unsigned release build (still builds).
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "Nudgery-${variant.versionName}-${variant.buildType.name}.apk"
        }
    }
}

aboutLibraries {
    collect {
        // Manual entries for things that aren't Maven dependencies (e.g. the bundled font) live here.
        configPath = file("config")
    }
}

dependencies {
    implementation(project(":shared"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.compose.lifecycle.viewmodel)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.workmanager)
    implementation(libs.kotlinx.datetime)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.material.icons.extended)

    // Open-source license data. We use only the core (no Compose UI module) and render the licenses
    // with our own AndroidX Compose screen — the plugin generates R.raw.aboutlibraries at build time.
    implementation(libs.aboutlibraries.core)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.workmanager.testing)
    androidTestImplementation(libs.kotlinx.datetime)
    androidTestImplementation(libs.sqldelight.android.driver)
}

// Regenerates the human-readable CREDITS.md at the repo root from the license data AboutLibraries
// harvests for the *release* build (the shipped classpath). The in-app licenses screen is
// regenerated automatically on every build and is the always-current source; CREDITS.md is a
// committed snapshot, so re-run this task when dependencies are added, updated, or removed:
//     ./gradlew :androidApp:generateCredits
// License texts are taken straight from the harvested data, so no network fetch or hardcoded text.
tasks.register("generateCredits") {
    group = "documentation"
    description = "Regenerates CREDITS.md from the harvested open-source license data (release)."
    dependsOn("prepareLibraryDefinitionsRelease")

    val licenseJson = layout.buildDirectory
        .file("generated/aboutLibraries/release/res/raw/aboutlibraries.json")
    val creditsFile = rootProject.layout.projectDirectory.file("CREDITS.md")
    // Manual license definitions (e.g. Unicode-3.0) for licenses AboutLibraries doesn't bundle text for.
    val licensesConfigDir = layout.projectDirectory.dir("config/licenses").asFile
    inputs.file(licenseJson)
    inputs.dir(licensesConfigDir)
    outputs.file(creditsFile)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val data = groovy.json.JsonSlurper()
            .parseText(licenseJson.get().asFile.readText()) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val libraries = data["libraries"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val licenses = data["licenses"] as Map<String, Map<String, Any?>>

        // Maps a Maven group to a human author/vendor for grouping (most of the list is one vendor).
        fun vendorFor(uniqueId: String): Pair<String, String?> {
            val group = uniqueId.substringBefore(':').lowercase()
            val table = listOf(
                Triple(listOf("org.jetbrains.kotlinx"), "JetBrains — Kotlin libraries (kotlinx)", "https://github.com/Kotlin"),
                Triple(listOf("org.jetbrains.kotlin"), "JetBrains — Kotlin", "https://kotlinlang.org"),
                Triple(listOf("org.jetbrains"), "JetBrains", "https://www.jetbrains.com"),
                Triple(listOf("androidx.", "com.google.android", "com.google.guava", "com.google.code"), "Google / Android Open Source Project", "https://developer.android.com/jetpack"),
                Triple(listOf("io.insert-koin"), "Koin — Arnaud Giuliani (Kotzilla)", "https://insert-koin.io"),
                Triple(listOf("com.patrykandpatrick.vico"), "Vico — Patryk Goworowski & Patrick Michalik", "https://github.com/patrykandpatrick/vico"),
                Triple(listOf("app.cash", "com.squareup"), "Square / Cash App", "https://github.com/cashapp"),
                Triple(listOf("com.mikepenz"), "Mike Penz", "https://mikepenz.dev"),
                Triple(listOf("co.touchlab"), "Touchlab", "https://touchlab.co"),
                Triple(listOf("org.brailleinstitute"), "Braille Institute of America", "https://www.brailleinstitute.org"),
            )
            for ((prefixes, name, url) in table) {
                if (prefixes.any { group.startsWith(it) }) return name to url
            }
            return group to null
        }

        fun licensesOf(lib: Map<String, Any?>): List<String> =
            @Suppress("UNCHECKED_CAST") (lib["licenses"] as? List<String> ?: emptyList())

        // Many projects ship several artifacts that read identically (e.g. Koin's 8 modules, or
        // AndroidX KMP -jvm/-android variants), so collapse entries with the same name, version,
        // and licenses to a single credit — matching the in-app licenses screen.
        val dedupedLibraries = libraries.distinctBy { lib ->
            "${lib["name"]} ${lib["artifactVersion"]} " + licensesOf(lib).sorted().joinToString(",")
        }

        val grouped = dedupedLibraries.groupBy { vendorFor(it["uniqueId"] as String) }
        val licUsage = mutableMapOf<String, Int>()
        dedupedLibraries.forEach { lib -> licensesOf(lib).forEach { licUsage.merge(it, 1, Int::plus) } }

        val sb = StringBuilder()
        sb.appendLine("# Credits").appendLine()
        sb.appendLine("Nudgery is built on the work of many open-source projects, and ships a typeface designed for accessibility. We're grateful to everyone who made and maintains them.").appendLine()
        sb.appendLine("This file is generated by `./gradlew :androidApp:generateCredits` from the release build's actual dependency graph (harvested by [AboutLibraries](https://github.com/mikepenz/AboutLibraries)). The in-app *Settings → About → Open-source licenses* screen is regenerated on every build and is the always-current source; this file is a snapshot of **${dedupedLibraries.size} libraries**. Re-run the task when dependencies change.").appendLine()

        sb.appendLine("## License summary").appendLine()
        licUsage.entries.sortedByDescending { it.value }.forEach { (k, c) ->
            sb.appendLine("- **$k** — $c librar${if (c == 1) "y" else "ies"}")
        }
        sb.appendLine()

        sb.appendLine("## Libraries by author").appendLine()
        grouped.entries
            .sortedWith(compareByDescending<Map.Entry<Pair<String, String?>, List<Map<String, Any?>>>> { it.value.size }
                .thenBy { it.key.first.lowercase() })
            .forEach { (vendor, items) ->
                val (name, url) = vendor
                sb.appendLine("### $name")
                if (url != null) sb.appendLine("<$url>").appendLine()
                items.sortedBy { (it["name"] as String).lowercase() }.forEach { lib ->
                    val ver = (lib["artifactVersion"] as? String)?.takeIf { it.isNotBlank() }
                    val verSuffix = if (ver != null) " `$ver`" else ""
                    val lics = licensesOf(lib).joinToString(", ").ifBlank { "—" }
                    sb.appendLine("- ${lib["name"]}$verSuffix — $lics")
                }
                sb.appendLine()
            }

        sb.appendLine("---").appendLine()
        // AboutLibraries may key a manually-defined license by a content hash rather than its SPDX id,
        // so also index by spdxId to resolve a library's license reference.
        val licensesBySpdx = licenses.values.mapNotNull { lic ->
            (lic["spdxId"] as? String)?.let { it to lic }
        }.toMap()

        // AboutLibraries only bundles text for licenses it recognizes; for the rest (e.g. Unicode-3.0)
        // fall back to the licenseContent in our manual config/licenses/<spdxId>.json.
        fun configLicenseContent(spdxId: String?): String? {
            if (spdxId.isNullOrBlank()) return null
            val file = licensesConfigDir.resolve("${spdxId.lowercase()}.json")
            if (!file.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val data = groovy.json.JsonSlurper().parseText(file.readText()) as Map<String, Any?>
            return (data["licenseContent"] as? String)?.takeIf { it.isNotBlank() }
        }

        sb.appendLine("## License texts").appendLine()
        licUsage.keys.sorted().forEach { key ->
            val lic = licenses[key] ?: licensesBySpdx[key]
            val spdxId = (lic?.get("spdxId") as? String) ?: key
            sb.appendLine("### ${(lic?.get("name") as? String) ?: key}").appendLine()
            val content = (lic?.get("content") as? String)?.takeIf { it.isNotBlank() }
                ?: configLicenseContent(spdxId)
            if (content != null) {
                sb.appendLine("```").appendLine(content.trimEnd()).appendLine("```").appendLine()
            } else {
                sb.appendLine("See <${(lic?.get("url") as? String) ?: key}>.").appendLine()
            }
        }

        creditsFile.asFile.writeText(sb.toString())
        logger.lifecycle("Wrote CREDITS.md (${libraries.size} libraries).")
    }
}
