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
    fun replaceLineInList(lines: MutableList<String>, lineNumber: Int, oldValue: String, newValue: String): MutableList<String> {
        lines[lineNumber] = lines[lineNumber].replaceFirst(oldValue, newValue)

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