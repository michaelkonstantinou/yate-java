package com.mkonst.providers

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.analysis.KotlinClassContainer
import com.mkonst.types.ProgramLangType
import kotlin.jvm.Throws

object ClassContainerProvider {

    @Throws
    fun getFromFile(classPath: String): ClassContainer {
        if (classPath.endsWith(".java")) {
            return JavaClassContainer.createFromFile(classPath)
        } else if (classPath.endsWith(".kt")) {
            return KotlinClassContainer.createFromFile(classPath)
        }

        throw Exception("Given class path is not supported. Supported files: .java, .kt")
    }

    fun getFromContent(className: String, content: String?, lang: ProgramLangType): ClassContainer {
        return when (lang) {
            ProgramLangType.JAVA -> JavaClassContainer(className, content)
            ProgramLangType.KOTLIN -> KotlinClassContainer(className, content)
        }
    }
}