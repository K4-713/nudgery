// SPDX-License-Identifier: CC0-1.0

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nudgery"
include(":shared")
include(":androidApp")
