package com.manus.assistant.projectconfig

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Tests for [findRepoRoot], the helper used by the other `projectconfig` tests to locate
 * repo-root-relative files (e.g. `gradle.properties`, `.github/workflows/android.yml`)
 * regardless of which directory the test JVM starts in.
 */
class RepoRootTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var originalUserDir: String

    @Before
    fun setUp() {
        originalUserDir = System.getProperty("user.dir")
    }

    @After
    fun tearDown() {
        System.setProperty("user.dir", originalUserDir)
    }

    @Test
    fun `finds repo root when the starting directory itself contains settings-gradle-kts`() {
        val root = tempFolder.newFolder("repo")
        File(root, "settings.gradle.kts").createNewFile()
        System.setProperty("user.dir", root.absolutePath)

        val found = findRepoRoot()

        assertEquals(root.canonicalFile, found.canonicalFile)
    }

    @Test
    fun `walks up several parent directories to find settings-gradle-kts`() {
        val root = tempFolder.newFolder("repo")
        File(root, "settings.gradle.kts").createNewFile()
        val nested = File(root, "app/src/test/java").apply { mkdirs() }
        System.setProperty("user.dir", nested.absolutePath)

        val found = findRepoRoot()

        assertEquals(root.canonicalFile, found.canonicalFile)
    }

    @Test
    fun `stops at the nearest ancestor containing settings-gradle-kts`() {
        // Two settings.gradle.kts files exist at different levels; the search must stop
        // at the closest one when walking upward from the starting directory, not the
        // outermost one.
        val outer = tempFolder.newFolder("outer")
        File(outer, "settings.gradle.kts").createNewFile()
        val inner = File(outer, "inner").apply { mkdirs() }
        File(inner, "settings.gradle.kts").createNewFile()
        val startDir = File(inner, "module").apply { mkdirs() }
        System.setProperty("user.dir", startDir.absolutePath)

        val found = findRepoRoot()

        assertEquals(inner.canonicalFile, found.canonicalFile)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws IllegalStateException when no ancestor directory contains settings-gradle-kts`() {
        val isolated = tempFolder.newFolder("no-repo-here")
        System.setProperty("user.dir", isolated.absolutePath)

        findRepoRoot()
    }

    @Test
    fun `exception message explains why the repo root could not be located`() {
        val isolated = tempFolder.newFolder("no-repo-here-2")
        System.setProperty("user.dir", isolated.absolutePath)

        val exception = runCatching { findRepoRoot() }.exceptionOrNull()

        assertTrue(
            "Expected an IllegalStateException, got: $exception",
            exception is IllegalStateException
        )
        assertTrue(
            "Expected message to mention settings.gradle.kts, got: ${exception?.message}",
            exception?.message?.contains("settings.gradle.kts") == true
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `a similarly named file such as settings-gradle does not satisfy the search`() {
        val isolated = tempFolder.newFolder("similar-name-only")
        File(isolated, "settings.gradle").createNewFile()
        System.setProperty("user.dir", isolated.absolutePath)

        findRepoRoot()
    }

    @Test
    fun `returns the actual repository root when run against the real project layout`() {
        // No property override here: exercises findRepoRoot() against whatever working
        // directory the test JVM actually started with, matching how it is used by
        // AndroidCiWorkflowTest and GradlePropertiesFileTest.
        val found = findRepoRoot()

        assertTrue(
            "Expected ${found.path} to contain settings.gradle.kts",
            File(found, "settings.gradle.kts").isFile
        )
    }
}
