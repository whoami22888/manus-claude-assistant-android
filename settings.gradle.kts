pluginManagement {
    repositories {
        googleMavenRepository(settings.googleMavenRepositoryUrl())
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        googleMavenRepository(settings.googleMavenRepositoryUrl())
        mavenCentral()
    }
}

rootProject.name = "PersonalAssistant"
include(":app")
