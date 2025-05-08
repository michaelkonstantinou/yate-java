package com.mkonst.services

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.types.OracleError
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.io.path.Path

class ErrorService(private val repositoryPath: String) {

    /**
     * Runs the tests in the repository and returns a map with the non-passing tests for each test class
     */
    fun findNonPassingTests(dependencyTool: String): Map<String, MutableSet<String>> {
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

    fun findExceptionErrorsFromReport(qualifiedClassName: String? = null): List<OracleError> {
        val reportXmlPath = Path(repositoryPath, "target/surefire-reports/TEST-${qualifiedClassName}.xml").toString()
        val exceptionErrors = mutableListOf<OracleError>()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(File(reportXmlPath))
        val testCases = doc.getElementsByTagName("testcase")

        val regex = Regex("""\((\w+)\.java:(\d+)\)""")

        for (i in 0 until testCases.length) {
            val elTestCase = testCases.item(i) as Element
            val testMethod = elTestCase.getAttribute("name")
            val testClass = elTestCase.getAttribute("classname")

            val errorNodes = elTestCase.getElementsByTagName("error")
            if (qualifiedClassName == null || testClass == qualifiedClassName) {
                if (errorNodes.length > 0) {
                    val errorElement = errorNodes.item(0) as Element
                    val errorText = errorElement.textContent ?: continue

                    val match = regex.find(errorText)
                    if (match != null) {
                        val lineNumber = match.groupValues[2].toIntOrNull() ?: continue
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

    fun extractFullTestMethods(file: File, testMethods: Set<String>): Map<String, String> {
        val parser = JavaParser()
        val result = parser.parse(file)

        if (!result.result.isPresent) return emptyMap()

        val compilationUnit: CompilationUnit = result.result.get()
        val fullMethods = mutableMapOf<String, String>()

        compilationUnit.findAll(MethodDeclaration::class.java).forEach { method ->
            val methodName = method.nameAsString
            if (methodName in testMethods) {
                // Get full method declaration as written in source
                fullMethods[methodName] = method.toString()
            }
        }

        return fullMethods
    }
}