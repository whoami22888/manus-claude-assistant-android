package com.example.personalassistant.projectconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for the "Decode release keystore" step of `.github/workflows/android.yml`.
 *
 * These guard against a class of bug where a step's `run:` (or `if:`) key ends up
 * indented differently from its sibling `name:` key. Such a mismatch breaks the YAML
 * block-mapping structure of the step, since sibling keys in a YAML mapping must share
 * the same indentation.
 */
class AndroidCiWorkflowTest {

    private lateinit var lines: List<String>
    private lateinit var stepLines: List<String>
    private var stepBaseIndent: Int = -1

    @Before
    fun setUp() {
        val workflowFile = File(findRepoRoot(), ".github/workflows/android.yml")
        assertTrue("Workflow file should exist at ${workflowFile.path}", workflowFile.isFile)
        lines = workflowFile.readLines()

        val stepStart = lines.indexOfFirst { it.trim() == "- name: Decode release keystore" }
        assertTrue("Could not find the 'Decode release keystore' step in the workflow", stepStart >= 0)
        stepBaseIndent = lines[stepStart].indexOf('-')

        val relativeEnd = lines.drop(stepStart + 1)
            .indexOfFirst { it.isNotBlank() && leadingSpaces(it) <= stepBaseIndent }
        val stepEnd = if (relativeEnd == -1) lines.size else stepStart + 1 + relativeEnd

        stepLines = lines.subList(stepStart, stepEnd)
    }

    private fun leadingSpaces(line: String): Int {
        val firstNonSpace = line.indexOfFirst { it != ' ' }
        return if (firstNonSpace == -1) line.length else firstNonSpace
    }

    /** Indentation of the step's own top-level keys, derived from the `- name:` marker. */
    private val stepKeyIndent: Int
        get() = stepBaseIndent + 2

    private fun keyIndent(keyName: String): Int {
        val line = stepLines.drop(1).firstOrNull { it.trimStart().startsWith("$keyName:") }
        assertTrue("Expected the step to contain a '$keyName:' key", line != null)
        return leadingSpaces(line!!)
    }

    @Test
    fun `if and run keys are indented the same as the step's name key`() {
        assertEquals("'if:' should be indented like the step's 'name:' key", stepKeyIndent, keyIndent("if"))
        assertEquals("'run:' should be indented like the step's 'name:' key", stepKeyIndent, keyIndent("run"))
    }

    @Test
    fun `if condition uses the bare secrets expression without curly braces`() {
        val ifLine = stepLines.first { it.trimStart().startsWith("if:") }.trim()
        assertEquals("if: secrets.KEYSTORE_FILE", ifLine)
    }

    @Test
    fun `run block scalar is nested deeper than the run key and contains the keystore commands`() {
        val runIndent = keyIndent("run")
        val runLineIndex = stepLines.indexOfFirst { it.trimStart().startsWith("run:") }
        val body = stepLines.drop(runLineIndex + 1).filter { it.isNotBlank() }

        assertTrue("run: block should contain command lines", body.isNotEmpty())
        body.forEach { line ->
            assertTrue(
                "Command line '$line' must be indented deeper than 'run:' (indent=$runIndent) " +
                    "to remain part of the block scalar",
                leadingSpaces(line) > runIndent
            )
        }

        val joined = body.joinToString("\n")
        assertTrue(joined.contains("base64 --decode"))
        assertTrue(joined.contains("KEYSTORE_PATH="))
        assertTrue(joined.contains("KEYSTORE_PASSWORD="))
        assertTrue(joined.contains("KEY_ALIAS="))
        assertTrue(joined.contains("KEY_PASSWORD="))
    }

    @Test
    fun `every step-level if condition in the workflow is indented like its own name key`() {
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.trim().startsWith("- name:")) {
                val base = leadingSpaces(line)
                val expectedKeyIndent = base + 2
                var j = index + 1
                while (j < lines.size && !(lines[j].isNotBlank() && leadingSpaces(lines[j]) <= base)) {
                    val trimmed = lines[j].trimStart()
                    if (trimmed.startsWith("if:")) {
                        assertEquals(
                            "Step starting at line ${index + 1} has an 'if:' key misaligned with its 'name:' key",
                            expectedKeyIndent,
                            leadingSpaces(lines[j])
                        )
                    }
                    j++
                }
            }
            index++
        }
    }

    @Test
    fun `build step conditions use github event inputs with non-dispatch guard`() {
        assertTrue(
            lines.any {
                it.trim() ==
                    "if: \${{ github.event_name != 'workflow_dispatch' || (github.event_name == 'workflow_dispatch' && (github.event.inputs.build_type == 'debug' || github.event.inputs.build_type == 'both')) }}"
            }
        )
        assertTrue(
            lines.any {
                it.trim() ==
                    "if: \${{ github.event_name == 'workflow_dispatch' && (github.event.inputs.build_type == 'release' || github.event.inputs.build_type == 'both') }}"
            }
        )
    }

    @Test
    fun `apk packaging step fails when no apks are copied`() {
        val workflow = lines.joinToString("\n")
        assertTrue(workflow.contains("apks=(artifacts/*.apk)"))
        assertTrue(workflow.contains("if [ \${#apks[@]} -eq 0 ]; then"))
        assertTrue(workflow.contains("exit 1"))
        assertTrue(workflow.contains("No APK files were built to package."))
        assertTrue(workflow.contains("cd artifacts && zip -r ../manus-assistant-apks.zip ."))
        assertFalse(workflow.contains("touch manus-assistant-apks.zip"))
    }
}
