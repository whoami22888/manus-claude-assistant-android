val googleMavenRepositoryUrl = providers.gradleProperty("googleMavenRepositoryUrl").orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

fun org.gradle.api.artifacts.dsl.RepositoryHandler.addGoogleRepository() {
    if (googleMavenRepositoryUrl == null) {
        google()
    } else {
        require(googleMavenRepositoryUrl.startsWith("https://")) {
            "googleMavenRepositoryUrl must use HTTPS, got: $googleMavenRepositoryUrl"
        }
        maven {
            url = uri(googleMavenRepositoryUrl)
            name = "google"
        }
    }
}

pluginManagement {
    repositories {
        addGoogleRepository()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        addGoogleRepository()
        mavenCentral()
    }
}

rootProject.name = "PersonalAssistant"
include(":app")
