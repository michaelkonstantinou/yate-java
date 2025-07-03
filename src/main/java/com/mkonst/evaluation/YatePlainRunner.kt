package com.mkonst.evaluation

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YatePlainErrorFixer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.evaluation.ablation.ExcludeSummarisationRunner
import com.mkonst.helpers.YateConsole
import com.mkonst.runners.YateAbstractRunner
import com.mkonst.types.MethodPosition
import com.mkonst.types.ProgramLangType
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse

class YatePlainRunner(repositoryPath: String,
                      outputDirectory: String? = null,
                      modelName: String? = null,
                      private val maxFixIterations: Int = 5): YateAbstractRunner(repositoryPath, ProgramLangType.JAVA, outputDirectory) {
    private val simpleGenerator: YateUnitGenerator = ExcludeSummarisationRunner(modelName)
    private val simpleFixer: YatePlainErrorFixer = YatePlainErrorFixer(repositoryPath, dependencyTool, modelName)

    override fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse {
        // Use the Simple Generation Component to generate a test
        YateConsole.debug("Using a simple generator to generate the test cases for the given class")

        return if (testLevel === TestLevel.CLASS) {
            simpleGenerator.generateForClass(cutContainer)
        } else {
            throw Exception("Method generateTestsForClass does not support the generation of such tests")
        }
    }

    override fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse {
        throw Exception("Method generateTestsForClass does not support the generation of such tests")
    }

    override fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse {
        var hasErrors: Boolean = true
        var i = 0

        while (i < maxFixIterations && hasErrors) {
            YateConsole.debug("Fixing iteration #$i")
            hasErrors = this.simpleFixer.fixErrors(response)

            i += 1
        }

        return response
    }

    override fun fixOraclesInTestClass(response: YateResponse): YateResponse {
        /**
         * The YatePlainRunner does not differentiate between compilation fixing and oracle fixing
         * It handles both cases in the fixGeneratedTestClass method
         */
        return response
    }

    override fun enhanceCoverageForClass(
        cutContainer: ClassContainer,
        testClassContainer: ClassContainer,
        methodPosition: MethodPosition?
    ): YateResponse? {
        return null
    }

    override fun close() {
        simpleGenerator.closeConnection()
        simpleFixer.closeConnection()
    }

    fun getNrRequests(): RequestsCounter {
        return RequestsCounter(
            simpleGenerator.getNrRequests(),
            simpleFixer.getNrRequests())
    }

    fun resetNrRequests() {
        simpleGenerator.resetNrRequests()
        simpleFixer.resetNrRequests()
    }
}