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
        // Hardcoded working mirror proxies for project dependencies
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        // Standard fallbacks
        mavenCentral()
        google()
    }
}

// FIX: Aligned with your actual package structure and repository name
rootProject.name = "manus-claude-assistant-android"
include(":app")
