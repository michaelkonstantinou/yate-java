package com.mkonst.providers

import com.aallam.openai.api.chat.ChatMessage
import com.google.genai.types.Content
import com.google.genai.types.Part
import com.mkonst.types.genai.GenAIRole

object GenAIMessageProvider {

    /**
     * Returns a valid Content message that can be used as history by Google's GenAI library
     */
    fun fromText(role: GenAIRole, message: String): Content {
        return Content.builder().role(role.name.lowercase()).parts(Part.builder().text(message).build()).build()
    }

    /**
     * Provides a valid Content instance to be used by Google's GenAI library, given OpenAI's ChatMessage instance
     */
    fun fromOpenAIChatMessage(chatMessage: ChatMessage): Content {
        val role: GenAIRole = when (chatMessage.role.role) {
            "user" -> GenAIRole.USER
            "assistant" -> GenAIRole.MODEL
            "system" -> GenAIRole.SYSTEM
            else -> throw Exception("Not supported role provided")
        }

        return fromText(role, chatMessage.content ?: "")
    }
}