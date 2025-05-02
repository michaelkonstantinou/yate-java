package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import kotlinx.coroutines.Job

class YateJavaRunner(
        val repositoryPath: String,
        val includeOracleFixing: Boolean = false,
        val skipIfExists: Boolean = false,
) {
    val yateGenerator: YateUnitGenerator = YateUnitGenerator(repositoryPath)

    fun generateTestsForClass(classPath: String, testLevel: TestLevel = TestLevel.CLASS) {
        // Create a ClassContainer for the original class under test
        val cutContainer: ClassContainer = JavaClassContainer.createFromFile(classPath)
        cutContainer.paths.cut = classPath

        // Depending on the selected test level, generate a new test class (Saved in YateResponse)
        if (testLevel.equals(TestLevel.CLASS)) {
            val response: YateResponse = yateGenerator.generateForClass(cutContainer)

            response.testClassContainer.toTestFile()
        }


//        println(cutContainer.className)
//        println(cutContainer.paths)
//        yateGenerator.generateForClass()

    }

    fun close() {
        yateGenerator.closeConnection()
    }
}