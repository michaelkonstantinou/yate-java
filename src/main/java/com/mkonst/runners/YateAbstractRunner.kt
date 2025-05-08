package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse

abstract class YateAbstractRunner(val lang: String = "java") {

    fun generate(classPath: String, testLevel: TestLevel = TestLevel.CLASS) {
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

            fixGeneratedTestClass(cutContainer, response)
            response.testClassContainer.toTestFile()
            response.save()

            fixOraclesInTestClass(response)
            response.testClassContainer.toTestFile()
            response.save()
        }
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
}