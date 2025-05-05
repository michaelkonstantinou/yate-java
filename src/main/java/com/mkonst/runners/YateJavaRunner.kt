package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.java.JavaImportsAnalyzer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.components.YateUnitTestFixer
import com.mkonst.helpers.YateConsole
import com.mkonst.types.YateResponse

class YateJavaRunner(
        val repositoryPath: String,
        val includeOracleFixing: Boolean = false,
        val packageName: String = ""
): YateAbstractRunner(lang = "java") {
    val yateGenerator: YateUnitGenerator = YateUnitGenerator(repositoryPath)
    val yateTestFixer: YateUnitTestFixer = YateUnitTestFixer()

    /**
     * Generates unit tests regarding a given class. Depending on the test-level, the method will generate tests
     * for the whole class, for the constructors, for the methods or for a combination of the aforementioned
     */
    override fun generateTestsForClass(cutContainer: ClassContainer): YateResponse {
        YateConsole.debug("Using YateUnitGenerator to generate the test cases for the given class")

        return yateGenerator.generateForClass(cutContainer)
    }

    override fun fixOraclesInRepository() {
        TODO("Not yet implemented")
    }

    override fun fixGeneratedTestClass(cutContainer: ClassContainer, response: YateResponse) {
        YateConsole.debug("Looking for suggested import statements and removing possibly wrong ones")
        yateGenerator.appendSuggestImports(response)
        removeInvalidImports(response)
        response.testClassContainer.toTestFile()
    }

    override fun close() {
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