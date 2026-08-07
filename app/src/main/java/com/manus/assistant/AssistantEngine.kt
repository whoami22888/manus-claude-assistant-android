package com.manus.assistant

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Prefers the app-owned HTTPS backend and remains useful offline when it is unset or unavailable. */
class AssistantEngine(
    private val context: Context,
    private val backendClient: AgentBackendClient = AgentBackendClient(),
    private val googleIdTokenProvider: () -> String? = { null }
) {
    private var turboModeEnabled = false

    companion object {
        private const val TAG = "AssistantEngine"
        const val PREFS_NAME = "app_prefs"
        const val PREF_BACKEND_URL = "agent_backend_url"

        fun processInputLocal(input: String): String {
            val lower = input.lowercase(Locale.getDefault()).trim()
            NativeCommandProcessor.processOrNull(input)?.let { return it }
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> "Hello! How can I assist you today?"
                lower.contains("what time") || lower == "time" -> "The current time is ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}."
                lower.contains("what date") || lower.contains("today") || lower == "date" -> "Today's date is ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}."
                lower.contains("upload to cloud") || lower.contains("cloud upload") -> "Say 'upload to cloud' to select a file for the cloud storage placeholder."
                lower.contains("download from cloud") || lower.contains("cloud download") -> "Say 'download from cloud' to save placeholder content from cloud storage."
                lower.contains("list cloud") || lower.contains("cloud files") -> "Say 'list cloud files' to inspect the placeholder cloud storage inventory."
                lower.contains("upload") -> "Tap the upload button or say 'upload' to pick a file from your device."
                lower.contains("download") -> "Say 'download' to save a note to your device."
                lower.contains("list files") || lower.contains("show files") -> "Say 'list files' to view files in the app's local storage."
                lower.contains("load example script") -> "Say 'load example script' to load the bundled Python extension example."
                lower.contains("load python script") || lower.contains("python script") -> "Say 'load python script' to import a .py file for future assistant extensions."
                lower.contains("list scripts") || lower.contains("show scripts") -> "Say 'list scripts' to review the Python scripts currently loaded."
                lower.startsWith("terminal run") || lower.contains("terminal help") -> "Terminal commands are unavailable in this app. I can help draft a reviewable plan; any external action requires explicit approval."
                lower.contains("help") -> "I can help with time and date, local files, cloud placeholders, native checks, Python scripts, and configured agent-backend chat."
                lower.contains("skills dashboard") -> "Say 'skills dashboard' to view your configured skills and turbo mode status."
                lower == "turbo on" -> "Turbo mode is now ON. Responses will use local processing for lower latency."
                lower == "turbo off" -> "Turbo mode is now OFF. Network agent responses are enabled when a backend URL is set."
                lower == "turbo status" -> "Turbo mode status can be viewed with 'skills dashboard'."
                lower.contains("bye") || lower.contains("goodbye") || lower.contains("exit") -> "Goodbye! Have a great day!"
                lower.contains("thank") -> "You're welcome! Let me know if there's anything else I can do."
                else -> "I received: \"$input\".\nI'm still learning. Type \"help\" to see what I can do."
            }
        }
    }

    private fun backendConfig(): BackendUrlConfig {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_BACKEND_URL, null)
        return BackendUrlConfig(saved?.takeIf { it.isNotBlank() } ?: BuildConfig.AGENT_BACKEND_URL)
    }

    fun clearHistory() = Unit
    fun setTurboMode(enabled: Boolean) { turboModeEnabled = enabled }
    fun isTurboModeEnabled(): Boolean = turboModeEnabled

    suspend fun processInput(input: String): String {
        if (turboModeEnabled) return processInputLocal(input)
        if (ApprovalPolicy.requiresApproval(input)) {
            return "This request may affect an account, access, security, or an external action. Review it and obtain explicit approval before continuing."
        }
        val config = backendConfig()
        if (!config.isValid) return processInputLocal(input)
        return try {
            withContext(Dispatchers.IO) { backendClient.chat(config, input, googleIdTokenProvider()) }
        } catch (e: Exception) {
            Log.w(TAG, "Agent backend failed; using local rules: ${e.message}")
            processInputLocal(input)
        }
    }
}
