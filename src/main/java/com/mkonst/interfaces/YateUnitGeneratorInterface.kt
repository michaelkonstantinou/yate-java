package com.mkonst.interfaces

import com.mkonst.analysis.ClassContainer
import com.mkonst.types.YateResponse

interface YateUnitGeneratorInterface {

    fun generateForClass(cutContainer: ClassContainer): YateResponse

    fun generateForConstructors(cutContainer: ClassContainer): YateResponse

    fun generateForMethod(cutContainer: ClassContainer, methodName: String): YateResponse

    fun closeConnection()
}