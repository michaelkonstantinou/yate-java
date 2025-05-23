package com.mkonst.components

import com.aallam.openai.api.chat.ChatMessage
import com.mkonst.analysis.ClassContainer
import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse

class YateSecondAgentOracleFixer(repositoryPath: String,
                                 dependencyTool: String
): YateOracleFixer(repositoryPath, dependencyTool) {
    private var modelSecondFixer: ChatOpenAIModel = ChatOpenAIModel()

//    override fun fixErrorsUsingModel(response: YateResponse): Int {
//        return 0
//    }

    /**
     * Uses a second agent that will attempt to identify the reason behind failing tests due to NullPointerException
     * or RuntimeException
     */
    fun fixErrorsUsingSecondAgent(response: YateResponse): Int {
        if (response.testClassContainer.paths.cut === null) {
            return 0
        }

        var nonPassingTests = errorService.findNonPassingTests(dependencyTool)
        var classRelatedInvalidTests = nonPassingTests[response.testClassContainer.className]

        // There is no point on fixing invalid tests that do not belong to the class under test
        if (classRelatedInvalidTests.isNullOrEmpty()) {
            return 0
        }

        // Create a copy to allow reverting if needed
        val testClassWithoutChanges: ClassContainer = response.testClassContainer.copy()

        // Prepare prompts for fixing oracles
        val prompts: MutableList<String> = mutableListOf()

        // Prompt 1: Identify failing tests and potential reasons
        val failingTests = classRelatedInvalidTests.joinToString(",")
        val testClassContent: String = response.testClassContainer.getCompleteContent()
        prompts.add(PromptService.get("identify_failing_test_reasons", hashMapOf(
            "FAILING_TESTS" to failingTests,
            "CLASS_CONTENT" to testClassContent)))

        // Followup prompt: Provide class under test
        val cutContent = YateIO.readFile(response.testClassContainer.paths.cut!!)
        prompts.add(PromptService.get("implementation_of_failing_cut", hashMapOf("CLASS_CONTENT" to cutContent)))

        // Document steps prompt & Generate fixed test class prompt
        prompts.add(PromptService.get("document_fixing_steps"))
        prompts.add(PromptService.get("generate_fixed_test_class"))

        // Attempt 1: Generate Tests using the previous prompt
        val (hasTests, modelResponse) = generateUsingModel(prompts, response)
        if (!hasTests) {
            response.testClassContainer = testClassWithoutChanges
            response.hasChanges = false

            return 0
        }

        // Attempt 2: Check whether tests are not passing and try again
        nonPassingTests = errorService.findNonPassingTests(dependencyTool)
        classRelatedInvalidTests = nonPassingTests[response.testClassContainer.className]

        // There is no point on fixing invalid tests that do not belong to the class under test
        if (classRelatedInvalidTests.isNullOrEmpty()) {
            return 0
        }

        prompts.add(PromptService.get("regenerate_tests"))
        val (hasNewTests, newResponse) = generateUsingModel(prompts, response, modelResponse.conversation)
        if (!hasNewTests) {
            response.testClassContainer = testClassWithoutChanges
            response.hasChanges = false

            return 0
        }

        return 0
    }

    override fun getNrRequests(): Int {
        return model.nrRequests + modelSecondFixer.nrRequests
    }

    override fun resetNrRequests() {
        model.nrRequests = 0
        modelSecondFixer.nrRequests = 0
    }

    private fun generateUsingModel(prompts: List<String>, response: YateResponse, conversation: MutableList<ChatMessage>? = null): Pair<Boolean, CodeResponse> {
        val modelResponse: CodeResponse = model.ask(prompts, PromptService.get("system"), conversation)
        if (modelResponse.codeContent !== null) {

            // Verify that the model's response is compiling before updating the response object
            if (YateJavaUtils.isClassParsing(modelResponse.codeContent!!)) {
                println("Generated new test class using multi agent")
                response.recreateTestClassContainer(modelResponse.codeContent)
                response.testClassContainer.toTestFile()

                return Pair(true, modelResponse)
            }
        }

        println("ERROR: Generated new test class using multi agent")
        return Pair(false, modelResponse)
    }
}