package com.manus.assistant.projectconfig

import org.junit.Assert.assertEquals
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
    private val stepStarterPrefixes = listOf("- name:", "- uses:")

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
        val runLineIndex = stepLines.indexOfFirst { it.trimStart().startsWith("run:") }
        val searchLines = if (runLineIndex != -1) {
            val runIndent = leadingSpaces(stepLines[runLineIndex])
            stepLines.filterIndexed { index, line ->
                index <= runLineIndex || (line.isNotBlank() && leadingSpaces(line) <= runIndent)
            }
        } else {
            stepLines
        }
        val line = searchLines.drop(1).firstOrNull { it.trimStart().startsWith("$keyName:") }
        assertTrue("Expected the step to contain a '$keyName:' key", line != null)
        return leadingSpaces(line!!)
    }

    @Test
    fun `if and run keys are indented the same as the step's name key`() {
        assertEquals("'if:' should be indented like the step's 'name:' key", stepKeyIndent, keyIndent("if"))
        assertEquals("'run:' should be indented like the step's 'name:' key", stepKeyIndent, keyIndent("run"))
    }

    @Test
    fun `if condition uses the GitHub Actions expression for the keystore secret`() {
        val runLineIndex = stepLines.indexOfFirst { it.trimStart().startsWith("run:") }
        val searchLines = if (runLineIndex != -1) {
            val runIndent = leadingSpaces(stepLines[runLineIndex])
            stepLines.filterIndexed { index, line ->
                index <= runLineIndex || (line.isNotBlank() && leadingSpaces(line) <= runIndent)
            }
        } else {
            stepLines
        }
        val ifLine = searchLines.firstOrNull { it.trimStart().startsWith("if:") }?.trim()
        assertEquals("if: \${{ env.KEYSTORE_FILE != '' }}", ifLine)
    }

    @Test
    fun `run block scalar is nested deeper than the run key and contains the keystore commands`() {
        val runIndent = keyIndent("run")
        val runLineIndex = stepLines.indexOfFirst { it.trimStart().startsWith("run:") }
        val body = stepLines.drop(runLineIndex + 1)
            .takeWhile { leadingSpaces(it) > runIndent || it.isBlank() }
            .filter { it.isNotBlank() }

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
            val trimmedStarter = line.trimStart()
            val isStepStarter = stepStarterPrefixes.any { trimmedStarter.startsWith(it) }
            if (isStepStarter) {
                val base = leadingSpaces(line)
                val expectedKeyIndent = base + 2
                var j = index + 1
                var insideRunBlock = false
                var runBlockIndent = 0
                while (j < lines.size && !(lines[j].isNotBlank() && leadingSpaces(lines[j]) <= base)) {
                    val lineJ = lines[j]
                    if (lineJ.isBlank()) {
                        j++
                        continue
                    }
                    val indentJ = leadingSpaces(lineJ)
                    if (insideRunBlock) {
                        if (indentJ <= runBlockIndent) {
                            insideRunBlock = false
                        } else {
                            j++
                            continue
                        }
                    }
                    val trimmed = lineJ.trimStart()
                    if (trimmed.startsWith("run:")) {
                        insideRunBlock = true
                        runBlockIndent = indentJ
                    } else if (trimmed.startsWith("if:")) {
                        assertEquals(
                            "Step starting at line ${index + 1} has an 'if:' key misaligned with its step starter",
                            expectedKeyIndent,
                            indentJ
                        )
                    }
                    j++
                }
            }
            index++
        }
    }
}
