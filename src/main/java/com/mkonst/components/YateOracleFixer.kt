package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.helpers.*
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.ErrorService
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.OracleError
import com.mkonst.types.TestErrorLog
import com.mkonst.types.YateResponse
import com.openai.errors.BadRequestException

open class YateOracleFixer(protected var repositoryPath: String,
                           protected var dependencyTool: String,
                           protected var expectedTypesToIgnore: MutableList<String> = mutableListOf()
): AbstractModelComponent()
{
    protected val errorService: ErrorService = ErrorService(repositoryPath)

    /**
     * Executes the tests in the repository, reads the errors and attempts to fix the tests to make them pass.
     *
     * Returns the number of errors found and attempted to fix (potentially fixed as well)
     */
    fun fixUsingOutput(response: YateResponse): Int {
        // Step 1: Execute tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)

        // Early exit: If no errors are present then this procedure is pointless
        if (errors === null) {
            response.hasChanges = false

            return 0
        }

        // Step 2: Scan for error-message patterns and fix lines that fall into them
        var nrErrorsFixed: Int = 0
        for (errorLine: String in errors.lines()) {
            val errorInfo: OracleError? = YateJavaExecution.parseFailedTestLine(errorLine, dependencyTool)
            if (errorInfo !== null && errorInfo.className.equals(response.testClassContainer.className)) {
                fixLineUsingOutputLog(errorInfo, response)
                if (response.hasChanges) {
                    nrErrorsFixed++
                }
            }
        }

        return nrErrorsFixed
    }

    /**
     * Creates an exception oracle in a test that already throws an exception. In other words it verifies the behaviour
     * NOTE: Depending on the component's configuration of expectedTypesToIgnore variable, certain exceptions are being
     * ignored and not fixed
     */
    fun fixTestsThatThrowExceptions(response: YateResponse): Int {
        // Step 1: Execute tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)

        // Early exit: If no errors are present then this procedure is pointless
        if (errors === null) {
            response.hasChanges = false

            return 0
        }

        // Create a copy to allow reverting if needed
        val testClassWithoutChanges: ClassContainer = response.testClassContainer.copy()
        val codeLinesWithChanges = response.testClassContainer.getCompleteContent().lines().toMutableList()

        // Iterate exception errors and attempt to fix each line, initially with rules, otherwise using the model
        var nrErrorsFixed = 0
        val exceptionErrors = errorService.findExceptionErrorsFromReport(response.testClassContainer.getQualifiedName())
        for (error: OracleError in exceptionErrors) {

            // Check whether the OracleError's expected type is not in the ones to ignore
            if (error.exceptionType in this.expectedTypesToIgnore) {
                YateConsole.debug("Ignoring exception oracle: ${error.exceptionType}")

                continue
            }

            // Find the line's content (is -1 from the reported error line in the log)
            val lineToChange = error.lineNumber - 1
            val lineContent = codeLinesWithChanges[lineToChange].trim()

            // Attempt to create an exception oracle statically without the use of an LLM first
            val exceptionOracle = YateJavaUtils.createExceptionOracleFromStatement(lineContent, "${error.exceptionType}.class")
            if (exceptionOracle !== null) {
                YateConsole.debug("Adding exception oracle statically in $lineToChange: $exceptionOracle ")
                codeLinesWithChanges[lineToChange] = exceptionOracle
                appendNewContentFromLines(codeLinesWithChanges, response)
                nrErrorsFixed += if (response.hasChanges) 1 else 0

                continue
            }

            // Attempt to fix exception oracle using LLM
            val promptVars = hashMapOf(
                    "LINE_CONTENT" to lineContent,
                    "LINE_NUMBER" to error.lineNumber.toString(),
                    "CLASS_CONTENT" to response.testClassContainer.getCompleteContent(),
                    "EXCEPTION_TYPE" to error.exceptionType,
                    "TEST_METHOD" to error.testMethodName
            )

            val promptFixExceptionOracle: String = PromptService.get("fix_oracle_that_throws_exception", promptVars)
            replaceLineUsingModel(mutableListOf(promptFixExceptionOracle), codeLinesWithChanges, lineToChange, response)
            if (response.hasChanges) {
                nrErrorsFixed++
            }
        }

        // Once all errors are fixed, ask the LLM to remove all lines after the exception oracles
        if (nrErrorsFixed == 0) {
            return 0
        }

        // Clean up content using LLM: If fails, then revert back to the test class container without any changes
        YateConsole.debug("Cleaning up code after exception oracles")
        val newContent = YateJavaUtils.removeCodeAfterExceptionOracle(response.testClassContainer)
        response.recreateTestClassContainer(newContent)

        return nrErrorsFixed
    }

    /**
     * Attempts to fix all oracles, line by line using the model. It doesn't provide the whole class to the LLM, only
     * the oracle that is failing
     */
    open fun fixErrorsUsingModel(response: YateResponse): Int {
        // Step 1: Execute tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)

        // Early exit: If no errors are present then this procedure is pointless
        if (errors === null) {
            response.hasChanges = false

            return 0
        }

        // Create a copy to allow reverting if needed
        val codeLines = response.testClassContainer.getCompleteContent().lines().toMutableList()

        // Scan for error-message patterns and fix lines that fall into them
        var nrErrorsFixed: Int = 0
        val errorLogs: List<TestErrorLog> = errorService.findErrorsFromReport(response.testClassContainer.getQualifiedName())
        for (errorLog: TestErrorLog in errorLogs) {
            val lineToChange = errorLog.lineNumber - 1
            val codeLine = codeLines[lineToChange].trim()

            // Prepare prompt for LLM
            // Attempt to fix exception oracle using LLM
            YateConsole.debug("Attempting to fix oracle in line $lineToChange: $codeLine")
            val promptVars = hashMapOf("ERROR_CODE_LINE" to codeLine, "ERROR_LOG" to errorLog.content)
            val prompt = PromptService.get("static_fix_oracle", promptVars)

            replaceLineUsingModel(mutableListOf(prompt), codeLines, lineToChange, response)
            if (response.hasChanges) {
                nrErrorsFixed++
            }
        }

        return nrErrorsFixed
    }

    /**
     * Attempts to fix the error logs of the WHOLE CLASS, using the model, in a similar way
     * YATE fixes non-compiling errors
     */
    fun fixClassErrorsUsingModel(response: YateResponse, includeClassCodeInPrompt: Boolean = true): YateResponse {
        val errors: String? = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, true)

        if (errors === null) {
            response.hasChanges = false

            return response
        }

        // Use the model and attempt to fix the errors based on the error log provided from execution
        val promptVars = hashMapOf("CLASS_CONTENT" to response.testClassContainer.getCompleteContent(), "ERRORS" to errors)
        val promptInstruction: String = if (includeClassCodeInPrompt) "fix_errors" else "fix_errors_no_class_code"
        val prompts = mutableListOf<String>(PromptService.get(promptInstruction, promptVars))

        val modelResponse: CodeResponse = model.ask(prompts, history = response.conversation)

        // Update the history of the chat conversation only if the model responded with a valid code content
        if (modelResponse.codeContent !== null) {
            response.recreateTestClassContainer(modelResponse.codeContent)
            response.conversation = modelResponse.conversation
        }

        return response
    }

    private fun fixLineUsingOutputLog(
            errorLogItem: OracleError,
            response: YateResponse
    ): YateResponse {
        response.hasChanges = false

        // Line to change, programmatically, is -1 from the lineNumber reported from the error log
        val lineToChange = errorLogItem.lineNumber.minus(1)

        // Create a copy to allow reverting if needed
        val codeLinesWithChanges = response.testClassContainer.getCompleteContent().lines().toMutableList()
        if (errorLogItem.actualValue == null) {

            // Case: No exception thrown — attempt to invert exception oracle
            val newOracle = YateJavaUtils.invertExceptionOracle(codeLinesWithChanges[lineToChange])
            codeLinesWithChanges[lineToChange] = newOracle ?: ""
            YateConsole.info("Changing line ${errorLogItem.lineNumber}: Inverting exception oracle")
        } else {

            // Case: Incorrect actual/expected value
            val expected = errorLogItem.expectedValue ?: return response
            val actual = errorLogItem.actualValue ?: return response

            // Replace original value with actual
            YateConsole.info("Changing line ${errorLogItem.lineNumber}: Value $expected will change to $actual")
            YateCodeUtils.replaceLineInList(codeLinesWithChanges, lineToChange, expected, YateUtils.sanitizeString(actual))

            // Handle fully qualified expected values (e.g., java.lang.Exception)
            val expectedValueParts = expected.split(".")
            if (expectedValueParts.size > 1) {
                YateCodeUtils.replaceLineInList(codeLinesWithChanges, lineToChange, expectedValueParts.last(), actual)
            }
        }

        // Validate new class code. If it is parsable, append the changes to the response instance
        appendNewContentFromLines(codeLinesWithChanges, response)

        return response
    }

    /**
     * Uses a model to replace the line_to_change based on the prompt given
     * It checks whether the model produced a parsable code before applying the change
     * In case of error, it reverts to the state of code_lines provided.
     */
    private fun replaceLineUsingModel(
            prompts: List<String>,
            codeLines: List<String>,
            lineToChange: Int,
            response: YateResponse
    ): YateResponse {

        // Call LLM to replace line
        val modelResponse: CodeResponse = model.ask(prompts)

        if (modelResponse.codeContent === null) {
            YateConsole.error("LLM could not fix the error")
            response.hasChanges = false
        } else {
            val newCodeLines = codeLines.toMutableList()
            newCodeLines[lineToChange] = modelResponse.codeContent!!
            appendNewContentFromLines(newCodeLines, response)

            // NOTE: The new conversation is not stored to the response but its nr of requests is still recorded
        }

        // In case of error return the old codeLines state
        return response
    }

    /**
     * Joins the code lines to a single multiline string and checks whether the new code lines are a parsable class
     * If parsable, then the content is being added to the YateResponse's testClassContainer
     */
    private fun appendNewContentFromLines(newCodeLines: MutableList<String>, response: YateResponse): Boolean {
        val newBodyContent: String = newCodeLines.joinToString("\n")
        if (YateJavaUtils.isClassParsing(newBodyContent)) {
            response.recreateTestClassContainer(newBodyContent)

            return true
        }

        YateConsole.debug("Change is not parsing")
        response.hasChanges = false

        return false
    }

    /**
     * Uses the LLM to clean the code that exists in the test cases... after exception oracles
     * In case the operation is successful, the new content is updated in the response object
     * and the method returns true.
     * Otherwise, no updated are done and the method returns false
     */
    private fun cleanContentAfterExceptionOracles(response: YateResponse): Boolean {
        YateConsole.info("Cleaning up code after exception oracles")
        val promptVars = hashMapOf("CLASS_CONTENT" to response.testClassContainer.getCompleteContent())
        val prompt = PromptService.get("fix_oracles_clean_after_exceptions", promptVars)

        // Use LLM to clean up content
        val modelResponse: CodeResponse = model.ask(mutableListOf(prompt))
        if (modelResponse.codeContent === null) {

            // LLM failed to clean up the content
            return false
        }

        // Verify that the model's response is compiling before updating the response object
        if (YateJavaUtils.isClassParsing(modelResponse.codeContent!!)) {
            response.recreateTestClassContainer(modelResponse.codeContent)
            // NOTE: The new conversation is not stored to the response but its nr of requests is still recorded

            return true
        }

        // Content is not parsing and no updated have been done at this point
        return false
    }

    fun closeConnection() {
        this.model.closeConnection()
    }
}