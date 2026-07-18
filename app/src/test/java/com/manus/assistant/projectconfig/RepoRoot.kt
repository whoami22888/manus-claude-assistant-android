package com.manus.assistant.projectconfig

import java.io.File

/**
 * Locates the repository root by walking up from the current working directory until a
 * directory containing `settings.gradle.kts` is found.
 *
 * Gradle unit test tasks typically run with the module directory (e.g. `app/`) as the
 * process working directory, so this makes repo-root-relative file lookups in tests
 * independent of exactly which directory the test JVM starts in.
 */
internal fun findRepoRoot(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").exists()) return dir
        dir = dir.parentFile
    }
    throw IllegalStateException("Unable to locate repository root (settings.gradle.kts not found)")
}