package com.mkonst.helpers

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.mkonst.analysis.ClassContainer
import java.io.File

object YateJavaUtils {

    fun getTestClassPath(cutContainer: ClassContainer, testClassContainer: ClassContainer): String {
        val testClassPath: String? = cutContainer.paths.cut
        if (testClassPath === null) {
            throw Exception("CutContainer requires the class path")
        }

        return testClassPath
                .replace("src/main", "src/test")
                .replace("${cutContainer.className}.java", "${testClassContainer.className}.java")
    }

    fun getTestClassDirectoryPath(classPath: String): String {
        return classPath.replace("src/main", "src/test")
    }

    /**
     * Checks if a given Java class exists given its package name.
     */
    fun classPackageExists(repositoryPath: String, packageName: String): Boolean {
        val classFilePath = getClassFileFromPackage(repositoryPath, packageName)

        return File(classFilePath).isFile
    }

    /**
     * Returns the actual path of the class that a given package name represents (regardless of whether exists or not)
     */
    fun getClassFileFromPackage(repository: String, packageName: String): String {
        var mainJavaPath = File(repository, "src/main/java")

        if (mainJavaPath.exists() && mainJavaPath.isDirectory) {
            return File(mainJavaPath, packageName.replace(".", "/") + ".java").path
        }

        mainJavaPath = File(repository, "src/main")
        return File(mainJavaPath, packageName.replace(".", "/") + ".java").path
    }

    /**
     *  Reads the context of the given class, and returns a new modified version with the given methods removed
     */
    fun removeMethodsInClass(classPath: String, methods: MutableSet<String>): String {
        val file = File(classPath)
        if (!file.exists()) {
            throw Exception("Cannot remove methods from given class path: Class path does not exist ($classPath)")
        }

        val parser = JavaParser()
        val result = parser.parse(file)

        if (!result.result.isPresent) {
            return file.readText()
        }

        val compilationUnit = result.result.get()

        // Collect methods to remove (avoid modifying list while iterating)
        val methodsToRemove = compilationUnit.findAll(MethodDeclaration::class.java)
                .filter { it.nameAsString in methods }

        methodsToRemove.forEach { it.remove() }

        return compilationUnit.toString()
    }

}