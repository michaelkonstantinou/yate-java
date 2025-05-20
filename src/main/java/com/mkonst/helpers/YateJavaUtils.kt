package com.mkonst.helpers

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import java.io.File

object YateJavaUtils {

    /**
     * Generates the complete path of the test class container based on the class that it tests.
     * It will generate a path that follows the java conventions, which is the test class to be in the src/test
     * directory and under the same directory names as the class under test
     */
    fun getTestClassPath(cutContainer: ClassContainer, testClassContainer: ClassContainer): String {
        val testClassPath: String? = cutContainer.paths.cut
        if (testClassPath === null) {
            throw Exception("CutContainer requires the class path")
        }

        return testClassPath
                .replace("src/main", "src/test")
                .replace("${cutContainer.className}.java", "${testClassContainer.className}.java")
    }

    fun getTestClassDirectoryPath(classPath: String): String {
        return classPath.replace("src/main", "src/test")
    }

    /**
     * Checks if a given Java class exists given its package name, with the exact string case
     */
    fun classPackageExists(repositoryPath: String, classQualifiedName: String): Boolean {
        val classFile = File(getClassFileFromPackage(repositoryPath, classQualifiedName))
        val className = classQualifiedName.substringAfterLast(".")

        // To check for case-sensitive files, we need to find the parent and output the filenames as found there
        val parentFile = classFile.parentFile
        if (!parentFile.exists() || !classFile.exists() || !classFile.isFile) {
            return false
        }

        val filesInDir = parentFile.list() ?: return false

        // Check that the parent file contains a file with the EXACT name and the EXACT same case
        return filesInDir.contains("$className.java")
    }

    /**
     * Returns the actual path of the class that a given package name represents (regardless of whether exists or not)
     */
    fun getClassFileFromPackage(repository: String, packageName: String): String {
        var mainJavaPath = File(repository, "src/main/java")

        if (mainJavaPath.exists() && mainJavaPath.isDirectory) {
            return File(mainJavaPath, packageName.replace(".", "/") + ".java").path
        }

        mainJavaPath = File(repository, "src/main")
        return File(mainJavaPath, packageName.replace(".", "/") + ".java").path
    }

    /**
     *  Reads the context of the given class, and returns a new modified version with the given methods removed
     */
    fun removeMethodsInClass(classPath: String, methods: MutableSet<String>): String {
        val file = File(classPath)
        if (!file.exists()) {
            throw Exception("Cannot remove methods from given class path: Class path does not exist ($classPath)")
        }

        val parser = JavaParser()
        val result = parser.parse(file)

        if (!result.result.isPresent) {
            return file.readText()
        }

        val compilationUnit = result.result.get()

        // Collect methods to remove (avoid modifying list while iterating)
        val methodsToRemove = compilationUnit.findAll(MethodDeclaration::class.java)
                .filter { it.nameAsString in methods }

        methodsToRemove.forEach { it.remove() }

        return compilationUnit.toString()
    }

    /**
     * Returns whether JavaParser can parse successfully the given method code
     */
    fun isMethodParsing(methodSource: String): Boolean {
        // Wrap the method inside a dummy class to make it parseable
        val wrappedSource = """
        public class Dummy {
            $methodSource
        }
    """.trimIndent()

        return try {
            val result = JavaParser().parse(wrappedSource)
            result.isSuccessful
        } catch (e: ParseProblemException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns whether JavaParser can parse successfully the given method code
     */
    fun isClassParsing(classSource: String): Boolean {
        return try {
            val result = JavaParser().parse(classSource)
            result.isSuccessful
        } catch (e: ParseProblemException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scans using regex the given exception oracle and returns an assertDoesNotThrow version of it
     * In case the oracle could not be decoded as an exception oracle, the method returns null
     */
    fun invertExceptionOracle(exceptionOracle: String): String? {
        // Multiline assertThrows block pattern
        val multilinePattern = Regex(
                """assertThrows\s*\(\s*[\w.]+\.class\s*,\s*\(\s*\)\s*->\s*\{"""
        )

        if (multilinePattern.containsMatchIn(exceptionOracle)) {
            return "assertDoesNotThrow(() -> {"
        }

        // Inline assertThrows pattern
        val inlinePattern = Regex(
                """assertThrows\(\s*[\w.]+\.class,\s*\(\s*\)\s*->\s*(.+?)\);"""
        )

        val match = inlinePattern.find(exceptionOracle)
        if (match != null) {
            val executionPart = match.groupValues[1].trim()
            return "assertDoesNotThrow(() -> $executionPart);"
        }

        return null
    }

    /**
     * Parses the given statement and attempts to generate an exception oracle of the
     * format: assertThrows(exception_class, () -> clean_statement);
     *
     * Returns null if the statement could not be decoded into a simple expression
     */
    fun createExceptionOracleFromStatement(statement: String, exceptionClass: String): String? {
        // Capture format: <object> <variable> = <statement>;
        val reEqualityStatement = Regex("""(\w+(<.*?>)?)\s+(\w+)\s*=\s*(.+);""")

        val match = reEqualityStatement.find(statement)
        return if (match != null) {
            val cleanStatement = match.groupValues[4] // e.g., someMethodCall()

            "assertThrows($exceptionClass, () -> $cleanStatement);"
        } else {
            null
        }
    }

    fun findAmbiguousReferences(errorLog: String): List<Pair<String, String>> {
        val pattern = Regex(""":\[(\d+),\d+] reference to (\w+) is ambiguous""")
        val results = mutableListOf<Pair<String, String>>()

        val logLines = errorLog.lines()

        for (line in logLines) {
            val match = pattern.find(line)
            if (match != null) {
                val lineNumber = match.groupValues[1]
                val className = match.groupValues[2]
                results.add(Pair(lineNumber, className))
            }
        }

        return results
    }

    fun findMethodsFromLines(file: File, lineNumbers: Set<Int>): Set<String> {
        val result = mutableSetOf<String>()

        val cu: CompilationUnit = StaticJavaParser.parse(file)
        val methods = cu.findAll(MethodDeclaration::class.java)

        for (line in lineNumbers) {
            for (method in methods) {
                val range = method.range.orElse(null)
                if (range != null && line in range.begin.line..range.end.line) {
                    result.add(method.nameAsString)
                    break
                }
            }
        }

        return result
    }

    /**
     * Scans the given filepath, and returns the number of methods that contain the @Test annotation
     */
    fun countTestMethods(filePath: String): Int {
        val file = File(filePath)
        val cu = StaticJavaParser.parse(file)

        return cu.findAll(MethodDeclaration::class.java)
            .count { method ->
                method.annotations.any { it.nameAsString == "Test" }
            }
    }

    /**
     * Scans the code content of a given class container,
     * and returns the number of methods that contain the @Test annotation
     */
    fun countTestMethods(classContainer: ClassContainer): Int {
        val cu = StaticJavaParser.parse(classContainer.getCompleteContent())

        return cu.findAll(MethodDeclaration::class.java)
            .count { method ->
                method.annotations.any { it.nameAsString == "Test" }
            }
    }
}