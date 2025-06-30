package com.mkonst.evaluation

import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateIO
import com.mkonst.types.ProgramLangType

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
        val errors = YateIO.readFile("${repositoryPath}mvn_errors.txt")
//        println(convertToJavaFilePath(repositoryPath, "ch.jalu.configme.properties.types.InlineArrayPropertyType_toExportValue_Test"))

        val testsToRemoveByClassPath: MutableMap<String, MutableSet<String>> = parseErrorLog(errors)
        var nrTestsRemoved: Int = 0
        for ((testClassPath, testNames) in testsToRemoveByClassPath) {
            nrTestsRemoved += testNames.size
            val classPathToUpdate = convertToJavaFilePath(repositoryPath, "src/test/java/", testClassPath)
            val newContent: String = YateCodeUtils.removeMethodsInClass(classPathToUpdate, testNames, ProgramLangType.JAVA)
            YateIO.writeFile(classPathToUpdate, newContent)
        }

        println("# tests removed: $nrTestsRemoved")
    }
}