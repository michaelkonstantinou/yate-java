package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.java.JavaImportsAnalyzer
import com.mkonst.components.YateOracleFixer
import com.mkonst.components.YateSecondAgentOracleFixer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.components.YateUnitTestFixer
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.services.ErrorService
import com.mkonst.types.ProgramLangType
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import java.io.File

class YateJavaRunner(
    repositoryPath: String,
    private val includeOracleFixing: Boolean = true,
    private val outputDirectory: String? = null
): YateAbstractRunner(repositoryPath = repositoryPath, lang = ProgramLangType.JAVA, outputDirectory = outputDirectory) {
    private val yateGenerator: YateUnitGenerator = YateUnitGenerator()
    private var yateTestFixer: YateUnitTestFixer
    private var yateOracleFixer: YateOracleFixer
    private val importsAnalyzer: JavaImportsAnalyzer

    init {
        yateTestFixer = YateUnitTestFixer(repositoryPath, packageName, dependencyTool)
        yateOracleFixer = YateOracleFixer(repositoryPath, dependencyTool)
        importsAnalyzer = JavaImportsAnalyzer(repositoryPath, packageName)
    }

    /**
     * Generates unit tests regarding a given class. Depending on the test-level, the method will generate tests
     * for the whole class, for the constructors, for the methods or for a combination of the aforementioned
     */
    override fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse {
        YateConsole.debug("Using YateUnitGenerator to generate the test cases for the given class")

        return if (testLevel === TestLevel.CLASS) {
            yateGenerator.generateForClass(cutContainer)
        } else if (testLevel === TestLevel.CONSTRUCTOR){
            yateGenerator.generateForConstructors(cutContainer)
        } else {
            throw Exception("Method generateTestsForClass does not support the generation of such tests")
        }
    }

    override fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse {
        YateConsole.debug("Using YateUnitGenerator to generate the test cases for method: $methodUnderTest")

        return yateGenerator.generateForMethod(cutContainer, methodUnderTest)
    }

    /**
     * The method uses a 3-step solution to fix failing oracles, based on the execution trace
     * Step 1: Attempts to fix oracles using a rule-based system
     * Step 2: Attempts to generate missing exception oracles using a rule-based system or the LLM in case is difficult
     * Step 3: Uses the LLM to fix failing oracles based on the error's context
     *
     * The method is executed only if the flag includeOracleFixing is present.
     */
    override fun fixOraclesInTestClass(response: YateResponse): YateResponse {
        if (!includeOracleFixing) {
            YateConsole.info("Oracle fixing is skipped because this feature is disabled")

            return response
        }

        YateConsole.info("Fixing the oracles of non-passing tests")

        // Make sure that the current test file, is also the one that reflects the test container's content
        response.testClassContainer.toTestFile()

        // Output log: rule-based fixing
        val errorsFixedFromLog = yateOracleFixer.fixUsingOutput(response)
        YateConsole.info("$errorsFixedFromLog fixed using the output log and rules")
        response.testClassContainer.toTestFile()

        // Output log: rule-based & llm exception oracle fixing
        val errorsFixedExceptions = yateOracleFixer.fixTestsThatThrowExceptions(response)
        YateConsole.info("$errorsFixedExceptions fixed using the output log and rules")
        response.testClassContainer.toTestFile()
        removeNonCompilingTests(response)

//        // Second agent based fixing (if enabled)
//        val errorsFixedFromSecondAgent = yateOracleFixer.fixErrorsUsingSecondAgent(response)
//        YateConsole.info("$errorsFixedFromSecondAgent fixed using the output log and the second agent")
//        response.testClassContainer.toTestFile()
//        removeNonCompilingTests(response)

        // LLM-based fixing (if enabled)
        val errorsFixedFromLLM = yateOracleFixer.fixErrorsUsingModel(response)
        YateConsole.info("$errorsFixedFromLLM fixed using the output log and the LLM")
        response.testClassContainer.toTestFile()
        removeNonCompilingTests(response)

        removeNonPassingTests(response)

        return response
    }

    override fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse {
        YateConsole.debug("Looking for suggested import statements and removing possibly wrong ones")
        appendSuggestImports(response)
        removeInvalidImports(response)
        response.testClassContainer.toTestFile()

        fixFromErrorLog(response)
        if (isCompiling()) {
            return response
        }

        // Fix by checking external constructor invocations and wrong method usages
        YateConsole.debug("Analyzing calls to other objects")
        yateTestFixer.fixUsingExternalConstructors(cutContainer.getQualifiedName(), response)
        response.testClassContainer.toTestFile()

        // Analyze code for wrong method/mock usages and fix accordingly
        fixByFindingWrongInvocations(response)
        if (isCompiling()) {
            return response
        }

        // Use the MCG to provide more content to the LLM regarding its usage
        fixUsingExternalMethodContent(cutContainer, response)
        if (isCompiling()) {
            return response
        }

        println("Code is still not compiling. Removing non-compiling tests")
        removeNonCompilingTests(response)

        return response
    }

    override fun close() {
        yateGenerator.closeConnection()
        yateTestFixer.closeConnection()
        yateOracleFixer.closeConnection()
    }

    fun getNrRequests(): Int {
        return yateGenerator.getNrRequests() + yateTestFixer.getNrRequests() + yateOracleFixer.getNrRequests()
    }

    fun resetNrRequests() {
        yateGenerator.resetNrRequests()
        yateTestFixer.resetNrRequests()
        yateOracleFixer.resetNrRequests()
    }

    /**
     * For each import statement in the testClassContainer of the response, the method leverages the ImportsAnalyzer
     * to check for import statements that do not reflect a valid class in the repository
     *
     * Removes the invalid import statements in the testClassContainer of the given response
     * Returns whether such import statements have been found
     */
    private fun removeInvalidImports(response: YateResponse): Boolean {
        val invalidImports: MutableList<String> = importsAnalyzer.getInvalidPackageImports(response.testClassContainer.body.imports)

        if (invalidImports.size > 0) {
            YateConsole.debug("The following imports are invalid and are being removed: ${invalidImports.joinToString()}")
            response.testClassContainer.removeImports(invalidImports)

            return true
        }

        return false
    }

    /**
     * Uses the JavaImportsAnalyzer and searches for import statements that may be missing.
     * If such statements are found, the method will append them to the ClassContainer of the YateResponse object
     */
    private fun appendSuggestImports(response: YateResponse): YateResponse {
        try {
            val suggestedImportStatements = importsAnalyzer.getSuggestedImports(response.testClassContainer.getQualifiedName())
            response.testClassContainer.appendImports(suggestedImportStatements)
        } catch (e: Exception) {
            YateConsole.debug("An error occurred when analyzing the import statements. Perhaps Spoon could not analyze the class")
            YateConsole.error(e.message ?: "")
        }

        return response
    }

    /**
     * Returns whether the repository contains any compilation errors when the test suite is run
     */
    private fun isCompiling(): Boolean {
        return YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool) === null
    }

    private fun fixFromErrorLog(response: YateResponse): YateResponse {
        for (i in 1..ConfigYate.getInteger("MAX_FIX_ITERATIONS")) {
            YateConsole.debug("Running tests and attempt to fix them using the error log")
            yateTestFixer.fixTestsFromErrorLog(response, i == 1)

            if (response.hasChanges) {
                println("Test has changes. Saving results")
                removeInvalidImports(response)
                response.testClassContainer.toTestFile()
            } else {
                return response
            }
        }

        return response
    }

    /**
     * Uses YateTestFixer (LLM-based) to analyze and fix any method invocations
     * that are being used in tests incorrectly.
     *
     * Returns the new ClassContainer, the LLM conversation and a boolean indicating
     * whether more compilation errors exist
     */
    private fun fixByFindingWrongInvocations(response: YateResponse): YateResponse {
        for (i in 1..ConfigYate.getInteger("MAX_FIX_WRONG_INVOCATIONS")) {
            yateTestFixer.fixWrongMethodInvocations(response)

            if (response.hasChanges) {
                response.testClassContainer.toTestFile()
                println("Done! Running for compilation errors")

                fixFromErrorLog(response)
            } else {
                println("No wrong method invocations detected")
            }
        }

        return response
    }

    /**
     * Uses YateTestFixer (LLM-based) to analyze and fix wrong tests, by providing more
     * content from relevant classes-methods.
     *
     * Returns the new ClassContainer, the LLM conversation and a boolean indicating
     * whether more compilation errors exist
     */
    private fun fixUsingExternalMethodContent(cutContainer: ClassContainer, response: YateResponse): YateResponse {
        for (i in 1..ConfigYate.getInteger("MAX_FIX_USING_MORE_CONTENT")) {
            yateTestFixer.fixUsingExternalMethods(cutContainer, response)

            if (response.hasChanges) {
                response.testClassContainer.toTestFile()
                println("Done! Running for compilation errors")

                fixFromErrorLog(response)
            } else {
                println("No external method calls found")
            }
        }

        return response
    }
}