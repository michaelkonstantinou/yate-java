package com.mkonst.models

import com.aallam.openai.api.chat.ChatMessage
import com.google.genai.Client
import com.google.genai.errors.ApiException
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.ThinkingConfig
import com.mkonst.config.ConfigYate
import com.mkonst.evaluation.YateStats
import com.mkonst.helpers.YateConsole
import com.mkonst.interfaces.ChatModel
import com.mkonst.providers.GenAIMessageProvider
import com.mkonst.types.CodeResponse
import com.mkonst.types.exceptions.EmptyPromptsInRequestException
import com.mkonst.types.genai.GenAIMessage
import com.mkonst.types.genai.GenAIRole


class ChatGeminiModel(private val modelName: String): ChatModel {
    override var nrRequests: Int = 0
    private val client: Client = Client.builder().apiKey(ConfigYate.getString("GOOGLE_API_KEY")).build()
    private var config: GenerateContentConfig.Builder

    init {
        // Gemma models cannot contain thinking config
        if (modelName.startsWith("gemma")) {
            config = GenerateContentConfig.builder()
                .temperature(0.1F)
        } else {
            config = GenerateContentConfig.builder()
                .temperature(0.1F)
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
        }
    }

    override fun ask(prompts: List<String>, systemPrompt: String?, history: MutableList<ChatMessage>?): CodeResponse {
        YateStats.startTime("gemini_request")

        // Make sure the prompts given contains data
        if (prompts.isEmpty()) {
            throw EmptyPromptsInRequestException()
        }

        val chatHistory: MutableList<Content> = mutableListOf()
        if (history !== null && history.isNotEmpty()) {
            for (historyItem in history) {
                chatHistory.add(GenAIMessageProvider.fromOpenAIChatMessage(historyItem))
            }
        }

        // If no history is present and system prompt is provided, initialize history with system prompt
        if (history === null && systemPrompt !== null && !modelName.startsWith("gemma")) {
            config = config.systemInstruction(Content.builder().parts(Part.builder().text(systemPrompt).build()))
        }

        var answer: String? = null
        for (prompt in prompts) {
            this.nrRequests += 1

            // Add prompt to history
            chatHistory.add(GenAIMessageProvider.fromText(GenAIRole.USER, prompt))

            // Execute query to Google API
            answer = executeRequest(chatHistory)
            if (answer !== null) {
                chatHistory.add(GenAIMessageProvider.fromText(GenAIRole.MODEL, answer))
            }
        }

        YateStats.endTime("gemini_request", true)
        return CodeResponse(answer, transformHistoryToChatMessages(chatHistory))
    }

    override fun closeConnection() {
        // Nothing to close
    }

    private fun executeRequest(chatHistory: List<Content>): String? {
        var queryIterations: Int = 0
        var response: String? = null

        while (queryIterations < 4) {
            queryIterations += 1
            try {
                    response = client.models.generateContent(modelName, chatHistory, config.build()).text()
            } catch (e: ApiException) {

                // Rate Limit
                if (e.code() == 429) {

                    YateConsole.error("Rate API limit hit: Pausing execution for 20 seconds and trying again")
                    Thread.sleep(20_000)
                }

                // On Bad requests abort
                if (e.code() == 400) {
                    YateConsole.error(e.message())
                    throw Exception("Bad request API call on ChatGeminiModel")
                }
            }
        }

        return response
    }

    /**
     * The function will transform the Content instances of gen-ai library, to OpenAI's ChatMessage instances
     * for cross-compatibility
     */
    private fun transformHistoryToChatMessages(history: MutableList<Content>): MutableList<ChatMessage> {
        val newHistory = mutableListOf<ChatMessage>()
        for (historyItem in history) {
            val content = GenAIMessage(historyItem.role().get(), historyItem.text().toString())
            newHistory.add(content.toOpenAIChatMessage())
        }

        return newHistory
    }

}