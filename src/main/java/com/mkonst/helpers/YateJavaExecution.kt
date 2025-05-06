package com.mkonst.helpers

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
}