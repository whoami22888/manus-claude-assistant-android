package com.manus.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unit tests for [AssistantEngine.processInputLocal].
 *
 * All tests run on the JVM without an Android runtime because
 * [processInputLocal] is a pure Kotlin function in the companion object.
 */
class AssistantEngineTest {

    private fun respond(input: String) = AssistantEngine.processInputLocal(input)

    // -----------------------------------------------------------------------
    // Greetings
    // -----------------------------------------------------------------------

    @Test fun `hello triggers greeting`() {
        assertTrue(respond("hello").contains("Hello!", ignoreCase = true))
    }

    @Test fun `hi triggers greeting`() {
        assertTrue(respond("hi there").contains("Hello!", ignoreCase = true))
    }

    @Test fun `hey triggers greeting`() {
        assertTrue(respond("hey assistant").contains("Hello!", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Time / Date
    // -----------------------------------------------------------------------

    @Test fun `'time' alone returns current time`() {
        val response = respond("time")
        val expectedPrefix = "The current time is"
        assertTrue("Expected time response, got: $response", response.startsWith(expectedPrefix))
    }

    @Test fun `'what time is it' returns current time`() {
        val response = respond("what time is it")
        assertTrue(response.startsWith("The current time is"))
    }

    @Test fun `'today' returns current date`() {
        val response = respond("today")
        val expectedMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        assertTrue("Expected date with month $expectedMonth, got: $response",
            response.contains(expectedMonth))
    }

    @Test fun `'what date' returns current date`() {
        val response = respond("what date is it")
        assertTrue(response.startsWith("Today's date is"))
    }

    @Test fun `'date' alone returns current date`() {
        val response = respond("date")
        assertTrue(response.startsWith("Today's date is"))
    }

    // -----------------------------------------------------------------------
    // File operations
    // -----------------------------------------------------------------------

    @Test fun `upload returns descriptive hint`() {
        val response = respond("upload")
        assertTrue(response.isNotBlank())
    }

    @Test fun `download returns descriptive hint`() {
        val response = respond("download something")
        assertTrue(response.isNotBlank())
    }

    @Test fun `list files returns descriptive hint`() {
        val response = respond("list files")
        assertTrue(response.isNotBlank())
    }

    @Test fun `show files returns descriptive hint`() {
        val response = respond("show files")
        assertTrue(response.isNotBlank())
    }

    // -----------------------------------------------------------------------
    // Help
    // -----------------------------------------------------------------------

    @Test fun `help lists capabilities`() {
        val response = respond("help")
        assertTrue(response.contains("Time", ignoreCase = true))
        assertTrue(response.contains("File", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Farewells
    // -----------------------------------------------------------------------

    @Test fun `bye triggers goodbye`() {
        assertTrue(respond("bye").contains("Goodbye", ignoreCase = true))
    }

    @Test fun `goodbye triggers goodbye`() {
        assertTrue(respond("goodbye").contains("Goodbye", ignoreCase = true))
    }

    @Test fun `exit triggers goodbye`() {
        assertTrue(respond("exit").contains("Goodbye", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Thanks
    // -----------------------------------------------------------------------

    @Test fun `thank you acknowledged`() {
        assertTrue(respond("thank you").contains("welcome", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Unknown input fallback
    // -----------------------------------------------------------------------

    @Test fun `unknown input echoes back`() {
        val input = "xyzzy plugh"
        val response = respond(input)
        assertTrue("Expected echo of input, got: $response", response.contains(input))
    }

    @Test fun `unknown input suggests help`() {
        val response = respond("what is the meaning of life")
        assertTrue(response.contains("help", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Case insensitivity
    // -----------------------------------------------------------------------

    @Test fun `HELLO (upper case) triggers greeting`() {
        assertTrue(respond("HELLO").contains("Hello!", ignoreCase = true))
    }

    @Test fun `mixed case 'Hello World' triggers greeting`() {
        assertTrue(respond("Hello World").contains("Hello!", ignoreCase = true))
    }

    @Test fun `leading and trailing whitespace is ignored`() {
        assertTrue(respond("  hello  ").contains("Hello!", ignoreCase = true))
    }
}
