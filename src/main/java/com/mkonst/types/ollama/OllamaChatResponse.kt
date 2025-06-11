package com.mkonst.types.ollama

import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatResponse(
    val message: OllamaMessageContent,
    val done: Boolean,
)

@Serializable
data class OllamaMessageContent(
    val role: String,
    val content: String
)
