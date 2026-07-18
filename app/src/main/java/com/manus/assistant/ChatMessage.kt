package com.manus.assistant

/**
 * Represents a single message in the chat conversation.
 *
 * [Sender.USER] messages are right-aligned; [Sender.ASSISTANT] messages are
 * left-aligned; [Sender.SYSTEM] notifications are centred.
 */
data class ChatMessage(
    val sender: Sender,
    val text: String
) {
    enum class Sender { USER, ASSISTANT, SYSTEM }
}
