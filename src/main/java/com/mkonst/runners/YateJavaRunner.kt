package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.java.JavaImportsAnalyzer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.components.YateUnitTestFixer
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.types.YateResponse
import java.io.File

class YateJavaRunner(
        val repositoryPath: String,
        val includeOracleFixing: Boolean = false
): YateAbstractRunner(lang = "java") {
    private val yateGenerator: YateUnitGenerator = YateUnitGenerator(repositoryPath)
    private var yateTestFixer: YateUnitTestFixer
    private var dependencyTool: String
    private var packageName: String

    init {
        // Identify whether a pom.xml file is present
        // The purpose of this process is to check whether the repository is using maven or gradle
        val pomFile = File(repositoryPath, "pom.xml")
        dependencyTool = if (pomFile.exists() && pomFile.isFile) "maven" else "gradle"
        println("The given repository is using $dependencyTool")

        yateTestFixer = YateUnitTestFixer(repositoryPath, dependencyTool)
        packageName = YateCodeUtils.getRootPackage(repositoryPath)
    }

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

        // Execute tests and fix errors from error log
        for (i in 1..ConfigYate.getInteger("MAX_FIX_ITERATIONS")) {
            yateTestFixer.fixTestsFromErrorLog(response, i == 1)

            if (response.hasChanges) {
                println("Test has changes. Saving results")
                response.testClassContainer.toTestFile()
            }
        }

        response.save()
    }

    override fun close() {
        yateGenerator.closeConnection()
        yateTestFixer.closeConnection()
    }

    /**
     * For each import statement in the testClassContainer of the response, the method leverages the ImportsAnalyzer
     * to check for import statements that do not reflect a valid class in the repository
     *
     * Removes the invalid import statements in the testClassContainer of the given response
     * Returns whether such import statements have been found
     */
    private fun removeInvalidImports(response: YateResponse): Boolean {
        val invalidImports: MutableList<String> = JavaImportsAnalyzer.getInvalidPackageImports(repositoryPath, packageName, response.testClassContainer.body.imports)

        if (invalidImports.size > 0) {
            response.testClassContainer.removeImports(invalidImports)

            return true
        }

        return false
    }
}