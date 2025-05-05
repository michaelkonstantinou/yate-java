package com.mkonst.analysis

import com.mkonst.analysis.java.JavaClassParser
import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import com.mkonst.types.ClassPathsContainer


class JavaClassContainer(className: String, bodyContent: String? = null) : ClassContainer(className, bodyContent) {
//    private val body = mutableMapOf("package" to null, "imports" to null, "methods" to null, "content" to null)
    companion object {
        @JvmStatic
        fun createFromFile(classPath: String): ClassContainer {
            val className = YateIO.getClassNameFromPath(classPath)
            val classContent = YateIO.readFile(classPath)

            val classContainer = JavaClassContainer(className, classContent)

            // Check whether the class reflects a Test class or a regular class by checking its name and append its path
            if (className.contains("Test") && classPath.contains("test")) {
                val outputFolder = YateIO.getFolderFromPath(classPath)
                classContainer.paths = ClassPathsContainer(outputDirectory = outputFolder, testClass = classPath)
            } else {
                val outputFolder = YateJavaUtils.getTestClassDirectoryPath(classPath)
                classContainer.paths = ClassPathsContainer(outputDirectory = outputFolder, cut = classPath)
            }

            return classContainer
        }
    }

    /**
     * Combines the fields included in the body and returns a complete class implementation
     */
    override fun getCompleteContent(): String {
        var completeClassContent: String = ""

        // Append package name (if available)
        if (body.packageName !== null) {
            completeClassContent += "package ${body.packageName};\n\n"
        }

        // Append import statements todo: add required import statements
        for (importStatement: String in body.imports) {
            completeClassContent += importStatement + "\n"
        }

        // Append the rest of the (clean) body
        completeClassContent += body.content

        return completeClassContent
    }

    /**
     * Iterates the methods under body and returns the method names of the ones who have the private modifier
     */
    override fun getPrivateMethods(): MutableList<String> {
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
    override fun getProtectedMethods(): MutableList<String> {
        val methodsToReturn: MutableList<String> = mutableListOf()
        for ((methodName, modifier) in body.methods) {
            if (modifier == "protected") {
                methodsToReturn.add(methodName)
            }
        }

        return methodsToReturn
    }

    /**
     * Checks whether any of the given import statements are not already present in this object
     * and appends the missing ones.
     *
     * The format of the given import statement should be only its qualifying name (package + class_name). It must not
     * include a semicolon (;) or the import keyword at the beginning
     */
    override fun appendImports(importsToAppend: MutableList<String>) {
        for (importStatement in importsToAppend) {
            val completeStatement: String

            if (!importStatement.startsWith("import")) {
                if (importStatement.startsWith(".")) continue
                completeStatement = "import $importStatement;"
            } else {
                if (importStatement.startsWith("import .")) continue
                completeStatement = importStatement
            }

            if (!this.body.imports.contains(completeStatement)) {
                this.body.imports.add(completeStatement)
            }
        }
    }

    override fun removeImports(importsToRemove: MutableList<String>) {
        TODO("Not yet implemented")
    }

    override fun convertRawContentToStructure(): ClassBody {
        if (this.bodyContent === null) {
            return ClassBody()
        }

        return JavaClassParser().getBodyDecoded(this.bodyContent)
    }

    fun setPathsFromCut(cutContainer: ClassContainer) {
        val testClassPath = YateJavaUtils.getTestClassPath(cutContainer, this)
        paths = ClassPathsContainer(cutContainer.paths.cut, testClassPath)
    }
}