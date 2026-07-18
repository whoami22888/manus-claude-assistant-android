buildscript {
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
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}
