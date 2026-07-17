package com.manus.assistant

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP client for the Groq chat-completions API.
 *
 * Maintains a rolling conversation history of up to [MAX_HISTORY] messages so
 * that the model can answer follow-up questions coherently.  Call [clearHistory]
 * to start a fresh conversation.
 *
 * The API is OpenAI-compatible, so switching to a different provider
 * (OpenAI, Mistral, etc.) only requires changing [BASE_URL] and [MODEL].
 *
 * Usage:
 *   val reply = GroqApiClient().chat(userMessage, BuildConfig.GROQ_API_KEY)
 *
 * Throws an [Exception] on network or API errors — callers should catch and
 * fall back to the local rule-based engine.
 */
class GroqApiClient(
    /** Overridable for testing; defaults to the real Groq endpoint. */
    private val baseUrl: String = BASE_URL
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Rolling history: alternating user / assistant messages.
    private val history = mutableListOf<JSONObject>()

    companion object {
        private const val TAG = "GroqApiClient"
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Maximum number of user+assistant messages kept in history (10 turns = 20 messages,
         *  excluding the system prompt which is always prepended separately). */
        private const val MAX_HISTORY = 20

        private const val SYSTEM_PROMPT =
            "You are a helpful personal assistant running on an Android device. " +
            "Be concise — answer in 1-3 sentences. " +
            "The app natively handles: current time, current date, file upload/download/list, " +
            "and voice input/output. For those topics, you may mention the native feature."
    }

    /** Clears the conversation history. */
    fun clearHistory() {
        history.clear()
    }

    /**
     * Send [userMessage] to the Groq API and return the model's reply text.
     * The exchange is appended to the rolling [history].
     *
     * This is a **blocking** call and must be called from a background thread
     * or a coroutine with [kotlinx.coroutines.Dispatchers.IO].
     *
     * @throws IllegalStateException if [apiKey] is blank.
     * @throws Exception on network failure or non-2xx HTTP response.
     */
    fun chat(userMessage: String, apiKey: String): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("GROQ_API_KEY is not configured")
        }

        // Append user turn to history before the request.
        history.add(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })
        // Trim history so it never exceeds MAX_HISTORY entries.
        while (history.size > MAX_HISTORY) history.removeAt(0)

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            })
            history.forEach { put(it) }
        }

        val bodyJson = JSONObject().apply {
            put("model", MODEL)
            put("messages", messagesArray)
            put("max_tokens", 256)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", buildString {
                append("Bearer ")
                append(apiKey)
            })
            .post(bodyJson.toString().toRequestBody(JSON_TYPE))
            .build()

        Log.d(TAG, "Sending request to Groq API…")
        http.newCall(request).execute().use { response ->
            val bodyText = response.body?.string()
            if (!response.isSuccessful) {
                // Remove the user message we just added so a retry doesn't double-add it.
                if (history.isNotEmpty()) history.removeAt(history.size - 1)
                Log.e(TAG, "API error ${response.code}: $bodyText")
                throw Exception("Groq API error ${response.code}")
            }
            if (bodyText.isNullOrBlank()) {
                if (history.isNotEmpty()) history.removeAt(history.size - 1)
                throw Exception("Empty response from Groq API")
            }
            val reply = JSONObject(bodyText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            Log.d(TAG, "Groq reply: $reply")
            // Append assistant turn so future messages have full context.
            history.add(JSONObject().apply {
                put("role", "assistant")
                put("content", reply)
            })
            return reply
        }
    }
}
