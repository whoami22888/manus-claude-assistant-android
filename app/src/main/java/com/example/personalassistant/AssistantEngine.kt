package com.example.personalassistant

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
 * [processInput] is a suspend function and must be called from a coroutine
 * (e.g. inside [androidx.lifecycle.lifecycleScope]).
 */
class AssistantEngine {

    private val groqClient = GroqApiClient()

    companion object {
        private const val TAG = "AssistantEngine"
    }

    /**
     * Process [input] via the Groq AI API with an offline rule-based fallback.
     *
     * When [BuildConfig.GROQ_API_KEY] is blank the local rules are used
     * immediately without making a network call.
     */
    suspend fun processInput(input: String): String {
        val apiKey = BuildConfig.GROQ_API_KEY
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

    /** Offline rule-based fallback — always available, no network required. */
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
                "File upload is ready. Tap the menu to select a file from your device."

            lower.contains("download") ->
                "File download is ready. Please specify a path or URL."

            lower.contains("list files") || lower.contains("show files") ->
                "Local files available: notes.txt, photo.jpg, document.pdf."

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
