package com.manus.assistant

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Assistant engine that tries the Groq AI API first and falls back to an
 * offline rule-based implementation when the API is unavailable or the key
 * is not configured.
 *
 * The API key is resolved at call time: SharedPreferences ("groq_api_key") takes
 * precedence, then [BuildConfig.GROQ_API_KEY] (baked in at build time).
 *
 * [processInput] is a suspend function and must be called from a coroutine
 * (e.g. inside [androidx.lifecycle.lifecycleScope]).
 */
class AssistantEngine(private val context: Context) {

    private val groqClient = GroqApiClient()

    companion object {
        private const val TAG = "AssistantEngine"
        private const val PREFS_NAME = "app_prefs"
        private const val PREF_API_KEY = "groq_api_key"

        /**
         * Offline rule-based responder — always available, no network required.
         * Kept in the companion object so it can be tested without a [Context].
         */
        fun processInputLocal(input: String): String {
            val lower = input.lowercase(Locale.getDefault()).trim()
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    "Hello! How can I assist you today?"

                lower.contains("what time") || lower == "time" ->
                    "The current time is ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}."

                lower.contains("what date") || lower.contains("today") || lower == "date" ->
                    "Today's date is ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}."

                lower.contains("upload") ->
                    "Tap the upload button or say 'upload' to pick a file from your device."

                lower.contains("download") ->
                    "Say 'download' to save a note to your device."

                lower.contains("list files") || lower.contains("show files") ->
                    "Say 'list files' to view files in the app's local storage."

                lower.contains("help") ->
                    "I can help with:\n• Time and date queries\n• File operations (upload / download / list)\n• General questions — powered by Groq AI when a key is configured.\nJust type or speak your request!"

                lower.contains("bye") || lower.contains("goodbye") || lower.contains("exit") ->
                    "Goodbye! Have a great day!"

                lower.contains("thank") ->
                    "You're welcome! Let me know if there's anything else I can do."

                else ->
                    "I received: \"$input\".\nI'm still learning. Type \"help\" to see what I can do."
            }
        }
    }

    /** Returns the runtime API key from SharedPreferences, falling back to the build-time value. */
    private fun getApiKey(): String {
        val saved = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_API_KEY, null)
        return if (!saved.isNullOrBlank()) saved else BuildConfig.GROQ_API_KEY
    }

    /**
     * Clears the Groq conversation history (useful when starting a fresh session).
     */
    fun clearHistory() {
        groqClient.clearHistory()
    }

    /**
     * Process [input] via the Groq AI API with an offline rule-based fallback.
     *
     * When the resolved API key is blank the local rules are used immediately
     * without making a network call.
     */
    suspend fun processInput(input: String): String {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            return try {
                withContext(Dispatchers.IO) {
                    groqClient.chat(input, apiKey)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Groq API call failed, using local rules: ${e.message}")
                processInputLocal(input)
            }
        }
        return processInputLocal(input)
    }
}
