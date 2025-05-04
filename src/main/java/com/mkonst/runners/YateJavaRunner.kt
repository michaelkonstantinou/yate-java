package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.analysis.java.JavaImportsAnalyzer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.components.YateUnitTestFixer
import com.mkonst.helpers.YateConsole
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import kotlinx.coroutines.Job

class YateJavaRunner(
        val repositoryPath: String,
        val includeOracleFixing: Boolean = false,
        val packageName: String = ""
) {
    val yateGenerator: YateUnitGenerator = YateUnitGenerator(repositoryPath)
    val yateTestFixer: YateUnitTestFixer = YateUnitTestFixer()

    /**
     * Generates unit tests regarding a given class. Depending on the test-level, the method will generate tests
     * for the whole class, for the constructors, for the methods or for a combination of the aforementioned
     */
    fun generateTestsForClass(classPath: String, testLevel: TestLevel = TestLevel.CLASS) {

        // Create a ClassContainer for the original class under test
        val cutContainer: ClassContainer = JavaClassContainer.createFromFile(classPath)
        cutContainer.paths.cut = classPath

        // Depending on the selected test level, generate a new test class (Saved in YateResponse)
        if (testLevel.equals(TestLevel.CLASS)) {
            YateConsole.debug("Using YateUnitGenerator to generate the test cases for the given class")
            val response: YateResponse = yateGenerator.generateForClass(cutContainer)

            response.testClassContainer.toTestFile()

            fixTestClassFromResponse(cutContainer, response)
        }


//        println(cutContainer.className)
//        println(cutContainer.paths)
//        yateGenerator.generateForClass()

    }

    fun fixTestClassFromResponse(cutContainer: ClassContainer, response: YateResponse) {
        YateConsole.debug("Looking for suggested import statements and removing possibly wrong ones")
        yateGenerator.appendSuggestImports(response)
        removeInvalidImports(response)
        response.testClassContainer.toTestFile()
    }

    fun close() {
        yateGenerator.closeConnection()
        yateTestFixer.closeConnection()
    }

    private fun removeInvalidImports(response: YateResponse) {
        val invalidImports: MutableList<String> = JavaImportsAnalyzer.getInvalidPackageImports(repositoryPath, packageName, response.testClassContainer.body.imports)

        if (invalidImports.size > 0) {
            response.testClassContainer.removeImports(invalidImports)
        }
    }
}