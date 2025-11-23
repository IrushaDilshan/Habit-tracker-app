pluginManagement {
    repositories {
        // Use standard plugin repositories so Gradle can resolve the Android Gradle Plugin and Kotlin plugin
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.13.0"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "new mad"
include(":app")
