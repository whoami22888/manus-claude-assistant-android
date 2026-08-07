pluginManagement {
    repositories {
        // Keep this lookup inline: pluginManagement is compiled in an earlier
        // settings phase and cannot see helper declarations reliably.
        val googleMavenRepositoryUrl = settings.providers.gradleProperty("googleMavenRepositoryUrl")
            .orElse(settings.providers.environmentVariable("GOOGLE_MAVEN_REPOSITORY_URL"))
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        // Allow CI/developers to override the Google Maven endpoint when
        // official Google infrastructure is unreachable from their network.
        if (googleMavenRepositoryUrl != null) {
            maven(url = uri(googleMavenRepositoryUrl))
        } else {
            google()
        }

        // Use only authoritative repositories. Third-party mirrors can return
        // transient gateway failures during CI dependency resolution.
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val googleMavenRepositoryUrl = settings.providers.gradleProperty("googleMavenRepositoryUrl")
            .orElse(settings.providers.environmentVariable("GOOGLE_MAVEN_REPOSITORY_URL"))
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (googleMavenRepositoryUrl != null) {
            maven(url = uri(googleMavenRepositoryUrl))
        } else {
            google()
        }
        mavenCentral()

    }
}

// FIX: Aligned with your actual package structure and repository name
rootProject.name = "manus-claude-assistant-android"
include(":app")
