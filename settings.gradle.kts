pluginManagement {
    repositories {
        val googleMavenRepositoryUrl =
            providers.gradleProperty("googleMavenRepositoryUrl").orNull
                ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")
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
        val googleMavenRepositoryUrl =
            providers.gradleProperty("googleMavenRepositoryUrl").orNull
                ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")
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
