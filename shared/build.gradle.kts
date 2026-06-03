import com.nudgery.buildtools.emoji.EmojiCatalogGenerator
import com.nudgery.buildtools.emoji.EmojiTestParser
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

// Generates the emoji catalog (ED-5): parses the vendored Unicode emoji-test.txt and emits the
// catalog as Kotlin source into a generated commonMain source set, compiled into the app. The
// parser/generator live in buildSrc (build-time only, never shipped). Generated-on-build (not
// committed) — the vendored emoji-test.txt is the source of truth. Annual refresh = swap that file.
val emojiTestFile = layout.projectDirectory.file("emoji-data/emoji-test.txt").asFile
val emojiCatalogOutputDir = layout.buildDirectory.dir("generated/emojiCatalog/kotlin")

val generateEmojiCatalog = tasks.register("generateEmojiCatalog") {
    group = "build"
    description = "Generates the emoji catalog Kotlin source from emoji-data/emoji-test.txt (ED-5)."
    val input = emojiTestFile
    val outputDir = emojiCatalogOutputDir
    inputs.file(input)
    outputs.dir(outputDir)
    doLast {
        val baseConcepts = EmojiCatalogGenerator.baseConcepts(EmojiTestParser.parse(input.readText()))
        require(baseConcepts.size > 1000) {
            "Emoji catalog looks malformed: only ${baseConcepts.size} base concepts parsed from $input"
        }
        val packageDir = outputDir.get().asFile.resolve("com/nudgery/shared/emoji")
        packageDir.mkdirs()
        packageDir.resolve("GeneratedEmojiCatalog.kt")
            .writeText(EmojiCatalogGenerator.generateSource(baseConcepts))
        logger.lifecycle(
            "generateEmojiCatalog: ${baseConcepts.size} base concepts " +
                "(${baseConcepts.count { it.acceptsSkinTone }} accept skin tone, " +
                "${baseConcepts.count { it.hairCapable }} hair-capable)"
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    // iOS targets — uncomment when iOS support is added
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        commonMain {
            // Generated emoji catalog (ED-5); the task wiring makes compilation depend on it.
            kotlin.srcDir(generateEmojiCatalog)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.sqldelight.sqlite.driver)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.workmanager)
        }

        // iosMain.dependencies {
        //     implementation(libs.sqldelight.native.driver)
        // }
    }
}

android {
    namespace = "com.nudgery.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("NudgeryDatabase") {
            packageName.set("com.nudgery.shared.db")
        }
    }
}
