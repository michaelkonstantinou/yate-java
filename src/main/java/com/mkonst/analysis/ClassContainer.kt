package com.mkonst.analysis

import com.mkonst.analysis.java.JavaClassParser
import com.mkonst.helpers.YateIO
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import com.mkonst.types.ClassPathsContainer

abstract class ClassContainer(val className: String, val bodyContent: String? = null) {
    var body: ClassBody = ClassBody()
    var paths: ClassPathsContainer = ClassPathsContainer()

    init {
        body = convertRawContentToStructure()
    }

    /**
     * Returns the complete content of the Class
     * It should consist of all aspects that create a class (i.e. namespace or package, import statements etc...)
     */
    abstract fun getCompleteContent(): String

    /**
     * Iterates the methods under body and returns the method names of the ones who have the private modifier
     */
    fun getPrivateMethods(): MutableList<String> {
        val methodsToReturn: MutableList<String> = mutableListOf()
        for ((methodName, modifier) in body.methods) {
            if (modifier == "private") {
                methodsToReturn.add(methodName)
            }
        }

        return methodsToReturn
    }

    /**
     * Iterates the methods under body and returns the method names of the ones who have the protected modifier
     */
    fun getProtectedMethods(): MutableList<String> {
        val methodsToReturn: MutableList<String> = mutableListOf()
        for ((methodName, modifier) in body.methods) {
            if (modifier == "protected") {
                methodsToReturn.add(methodName)
            }
        }

        return methodsToReturn
    }

    fun removeImports(importsToRemove: MutableList<String>) {
        body.imports.removeAll(importsToRemove)
    }

    abstract fun appendImports(importsToAppend: MutableList<String>)

    abstract fun convertRawContentToStructure(): ClassBody

    /**
     * Creates a copy of the current instance.
     * It is used to create a backup of the current class before attempting any changes on it
     */
    abstract fun copy(): ClassContainer

    /**
     * Returns the package + class_name (aka QualifyingName).
     * If method_name is provided, the method_name will be appended as a suffix
     */
    fun getQualifiedName(methodName: String? = null): String
    {
        var name = ""

        if (body.packageName != null) {
            name += "${body.packageName}."
        }

        name += className

        if (methodName != null) {
            name += "#$methodName"
        }

        return name
    }

    fun toTestFile() {
        if (this.paths.testClass === null) {
            throw Exception("Cannot create a test file from the ClassContainer as the testClass path is not specified")
        } else {
            YateIO.writeFile(this.paths.testClass!!, getCompleteContent())
        }
    }
}