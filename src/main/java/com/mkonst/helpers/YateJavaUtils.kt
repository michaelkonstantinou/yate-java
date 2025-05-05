package com.mkonst.helpers

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
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

}