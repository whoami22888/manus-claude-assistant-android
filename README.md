# Personal Assistant Android Application

This Android application is a personal assistant with voice recognition, text-to-speech, local file actions, and optional Groq-powered responses.

## Features

- **Voice Recognition**: Uses Android's built-in speech recognition for voice commands
- **Text-to-Speech**: Provides spoken responses using Android's TTS engine
- **File Actions**: Supports upload, download, and listing files through Android storage APIs
- **Groq Integration**: Uses the Groq chat API when an API key is configured, with an offline fallback when it is not

## Project Structure

- `app/src/main/java`: Kotlin source files
- `app/src/main/res`: Android resources (layouts, strings, etc.)
- `app/src/main/AndroidManifest.xml`: App manifest with permissions

## Building the Project

1. Create `local.properties` in the project root with `sdk.dir=/path/to/Android/Sdk`
2. Optionally add `groq.api.key=...` to `local.properties`, or export `GROQ_API_KEY`
3. If Google Maven is not reachable from your network, export `GOOGLE_MAVEN_REPOSITORY_URL` or pass `-PgoogleMavenRepositoryUrl=https://your-mirror.example.com/android/maven2`
4. Build using Gradle: `./gradlew assembleDebug --no-daemon`

## Requirements

- Android SDK 24+
- JDK 17+
- Access to Google Maven (`https://dl.google.com/dl/android/maven2/`) or a compatible mirror/proxy hosting Android Gradle Plugin artifacts

## Dependencies

The app uses the following major dependencies:
- AndroidX libraries
- Android speech recognition and text-to-speech APIs
- OkHttp for Groq API requests
- Kotlin coroutines for background work
