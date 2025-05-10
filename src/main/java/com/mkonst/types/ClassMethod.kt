package com.mkonst.types

class ClassMethod {
    var className: String
        private set
    var methodName: String
        private set

    constructor(className: String, methodName: String) {
        this.className = className
        this.methodName = methodName
    }

    constructor(classMethodQualifiedName: String) {
        val qualifiedNames = classMethodQualifiedName.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (qualifiedNames.size != 2) {
            throw Exception("The invoked ClassMethod constructor requires a string of the format classQualifiedName#methodName")
        }

        this.className = qualifiedNames[0]
        this.methodName = qualifiedNames[1]
    }

    override fun toString(): String {
        return this.className + "#" + this.methodName
    }

    override fun equals(other: Any?): Boolean {
        if (other is ClassMethod) {
            return (other.className == this.className) && other.methodName == this.methodName
        }

        return false
    }

    override fun hashCode(): Int {
        return className.hashCode() + 31 * methodName.hashCode()
    }
}

