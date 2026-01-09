package com.mkonst.providers

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.mkonst.services.PromptService

class ChatHistoryBuilder {
    private var systemPrompt: ChatMessage? = null
    private val history: MutableList<ChatMessage> = mutableListOf()

    fun setSystemPrompt(promptName: String): ChatHistoryBuilder {
        this.systemPrompt = ChatMessage(ChatRole.System, PromptService.get(promptName))

        return this
    }

    fun addUserPrompt(promptName: String, variables: HashMap<String, String>): ChatHistoryBuilder {
        this.history.add(ChatMessage(ChatRole.User, PromptService.get(promptName, variables)))

        return this
    }

    fun addAssistantPrompt(promptName: String, variables: HashMap<String, String>): ChatHistoryBuilder {
        this.history.add(ChatMessage(ChatRole.Assistant, PromptService.get(promptName, variables)))

        return this
    }

    fun build(): MutableList<ChatMessage> {
        val chatHistory: MutableList<ChatMessage> = mutableListOf()

        if (this.systemPrompt !== null) {
            chatHistory.add(systemPrompt!!)
        }

        chatHistory.addAll(this.history)

        return chatHistory
    }
}