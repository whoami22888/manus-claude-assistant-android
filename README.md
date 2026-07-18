# Manus Claude Assistant — Android

An Android personal-assistant app with voice input/output, Groq AI integration, a JNI-backed native command layer, skills management, a sandboxed terminal, and local/cloud storage helpers.

## Features

- **Voice Recognition** — uses Android's built-in speech recognizer for hands-free input
- **Text-to-Speech** — spoken responses via Android's TTS engine
- **Groq AI** — chat via the Groq API (`llama-3.3-70b-versatile`) with a rolling conversation history; falls back to offline rule-based responses when a key is not configured or the network is unavailable
- **Turbo Mode** — `turbo on/off/status` forces all responses through the local rule engine for zero-latency replies
- **Skills Dashboard** — list, add, update, and remove named skills; view turbo-mode status
- **Sandbox Terminal** — safe in-app terminal commands: `pwd`, `ls`, `cat`, `date`, `echo`, `build-tools`
- **Native JNI Layer** — a small C++ core (`assistantcore` shared library) handles `native status`, `native help`, and `native process` commands; a pure-Kotlin fallback is used automatically when the library is not available
- **Local File Actions** — upload and download files through Android's Storage Access Framework (no storage permissions required)
- **Cloud Storage Placeholders** — prototype upload/download/list flows ready to be wired to a real cloud back-end
- **Python Script Loading** — load bundled or user-selected `.py` files for future assistant extensions

## Project Structure

```
app/src/main/
├── java/com/manus/assistant/   # Kotlin source files
│   ├── MainActivity.kt          # UI and input routing
│   ├── AssistantEngine.kt       # Groq AI + local-rule orchestrator
│   ├── GroqApiClient.kt         # Groq chat-completions HTTP client
│   ├── SkillsManager.kt         # Persistent skills registry
│   ├── TerminalManager.kt       # Sandboxed terminal command runner
│   ├── NativeCommandProcessor.kt# JNI bridge (Kotlin side)
│   ├── PythonScriptManager.kt   # Python script loader
│   ├── CloudStorageManager.kt   # Cloud storage placeholder manager
│   ├── ChatAdapter.kt           # RecyclerView adapter for chat messages
│   └── ChatMessage.kt           # Chat message data model
├── cpp/
│   ├── CMakeLists.txt           # CMake build for the native library
│   └── native_command_processor.cpp
├── res/                         # Layouts, strings, drawables
└── AndroidManifest.xml          # Permissions: RECORD_AUDIO, INTERNET
```

## Building

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17+ |
| Android SDK | API 24 – 34 |
| Android NDK | 26.1.10909125 |
| CMake | 3.22.1 |

### Steps

1. Clone the repository.
2. Create `local.properties` in the project root and set `sdk.dir=/path/to/Android/Sdk`.
3. *(Optional)* Add `groq.api.key=<your-key>` to `local.properties`, or export `GROQ_API_KEY` in your shell. The app works without a key using its offline rule engine.
4. Install NDK and CMake if not already present:
   ```
   sdkmanager "ndk;26.1.10909125" "cmake;3.22.1"
   ```
5. Build:
   ```
   ./gradlew assembleDebug --no-daemon
   ```

### Google Maven mirror

`settings.gradle.kts` is configured to use Alibaba Cloud's public mirrors for Google and common artifacts:

- `https://maven.aliyun.com/repository/google`
- `https://maven.aliyun.com/repository/public`

### Release signing (optional)

Set `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables. When absent the build falls back to the debug key automatically.

## CI

GitHub Actions (`android.yml`) runs on every push/PR to `main`:

1. Sets up JDK 17 and Android SDK
2. Installs NDK 26.1.10909125 and CMake 3.22.1
3. Runs unit tests (`./gradlew test`)
4. Builds both debug and release APKs
5. Uploads APKs as workflow artifacts (retained 14 days)

## Dependencies

| Library | Purpose |
|---------|---------|
| AndroidX (core-ktx, appcompat, recyclerview) | UI and Jetpack utilities |
| Material Components | UI theming |
| OkHttp 4.12 | Groq API HTTP calls |
| Kotlin Coroutines 1.7 | Off-main-thread networking |
| Lifecycle Runtime KTX 2.7 | `lifecycleScope` coroutine support |
| JUnit 4 / OkHttp MockWebServer | Unit testing |
| Espresso | Instrumentation testing |
