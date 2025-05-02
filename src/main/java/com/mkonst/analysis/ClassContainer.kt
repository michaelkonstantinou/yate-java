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

    abstract fun getPrivateMethods(): MutableList<String>

    abstract fun getProtectedMethods(): MutableList<String>

    abstract fun appendImports(importsToAppend: MutableList<String>)

    abstract fun convertRawContentToStructure(): ClassBody

    fun toTestFile() {
        if (this.paths.testClass === null) {
            throw Exception("Cannot create a test file from the ClassContainer as the testClass path is not specified")
        } else {
            YateIO.writeFile(this.paths.testClass!!, getCompleteContent())
        }
    }
}