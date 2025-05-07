package com.mkonst.services

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.mkonst.helpers.YateJavaExecution
import java.io.File

class ErrorService {

    fun removeMethods(file: File, testMethods: Set<String>): String {
        val parser = JavaParser()
        val result = parser.parse(file)

        if (!result.result.isPresent) return file.readText()

        val compilationUnit = result.result.get()

        // Collect methods to remove (avoid modifying list while iterating)
        val methodsToRemove = compilationUnit.findAll(MethodDeclaration::class.java)
                .filter { it.nameAsString in testMethods }

        methodsToRemove.forEach { it.remove() }

        println(compilationUnit.toString())
        return compilationUnit.toString()  // Returns the modified source as a string
    }

    fun findNonPassingTests(repositoryPath: String, dependencyTool: String): Map<String, MutableSet<String>> {
        val testsByTestClass = mutableMapOf<String, MutableSet<String>>()

        // Step 1: Run tests and get errors
        val errors = YateJavaExecution.runTestsForErrors(repositoryPath, dependencyTool, includeCompilingTests = true)
        if (errors == null) {
            return emptyMap()
        }

        val reNonPassingErrorLine = Regex("""^\[ERROR\]\s+([\w\d_]+)\.([\w\d_]+):(\d+)""")

        // Step 2: Iterate errors and collect faulty lines per file
        for (errorLine in errors.lineSequence()) {
            val matchResult = reNonPassingErrorLine.find(errorLine)

            if (matchResult != null) {
                val (testClass, testMethod) = matchResult.destructured

                val methods = testsByTestClass.getOrPut(testClass) { mutableSetOf() }
                methods.add(testMethod)
            }
        }

        return testsByTestClass
    }

    fun extractFullTestMethods(file: File, testMethods: Set<String>): Map<String, String> {
        val parser = JavaParser()
        val result = parser.parse(file)

        if (!result.result.isPresent) return emptyMap()

        val compilationUnit: CompilationUnit = result.result.get()
        val fullMethods = mutableMapOf<String, String>()

        compilationUnit.findAll(MethodDeclaration::class.java).forEach { method ->
            val methodName = method.nameAsString
            if (methodName in testMethods) {
                // Get full method declaration as written in source
                fullMethods[methodName] = method.toString()
            }
        }

        return fullMethods
    }
}