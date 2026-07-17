package com.manus.assistant

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GroqApiClient] using OkHttp's [MockWebServer].
 *
 * A [GroqApiClient] is constructed with the mock server's URL so no real
 * network calls are made.
 */
class GroqApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: GroqApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = GroqApiClient(baseUrl = server.url("/openai/v1/chat/completions").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun successResponse(content: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(
            """{"choices":[{"message":{"role":"assistant","content":"$content"}}]}"""
        )

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test fun `chat returns assistant content on success`() {
        server.enqueue(successResponse("Hello from mock!"))
        val reply = client.chat("Hello", "test-key")
        assertEquals("Hello from mock!", reply)
    }

    @Test fun `chat sends the user message in request body`() {
        server.enqueue(successResponse("ok"))
        client.chat("What is 2+2?", "test-key")
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue("Request body should contain user message", body.contains("What is 2+2?"))
    }

    @Test fun `chat sends system prompt in request body`() {
        server.enqueue(successResponse("ok"))
        client.chat("hello", "test-key")
        val body = server.takeRequest().body.readUtf8()
        assertTrue("Request body should contain system role", body.contains("\"role\":\"system\""))
    }

    @Test fun `chat includes model field in request`() {
        server.enqueue(successResponse("ok"))
        client.chat("hi", "test-key")
        val body = server.takeRequest().body.readUtf8()
        assertTrue("Request body should contain model field", body.contains("\"model\""))
    }

    @Test fun `chat sends bearer authorization header`() {
        server.enqueue(successResponse("ok"))
        client.chat("hi", "test-key")
        val request = server.takeRequest()
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
    }

    // -----------------------------------------------------------------------
    // Conversation history
    // -----------------------------------------------------------------------

    @Test fun `second message includes prior exchange in history`() {
        server.enqueue(successResponse("first reply"))
        server.enqueue(successResponse("second reply"))
        client.chat("First message", "test-key")
        client.chat("Second message", "test-key")
        // Consume first request
        server.takeRequest()
        val secondBody = server.takeRequest().body.readUtf8()
        assertTrue("Second request should include earlier user message",
            secondBody.contains("First message"))
        assertTrue("Second request should include earlier assistant reply",
            secondBody.contains("first reply"))
    }

    @Test fun `clearHistory removes prior messages`() {
        server.enqueue(successResponse("first reply"))
        server.enqueue(successResponse("second reply"))
        client.chat("First message", "test-key")
        client.clearHistory()
        client.chat("After clear", "test-key")
        server.takeRequest()
        val body = server.takeRequest().body.readUtf8()
        assertTrue("After clear the previous message should not appear",
            !body.contains("First message"))
    }

    // -----------------------------------------------------------------------
    // Error handling
    // -----------------------------------------------------------------------

    @Test(expected = Exception::class)
    fun `non-2xx response throws exception`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        client.chat("Hello", "bad-key")
    }

    @Test(expected = IllegalStateException::class)
    fun `blank api key throws IllegalStateException`() {
        client.chat("Hello", "")
    }

    @Test fun `failed request does not add user message to history`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server error"))
        server.enqueue(successResponse("ok"))
        // The failed call is expected to throw; swallow it intentionally so the test can continue.
        try { client.chat("Failed message", "test-key") } catch (_: Exception) { /* expected */ }
        client.chat("Retry", "test-key")
        server.takeRequest() // consume failed request
        val body = server.takeRequest().body.readUtf8()
        assertTrue("Failed message should not appear in retry request",
            !body.contains("Failed message"))
    }

    @Test fun `empty response does not add user message to history`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("")
        )
        server.enqueue(successResponse("ok"))
        try { client.chat("Empty response message", "test-key") } catch (_: Exception) { /* expected */ }
        client.chat("Retry", "test-key")
        server.takeRequest()
        val body = server.takeRequest().body.readUtf8()
        assertTrue("Empty-response message should not appear in retry request",
            !body.contains("Empty response message"))
    }
}
