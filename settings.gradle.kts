val googleMavenRepositoryUrl =
    providers.gradleProperty("googleMavenRepositoryUrl").orNull
        ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")

fun org.gradle.api.artifacts.dsl.RepositoryHandler.googleMavenRepository() {
    if (googleMavenRepositoryUrl.isNullOrBlank()) {
        google()
    } else {
        maven(url = googleMavenRepositoryUrl)
    }
}

pluginManagement {
    repositories {
        googleMavenRepository()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        googleMavenRepository()
        mavenCentral()
    }
}

rootProject.name = "PersonalAssistant"
include(":app")
