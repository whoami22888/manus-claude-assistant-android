pluginManagement {
    repositories {
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

        // Mirror fallbacks for constrained environments.
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
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

        // Mirror proxies as fallback for constrained environments
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// FIX: Aligned with your actual package structure and repository name
rootProject.name = "manus-claude-assistant-android"
include(":app")
