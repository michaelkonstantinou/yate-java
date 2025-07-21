package com.mkonst.providers

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.analysis.KotlinClassContainer
import com.mkonst.types.ProgramLangType

object ClassContainerProvider {

    fun getFromFile(classPath: String, lang: ProgramLangType): ClassContainer {
        return when (lang) {
            ProgramLangType.JAVA -> JavaClassContainer.createFromFile(classPath)
            ProgramLangType.KOTLIN -> KotlinClassContainer.createFromFile(classPath)
        }
    }

    fun getFromContent(className: String, content: String?, lang: ProgramLangType): ClassContainer {
        return when (lang) {
            ProgramLangType.JAVA -> JavaClassContainer(className, content)
            ProgramLangType.KOTLIN -> KotlinClassContainer(className, content)
        }
    }
}