package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.helpers.YateConsole
import com.mkonst.services.CoverageService
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse
import com.mkonst.types.coverage.MissingCoverage

class YateCoverageEnhancer(private var repositoryPath: String): AbstractModelComponent() {

    /**
     * Uses the CoverageService to find the missing code coverage of the given class.
     * If the coverage missed even at least 1 line, it will attempt to generate a new test class to with
     * tests to cover the missed lines
     */
    fun generateTestsForLineCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer): YateResponse? {
        // Step 1: Find missing coverage in cutContainer
        val missingCoverage: MissingCoverage? = CoverageService.getMissingCoverageForClass(repositoryPath, cutContainer.className)
        if (missingCoverage === null || missingCoverage.isFullyLineCovered()) {
            YateConsole.info("Enhancing line coverage is skipped as there are no missed lines for ${cutContainer.className}")

            return null
        }

        // Step 2: Find which of them are not fully branch covered
        val log = "// Uncovered Lines: " + missingCoverage.missedLines.toString()
        val newTestClassName = testClassContainer.className.replace("Test", "") + "EnhancedLineCoverageTest"

        return generateTestUsingModel(log, "lines", newTestClassName, cutContainer)
    }

    /**
     * Uses the CoverageService to find the missing code coverage of the given class.
     * If the coverage missed even at least 1 branch, it will attempt to generate a new test class to with
     * tests to cover the missed branches
     */
    fun generateTestsForBranchCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer): YateResponse? {
        // Step 1: Find missing coverage in cutContainer
        val missingCoverage: MissingCoverage? = CoverageService.getMissingCoverageForClass(repositoryPath, cutContainer.className)
        if (missingCoverage === null || missingCoverage.isFullyBranchCovered()) {
            YateConsole.info("Enhancing branch coverage is skipped as there are no missed branches for ${cutContainer.className}")

            return null
        }

        // Step 2: Find which of them are not fully branch covered
        val log = "// Uncovered Branches\n" + missingCoverage.missedBranches.toString()
        val newTestClassName = testClassContainer.className.replace("Test", "") + "EnhancedBranchCoverageTest"

        return generateTestUsingModel(log, "branches", newTestClassName, cutContainer)
    }

    /**
     * Generates the prompts to feed to the model, and uses the model to generate a new test class
     * In case the model fails to return a code, it returns null
     * On success, it returns a new YateResponse instance with a new ClassContainer for the generated test class
     */
    private fun generateTestUsingModel(log: String, coverageType: String, newTestClassName: String, cutContainer: ClassContainer): YateResponse? {
        val promptIntroduction = PromptService.get(
            "enhance_coverage_intro_cut",
            hashMapOf("CLASS_CONTENT" to cutContainer.bodyContent))
        val promptGiveLog = PromptService.get(
            "enhance_coverage_give_log",
            hashMapOf("LOG" to log, "TEST_CLASS_CONTENT" to cutContainer.bodyContent, "COVERAGE_TYPE" to coverageType))
        val promptGenerate = PromptService.get(
            "generate_tests_to_enhance_coverage",
            hashMapOf("CLASS_NAME" to newTestClassName))

        // Step 3: Generate Prompts
        val prompts = mutableListOf(promptIntroduction, promptGiveLog, promptGenerate)
        val modelResponse: CodeResponse = model.ask(prompts, PromptService.get("system"))

        if (modelResponse.codeContent === null) {
            return null
        }

        // Prepare a new ClassContainer for the generated test class
        val testContainer = JavaClassContainer(newTestClassName, modelResponse.codeContent)
        testContainer.body.packageName = cutContainer.body.packageName
        testContainer.appendImports(cutContainer.body.imports)

        // Find that paths of the class under test and the generated test class
        testContainer.setPathsFromCut(cutContainer)

        return YateResponse(testContainer, modelResponse.conversation)
    }
}