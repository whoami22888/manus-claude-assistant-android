package com.manus.assistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Calls the app-owned HTTPS agent backend; provider credentials never reach this client. */
class AgentBackendClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    fun chat(config: BackendUrlConfig, message: String, googleIdToken: String? = null): String {
        val endpoint = config.url ?: throw IllegalStateException("A valid HTTPS backend URL is required")
        val payload = JSONObject().put("message", message)
        if (!googleIdToken.isNullOrBlank()) payload.put("idToken", googleIdToken)
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody(JSON_TYPE))
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("Agent backend returned ${response.code}")
            return JSONObject(body).optString("reply").trim().takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Agent backend returned no reply")
        }
    }

    private companion object { val JSON_TYPE = "application/json; charset=utf-8".toMediaType() }
}
