package com.mkonst.analysis

import com.mkonst.analysis.java.JavaClassParser
import com.mkonst.analysis.kotlin.KotlinClassParser
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateJavaUtils
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import com.mkonst.types.ClassPathsContainer
import com.mkonst.types.ProgramLangType


class KotlinClassContainer(className: String, bodyContent: String? = null) : ClassContainer(className, bodyContent, ProgramLangType.KOTLIN) {

    companion object {
        @JvmStatic
        fun createFromFile(classPath: String): ClassContainer {
            val className = YateIO.getClassNameFromPath(classPath)
            val classContent = YateIO.readFile(classPath)

            val classContainer = KotlinClassContainer(className, classContent)

            // Check whether the class reflects a Test class or a regular class by checking its name and append its path
            if (className.endsWith("Test") && classPath.contains("test")) {
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
            completeClassContent += "package ${body.packageName}\n\n"
        }

        // Append import statements
        for (importStatement: String in body.imports) {
            completeClassContent += importStatement + "\n"
        }

        // Append the rest of the (clean) body
        completeClassContent += body.content

        return completeClassContent
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
                completeStatement = "import $importStatement"
            } else {
                if (importStatement.startsWith("import .")) continue
                completeStatement = importStatement
            }

            if (!this.body.imports.contains(completeStatement)) {
                this.body.imports.add(completeStatement)
            }
        }
    }

    override fun convertRawContentToStructure(): ClassBody {
        if (this.bodyContent === null) {
            return ClassBody()
        }

        val bodyDecoded = KotlinClassParser().getBodyDecoded(this.bodyContent)

        return bodyDecoded
    }

    override fun copy(): ClassContainer {
        val newInstance: ClassContainer = KotlinClassContainer(className, bodyContent)
        newInstance.body = this.body.copy()
        newInstance.paths = this.paths.copy()

        return newInstance
    }
}