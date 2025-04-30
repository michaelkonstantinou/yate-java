package com.mkonst.interfaces

import com.mkonst.analysis.ClassContainer

interface YateUnitGeneratorInterface {

    fun generateForClass(cutContainer: ClassContainer)

    fun generateForConstructors(cutContainer: ClassContainer)

    fun generateForMethod(cutContainer: ClassContainer, methodName: String)
}