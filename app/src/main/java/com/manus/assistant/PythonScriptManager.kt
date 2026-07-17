package com.manus.assistant

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

class PythonScriptManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadBundledExample(): String {
        val assetPath = "scripts/example_script.py"
        val preview = appContext.assets.open(assetPath).bufferedReader().use { reader ->
            reader.lineSequence().take(3).joinToString("\n").trim()
        }
        rememberScript("example_script.py")
        return buildString {
            append("Loaded bundled Python script example_script.py for extension use.")
            if (preview.isNotBlank()) {
                append("\nPreview:\n")
                append(preview)
            }
        }
    }

    fun loadScript(uri: Uri, contentResolver: ContentResolver): String {
        val scriptName = resolveFileName(uri, contentResolver)
        contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: return "Unable to read the selected Python script."
        rememberScript(scriptName)
        return "Loaded Python script \"$scriptName\" for future assistant extensions."
    }

    fun listScriptsMessage(): String {
        val scripts = prefs.getStringSet(KEY_SCRIPTS, emptySet()).orEmpty().sorted()
        return if (scripts.isEmpty()) {
            "No Python scripts are loaded. Say \"load python script\" or \"load example script\"."
        } else {
            "Loaded Python scripts:\n" + scripts.joinToString("\n") { "• $it" }
        }
    }

    private fun rememberScript(name: String) {
        val updated = prefs.getStringSet(KEY_SCRIPTS, emptySet()).orEmpty().toMutableSet()
        updated.add(name)
        prefs.edit().putStringSet(KEY_SCRIPTS, updated).apply()
    }

    private fun resolveFileName(uri: Uri, contentResolver: ContentResolver): String {
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        return cursor.getString(idx)
                    }
                }
            }
        }
        return uri.lastPathSegment ?: "script.py"
    }

    companion object {
        private const val PREFS_NAME = "python_script_prefs"
        private const val KEY_SCRIPTS = "loaded_scripts"
    }
}
