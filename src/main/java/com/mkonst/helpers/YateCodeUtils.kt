package com.mkonst.helpers

import com.mkonst.types.ProgramLangType
import java.io.File

object YateCodeUtils {

    /**
     * Iterates all java and kotlin files in the repository and finds their common package prefix
     */
    @JvmStatic
    fun getRootPackage(repositoryPath: String): String {
        val srcDir = "src/main"
        val packageNames = mutableSetOf<String>()

        val fullPath = File(repositoryPath, srcDir)
        if (!fullPath.exists()) {
            throw Exception("Cannot find the root package of the given repository. Such filepath does not exist.")
        }

        fullPath.walkTopDown().filter { it.isFile && (it.extension == "java" || it.extension == "kt") }.forEach { file ->
            file.useLines { lines ->
                val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                if (packageLine != null) {
                    val pkgName = packageLine.removePrefix("package").trim().removeSuffix(";")
                    if (pkgName.split(".").size >= 2) {
                        packageNames.add(pkgName)
                    }
                }
            }
        }

        return findCommonRootPackage(packageNames)
    }

    @JvmStatic
    fun replaceOracleInLines(lines: MutableList<String>, lineNumber: Int, oldValue: String, newValue: String): MutableList<String> {
        println("BEFORE: ${lines[lineNumber]}")
        val assertion = getAssertFunction(lines[lineNumber])
        println("Replacing oracle: Assertion is $assertion, newValue is $newValue")
        when {
            assertion.contains("assertTrue") && newValue == "false" -> {
                lines[lineNumber] = lines[lineNumber].replaceFirst("assertTrue", "assertFalse")
            }
            assertion.contains("assertFalse") && newValue == "true" -> {
                lines[lineNumber] = lines[lineNumber].replaceFirst("assertFalse", "assertTrue")
            }
            assertion.contains("assertNotNull") && newValue == "null" -> {
                lines[lineNumber] = lines[lineNumber].replaceFirst("assertNotNull", "assertNull")
            }
            else -> lines[lineNumber] = lines[lineNumber].replaceFirst(oldValue, newValue)
        }

        println("AFTER: ${lines[lineNumber]}")
        return lines
    }

    /**
     *  Reads the context of the given class, and returns a new modified version with the given methods removed
     */
    fun removeMethodsInClass(classPath: String, methods: MutableSet<String>, lang: ProgramLangType): String {
        return when(lang) {
            ProgramLangType.JAVA -> YateJavaUtils.removeMethodsInClass(classPath, methods)
            ProgramLangType.KOTLIN -> throw NotImplementedError("removeMethodsInClass function is not supported for kotlin")
        }
    }

    /**
     * Given a single line of code, the function will return the name of the assertion that is being used or unknown
     * if there is no assertion in place
     *
     * (e.g. assertTrue(myVar) -> assertTrue)
     */
    fun getAssertFunction(codeLine: String): String {
        return codeLine.substringBefore("(", missingDelimiterValue = "unknown").trim()
    }

    private fun findCommonRootPackage(packageNames: Set<String>): String {
        if (packageNames.isEmpty()) return ""

        var rootParts = packageNames.first().split(".")

        for (pkg in packageNames) {
            val parts = pkg.split(".")
            val minLength = minOf(rootParts.size, parts.size)
            var i = 0
            while (i < minLength && rootParts[i] == parts[i]) {
                i++
            }
            rootParts = rootParts.subList(0, i)
        }

        return rootParts.joinToString(".")
    }

}