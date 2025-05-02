package com.mkonst.helpers

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer

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
}