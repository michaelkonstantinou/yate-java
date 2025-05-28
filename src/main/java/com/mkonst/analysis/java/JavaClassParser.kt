package com.mkonst.analysis.java

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import com.mkonst.types.exceptions.CannotParseCodeException
import kotlin.jvm.Throws

class JavaClassParser: CodeClassParserInterface {
    private val packageRegex = Regex("""^\s*package\s+([\w.]+)\s*;?""")
    private val importRegex = Regex("""^\s*import.*""")

    /**
     * Attempts to parse the code and split the code into the following components: package, imports, body, methods
     * In case the code does not parse, the function will attempt to identify the components using regex and try again
     *
     * In the end, if the code is still not parsable, it will throw an exception
     */
    @Throws(CannotParseCodeException::class)
    override fun getBodyDecoded(classContent: String): ClassBody {
        var compilationUnit: CompilationUnit
        var packageName: String?
        var imports: MutableList<String>
        var classBody: String
        try {
            compilationUnit = parseContent(classContent)

            // 1. Package name
            packageName = compilationUnit.packageDeclaration.map { it.nameAsString }.orElse(null)

            // 2. Get all import statements (regardless of execution)
            imports = compilationUnit.imports.map { it.toString().trim() }.toMutableList()

            // 3. Class declaration + body: We'll rebuild it by removing package/imports and fixing minor typos
            classBody = getCleanBodyContent(classContent.trim(), packageName, imports)

            // Filter out import statements that are invalid (but somehow the parser did not catch them
            imports = filterInvalidImports(imports)
        } catch (e: CannotParseCodeException) {

            // Try to decode code using regex
            val lines = classContent.lines()
            val importLines = lines.filter { importRegex.matches(it) }.toMutableList()
            val packageLine = lines.firstOrNull { it.trim().startsWith("package") }

            packageName = packageLine?.let { line ->
                val match = packageRegex.find(line)
                match?.groupValues?.get(1)
            }

            imports = filterInvalidImports(importLines)
            classBody = getCleanBodyContent(classContent, packageName, importLines)

            // Attempt to parse code once again. If it doesn't work then fail
            compilationUnit = parseContent(classBody)
        }

        val methodModifiers = getMethodsWithModifiers(compilationUnit)
        val hasConstructors = hasConstructors(compilationUnit)

        return ClassBody(packageName, imports, methodModifiers, classBody, hasConstructors)
    }

    /**
     * Uses JavaParser to parse the given classContent. If successful, it returns a JavaParser CompilationUnit instance
     * In case of failure, it throws a CannotParseCodeException
     */
    @Throws(CannotParseCodeException::class)
    private fun parseContent(classContent: String): CompilationUnit {
        val parser = JavaParser()
        val result = parser.parse(classContent)

        if (!result.isSuccessful || !result.result.isPresent) {
            throw CannotParseCodeException()
        }

        val compilationUnit: CompilationUnit = result.result.get()

        return compilationUnit
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
        val linesToRemove: MutableList<String> = mutableListOf("package $packageName;")
        linesToRemove.addAll(imports)
        cleanContent = fullCode
                .lines()
                .filterNot { line -> linesToRemove.any { filter -> line.contains(filter) } }
                .joinToString("\n")

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

        return cleanContent.trimStart()
    }

    /**
     * Returns a new list of import statements that filtered out statements that won't compile in a java environment
     */
    private fun filterInvalidImports(imports: List<String>): MutableList<String> {
        val cleanImports: MutableList<String> = mutableListOf()

        for (import: String in imports) {

            val trimmed = import.trim()
            val trimmedWithoutComments = import.trim().substringBefore("//")

            // Skip empty lines and comment lines
            if (trimmed.isBlank() || trimmed.startsWith("//")) {
                continue
            }

            // Skip if import starts with a dot (e.g., "import .foo.Bar")
            if (trimmed.matches(Regex("""import\s+\..*"""))) {
                continue
            }

            // Skip if import statement contains a $ inside the class
            if (trimmedWithoutComments.contains("$")) {
                continue
            }

            // Skip if "import" is present but nothing follows, or it's malformed
            if (trimmed == "import"
                || trimmed == "import;"
                || trimmed.matches(Regex("""import\s*;"""))
                || trimmed.matches(Regex("""import\s*//.*"""))) {
                continue
            }

            cleanImports.add(trimmed)
        }

        return cleanImports
    }
}