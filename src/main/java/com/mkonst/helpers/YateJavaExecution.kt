package com.mkonst.helpers

import com.mkonst.types.OracleError
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object YateJavaExecution {

    /**
     * Runs the whole test suite in the given repository using the dependency tool given, and returns the error log
     * Depending on the flag includeCompilingTests, the method will either return the compilation errors, or the
     * errors from a compiling suite.
     */
    fun runTestsForErrors(repositoryPath: String, dependencyTool: String, includeCompilingTests: Boolean = false): String? {
        return when (dependencyTool) {
            "maven" -> runMavenTestsForErrors(repositoryPath, includeCompilingTests)
            "gradle" -> runMavenTestsForErrors(repositoryPath, includeCompilingTests)
            else -> throw Exception("Dependency tool $dependencyTool is not supported")
        }
    }

    /**
     * Runs the whole test suite in the given repository using maven, and returns the error log
     * Depending on the flag includeCompilingTests, the method will either return the compilation errors, or the
     * errors from a compiling suite.
     */
    private fun runMavenTestsForErrors(repositoryPath: String, includeCompilingTests: Boolean = false): String? {
        // todo: Make sure to switch to the correct java version

        val command = listOf("mvn", "clean", "test", "-Drat.skip=True")
        val processBuilder = ProcessBuilder(command)
                .directory(File(repositoryPath))
                .redirectErrorStream(true)

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        // Iterate each line and collect the error lines
        val errorLines = mutableListOf<String>()
        var isCodeCompiling = true
        reader.forEachLine { line ->
            if (line.startsWith("[ERROR]")) {
                val errorLine = line.trim()

                if ("COMPILATION ERROR" in errorLine || "Compilation failure" in errorLine) {
                    isCodeCompiling = false
                }

                if ("[ERROR] -> [Help 1]" in errorLine) {
                    return@forEachLine
                }

                errorLines.add(errorLine)
            }
        }

        // If the code compiles, but the given parameters do not require errors from compiling tests,
        // then no error log should be returned
        if (isCodeCompiling && !includeCompilingTests) {
            return null
        }

        return if (errorLines.isEmpty()) null else errorLines.joinToString("\n")
    }

    /**
     * Parses the error line exported by a test running framework and extracts
     * the class name, test name, line number, expected value, and actual value.
     *
     * @param errorLine The error message string.
     * @return A Map with extracted values or null if no match is found.
     */
    fun parseFailedTestLine(errorLine: String, dependencyTool: String): OracleError? {
        return when (dependencyTool) {
            "maven" -> parseFailedTestLineMaven(errorLine)
//            "gradle" -> runMavenTestsForErrors(repositoryPath, includeCompilingTests)
            else -> throw Exception("Dependency tool $dependencyTool is not supported")
        }
    }
    private fun parseFailedTestLineMaven(errorLine: String): OracleError? {
        // Standard pattern: class.method:line expected:<val> but was:<val>
        val standardPattern = Regex(
                """.*?([\w.]+)\.(\w+):(\d+)\s+(?:Unexpected exception type thrown, )?expected: <([^>]+)> but was: <([^>]+)>"""
        )

        // Exception pattern: class.method:line Expected SomeException to be thrown, but nothing was thrown.
        val exceptionPattern = Regex(
                """.*?([\w.]+)\.(\w+):(\d+)\s+Expected ([\w.]+) to be thrown, but nothing was thrown\."""
        )

        // Match standard failure
        standardPattern.matchEntire(errorLine)?.let { match ->
            val (className, testName, lineNumber, expectedValue, actualValue) = match.destructured
            return OracleError(
                    className = className,
                    testMethodName = testName,
                    expectedValue = expectedValue,
                    actualValue = actualValue,
                    lineNumber = lineNumber.toInt()
            )
        }

        // Match missing exception
        exceptionPattern.matchEntire(errorLine)?.let { match ->
            val (className, testName, lineNumber, expectedValue) = match.destructured
            return OracleError(
                    className = className,
                    testMethodName = testName,
                    expectedValue = expectedValue,
                    lineNumber = lineNumber.toInt()
            )
        }

        return null
    }


}