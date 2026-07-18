package com.manus.assistant

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter that renders a list of [ChatMessage] items as chat bubbles.
 *
 * USER messages are right-aligned with a blue background; ASSISTANT messages
 * are left-aligned with a gray background; SYSTEM notifications are centred
 * with a translucent background.
 */
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessage.text = when (msg.sender) {
            ChatMessage.Sender.USER       -> "You: ${msg.text}"
            ChatMessage.Sender.ASSISTANT  -> "Assistant: ${msg.text}"
            ChatMessage.Sender.SYSTEM     -> msg.text
        }
        val lp = holder.tvMessage.layoutParams as FrameLayout.LayoutParams
        when (msg.sender) {
            ChatMessage.Sender.USER -> {
                lp.gravity = Gravity.END
                holder.tvMessage.setBackgroundResource(R.drawable.bg_message_user)
            }
            ChatMessage.Sender.ASSISTANT -> {
                lp.gravity = Gravity.START
                holder.tvMessage.setBackgroundResource(R.drawable.bg_message_assistant)
            }
            ChatMessage.Sender.SYSTEM -> {
                lp.gravity = Gravity.CENTER
                holder.tvMessage.setBackgroundResource(R.drawable.bg_message_system)
            }
        }
        holder.tvMessage.layoutParams = lp
    }

    override fun getItemCount(): Int = messages.size
}
