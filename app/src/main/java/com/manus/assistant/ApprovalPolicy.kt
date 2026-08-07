package com.manus.assistant

/** Conservative local guard: potentially consequential requests need explicit approval. */
object ApprovalPolicy {
    private val actionWords = Regex("\\b(send|post|delete|purchase|pay|buy|grant|allow|share)\\b", RegexOption.IGNORE_CASE)
    private val protectedWords = Regex("\\b(permission|access|account|security|password|credential|token)\\b", RegexOption.IGNORE_CASE)

    fun requiresApproval(request: String): Boolean =
        actionWords.containsMatchIn(request) || protectedWords.containsMatchIn(request)
}
