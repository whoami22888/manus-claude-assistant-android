import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.google
import org.gradle.kotlin.dsl.maven

fun Settings.googleMavenRepositoryUrl(): String? =
    providers.gradleProperty("googleMavenRepositoryUrl").orNull
        ?: System.getenv("GOOGLE_MAVEN_REPOSITORY_URL")

fun RepositoryHandler.googleMavenRepository(url: String?) {
    if (url.isNullOrBlank()) {
        google()
    } else {
        maven(url = url)
    }
}
