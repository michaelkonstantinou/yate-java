package com.mkonst.analysis

abstract class ClassContainer(val className: String, bodyContent: String? = null) {

    /**
     * Returns the complete content of the Class
     * It should consist of all aspects that create a class (i.e. namespace or package, import statements etc...)
     */
    abstract fun getContent(): String

    abstract fun getPrivateMethods(): MutableList<String>

    abstract fun getProtectedMethods(): MutableList<String>
}