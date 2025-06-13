package com.mkonst.models

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.mkonst.config.ConfigYate
import com.mkonst.interfaces.ChatModel
import com.mkonst.types.CodeResponse
import com.mkonst.types.exceptions.EmptyPromptsInRequestException
import com.mkonst.types.ollama.OllamaChatRequest
import com.mkonst.types.ollama.OllamaChatResponse
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatOllamaModel(private val model: String): ChatModel {
    override var nrRequests: Int = 0
    private val ollamaChatUrl = ConfigYate.getString("OLLAMA_CHAT_URL")
    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Executes a request to the model, decodes the result into a code snippet and returns its value
     * Requires a list of prompts. Each prompt will be sent to the model, save its result and repeat for all elements
     * in the prompt list
     * If history is given, the request will firstly contain the history and then the prompt requests
     * If system prompt is given (and history is not given), the first message will be the provided system prompt
     */
    override fun ask(prompts: List<String>, systemPrompt: String?, history: MutableList<ChatMessage>?): CodeResponse {
        // Make sure the prompts given contains data
        if (prompts.isEmpty()) {
            throw EmptyPromptsInRequestException()
        }

        // Initialize conversation. Do not append the system prompt (even if given), if history is present
        val conversation: MutableList<ChatMessage> = if (history !== null) history else mutableListOf()
        if (history === null && systemPrompt !== null) {
            conversation.add(ChatMessage.System(systemPrompt))
        }

        var answer: String? = null
        for (prompt in prompts) {
            this.nrRequests += 1
            conversation.add(ChatMessage(ChatRole.User, prompt))

            // execute request
            answer = executeRequest(conversation)
            if (answer !== null) {
                conversation.add(ChatMessage(ChatRole.Assistant, answer))
            }
        }

        return CodeResponse(answer, conversation)
    }

    override fun closeConnection() {
        // Client has no open connection
    }

    /**
     * The method will execute a request to the Ollama model and return only its message content (if successful)
     * For unsuccessful requests the method returns null
     */
    private fun executeRequest(conversation: MutableList<ChatMessage>): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)   // useful for long Ollama responses
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()

        val requestBodyJson = Json.encodeToString(
            OllamaChatRequest(
                model = model,
                messages = conversation,
                stream = false
            )
        )

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = requestBodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(ollamaChatUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseAsString = response.body?.string()
                val parsed = jsonParser.decodeFromString<OllamaChatResponse>(responseAsString!!)

                return parsed.message.content
            }
        }

        return null
    }

}