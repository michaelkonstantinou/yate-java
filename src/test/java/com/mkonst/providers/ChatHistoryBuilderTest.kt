package com.mkonst.providers

import com.aallam.openai.api.chat.ChatRole
import com.mkonst.config.ConfigYate
import com.mkonst.services.PromptService
import org.junit.jupiter.api.Test
import kotlin.test.*

class ChatHistoryBuilderTest {

    @Test
    fun canBuildEmptyHistory() {
        val builder: ChatHistoryBuilder = ChatHistoryBuilder()
        val history = builder.build()

        assertEquals(history, mutableListOf())
    }

    @Test
    fun canBuildValidHistory() {
        ConfigYate.initialize()
        PromptService.initialize()
        val builder: ChatHistoryBuilder = ChatHistoryBuilder()
        builder.setSystemPrompt("system")
        builder.addUserPrompt("ablation_generate_simple", hashMapOf("CLASS_CONTENT" to "SAMPLE CODE"))
        builder.addAssistantPrompt("code_response", hashMapOf("CODE_LANG" to "java", "RESPONSE" to "MY CODE HERE"))
        val history = builder.build()

        assertEquals(history.first().role, ChatRole.System)
        assertEquals(history.get(1).role, ChatRole.User)
        assertEquals(history.get(2).role, ChatRole.Assistant)
    }
}