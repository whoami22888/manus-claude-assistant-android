package com.manus.assistant

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalManager(private val rootDir: File) {

    fun run(rawCommand: String): String {
        val parts = rawCommand.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return help()
        return when (parts.first().lowercase(Locale.getDefault())) {
            "help" -> help()
            "pwd" -> "PWD: ${rootDir.absolutePath}"
            "ls" -> listDirectory(parts.drop(1).firstOrNull())
            "date" -> "DATE: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
            "echo" -> parts.drop(1).joinToString(" ")
            "cat" -> readFile(parts.drop(1).firstOrNull())
            "build-tools" -> "Build tools: AGP 8.2.0, Kotlin 1.9.0, Gradle 8.7, NDK 27.3.13750724, CMake 3.31.5, minSdk 24, targetSdk 34"
            else -> "Unsupported command \"${parts.first()}\". Type \"terminal help\"."
        }
    }

    private fun listDirectory(pathArg: String?): String {
        val dir = resolvePath(pathArg ?: ".") ?: return "Path is outside the app sandbox."
        if (!dir.exists() || !dir.isDirectory) return "Directory not found: ${pathArg ?: "."}"
        val entries = dir.listFiles().orEmpty().sortedBy { it.name.lowercase(Locale.getDefault()) }
        if (entries.isEmpty()) return "(empty)"
        return entries.take(MAX_LIST_ENTRIES).joinToString("\n") { file ->
            if (file.isDirectory) "[D] ${file.name}" else "[F] ${file.name}"
        }
    }

    private fun readFile(pathArg: String?): String {
        if (pathArg.isNullOrBlank()) return "Usage: terminal run cat <relative-path>"
        val file = resolvePath(pathArg) ?: return "Path is outside the app sandbox."
        if (!file.exists() || !file.isFile) return "File not found: $pathArg"
        val content = runCatching { file.readText() }.getOrElse { return "Unable to read file: ${it.message}" }
        return if (content.length > MAX_FILE_CHARS) {
            content.take(MAX_FILE_CHARS) + "\n...(truncated)"
        } else {
            content
        }
    }

    private fun resolvePath(pathArg: String): File? {
        val candidate = File(rootDir, pathArg).canonicalFile
        val root = rootDir.canonicalFile
        return if (candidate.path.startsWith(root.path)) candidate else null
    }

    private fun help(): String =
        "Terminal commands:\n" +
            "• terminal run help\n" +
            "• terminal run pwd\n" +
            "• terminal run ls [relative-path]\n" +
            "• terminal run cat <relative-path>\n" +
            "• terminal run date\n" +
            "• terminal run echo <text>\n" +
            "• terminal run build-tools"

    companion object {
        private const val MAX_LIST_ENTRIES = 100
        private const val MAX_FILE_CHARS = 4000
    }
}
