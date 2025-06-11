package com.mkonst.interfaces

import com.aallam.openai.api.chat.ChatMessage
import com.mkonst.types.CodeResponse

interface ChatModel {

    /**
     * Executes a request to the model, decodes the result into a code snippet and returns its value
     * Requires a list of prompts. Each prompt will be sent to the model, save its result and repeat for all elements
     * in the prompt list
     * If history is given, the request will firstly contain the history and then the prompt requests
     * If system prompt is given (and history is not given), the first message will be the provided system prompt
     */
    fun ask(prompts: List<String>, systemPrompt: String? = null, history: MutableList<ChatMessage>? = null): CodeResponse

    fun closeConnection()
}