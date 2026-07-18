package com.manus.assistant

import android.content.Context

class SkillsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listSkillsMessage(): String {
        val skills = loadSkills()
        return if (skills.isEmpty()) {
            "No skills are registered yet."
        } else {
            "Registered skills:\n" + skills.joinToString("\n") { "• $it" }
        }
    }

    fun addSkill(rawName: String): String {
        val name = normalize(rawName)
        if (name.isBlank()) return "Skill name cannot be blank."
        val updated = loadSkills().toMutableSet()
        return if (updated.add(name)) {
            saveSkills(updated)
            "Added skill \"$name\"."
        } else {
            "Skill \"$name\" already exists."
        }
    }

    fun updateSkill(oldRaw: String, newRaw: String): String {
        val oldName = normalize(oldRaw)
        val newName = normalize(newRaw)
        if (oldName.isBlank() || newName.isBlank()) return "Both old and new skill names are required."
        val updated = loadSkills().toMutableSet()
        if (!updated.contains(oldName)) return "Skill \"$oldName\" was not found."
        updated.remove(oldName)
        updated.add(newName)
        saveSkills(updated)
        return "Updated skill \"$oldName\" to \"$newName\"."
    }

    fun removeSkill(rawName: String): String {
        val name = normalize(rawName)
        if (name.isBlank()) return "Skill name cannot be blank."
        val updated = loadSkills().toMutableSet()
        return if (updated.remove(name)) {
            saveSkills(updated)
            "Removed skill \"$name\"."
        } else {
            "Skill \"$name\" was not found."
        }
    }

    fun dashboardMessage(turboModeEnabled: Boolean): String {
        val skills = loadSkills()
        return buildString {
            appendLine("Skills dashboard")
            appendLine("Total skills: ${skills.size}")
            appendLine("Turbo mode: ${if (turboModeEnabled) "ON" else "OFF"}")
            if (skills.isEmpty()) {
                append("No skills registered.")
            } else {
                appendLine("Top skills:")
                skills.take(8).forEach { appendLine("• $it") }
            }
        }.trim()
    }

    private fun loadSkills(): List<String> {
        val persisted = prefs.getStringSet(KEY_SKILLS, null)
        val skills = if (persisted.isNullOrEmpty()) DEFAULT_SKILLS else persisted
        return skills.map { normalize(it) }.filter { it.isNotBlank() }.sorted()
    }

    private fun saveSkills(skills: Set<String>) {
        prefs.edit().putStringSet(KEY_SKILLS, skills).apply()
    }

    private fun normalize(name: String): String = name.trim()

    companion object {
        private const val PREFS_NAME = "skills_prefs"
        private const val KEY_SKILLS = "skills_set"
        private val DEFAULT_SKILLS = setOf(
            "c++",
            "python",
            "java",
            "terminal access",
            "build tools",
            "skills updater",
            "skills dashboard"
        )
    }
}
