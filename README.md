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

1. Open the project in Android Studio
2. Ensure Android SDK and NDK are installed
3. Build using Gradle: `./gradlew assembleRelease`

## Requirements

- Android SDK 24+
- Android NDK
- CMake 3.18.1+
- JDK 8+

## Dependencies

The app uses the following major dependencies:
- AndroidX libraries
- Android Speech Recognition and TTS
- Native C++ integration via JNI
