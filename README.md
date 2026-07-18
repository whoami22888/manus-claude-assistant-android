# Personal Assistant Android Application

This Android application is a personal assistant with voice recognition, text-to-speech, local and cloud storage placeholders, JNI-backed native command processing, Python script loading, and optional Groq-powered responses.

## Features

- **Voice Recognition**: Uses Android's built-in speech recognition for voice commands
- **Text-to-Speech**: Provides spoken responses using Android's TTS engine
- **File Actions**: Supports upload, download, and listing files through Android storage APIs
- **Cloud Storage Placeholders**: Includes placeholder flows for cloud upload, download, and file listing
- **Native Integration**: Uses JNI to connect to a small C++ core library for command handling
- **Python Script Loading**: Loads bundled or user-selected `.py` files for future assistant extensions
- **Groq Integration**: Uses the Groq chat API when an API key is configured, with an offline fallback when it is not
- **Skills Dashboard & Updater**: Supports listing, adding, updating, and removing skills in-app
- **Sandbox Terminal Commands**: Supports safe terminal-like commands (`pwd`, `ls`, `cat`, `date`, `echo`, `build-tools`)
- **Turbo Mode**: Supports `turbo on/off/status` to force fast local-response mode

## Project Structure

- `app/src/main/java`: Kotlin source files
- `app/src/main/res`: Android resources (layouts, strings, etc.)
- `app/src/main/AndroidManifest.xml`: App manifest with permissions

## Building the Project

1. Create `local.properties` in the project root with `sdk.dir=/path/to/Android/Sdk`
2. Optionally add `groq.api.key=...` to `local.properties`, or export `GROQ_API_KEY`
3. If Google Maven is not reachable from your network, export `GOOGLE_MAVEN_REPOSITORY_URL` or pass `-PgoogleMavenRepositoryUrl=https://your-mirror.example.com/android/maven2`
4. Install Android NDK 26.1.10909125 and CMake 3.22.1 if they are not already present
5. Build using Gradle: `./gradlew assembleDebug --no-daemon`

## Requirements

- Android SDK 24+
- Android NDK 26.1.10909125 with CMake 3.22.1 for the JNI layer
- JDK 17+ (required by Android Gradle Plugin 8.2.0 / Gradle 8.7)
- Access to Google Maven (`https://dl.google.com/dl/android/maven2/`) or a compatible mirror/proxy hosting Android Gradle Plugin artifacts

## Dependencies

The app uses the following major dependencies:
- AndroidX libraries
- Android speech recognition and text-to-speech APIs
- OkHttp for Groq API requests
- Kotlin coroutines for background work
