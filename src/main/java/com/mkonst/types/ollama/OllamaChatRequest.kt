package com.mkonst.types.ollama

import com.aallam.openai.api.chat.ChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequest(val model: String, val messages: List<ChatMessage>, val stream: Boolean)
