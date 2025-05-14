package com.mkonst.providers

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.types.ProgramLangType

object ClassContainerProvider {

    fun getFromFile(classPath: String, lang: ProgramLangType): ClassContainer {
        return when (lang) {
            ProgramLangType.JAVA -> JavaClassContainer.createFromFile(classPath)
            ProgramLangType.KOTLIN -> TODO()
        }
    }
}