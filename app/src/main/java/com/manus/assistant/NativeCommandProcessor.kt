package com.manus.assistant

object NativeCommandProcessor {

    private val nativeLibraryLoaded = runCatching {
        System.loadLibrary("assistantcore")
        true
    }.getOrDefault(false)

    fun processOrNull(input: String): String? {
        if (!shouldUseNative(input)) return null
        return if (nativeLibraryLoaded) {
            processCommandNative(input).takeIf { it.isNotBlank() }
        } else {
            fallbackProcess(input)
        }
    }

    internal fun fallbackProcess(input: String): String? {
        val lower = input.lowercase()
        return when {
            lower.contains("native status") ->
                "Native core placeholder is ready. JNI routing is enabled when the shared library is available."
            lower.contains("native help") ->
                "Native commands:\n• native status\n• native help\n• native process <command>"
            lower.contains("native process") ->
                "Native core placeholder processed: ${input.substringAfter("native process").trim().ifBlank { "no command provided" }}"
            else -> null
        }
    }

    private fun shouldUseNative(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("native status") ||
            lower.contains("native help") ||
            lower.contains("native process")
    }

    private external fun processCommandNative(input: String): String
}
