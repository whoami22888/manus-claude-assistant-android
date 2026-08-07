package com.manus.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendPolicyTest {
    @Test fun `accepts a normal HTTPS endpoint`() {
        val config = BackendUrlConfig("https://agent.example.com/v1/agent")
        assertTrue(config.isValid)
    }

    @Test fun `rejects non HTTPS malformed and credential URLs`() {
        listOf("http://agent.example.com", "https://user:pass@agent.example.com", "https://agent.example.com?x=1", "not a url", " https://agent.example.com").forEach {
            assertFalse("$it should be rejected", BackendUrlConfig(it).isValid)
        }
    }

    @Test fun `invalid configuration has no endpoint`() {
        assertNull(BackendUrlConfig("http://agent.example.com").url)
    }

    @Test fun `approval policy catches consequential action verbs`() {
        listOf("send this email", "post an update", "delete the note", "purchase an item").forEach {
            assertTrue("$it should require approval", ApprovalPolicy.requiresApproval(it))
        }
    }

    @Test fun `approval policy catches protected domains`() {
        listOf("change permission", "grant access", "view account", "update security settings").forEach {
            assertTrue("$it should require approval", ApprovalPolicy.requiresApproval(it))
        }
    }

    @Test fun `ordinary research does not require approval`() {
        assertFalse(ApprovalPolicy.requiresApproval("Research Kotlin coroutine cancellation"))
    }
}
