package com.mkonst.models

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.mkonst.config.ConfigYate
import com.mkonst.types.CodeResponse
import kotlinx.coroutines.runBlocking

class ChatOpenAIModel(model: String?) {
    private lateinit var model: String
    private var nrRequests: Int = 0
    private var client: OpenAI

    init {
        if (model === null) {
            this.model = ConfigYate.getString("GPT_MODEL")
        }

        if (model !== null && model.contains("deepseek")) {
            this.client = OpenAI(
                    token = ConfigYate.getString("DEEPSEEK_API_KEY"),
                    host = OpenAIHost(ConfigYate.getString("DEEPSEEK_BASE_URL"))
            )
        } else {
            this.client = OpenAI(
                    token = ConfigYate.getString("GPT_API_KEY"),
                    organization = ConfigYate.getString("GPT_ORGANIZATION")
            )
        }
    }

    /**
     * Executes an API request using the OpenAI library, decodes the result into a code snippet and returns its value
     * Requires a list of prompts. Each prompt will be sent to the API, save its result and repeat for all elements
     * in the prompt list
     * If history is given, the request will firstly contain the history and then the prompt requests
     * If system prompt is given (and history is not given), the first message will be the provided system prompt
     */
    fun ask(prompts: List<String>, systemPrompt: String? = null, history: MutableList<ChatMessage>? = null): CodeResponse {
        // Make sure the prompts given contains data
        if (prompts.isEmpty()) {
            throw Exception("Prompt list cannot be empty when making an API request")
        }

        // Initialize conversation. Do not append the system prompt (even if given), if history is present
        val conversation: MutableList<ChatMessage> = if (history !== null) history else mutableListOf()
        if (history !== null && systemPrompt !== null) {
            conversation.add(ChatMessage(ChatRole.System, systemPrompt))
        }

        var answer: String? = null
        for (prompt in prompts) {
            this.nrRequests += 1
            conversation.add(ChatMessage(ChatRole.User, prompt))

            runBlocking {
                answer = executeRequest(conversation)
                conversation.add(ChatMessage(ChatRole.Assistant, answer))
            }
        }

        return CodeResponse(answer, conversation)
    }

    fun getNrRequests(): Int = nrRequests

    fun setNrRequests(value: Int) {
        nrRequests = value
    }

    fun closeConnection() {
        client.close()
    }

    /**
     * Makes the (possibly network) request to the model and returns its message response as a String
     */
    private suspend fun executeRequest(conversation: MutableList<ChatMessage>): String? {
        val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId(this.model),
                temperature = 0.1,
                messages = conversation
        )

        val completion: ChatCompletion = client.chatCompletion(chatCompletionRequest)

        return completion.choices.first().message.content
    }
}