package com.mkonst.analysis

abstract class ClassContainer(className: String, bodyContent: String? = null) {

    /**
     * Returns the complete content of the Class
     * It should consist of all aspects that create a class (i.e. namespace or package, import statements etc...)
     */
    abstract fun getContent(): String

}