package com.mkonst.models

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateConsole
import org.apache.maven.model.Organization
import kotlin.time.Duration.Companion.seconds

/**
 * The purpose of this class is to construct a new Model Client (e.g. OpenAI client) given the modelName
 * Typically a model name can be in one of the following 2 formats:
 * 1. provider@model-family/model-name
 * 2. model name (accepted only for gpt models)
 *
 * The class will isolate the provider from the model name and construct a new client based on the provider's
 * API KEY and BASE_URL specified in .env variables
 */
object ModelClientProvider {

    fun get(modelName: String): Pair<String, OpenAI> {
        if (!modelName.contains("@")) {
            val gptClient = createClient(ConfigYate.getString("GPT_API_KEY"), "", ConfigYate.getString("GPT_ORGANIZATION"))

            return Pair(modelName, gptClient)
        }

        val providerModelName = modelName.split("@")
        val providerName = providerModelName[0]
        val model = providerModelName[1]
        YateConsole.debug("Constructing a new model client to communicate with $providerName for model $model")

        val baseUrl = ConfigYate.getString("${providerName.uppercase()}_BASE_URL")
        val apiKey = ConfigYate.getString("${providerName.uppercase()}_API_KEY")

        return Pair(model, createClient(apiKey, baseUrl, null))
    }

    private fun createClient(token: String, baseUrl: String, organization: String? = null): OpenAI {
        if (organization !== null) {
            return OpenAI(
                token = token,
                organization = organization,
                logging = LoggingConfig(LogLevel.None),
                timeout = Timeout(socket = 600.seconds),
            )
        }

        return OpenAI(
            token = token,
            host = OpenAIHost(baseUrl),
            logging = LoggingConfig(LogLevel.None),
            timeout = Timeout(socket = 600.seconds),
        )

    }
}