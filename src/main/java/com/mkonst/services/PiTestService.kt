package com.mkonst.services

import com.mkonst.evaluation.RemoveTestsForMutationScript
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.types.DependencyTool
import com.mkonst.types.ProgramLangType
import com.mkonst.types.coverage.MutationScore

class PiTestService(private val repositoryPath: String) {

    fun runMutationScore(dependencyTool: DependencyTool, failOnError: Boolean = false): MutationScore {
        var output = YateJavaExecution.runPiTest(repositoryPath, dependencyTool)
        var hasErrors: Boolean = !output.contains("[INFO] BUILD SUCCESS")
        if (hasErrors && failOnError) {
            throw Exception("Mutation score cannot be calculated due to failing tests. (You can remove failing tests by setting the failOnError flag to false)")
        }

        // Attempt to fix errors by removing failing tests
        var iterations: Int = 0
        while (hasErrors && iterations < 5) {
            iterations += 1
            YateConsole.debug("Fixing failing tests for mutation score. Iteration: #$iterations")

            // Extract content that contains failing tests
            var failingTestsContentLog = extractFailingTestsLog(output)
            if (failingTestsContentLog === null) {
                throw Exception("Could not identify failing tests for mutation score")
            }

            // Parse failing tests content and extract the test class alongside the failing test method
            val testsToRemoveByClassPath: MutableMap<String, MutableSet<String>> = extractFailingTestsFromLog(failingTestsContentLog)
            var nrTestsRemoved: Int = 0
            for ((testClassPath, testNames) in testsToRemoveByClassPath) {
                nrTestsRemoved += testNames.size
                val classPathToUpdate = YateCodeUtils.getClassPathFromQualifiedName(repositoryPath, testClassPath)
                val newContent: String = YateCodeUtils.removeMethodsInClass(classPathToUpdate, testNames, ProgramLangType.JAVA)
                YateIO.writeFile(classPathToUpdate, newContent)
            }

            YateConsole.debug("# tests removed to calculate mutation score: $nrTestsRemoved")

            output = YateJavaExecution.runPiTest(repositoryPath, dependencyTool)
            hasErrors = !output.contains("[INFO] BUILD SUCCESS")
        }

        // Check whether the mutation score can be calculated, or whether errors exist
        if (!hasErrors) {
            return getMutationScoreFromLogContent(output)
        }

        return MutationScore(0, 0)
    }

    fun getMutationScoreFromLogContent(logContent: String): MutationScore {
        var totalMutationsGenerated: Int = 0
        var totalMutationsKilled: Int = 0
        val mutationStats: List<MutationScore> = extractAllMutationsKilled(logContent)

        mutationStats.forEachIndexed { index, (generated, killed) ->
            YateConsole.debug("Module ${index + 1}: Generated = $generated, Killed = $killed")
            totalMutationsGenerated += generated
            totalMutationsKilled += killed
        }

        return MutationScore(totalMutationsGenerated, totalMutationsKilled)
    }

    private fun extractFailingTestsLog(input: String): String? {
        val targetLine = "PIT >> SEVERE : Tests failing without mutation:"

        val index = input.indexOf(targetLine)
        return if (index != -1) {
            input.substring(index + targetLine.length).trimStart()
        } else {
            null
        }
    }

    private fun extractFailingTestsFromLog(log: String): MutableMap<String, MutableSet<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        // Regex to extract testClass and method from each log line
        val regex = Regex("""testClass=([^\],]+).*?\[(?:method|test):([^\(\]\[]+)""")

        log.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val testClass = match.groupValues[1]
                val method = match.groupValues[2]
                result.computeIfAbsent(testClass) { mutableSetOf() }.add(method)
            }
        }

        return result
    }


    /**
     * Uses Regex to extract all statistics that contain the # of mutation killed and the # of generated mutations
     */
    private fun extractAllMutationsKilled(input: String): List<MutationScore> {
        val regex = Regex(">>\\s*Generated\\s+(\\d+)\\s+mutations\\s+Killed\\s+(\\d+)")
        return regex.findAll(input).map { match ->
            val (generated, killed) = match.destructured
            MutationScore(generated.toInt(), killed.toInt())
        }.toList()
    }



}