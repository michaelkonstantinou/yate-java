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

    fun generateTestsForLineCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer) {}

    fun generateTestsForBranchCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer): YateResponse? {
        // Step 1: Find missing coverage in cutContainer
        val missingCoverage: MissingCoverage? = CoverageService.getMissingCoverageForClass(repositoryPath, cutContainer.className)
        if (missingCoverage === null) {
            YateConsole.info("Enhancing branch coverage is skipped as there are no missed branches for ${cutContainer.className}")

            return null
        }

        // Step 2: Find which of them are not fully branch covered
        val log = "// Uncovered Branches\n" + missingCoverage.missedBranches.toString()
        val newTestClassName = testClassContainer.className.replace("Test", "") + "EnhancedCoverageTest"
        val promptIntroduction = PromptService.get("enhance_coverage_intro_cut", hashMapOf("CLASS_CONTENT" to cutContainer.bodyContent))
        val promptGiveLog = PromptService.get("enhance_coverage_give_log", hashMapOf("LOG" to log, "TEST_CLASS_CONTENT" to cutContainer.bodyContent))
        val promptGenerate = PromptService.get("generate_tests_to_enhance_coverage", hashMapOf("CLASS_NAME" to newTestClassName))

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