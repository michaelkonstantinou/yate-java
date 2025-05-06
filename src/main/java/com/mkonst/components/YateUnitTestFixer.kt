package com.mkonst.components

import com.aallam.openai.api.chat.ChatMessage
import com.github.javaparser.symbolsolver.resolution.typeinference.bounds.FalseBound
import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.interfaces.YateUnitTestFixerInterface
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse
import com.openai.errors.BadRequestException

class YateUnitTestFixer(private var repositoryPath: String, private var dependencyTool: String): YateUnitTestFixerInterface {
    private var model: ChatOpenAIModel = ChatOpenAIModel();

    override fun closeConnection() {
        model.closeConnection()
    }

    fun fixTestsFromErrorLog(response: YateResponse, includeClassCodeInPrompt: Boolean = true): YateResponse {
        val errors: String? = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool)

        if (errors === null) {
            response.hasChanges = false

            return response
        }

        // todo: Find ambiguous references

        // Use the model and attempt to fix the errors based on the error log provided from execution
        val promptVars = hashMapOf("CLASS_CONTENT" to response.testClassContainer.getCompleteContent(), "ERRORS" to errors)
        val promptInstruction: String = if (includeClassCodeInPrompt) "fix_errors" else "fix_errors_no_class_code"
        val prompt: String = PromptService.get(promptInstruction, promptVars)

        return generateNewTestClass(mutableListOf(prompt), response)
    }

    /**
     * The method that interacts with the LLM to generate Tests.
     * Returns a new YateResponse instance
     * based on the response along with the history
     */
    private fun generateNewTestClass(prompts: MutableList<String>, response: YateResponse): YateResponse {
        var modelResponse: CodeResponse
        try {
            modelResponse = model.ask(prompts, history = response.conversation)
        } catch (e: BadRequestException) {
            if (e.statusCode() == 400) {
                modelResponse = model.ask(prompts, history = response.conversation)
            } else {
                throw e
            }
        }

        // Check whether there was a decoding issue that needs to be regenerated
        if (modelResponse.codeContent === null) {
            modelResponse = fixDecodingError(response.conversation)
        }

        // Create a new test class container with the new information
        response.recreateTestClassContainer(modelResponse.codeContent)

        return response
    }

    /**
     * Makes a request to the LLM that its last response could not be decoded and should be regenerated
     * Extremely useful if YATE asked the LLM to generate tests but the LLM returned mostly conversation text
     */
    private fun fixDecodingError(conversation: MutableList<ChatMessage>): CodeResponse {
        val prompt = PromptService.get("decoding_error")

        return this.model.ask(mutableListOf(prompt), history = conversation)
    }
}