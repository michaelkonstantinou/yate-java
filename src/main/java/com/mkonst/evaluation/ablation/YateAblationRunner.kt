package com.mkonst.evaluation.ablation

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.helpers.YateConsole
import com.mkonst.runners.YateJavaRunner
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse

class YateAblationRunner(repositoryPath: String,
                         includeOracleFixing: Boolean = true,
                         outputDirectory: String? = null,
                         modelName: String? = null,
                         private val hasGenerationComponent: Boolean = true,
                         private val hasCompilationFixingComponent: Boolean = true,
                         private val hasOracleFixComponent: Boolean = true,
                         private val hasCoverageEnhancementComponent: Boolean = true) : YateJavaRunner(repositoryPath, includeOracleFixing, outputDirectory, modelName) {
    private val simpleGenerator: YateUnitGenerator = ExcludeSummarisationRunner()

    init {
        YateConsole.debug("Active components:")
        YateConsole.debug("--> Generation: $hasGenerationComponent")
        YateConsole.debug("--> Compilation fixing: $hasCompilationFixingComponent")
        YateConsole.debug("--> Oracle fixing: $hasOracleFixComponent")
        YateConsole.debug("--> Coverage Enhancement: $hasCoverageEnhancementComponent")
    }

    override fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse {
        if (hasGenerationComponent) {
            return super.generateTestsForClass(cutContainer, testLevel)
        }

        // Use the Simple Generation Component to generate a test
        YateConsole.debug("Using a simple generator to generate the test cases for the given class")

        return if (testLevel === TestLevel.CLASS) {
            simpleGenerator.generateForClass(cutContainer)
        } else if (testLevel === TestLevel.CONSTRUCTOR){
            simpleGenerator.generateForConstructors(cutContainer)
        } else {
            throw Exception("Method generateTestsForClass does not support the generation of such tests")
        }
    }

    /**
     * Ablation Study: Depending on the hasOracleFixComponent the method will either execute the default YATE oracle fixing process,
     * or make 5 requests to the LLM to fix the oracles
     */
    override fun fixOraclesInTestClass(response: YateResponse): YateResponse {
        if (hasOracleFixComponent) {
            return super.fixOraclesInTestClass(response)
        }

        // Make 5 iterations to fix oracles using the LLM
        var hadErrors: Boolean = true
        for (i: Int in 1..5) {
            YateConsole.debug("Fixing non-passing oracles using LLM #$i")

            // LLM-based fixing for the whole class
            hadErrors = yateOracleFixer.fixClassErrorsUsingModel(response, true)
            response.testClassContainer.toTestFile()

            if (!hadErrors) {
                break
            }
        }

        removeNonCompilingTests(response)
        removeNonPassingTests(response)

        return response
    }

    /**
     * Ablation Study: Depending on the hasCoverageEnhancementComponent the method will either execute the
     * default YATE coverage enhancement process or return null
     */
    override fun enhanceCoverageForClass(cutContainer: ClassContainer, testClassContainer: ClassContainer): YateResponse? {
        if (hasCoverageEnhancementComponent) {
            return super.enhanceCoverageForClass(cutContainer, testClassContainer)
        }

        return null
    }

    /**
     * Ablation Study: Depending on the hasCompilationFixingComponent the method will either execute the
     * default YATE compilation fixing process or make a maximum of 5 requests to the LLM to fix compilation errors
     */
    override fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse {
        if (hasCompilationFixingComponent) {
            return super.fixGeneratedTestClass(cutContainer, response)
        }

        // Make 5 requests to the LLM to fix the generate test class
        for (i in 1..5) {
            YateConsole.debug("Compilation fixing iteration #$i")
            yateTestFixer.fixTestsFromErrorLog(response, i == 1)

            if (response.hasChanges) {
                println("Test has changes. Saving results")
                response.testClassContainer.toTestFile()
            } else {
                return response
            }
        }

        return response
    }
}