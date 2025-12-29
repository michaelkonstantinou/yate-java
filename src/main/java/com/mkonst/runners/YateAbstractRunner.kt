package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.config.ConfigYate.getInteger
import com.mkonst.evaluation.RequestsCounter
import com.mkonst.helpers.*
import com.mkonst.providers.ClassContainerProvider
import com.mkonst.services.CoverageService
import com.mkonst.services.ErrorService
import com.mkonst.types.*
import com.mkonst.types.coverage.MethodCoverage
import java.io.File

abstract class YateAbstractRunner(
    protected open val repositoryPath: String,
    val lang: ProgramLangType = ProgramLangType.JAVA,
    private val outputDirectory: String? = null,
    private val requireGreenSuiteOnGeneration: Boolean = true) {
    protected var dependencyTool: DependencyTool
    protected var packageName: String
    protected val errorService: ErrorService = ErrorService(repositoryPath)

    init {
        // Identify whether a pom.xml file is present
        // The purpose of this process is to check whether the repository is using maven or gradle
        dependencyTool = YateUtils.getDependencyTool(repositoryPath)
        println("The given repository is using ${dependencyTool.name}")

        packageName = YateCodeUtils.getRootPackage(repositoryPath)
        println("The package name of the repository under test is: $packageName")
    }

    /**
     * Given the absolute path of the class under test, the method will generate a number of test cases
     * depending on the TestLevel provided
     *
     * CLASS -> Generates 1 test class for the whole file
     * CONSTRUCTORS -> Generates 1 test class for all the constructors ONLY
     * METHOD -> Generates 1 test class PER METHOD, and 1 test class for the constructors
     * METHOD_RESTRICT -> Generates 1 test class per method but not for the constructors
     * HYBRID -> Starts with Class-level testing, and the proceeds to selective method testing based on coverage
     */
    fun generate(classPath: String, testLevel: TestLevel = TestLevel.CLASS): MutableList<YateResponse> {
        val results: MutableList<YateResponse> = mutableListOf()

        // Verify that all tests pass (if enabled)
        val isGreenSuite: Boolean = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true) === null
        if (!isGreenSuite && requireGreenSuiteOnGeneration) {
            YateConsole.error("Current setup of Runner required a green suite (all tests to pass) before starting the generation process")
            return results
        }

        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath)
        var hasFailed: Boolean = false

        // Depending on the selected test level, generate a new test class (Saved in YateResponse)
        when (testLevel) {
            TestLevel.CLASS -> {
                val response: YateResponse = generateTestsForClass(cutContainer, TestLevel.CLASS)
                response.testClassContainer.paths.cut = classPath
                response.testClassContainer.toTestFile()
                hasFailed = onValidation(cutContainer, response)

                if (!hasFailed) {
                    results.add(response)
                    val enhancedResponse: YateResponse? = this.enhanceCoverage(classPath, response.testClassContainer.paths.testClass!!)

                    // Add enhanced response to results and remove generated test
                    if (enhancedResponse !== null) {
                        results.add(enhancedResponse)
                    }
                }

                // Remove original generated test
                YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)
            }
            TestLevel.CONSTRUCTOR -> {
                if (cutContainer.body.hasConstructors) {
                    val response: YateResponse = generateTestsForClass(cutContainer, TestLevel.CONSTRUCTOR)
                    response.testClassContainer.toTestFile()
                    hasFailed = onValidation(cutContainer, response)

                    if (!hasFailed) {
                        results.add(response)
                    }

                    // Remove original generated test
                    YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)
                }
            }
            TestLevel.METHOD -> {
                // Initially generate tests for constructors
                results.addAll(this.generate(classPath, TestLevel.CONSTRUCTOR))

                // Iterate all available methods and generate tests for each one of them individually
                results.addAll(this.generate(classPath, TestLevel.METHOD_RESTRICT))
            }
            TestLevel.METHOD_RESTRICT -> {
                // Hold a list with all method names and their positions
                val methodPositions: MutableList<MethodPosition> = YateJavaUtils.extractMethodPositions(cutContainer.bodyContent!!)

                // Iterate all available methods and generate tests for each one of them individually
                for(mutPosition: MethodPosition in methodPositions) {

                    var i = 0
                    var hasFailed = true
                    while (hasFailed && i < ConfigYate.getInteger("MAX_REPEAT_FAILED_ITERATIONS")) {
                        i++

                        val response: YateResponse = generateTestsForMethod(cutContainer, mutPosition.name)
                        response.testClassContainer.toTestFile()
                        hasFailed = onValidation(cutContainer, response)

                        if (!hasFailed) {
                            results.add(response)
                            enhanceCoverage(classPath, response.testClassContainer.paths.testClass!!, mutPosition)
                        }

                        // Remove original generated test
                        YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)
                    }
                }
            }
            TestLevel.HYBRID -> {
                // Get uncovered methods and generate a test for each uncovered method
                YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, true)

                val uncoveredMethods: MutableList<MethodCoverage>
                try {
                    uncoveredMethods = CoverageService.getNotFullyCoveredMethodsForClass(repositoryPath, cutContainer.getQualifiedName()).toMutableList()
                } catch (e: Exception) {
                    YateConsole.error("Could not find coverage")
                    throw e
                }

                var hasUncoveredConstructors = false

                // Iterate all uncovered methods
                for (method: MethodCoverage in uncoveredMethods) {

                    // Ignore constructors, but make sure this is flagged to generate tests for constructors next
                    if (method.name.contains("(Constructor)")) {
                        hasUncoveredConstructors = true
                        continue
                    }

                    // Generate tests for given method
                    if (method.name.contains("static {...}")) {
                        val newMethodName = method.name.replace("static {...}", "static")
                        println("Using method name $newMethodName instead of ${method.name}")
                        results.addAll(this.generate(classPath, newMethodName))
                    } else if (method.name.contains("lambda$")) {
                        val newMethodName = method.name.replace("$", "_")
                        println("Using method name $newMethodName instead of ${method.name}")
                        results.addAll(this.generate(classPath, newMethodName))
                    } else {
                        results.addAll(this.generate(classPath, method.name))
                    }

                }

                // Generate tests for constructors if some of the uncovered methods is a constructor
                if (hasUncoveredConstructors) {
                    this.generate(classPath, TestLevel.CONSTRUCTOR)
                }
            }
        }

        return results
    }

    /**
     * Generates 1 test class for the given method under test and possibly 1 more if the enhancement component is used
     * Returns a list of YateResponse instances that include the generated Test classes, only if the operation
     * was successful
     */
    fun generate(classPath: String, methodName: String): MutableList<YateResponse> {
        val results: MutableList<YateResponse> = mutableListOf()
        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath)
        var hasFailed: Boolean = false
        val response: YateResponse = generateTestsForMethod(cutContainer, methodName)
        response.testClassContainer.toTestFile()
        hasFailed = onValidation(cutContainer, response)

        if (!hasFailed) {
            results.add(response)

            // TODO: Narrow down to only method-level coverage augmentation
            val enhancedResponse: YateResponse? = this.enhanceCoverage(classPath, response.testClassContainer.paths.testClass!!)

            // Add enhanced response to results and remove generated test
            if (enhancedResponse !== null) {
                results.add(enhancedResponse)
            }
        }

        // Remove original generated test
        YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)

        return results
    }

    /**
     * Invokes the fix process implemented in the Runner class given the original class under test path, and the
     * generated test class' path
     */
    fun fix(classPath: String, testClassPath: String) {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath)
        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath)
        val response = YateResponse(testContainer, mutableListOf())

        fixGeneratedTestClass(cutContainer, response)
    }

    /**
     * Invokes the process of fixing the non-passing oracles in the given test class
     */
    fun fixOracles(testClassPath: String) {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath)
        val response = YateResponse(testContainer, mutableListOf())

        fixOraclesInTestClass(response)
    }


    /**
     * Uses the implemented functionality to generate new tests that will enhance the coverage of the current
     * test suite. If mutPosition is given, then the coverage should be enhanced for the specific method under test
     */
    fun enhanceCoverage(classPath: String, testClassPath: String, mutPosition: MethodPosition? = null): YateResponse? {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath)
        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath)

        var hasFailed = true
        var i = 0
        while (i < ConfigYate.getInteger("MAX_REPEAT_FAILED_ITERATIONS")) {
            i++
            val response = enhanceCoverageForClass(cutContainer, testContainer, mutPosition)
            if (response !== null) {
                hasFailed = onValidation(cutContainer, response)
                YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)

                if (!hasFailed) {
                    return response
                }
            }
        }

        return null
    }

    abstract fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse

    abstract fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse

    abstract fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse

    abstract fun fixOraclesInTestClass(response: YateResponse): YateResponse

    abstract fun enhanceCoverageForClass(cutContainer: ClassContainer, testClassContainer: ClassContainer, methodPosition: MethodPosition? = null): YateResponse?

    abstract fun close()

    abstract fun getNrRequests(): RequestsCounter

    abstract fun resetNrRequests()

    /**
     * Executes the tests and finds the ones that did not compile
     * Based on the YateResponse's class, it will remove the tests that are relevant to the generated test class
     *
     * It DOES NOT remove all non-compiling tests, only the class-related ones (if any)
     */
    protected fun removeNonCompilingTests(response: YateResponse) {
        val nonPassingTests = errorService.findNonCompilingTests(dependencyTool)
        val classRelatedInvalidTests = nonPassingTests[response.testClassContainer.paths.testClass]

        if (!classRelatedInvalidTests.isNullOrEmpty()) {
            YateConsole.debug("$classRelatedInvalidTests tests must be removed as they do not compile")
            val newContent: String = YateCodeUtils.removeMethodsInClass(response.testClassContainer.paths.testClass ?: "", classRelatedInvalidTests, lang)

            response.recreateTestClassContainer(newContent)
            response.testClassContainer.toTestFile()
        }
    }

    /**
     * Executes the tests and finds the ones that did not compile
     * Based on the YateResponse's class, it will remove the tests that are relevant to the generated test class
     *
     * It DOES NOT remove all non-compiling tests, only the class-related ones (if any)
     */
    protected fun removeNonPassingTests(response: YateResponse) {
        if (!ConfigYate.getBoolean("REMOVE_NON_PASSING_TESTS")) {
            return
        }

        val nonPassingTests = errorService.findNonPassingTests(dependencyTool)
        val classRelatedInvalidTests = nonPassingTests[response.testClassContainer.className]

        if (!classRelatedInvalidTests.isNullOrEmpty()) {
            YateConsole.debug("${classRelatedInvalidTests.size} tests are being removed as they do not pass")
            val newContent: String = YateCodeUtils.removeMethodsInClass(response.testClassContainer.paths.testClass ?: "", classRelatedInvalidTests, lang)

            response.recreateTestClassContainer(newContent)
            response.testClassContainer.toTestFile()
        }
    }

    private fun onValidation(cutContainer: ClassContainer, response: YateResponse): Boolean {
        var hasFailed = false

        try {
            fixGeneratedTestClass(cutContainer, response)
            response.testClassContainer.toTestFile()

            fixOraclesInTestClass(response)
            response.testClassContainer.toTestFile()

            // It is still consider a failure if no tests are present in a test class
            hasFailed = YateCodeUtils.countTestMethods(response.testClassContainer) <= 0
        } catch (e: Exception) {
            YateConsole.error("An error occurred when generating/fixing tests!")
            YateConsole.error(e.message ?: "")
            hasFailed = true
        } finally {
            response.save()
        }

        return hasFailed
    }
}