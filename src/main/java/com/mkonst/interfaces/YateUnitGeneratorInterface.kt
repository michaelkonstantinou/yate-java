package com.mkonst.interfaces

import com.mkonst.analysis.ClassContainer

interface YateUnitGeneratorInterface {

    fun generate_for_class(cutContainer: ClassContainer)

    fun generate_for_constructors(cutContainer: ClassContainer)

    fun generate_for_method(cutContainer: ClassContainer, methodName: String)
}