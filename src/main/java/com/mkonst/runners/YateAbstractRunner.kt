package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.services.ErrorService
import com.mkonst.types.TestErrorType
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import java.io.File

abstract class YateAbstractRunner(protected open val repositoryPath: String, val lang: String = "java", private val outputDirectory: String? = null) {
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
        val cutContainer: ClassContainer
        if (lang.lowercase() == "java") {
            cutContainer = JavaClassContainer.createFromFile(classPath)
        } else {
            // todo()
            cutContainer = JavaClassContainer.createFromFile(classPath)
        }

        var hasFailed: Boolean = false

        // Depending on the selected test level, generate a new test class (Saved in YateResponse)
        if (testLevel == TestLevel.CLASS) {
            val response: YateResponse = generateTestsForClass(cutContainer)
            response.testClassContainer.toTestFile()

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

            moveGeneratedFile(response)

            if (!hasFailed) {
                return response
            }
        }

        return null
    }

    fun fix(classPath: String, testClassPath: String) {
        val testContainer: ClassContainer
        val cutContainer: ClassContainer
        if (lang.lowercase() == "java") {
            testContainer = JavaClassContainer.createFromFile(testClassPath)
            cutContainer = JavaClassContainer.createFromFile(classPath)
        } else {
            // todo()
            testContainer = JavaClassContainer.createFromFile(testClassPath)
            cutContainer = JavaClassContainer.createFromFile(classPath)
        }

        val response = YateResponse(testContainer, mutableListOf())
        fixGeneratedTestClass(cutContainer, response)
    }

    fun fixOracles(testClassPath: String) {
        val testContainer: ClassContainer
        if (lang.lowercase() == "java") {
            testContainer = JavaClassContainer.createFromFile(testClassPath)
        } else {
            // todo()
            testContainer = JavaClassContainer.createFromFile(testClassPath)
        }

        val response = YateResponse(testContainer, mutableListOf())
        fixOraclesInTestClass(response)
    }

    /**
     * Attempts to remove failing tests. Depending on the testType, this can either be non-compiling tests
     * or non-passing tests
     */
    fun removeFailingTests(testClassPath: String, testType: TestErrorType) {
        val testContainer: ClassContainer
        if (lang.lowercase() == "java") {
            testContainer = JavaClassContainer.createFromFile(testClassPath)
        } else {
            // todo()
            testContainer = JavaClassContainer.createFromFile(testClassPath)
        }

        val response = YateResponse(testContainer, mutableListOf())

        if (testType === TestErrorType.NON_COMPILING) {
            removeNonCompilingTests(response)
        } else if (testType === TestErrorType.NON_PASSING) {
            removeNonPassingTests(response)
        } else {
            throw Exception("Test type not supported for removing failing tests")
        }
    }

    abstract fun generateTestsForClass(cutContainer: ClassContainer): YateResponse

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

    /**
     * If the runner was instantiated with an outputDirectory, then this method will move the generate test file,
     * to the specified outputDirectory.
     * If the given response object does not contain a valid test class path, the
     * method will do nothing
     */
    private fun moveGeneratedFile(response: YateResponse) {
        val sourcePath: String? = response.testClassContainer.paths.testClass
        if (outputDirectory !== null && sourcePath !== null) {
            val directoriesAfterRepository: String = response.testClassContainer.paths.testClass!!.substringAfter("src/test").substringBefore(response.testClassContainer.className + "." + lang)
            val newDir = outputDirectory + directoriesAfterRepository
            val newPath = YateIO.moveFileToDirectory(response.testClassContainer.paths.testClass!!, newDir)

            if (newPath !== null) {
                YateConsole.info("Generated test file has been moved. New path: $newPath")
            }
        }
    }
}