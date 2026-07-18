pluginManagement {
    repositories {
        val googleMavenRepositoryUrl =
            providers.gradleProperty("googleMavenRepositoryUrl").orNull
                ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")
        if (googleMavenRepositoryUrl.isNullOrBlank()) {
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
            require(googleMavenRepositoryUrl.startsWith("https://")) {
                "googleMavenRepositoryUrl must use HTTPS, got: $googleMavenRepositoryUrl"
            }
            maven {
                url = uri(googleMavenRepositoryUrl)
                name = "google"
            }
        }
        mavenCentral()
    }
}

rootProject.name = "PersonalAssistant"
include(":app")
