# Add project specific ProGuard rules here.

# 1. Debugging - Keep line numbers for crash logs
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,EnclosingMethod
-keep public class com.manus.assistant.BuildConfig { *; }

# 2. OkHttp + Okio (for GroqApiClient) - more targeted than keeping everything
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
# Keep OkHttp's helper methods that R8 thinks are unused
-keepclassmembers class okhttp3.** {
    @okhttp3.internal.SuppressSignatureCheck <methods>;
}

# 3. Okio
-keep class okio.** { *; }

# 4. Kotlin Coroutines - official recommended rules
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# 5. App code - DON'T keep everything, only keep what R8 can't see via reflection
# Keep models / DTOs used for JSON parsing (Groq responses)
-keep class com.manus.assistant.data.** { *; }
-keep class com.manus.assistant.model.** { *; }
-keep class com.manus.assistant.api.** { <fields>; }

# Keep services / workers that are started by the system
-keep class com.manus.assistant.service.** { *; }
-keep class com.manus.assistant.worker.** { *; }

# If you use Room, WebView, TTS/STT later, uncomment these:
# -keep class androidx.room.** { *; }
# -keep class android.speech.tts.** { *; }
# -keep class android.speech.** { *; }

# 6. JSON - org.json + generic JSON model keeping
-keep class org.json.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.squareup.moshi.Json <fields>;
    @kotlinx.serialization.Serializable <fields>;
}

# 7. Groq / Generic - Keep generic signatures for parsers
-keepattributes Signature
-dontnote com.manus.assistant.**