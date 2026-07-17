val googleMavenRepositoryUrl =
    providers.gradleProperty("googleMavenRepositoryUrl").orNull
        ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")

pluginManagement {
    repositories {
        if (googleMavenRepositoryUrl.isNullOrBlank()) {
            google()
        } else {
            maven(url = googleMavenRepositoryUrl)
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (googleMavenRepositoryUrl.isNullOrBlank()) {
            google()
        } else {
            maven(url = googleMavenRepositoryUrl)
        }
        mavenCentral()
    }
}

rootProject.name = "PersonalAssistant"
include(":app")
