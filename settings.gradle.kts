val googleMavenRepositoryUrl = providers.gradleProperty("googleMavenRepositoryUrl")
    .orElse(providers.environmentVariable("GOOGLE_MAVEN_REPOSITORY_URL"))
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

pluginManagement {
    repositories {
        // Allow CI/developers to override the Google Maven endpoint when
        // official Google infrastructure is unreachable from their network.
        googleMavenRepositoryUrl?.let { maven(url = uri(it)) } ?: google()

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
        googleMavenRepositoryUrl?.let { maven(url = uri(it)) } ?: google()
        mavenCentral()

        // Mirror proxies as fallback for constrained environments
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// FIX: Aligned with your actual package structure and repository name
rootProject.name = "manus-claude-assistant-android"
include(":app")
