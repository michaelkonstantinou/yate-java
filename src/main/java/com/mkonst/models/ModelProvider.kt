package com.mkonst.models

import com.mkonst.interfaces.ChatModel

object ModelProvider {

    /**
     * Returns the ChatModel instance that contains the implementation of the given model name
     * In case the model is null, the default value is returned
     */
    fun get(modelName: String? = null): ChatModel {
        if (modelName === null) {
            return ChatOpenAIModel()
        }

        return if (modelName.startsWith("gpt") || modelName.startsWith("deepseek") || modelName.startsWith("mistral") || modelName.startsWith("codestral")) {

            if (modelName == "gpt_env") {
                ChatOpenAIModel()
            } else {
                ChatOpenAIModel(modelName)
            }
        } else if (modelName.startsWith("gemma") || modelName.startsWith("gemini")){
            ChatGeminiModel(modelName)
        } else {
            ChatOllamaModel(modelName)
        }
    }
}