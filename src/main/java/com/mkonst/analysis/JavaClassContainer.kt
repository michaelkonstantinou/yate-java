package com.mkonst.analysis

class JavaClassContainer(className: String, bodyContent: String? = null) : ClassContainer(className, bodyContent) {
    private val body = mutableMapOf("package" to null, "imports" to null, "methods" to null, "content" to null)

    override fun getContent(): String {
        TODO("Not yet implemented")
    }
}