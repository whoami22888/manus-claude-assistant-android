package com.example.personalassistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure-Kotlin assistant engine.
 *
 * Replaces the missing native shared_core library with a rule-based
 * implementation that handles the same set of commands the JNI bridge
 * was wired to support: greetings, time/date queries, file operations,
 * and general-purpose fall-through.
 */
class AssistantEngine {

    fun processInput(input: String): String {
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
                "I can help with:\n• Time and date queries\n• File operations (upload / download / list)\n• General questions\nJust type or speak your request!"

            lower.contains("bye") || lower.contains("goodbye") || lower.contains("exit") ->
                "Goodbye! Have a great day!"

            lower.contains("thank") ->
                "You're welcome! Let me know if there's anything else I can do."

            else ->
                "I received: \"$input\".\nI'm still learning. Type \"help\" to see what I can do."
        }
    }
}
