package com.mkonst.services

import com.mkonst.helpers.YateJavaExecution
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.types.DependencyTool
import com.mkonst.types.OracleError
import com.mkonst.types.TestErrorLog
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.io.path.Path

class ErrorService(private val repositoryPath: String) {

    /**
     * Runs the tests in the repository and returns a map with the non-passing tests for each test class
     */
    fun findNonPassingTests(dependencyTool: DependencyTool): Map<String, MutableSet<String>> {
        val testsByTestClass = mutableMapOf<String, MutableSet<String>>()

        // Step 1: Run tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)
        if (errors == null) {
            return emptyMap()
        }

        val reNonPassingErrorLine = Regex("""^\[ERROR\]\s+([\w\d_]+)\.([\w\d_]+):(\d+)""")

        // Step 2: Iterate errors and collect faulty lines per file
        for (errorLine in errors.lineSequence()) {
            val matchResult = reNonPassingErrorLine.find(errorLine)

            if (matchResult != null) {
                val (testClass, testMethod) = matchResult.destructured
                val methods = testsByTestClass.getOrPut(testClass) { mutableSetOf() }
                methods.add(testMethod)
            }
        }

        return testsByTestClass
    }

    /**
     * Runs the tests in the repository and returns a map with the non-passing tests for each test class
     */
    fun findNonCompilingTests(dependencyTool: DependencyTool): Map<String, MutableSet<String>> {
        // Step 1: Run tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = false)
        if (errors == null) {
            return emptyMap()
        }

        return getNonCompilingNodesFromLog(errors).first
    }

    /**
     * Runs the tests in the repository and returns a map with the non-passing tests for each test class
     */
    fun findNonCompilingClasses(dependencyTool: DependencyTool): Map<String, MutableSet<String>> {
        // Step 1: Run tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = false)
        if (errors == null) {
            return emptyMap()
        }

        return getNonCompilingNodesFromLog(errors).second
    }

    /**
     * Scans a surefire report and returns a list of OracleError instances that occurred due to an exception thrown
     */
    fun findExceptionErrorsFromReport(qualifiedClassName: String? = null): List<OracleError> {
        val reportXmlPath = Path(repositoryPath, "target/surefire-reports/TEST-${qualifiedClassName}.xml").toString()
        val exceptionErrors = mutableListOf<OracleError>()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(File(reportXmlPath))
        val testCases = doc.getElementsByTagName("testcase")

        for (i in 0 until testCases.length) {
            val elTestCase = testCases.item(i) as Element
            val testMethod = elTestCase.getAttribute("name")
            val testClass = elTestCase.getAttribute("classname")

            val errorNodes = elTestCase.getElementsByTagName("error")
            if (qualifiedClassName == null || testClass == qualifiedClassName) {
                if (errorNodes.length > 0) {
                    val errorElement = errorNodes.item(0) as Element
                    val errorText = errorElement.textContent ?: continue
                    val lineNumber = getErrorLineNumber(errorText)
                    if (lineNumber != null) {
                        val exceptionType = errorElement.getAttribute("type")
                        exceptionErrors.add(
                                OracleError(
                                        exceptionType = exceptionType,
                                        testMethodName = testMethod,
                                        lineNumber = lineNumber
                                )
                        )
                    }
                }
            }
        }

        return exceptionErrors
    }

    /**
     * Iterates a surefire report and returns a list of TestErrorLog instances, each of them holding the information
     * of an error thrown due to a failing test
     */
    fun findErrorsFromReport(qualifiedClassName: String? = null): List<TestErrorLog> {
        val reportXmlPath = Path(repositoryPath, "target/surefire-reports/TEST-${qualifiedClassName}.xml").toString()
        val errorMessages = mutableListOf<TestErrorLog>()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(File(reportXmlPath))
        val testCases = doc.getElementsByTagName("testcase")

        for (i in 0 until testCases.length) {
            val elTestCase = testCases.item(i) as Element
            val testMethod = elTestCase.getAttribute("name")
            val testClass = elTestCase.getAttribute("classname")

            if (qualifiedClassName == null || testClass == qualifiedClassName) {

                // Get first error/failure element from test case
                val errorElement = getChildFailingElement(elTestCase)
                if (errorElement !== null) {
                    val errorText = errorElement.textContent ?: continue
                    val lineNumber = getErrorLineNumber(errorText)

                    // If the context contains an error line, then add the error to the list of errors to return
                    if (lineNumber !== null) {
                        errorMessages.add(
                                TestErrorLog(
                                        testMethodName = testMethod,
                                        content = errorText,
                                        type = errorElement.getAttribute("type"),
                                        message = errorElement.getAttribute("message"),
                                        lineNumber = lineNumber
                                )
                        )
                    }
                }
            }
        }

        return errorMessages
    }

    private fun getErrorLineNumber(errorContent: String): Int? {
        val regex = Regex("""\(\w+\.java:(\d+)\)""")
        val match = regex.find(errorContent)

        return match?.groupValues?.get(1)?.toIntOrNull()
    }


    /**
     * Returns the first element with the tag "Error" or "Failure". If none of the options exist, returns null
     */
    private fun getChildFailingElement(elTestCase: Element): Element? {
        val errorNodes = elTestCase.getElementsByTagName("error")
        if (errorNodes.length > 0) {
            return errorNodes.item(0) as Element
        }
        val failureNodes = elTestCase.getElementsByTagName("failure")
        if (failureNodes.length > 0) {
            return failureNodes.item(0) as Element
        }

        return null
    }

    private fun getNonCompilingNodesFromLog(errors: String): Pair<MutableMap<String, MutableSet<String>>, MutableMap<String, MutableSet<String>>> {
        val errorLinesByFile = mutableMapOf<String, MutableSet<Int>>()
        val testsByTestClass = mutableMapOf<String, MutableSet<String>>()
        val importsByTestClass = mutableMapOf<String, MutableSet<String>>()

        val reNonPassingErrorLine = Regex("""^\[ERROR\]\s+(.+\.java):\[(\d+),(\d+)]""")

        // Step 1: Iterate errors and collect faulty lines per file
        for (errorLine in errors.lineSequence()) {
            val matchResult = reNonPassingErrorLine.find(errorLine)

            if (matchResult != null) {
                val (testClass, lineNumber) = matchResult.destructured

                val errorLines = errorLinesByFile.getOrPut(testClass) { mutableSetOf() }
                errorLines.add(lineNumber.toInt())
            }
        }

        // Step 2: Iterate all files that contain error lines, and get the method names that contain the error line
        for ((filename, errorLines) in errorLinesByFile) {

            // Verify that this is a test file and not anything from the source code
            if (filename.contains("/src/test/")) {

                // Get invalid methods
                val invalidMethods = YateJavaUtils.findMethodsFromLines(File(filename), errorLines)
                if (invalidMethods.isNotEmpty()) {
                    val methods = testsByTestClass.getOrPut(filename) { mutableSetOf() }
                    methods.addAll(invalidMethods)
                }

                // Get invalid import statements
                val invalidImports = YateJavaUtils.findImportsFromLineNumbers(File(filename), errorLines)
                if (invalidImports.isNotEmpty()) {
                    val imports = importsByTestClass.getOrPut(filename) { mutableSetOf() }
                    imports.addAll(invalidImports)
                }
            }
        }

        return Pair(testsByTestClass, importsByTestClass)
    }
}