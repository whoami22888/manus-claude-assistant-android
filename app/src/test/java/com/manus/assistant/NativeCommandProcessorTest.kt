package com.manus.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCommandProcessorTest {

    @Test fun `native help returns supported commands`() {
        val response = NativeCommandProcessor.fallbackProcess("native help")
        assertTrue(response?.contains("native status", ignoreCase = true) == true)
    }

    @Test fun `native process echoes payload`() {
        val response = NativeCommandProcessor.fallbackProcess("native process summarize backups")
        assertEquals(
            "Native core placeholder processed: summarize backups",
            response
        )
    }

    @Test fun `non native command is ignored`() {
        assertNull(NativeCommandProcessor.fallbackProcess("hello"))
    }
}
