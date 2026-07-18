buildscript {
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
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}
