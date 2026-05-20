plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
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

    buildTypes {
        release {
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

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.workmanager.testing)
    androidTestImplementation(libs.kotlinx.datetime)
    androidTestImplementation(libs.sqldelight.android.driver)
}
