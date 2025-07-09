package com.mkonst.types.genai

import com.aallam.openai.api.chat.ChatMessage

data class GenAIMessage(val role: String, val message: String) {

    fun toOpenAIChatMessage(): ChatMessage {
        return when (role) {
            "user" -> ChatMessage.User(message)
            "model" -> ChatMessage.Assistant(message)
            "system" -> ChatMessage.System(message)
            else -> throw Exception("Invalid role provided")
        }
    }
}
