package com.mkonst.components

import com.aallam.openai.api.chat.ChatMessage
import com.github.javaparser.symbolsolver.resolution.typeinference.bounds.FalseBound
import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.analysis.MethodCallGraph
import com.mkonst.analysis.java.JavaArgumentsAnalyzer
import com.mkonst.analysis.java.JavaInvocationsAnalyzer
import com.mkonst.analysis.java.JavaMethodProvider
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.interfaces.YateUnitTestFixerInterface
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.MethodCallGraphService
import com.mkonst.services.PromptService
import com.mkonst.types.ClassMethod
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse
import com.openai.errors.BadRequestException

class YateUnitTestFixer(private var repositoryPath: String, private var packageName: String, private var dependencyTool: String): YateUnitTestFixerInterface {
    private var model: ChatOpenAIModel = ChatOpenAIModel()
    private val argumentsAnalyzer: JavaArgumentsAnalyzer = JavaArgumentsAnalyzer(repositoryPath, packageName)
    private val invocationsAnalyzer: JavaInvocationsAnalyzer = JavaInvocationsAnalyzer(repositoryPath)
    private val methodProvider: JavaMethodProvider = JavaMethodProvider(repositoryPath)
    private val methodCallGraph: MethodCallGraph = MethodCallGraphService.getNewMethodCallGraph(repositoryPath, packageName) ?: throw Exception("Method Call Graph cannot be null or empty")

    init {
        if (methodCallGraph.size() <= 0) {
            throw Exception("Method Call Graph cannot be null or empty")
        }
    }

    override fun closeConnection() {
        model.closeConnection()
    }

    /**
     * Runs the tests and stores its output into an error log. If the log is not empty, the method
     * uses the LLM to fix the tests that do not compile.
     *
     * The method returns the new ClassContainer instance, the history of the LLM conversation and whether
     * there have been any changes (=the log was not empty)
     *
     * The flag include_cut_code determines whether the prompt should include the current test implementation.
     * If this method is called subsequently then it might be wise to turn it off for optimization.
     */
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
     * The method uses the code static analysis to find whether the test class uses other non-primitive objects that
     * belong to the repository, yet are unknown to the model. It finds and provides to the model their constructors
     * and asks the model to fix the tests
     */
    fun fixUsingExternalConstructors(cutQualifiedName: String, response: YateResponse): YateResponse {
        val suggestions: String? = argumentsAnalyzer.getClassesInArgumentsLog(cutQualifiedName)

        // If suggestions are not found, then this procedure ends here without any changes to the test class
        if (suggestions === null) {
            YateConsole.debug("No suggestions found for external object construction")
            response.hasChanges = false

            return response
        }

        // Prepare a prompt that will use the suggestions found, and regenerate the test class container
        val promptVars = hashMapOf("CONTENT" to suggestions)
        val prompt = PromptService.get("fix_with_external_constructors", promptVars)

        return generateNewTestClass(mutableListOf(prompt), response)
    }

    /**
     * The method used the LLM to fix the wrong tests, using as input a log of the wrong method invocations
     * used in the test. Initially it analyzes the class, and stores the log output.
     * If the log is empty (no wrong invocations found), the method returns the current implementation
     */
    fun fixWrongMethodInvocations(response: YateResponse): YateResponse {
        val wrongMethodUsages: String? = invocationsAnalyzer.getAllWrongUsagesLog(response.testClassContainer.getQualifiedName())
        if (wrongMethodUsages === null) {
            response.hasChanges = false

            return response
        }

        // Prepare a prompt that will include the wrong method invocations and ask the LLM to fix them
        val prompt: String = PromptService.get("fix_wrong_method_invocations") + wrongMethodUsages

        return generateNewTestClass(mutableListOf(prompt), response)
    }

    /**
     * Uses static analysis to find methods that are being used by the class under test and find potential
     * code snippets that are useful to the LLM to fix the generated tests
     */
    fun fixUsingExternalMethods(cutContainer: ClassContainer, response: YateResponse): YateResponse {
        val instructionMethodCalls: StringBuilder = StringBuilder()
        val instructionImplementations: StringBuilder = StringBuilder()

        val calledMethods: MutableList<ClassMethod> = mutableListOf()
        for (methodName in cutContainer.body.methods.values) {
            val methodCalledMethods: MutableList<ClassMethod> = methodCallGraph.getMethodCallsFrom(ClassMethod(cutContainer.getQualifiedName(methodName)))
            if (methodCalledMethods.isNotEmpty()) {
                calledMethods.addAll(methodCalledMethods)
                instructionMethodCalls
                        .append("Method $methodName calls the following methods:")
                        .append(methodCalledMethods.joinToString(", ") { it.methodName })
                        .append("\n")
            }
        }

        // If there are no external calls, then this prompt is pointless
        if (calledMethods.isEmpty()) {
            response.hasChanges = false

            return response
        }

        // Remove duplicated values
        val calledMethodsSet = calledMethods.toSet()
        for (classMethod: ClassMethod in calledMethodsSet) {
            val methodBody: String? = methodProvider.getMethodBody(classMethod)
            if (methodBody !== null) {
                instructionImplementations.append("// From Class: ${classMethod.className}\n$methodBody\n\n")
            }
        }

        val promptVars = hashMapOf(
                "METHOD_CALLS" to instructionMethodCalls.toString(),
                "METHOD_IMPLEMENTATIONS" to instructionImplementations.toString())
        val prompt = PromptService.get("fix_broken_tests_from_method_calls", promptVars)

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