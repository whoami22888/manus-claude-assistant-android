# Add project specific ProGuard rules here.

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Keep OkHttp and Okio (used by GroqApiClient) from being stripped.
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Kotlin coroutine internals needed at runtime.
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Preserve the app's own classes (package was renamed from com.example.personalassistant).
-keep class com.manus.assistant.** { *; }

# Keep JSON parsing classes.
-keep class org.json.** { *; }
