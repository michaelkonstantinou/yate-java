package com.mkonst.analysis.java

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody

class JavaClassParser: CodeClassParserInterface {

    override fun getBodyDecoded(classContent: String): ClassBody {
        val parser = JavaParser()
        val result = parser.parse(classContent)

        if (!result.isSuccessful || !result.result.isPresent) {
            throw IllegalArgumentException("Failed to parse Java code")
        }

        val compilationUnit: CompilationUnit = result.result.get()

        // 1. Package name
        val packageName = compilationUnit.packageDeclaration.map { it.nameAsString }.orElse(null)

        // 2. Imports
        val imports = compilationUnit.imports.map { it.toString().trim() }.toMutableList()

        // 3. Class declaration + body
        // We'll rebuild it by removing package/imports and fixing minor typos
        val classBody: String = getCleanBodyContent(classContent.trim(), packageName, imports)

        val methodModifiers = getMethodsWithModifiers(compilationUnit)
        val hasConstructors = hasConstructors(compilationUnit)

        return ClassBody(packageName, imports, methodModifiers, classBody, hasConstructors)
    }

    /**
     * Iterates all methods and returns a map of the methods' name -> modifier (public, private...)
     */
    private fun getMethodsWithModifiers(compilationUnit: CompilationUnit): MutableMap<String, String> {
        val methodModifiers = mutableMapOf<String, String>()
        compilationUnit.findAll(com.github.javaparser.ast.body.MethodDeclaration::class.java).forEach { method ->
            val modifier = when {
                method.isPublic -> "public"
                method.isPrivate -> "private"
                method.isProtected -> "protected"
                else -> "package-private"
            }
            methodModifiers[method.nameAsString] = modifier
        }

        return methodModifiers
    }

    /**
     * Checks whether the first class declaration of the CompilationUnit instance contains at least 1 constructor
     */
    private fun hasConstructors(compilationUnit: CompilationUnit): Boolean {
        // Find the first class or interface declaration
        val classDeclaration = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration::class.java)

        // Check if it has any constructors and return the outcome
        return classDeclaration
                .map { cls -> cls.constructors.isNotEmpty() }
                .orElse(false)
    }

    /**
     * The method attempts to return only the part of the class that contains the class declaration and implementation
     * In other words, package name, import statements and common faulty statements are removed
     *
     * For test methods, it attempts to fix any method signatures that do not follow a typical unit test format
     */
    private fun getCleanBodyContent(fullCode: String, packageName: String?, imports: MutableList<String>): String {
        var cleanContent = ""

        // 1. Remove package and import statements from class content
        cleanContent = fullCode.replace("package $packageName;", "")
        for (importStatement: String in imports) {
            cleanContent = cleanContent.replace(importStatement, "")
        }

        // 2. Remove faulty import statements like: import .something;
        val patternIncorrectImport = Regex("""^\s*import\s+\.\w.*\n""", RegexOption.MULTILINE)
        cleanContent = cleanContent.replace(patternIncorrectImport, "")

        // 3. Add 'public' modifier to test void methods missing a visibility modifier
        val patternFunctionWithMissingModifier = Regex(
                """(^\s*)(?!public|private|protected)(static\s+)?void\s+(\w+)\s*\(""",
                RegexOption.MULTILINE
        )

        cleanContent = patternFunctionWithMissingModifier.replace(cleanContent) { matchResult ->
            val indentation = matchResult.groupValues[1]
            val staticPart = matchResult.groupValues[2]
            val methodName = matchResult.groupValues[3]
            indentation + "public " + (staticPart.ifBlank { "" }) + "void $methodName("
        }

        // 4. Ensure test methods contain 'throws' clause
        val testMethodPattern = Regex(
                """^\s*public\s+void\s+test[\w]*\([^\)]*\)\s*(throws\s+[a-zA-Z_][a-zA-Z0-9_]*\s*)?(?=\s*\{)""",
                RegexOption.MULTILINE
        )

        cleanContent = testMethodPattern.replace(cleanContent) { matchResult ->
            val signature = matchResult.value
            if (!signature.contains("throws")) {
                "$signature throws Throwable"
            } else {
                signature
            }
        }

        // 5. Remove unexpected package declarations found in class
        val regexPackageInText = Regex("""\bpackage\b""", RegexOption.MULTILINE)
        val match = regexPackageInText.find(cleanContent)
        val unexpectedPackageLine = match?.value ?: ""
        cleanContent = cleanContent.replace(unexpectedPackageLine, "")

        return cleanContent
    }
}