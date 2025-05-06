package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.helpers.YateConsole
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
    abstract fun generateTestsForClass(cutContainer: ClassContainer): YateResponse

    abstract fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse)

    abstract fun fixOraclesInRepository()

    abstract fun close()
}