// Configures plugin repositories and management policies across the build.
pluginManagement {
    repositories {
        // Google Maven repository filtered for official Google/Android packages
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

// Global dependency resolution configuration enforcing centralized repository management
dependencyResolutionManagement {
    // Rejects any repositories declared in individual subproject build.gradle files
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Project identification and subproject module inclusion
rootProject.name = "AppCustodian App"
include(":app")