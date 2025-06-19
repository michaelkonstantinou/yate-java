package com.mkonst.analysis

import com.mkonst.analysis.java.JavaClassParser
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateIO
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import com.mkonst.types.ClassPathsContainer
import com.mkonst.types.ProgramLangType
import com.mkonst.types.serializable.ClassContainerJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class ClassContainer(val className: String, val bodyContent: String? = null, val lang: ProgramLangType) {
    var body: ClassBody = ClassBody()
    var paths: ClassPathsContainer = ClassPathsContainer()

    init {
        body = convertRawContentToStructure()
        appendRequiredImports()
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

    /**
     * Replaces all import statements with the given ones
     */
    fun setImports(imports: MutableList<String>) {
        body.imports = imports
    }

    fun appendRequiredImports() {
        for (requiredImport: String in ConfigYate.getArray("REQUIRED_IMPORTS")) {
            if (requiredImport !in this.body.imports) {
                this.body.imports.add(requiredImport)
            }
        }
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

    fun toJson() {
        val jsonString = Json.encodeToString(ClassContainerJson(className, body.packageName ?: "",
            body.imports,
            body.methods,
            body.content,
            body.hasConstructors,
            paths.cut,
            paths.testClass))

        val outputFilepath: String = ConfigYate.getString("DIR_OUTPUT") + className + ".json"
        YateIO.writeFile(outputFilepath, jsonString)
    }
}