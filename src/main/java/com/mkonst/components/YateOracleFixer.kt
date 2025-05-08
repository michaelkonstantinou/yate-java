package com.mkonst.components

import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.services.ErrorService
import com.mkonst.services.PromptService
import com.mkonst.types.OracleError
import com.mkonst.types.YateResponse

class YateOracleFixer(private var repositoryPath: String,
                      private var dependencyTool: String)
{
    private val errorService: ErrorService = ErrorService(repositoryPath)
    private val promptFixOracle: String = PromptService.get("static_fix_oracle")

    /**
     * Executes the tests in the repository, reads the errors and attempts to fix the tests to make them pass.
     *
     * Returns the number of errors found and attempted to fix (potentially fixed as well)
     */
    fun fixUsingOutput(response: YateResponse): Pair<YateResponse, Int> {
        // Step 1: Execute tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)

        // Early exit: If no errors are present then this procedure is pointless
        if (errors === null) {
            response.hasChanges = false

            return Pair(response, 0)
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

        return Pair(response, nrErrorsFixed)
    }

    fun fixTestsThatThrowExceptions(response: YateResponse): Pair<YateResponse, Int> {
        // Step 1: Execute tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)

        // Early exit: If no errors are present then this procedure is pointless
        if (errors === null) {
            response.hasChanges = false

            return Pair(response, 0)
        }

        val exceptionErrors = errorService.findExceptionErrorsFromReport(response.testClassContainer.getQualifiedName())
        for (error: OracleError in exceptionErrors) {
            val lineToChange = error.lineNumber - 1

            // Create a copy to allow reverting if needed
            val codeLinesWithChanges = response.testClassContainer.bodyContent!!.lines().toMutableList()

            // todo: Finish implementation
        }
        var nrErrorsFixed = 0

        return Pair(response, nrErrorsFixed)
    }

    private fun fixLineUsingOutputLog(
            errorLogItem: OracleError,
            response: YateResponse
    ): YateResponse {
        response.hasChanges = false

        // Line to change, programmatically, is -1 from the lineNumber reported from the error log
        val lineToChange = errorLogItem.lineNumber.minus(1)

        // Create a copy to allow reverting if needed
        val codeLinesWithChanges = response.testClassContainer.bodyContent!!.lines().toMutableList()
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
        val newBodyContent: String = codeLinesWithChanges.joinToString("\n")
        if (YateJavaUtils.isClassParsing(newBodyContent)) {
            response.recreateTestClassContainer(newBodyContent)
        }

        return response
    }

}