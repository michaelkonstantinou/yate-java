package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.*
import com.mkonst.providers.ClassContainerProvider
import com.mkonst.services.ErrorService
import com.mkonst.types.ProgramLangType
import com.mkonst.types.TestErrorType
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import java.io.File

abstract class YateAbstractRunner(protected open val repositoryPath: String, val lang: ProgramLangType = ProgramLangType.JAVA, private val outputDirectory: String? = null) {
    protected var dependencyTool: String
    protected var packageName: String
    protected val errorService: ErrorService = ErrorService(repositoryPath)

    init {
        // Identify whether a pom.xml file is present
        // The purpose of this process is to check whether the repository is using maven or gradle
        val pomFile = File(repositoryPath, "pom.xml")
        dependencyTool = if (pomFile.exists() && pomFile.isFile) "maven" else "gradle"
        println("The given repository is using $dependencyTool")

        packageName = YateCodeUtils.getRootPackage(repositoryPath)
        println("The package name of the repository under test is: $packageName")
    }

    fun generate(classPath: String, testLevel: TestLevel = TestLevel.CLASS): YateResponse? {
        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath, lang)
        var hasFailed: Boolean = false

        // Depending on the selected test level, generate a new test class (Saved in YateResponse)
        when (testLevel) {
            TestLevel.CLASS -> {
                val response: YateResponse = generateTestsForClass(cutContainer, TestLevel.CLASS)
                response.testClassContainer.toTestFile()
                hasFailed = onValidation(cutContainer, response)

                if (!hasFailed) {
                    return response
                }
            }
            TestLevel.CONSTRUCTOR -> {
                if (cutContainer.body.hasConstructors) {
                    val response: YateResponse = generateTestsForClass(cutContainer, TestLevel.CONSTRUCTOR)
                    response.testClassContainer.toTestFile()
                    hasFailed = onValidation(cutContainer, response)

                    if (!hasFailed) {
                        return response
                    }
                }
            }
            TestLevel.METHOD -> {
                for(mut: String in cutContainer.body.methods.values) {
                    val response: YateResponse = generateTestsForClass(cutContainer, TestLevel.METHOD)
                    response.testClassContainer.toTestFile()
                    hasFailed = onValidation(cutContainer, response)
                }
            }
            TestLevel.METHOD_RESTRICT -> TODO()
            TestLevel.HYBRID -> TODO()
        }

        return null
    }

    fun fix(classPath: String, testClassPath: String) {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath, lang)
        val cutContainer: ClassContainer = ClassContainerProvider.getFromFile(classPath, lang)
        val response = YateResponse(testContainer, mutableListOf())

        fixGeneratedTestClass(cutContainer, response)
    }

    fun fixOracles(testClassPath: String) {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath, lang)
        val response = YateResponse(testContainer, mutableListOf())

        fixOraclesInTestClass(response)
    }

    /**
     * Attempts to remove failing tests. Depending on the testType, this can either be non-compiling tests
     * or non-passing tests
     */
    fun removeFailingTests(testClassPath: String, testType: TestErrorType) {
        val testContainer: ClassContainer = ClassContainerProvider.getFromFile(testClassPath, lang)
        val response = YateResponse(testContainer, mutableListOf())

        if (testType === TestErrorType.NON_COMPILING) {
            removeNonCompilingTests(response)
        } else if (testType === TestErrorType.NON_PASSING) {
            removeNonPassingTests(response)
        } else {
            throw Exception("Test type not supported for removing failing tests")
        }
    }

    abstract fun generateTestsForClass(cutContainer: ClassContainer, testLevel: TestLevel): YateResponse

    abstract fun generateTestsForMethod(cutContainer: ClassContainer, methodUnderTest: String): YateResponse

    abstract fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse

    abstract fun fixOraclesInTestClass(response: YateResponse): YateResponse

    abstract fun close()

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
            // todo: check whether the language is java or kotlin
            val newContent: String = YateJavaUtils.removeMethodsInClass(response.testClassContainer.paths.testClass ?: "", classRelatedInvalidTests)

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
        val nonPassingTests = errorService.findNonPassingTests(dependencyTool)
        val classRelatedInvalidTests = nonPassingTests[response.testClassContainer.className]

        if (!classRelatedInvalidTests.isNullOrEmpty()) {
            // todo: check whether the language is java or kotlin
            val newContent: String = YateJavaUtils.removeMethodsInClass(response.testClassContainer.paths.testClass ?: "", classRelatedInvalidTests)

            response.recreateTestClassContainer(newContent)
            response.testClassContainer.toTestFile()
        }
    }

    private fun onValidation(cutContainer: ClassContainer, response: YateResponse): Boolean {
        var hasFailed = false

        try {
            fixGeneratedTestClass(cutContainer, response)
            response.testClassContainer.toTestFile()
            response.save()

            fixOraclesInTestClass(response)
            response.testClassContainer.toTestFile()
            response.save()
        } catch (e: Exception) {
            YateConsole.error("An error occurred when generating/fixing tests!")
            YateConsole.error(e.message ?: "")
            hasFailed = true
        }

        if (outputDirectory !== null) {
            YateUtils.moveGeneratedTestClass(response.testClassContainer, outputDirectory)
        }

        return hasFailed
    }
}