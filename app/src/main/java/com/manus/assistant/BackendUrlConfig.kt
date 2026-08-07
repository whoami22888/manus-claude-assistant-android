package com.manus.assistant

import java.net.URI

/** Immutable, provider-neutral configuration for the app-owned agent endpoint. */
data class BackendUrlConfig(val rawUrl: String?) {
    val url: String? = rawUrl?.trim()?.takeIf { isValid(it) }
    val isValid: Boolean get() = url != null

    companion object {
        /** Accept only absolute HTTPS URLs without credentials, fragments, or query parameters. */
        fun isValid(value: String?): Boolean {
            if (value.isNullOrBlank() || value != value.trim()) return false
            return try {
                val uri = URI(value)
                uri.scheme.equals("https", ignoreCase = true) &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null && uri.fragment == null && uri.query == null
            } catch (_: Exception) {
                false
            }
        }
    }
}
