package com.mkonst.components

import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.models.ModelProvider
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.DependencyTool
import com.mkonst.types.YateResponse
import com.openai.errors.BadRequestException

/**
 * The class is the naive implementation of a component that asks the LLM X times to fix the errors
 * from failing tests (compile or oracle errors)
 */
class YatePlainErrorFixer(private var repositoryPath: String, private var dependencyTool: DependencyTool, modelName: String? = null): AbstractModelComponent(modelName) {

    init {
        YateConsole.info("YatePlainErrorFixer initialized with model: $modelName")
    }
    
    fun fixErrors(response: YateResponse): Boolean {
        val errors: String? = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)
        if (errors === null) {

            return false
        }

        // Use the model and attempt to fix the errors based on the error log provided from execution
        val promptVars = hashMapOf("CLASS_CONTENT" to response.testClassContainer.getCompleteContent(), "ERRORS" to errors)
        val prompt: String = PromptService.get("fix_errors", promptVars)

        generateNewTestClass(mutableListOf(prompt), response)

        return true
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

        // Create a new test class container with the new information
        response.recreateTestClassContainer(modelResponse.codeContent)

        // Update the history of the chat conversation only if the model responded with a valid code content
        if (modelResponse.codeContent !== null) {
            response.conversation = modelResponse.conversation
        }

        return response
    }
}