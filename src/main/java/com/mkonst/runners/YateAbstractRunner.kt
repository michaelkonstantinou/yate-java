package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateIO
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse

abstract class YateAbstractRunner(val lang: String = "java", private val outputDirectory: String? = null) {

    fun generate(classPath: String, testLevel: TestLevel = TestLevel.CLASS): YateResponse? {
        val cutContainer: ClassContainer
        if (lang.lowercase() == "java") {
            cutContainer = JavaClassContainer.createFromFile(classPath)
        } else {
            // todo()
            cutContainer = JavaClassContainer.createFromFile(classPath)
        }

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
            } catch (_: Exception) {}

            moveGeneratedFile(response)

            return response
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

    abstract fun generateTestsForClass(cutContainer: ClassContainer): YateResponse

    abstract fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse): YateResponse

    abstract fun fixOraclesInTestClass(response: YateResponse): YateResponse

    abstract fun close()

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

    private fun onFailure(response: YateResponse) {
        // todo: move generated file and return sth maybe
    }
}