pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sudoku-trainer"

// The :app module is the Android app and needs the Google Maven repo to
// resolve the Android Gradle Plugin. If you want to build only the pure-
// JVM modules (for example in a sandbox without Google Maven access),
// disable inclusion with: ./gradlew -PincludeApp=false <task>.
val includeApp = providers.gradleProperty("includeApp").getOrElse("true").toBoolean()

include(":domain")
include(":tools:puzzle-gen")
if (includeApp) include(":app")
