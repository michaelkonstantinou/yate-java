package com.mkonst.evaluation

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateCoverageEnhancer
import com.mkonst.components.YatePlainErrorFixer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.evaluation.ablation.SimpleUnitTestGenerator
import com.mkonst.helpers.YateConsole
import com.mkonst.runners.YateAbstractRunner
import com.mkonst.types.*

class CriterionGuidedRunner(repositoryPath: String,
                            outputDirectory: String? = null,
                            modelName: String? = null,
                            private val maxFixIterations: Int = 5,
                            lang: ProgramLangType = ProgramLangType.JAVA,
                            private val criterionType: CoverageCriterionType = CoverageCriterionType.LINE):
    YateAbstractRunner(repositoryPath, lang, outputDirectory) {
    private val simpleGenerator: YateUnitGenerator = SimpleUnitTestGenerator(modelName, lang)
    private val simpleFixer: YatePlainErrorFixer = YatePlainErrorFixer(repositoryPath, dependencyTool, modelName)
    private val coverageEnhancer: YateCoverageEnhancer = YateCoverageEnhancer(repositoryPath, modelName)

    override fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse {
        // Use the Simple Generation Component to generate a test
        YateConsole.debug("Using a simple generator to generate the test cases for the given class")

        return if (testLevel === TestLevel.CLASS) {
            simpleGenerator.generateForClass(cutContainer)
        } else if (testLevel === TestLevel.CONSTRUCTOR) {
            simpleGenerator.generateForConstructors(cutContainer)
        } else {
            throw Exception("Method generateTestsForClass does not support the generation of such tests")
        }
    }

    override fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse {
        YateConsole.debug("Using YateUnitGenerator to generate the test cases for method: $methodUnderTest")

        return simpleGenerator.generateForMethod(cutContainer, methodUnderTest)
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
        YateConsole.debug("Enhancing coverage for class ${cutContainer.className}, based on test ${testClassContainer.className}")

        try {
            // Execute tests before attempting to enhance coverage
            if (!isCompiling()) {
                throw Exception("Tests do not compile")
            }
        } catch (e: Exception) {
            YateConsole.error("Tests must compile before enhancing coverage")
            return null
        }

        return when (this.criterionType) {
            CoverageCriterionType.LINE -> coverageEnhancer.generateTestsForLineCoverage(cutContainer, testClassContainer)
            CoverageCriterionType.BRANCH -> coverageEnhancer.generateTestsForBranchCoverage(cutContainer, testClassContainer)
            else -> throw Exception("Coverage criterion ${this.criterionType} is not supported")
        }
    }

    override fun close() {
        simpleGenerator.closeConnection()
        simpleFixer.closeConnection()
        coverageEnhancer.closeConnection()
    }

    override fun getNrRequests(): RequestsCounter {
        return RequestsCounter(
            simpleGenerator.getNrRequests(),
            simpleFixer.getNrRequests(),
            0,
            coverageEnhancer.getNrRequests())
    }

    override fun resetNrRequests() {
        simpleGenerator.resetNrRequests()
        simpleFixer.resetNrRequests()
        coverageEnhancer.resetNrRequests()
    }
}