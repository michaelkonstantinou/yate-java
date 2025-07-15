package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.evaluation.RequestsCounter
import com.mkonst.evaluation.ablation.SimpleUnitTestGenerator
import com.mkonst.helpers.YateConsole
import com.mkonst.types.MethodPosition
import com.mkonst.types.ProgramLangType
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse

class YateOnlyGenerationRunner(repositoryPath: String,
                               outputDirectory: String? = null,
                               modelName: String? = null,
                               programLangType: ProgramLangType = ProgramLangType.JAVA): YateAbstractRunner(repositoryPath, programLangType, outputDirectory) {
    private val simpleTestGenerator: YateUnitGenerator = SimpleUnitTestGenerator(modelName)

    override fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse {
        // Use the Simple Generation Component to generate a test
        YateConsole.debug("Using a simple generator to generate the test cases for the given class")

        return if (testLevel === TestLevel.CLASS) {
            simpleTestGenerator.generateForClass(cutContainer)
        } else if (testLevel === TestLevel.CONSTRUCTOR) {
            simpleTestGenerator.generateForConstructors(cutContainer)
        } else {
            throw Exception("Method generateTestsForClass does not support the generation of such tests")
        }
    }

    override fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse {
        YateConsole.debug("Using YateUnitGenerator to generate the test cases for method: $methodUnderTest")

        return simpleTestGenerator.generateForMethod(cutContainer, methodUnderTest)
    }

    override fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse {
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
        simpleTestGenerator.closeConnection()
    }

    override fun getNrRequests(): RequestsCounter {
        return RequestsCounter(simpleTestGenerator.getNrRequests())
    }

    override fun resetNrRequests() {
        simpleTestGenerator.resetNrRequests()
    }
}