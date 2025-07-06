package com.mkonst.evaluation

import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateIO
import com.mkonst.services.PiTestService
import com.mkonst.types.DependencyTool
import com.mkonst.types.ProgramLangType
import com.mkonst.types.coverage.MutationScore

object RemoveTestsForMutationScript {

    fun convertToJavaFilePath(repositoryPath: String, prefix: String, className: String): String {
        // Replace underscores with file separators only before the final class name
        val lastDotIndex = className.lastIndexOf('.')
        val packagePath = className.substring(0, lastDotIndex).replace('.', '/')
        val classSimpleName = className.substring(lastDotIndex + 1)
        return "$repositoryPath$prefix$packagePath/$classSimpleName.java"
    }

    fun parseErrorLog(log: String): MutableMap<String, MutableSet<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        // Regex to extract testClass and method from each log line
        val regex = Regex("""testClass=([^\],]+).*?\[method:([^\(\]]+)""")

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

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/"
        val piTestService: PiTestService = PiTestService(repositoryPath)
        val ms: MutationScore = piTestService.runMutationScore(DependencyTool.MAVEN)

        println(ms)
    }
}