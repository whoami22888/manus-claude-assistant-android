# Personal Assistant Android Application

This Android application is a personal assistant with voice recognition, cloud storage integration, and a Python scripting system for updates.

## Features

- **Voice Recognition**: Uses Android's built-in speech recognition for voice commands
- **Text-to-Speech**: Provides spoken responses using Android's TTS engine
- **Cloud Storage**: Includes placeholders for file upload, download, and listing operations
- **Native Integration**: Uses JNI to connect with a C++ core library for processing commands
- **Python Scripting**: Supports loading Python scripts to extend functionality

## Project Structure

- `app/src/main/java`: Kotlin source files
- `app/src/main/cpp`: Native C++ code (JNI bridge)
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
- Android Speech Recognition and TTS
- Native C++ integration via JNI
