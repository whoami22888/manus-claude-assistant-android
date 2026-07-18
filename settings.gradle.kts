pluginManagement {
    repositories {
        // Hardcoded working mirror proxies to bypass the container DNS block
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        // Standard fallbacks if the mirrors miss a plugin marker
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Official repositories as primary sources
        mavenCentral()
        google()

        // Mirror proxies as fallback for constrained environments
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// FIX: Aligned with your actual package structure and repository name
rootProject.name = "manus-claude-assistant-android"
include(":app")
