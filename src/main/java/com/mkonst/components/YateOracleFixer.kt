package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.ErrorService
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.OracleError
import com.mkonst.types.YateResponse

class YateOracleFixer(private var repositoryPath: String,
                      private var dependencyTool: String)
{
    private var model: ChatOpenAIModel = ChatOpenAIModel();
    private val errorService: ErrorService = ErrorService(repositoryPath)
    private val promptFixOracle: String = PromptService.get("static_fix_oracle")

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
            val lineToChange = error.lineNumber - 1
            val lineContent = codeLinesWithChanges[lineToChange].trim()
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
        if (!cleanContentAfterExceptionOracles(response)) {
            YateConsole.error("Could not cleanup code after execution oracles. Reverting changes")
            response.testClassContainer = testClassWithoutChanges
            response.hasChanges = false

            return 0
        }

        return nrErrorsFixed
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
            YateConsole.info("Changing line ${errorLogItem.lineNumber}: Value $actual to $expected")
            YateCodeUtils.replaceLineInList(codeLinesWithChanges, lineToChange, actual, expected)

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
            YateConsole.debug("Adding exception oracle using model in $lineToChange: ${modelResponse.codeContent} ")
            val newCodeLines = codeLines.toMutableList()
            newCodeLines[lineToChange] = modelResponse.codeContent!!
            appendNewContentFromLines(newCodeLines, response)
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

            return true
        }

        // Content is not parsing and no updated have been done at this point
        return false
    }


}