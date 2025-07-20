package com.mkonst.analysis.kotlin

import com.mkonst.interfaces.analysis.CodeClassParserInterface
import com.mkonst.types.ClassBody
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class KotlinClassParser: CodeClassParserInterface {
    override fun getBodyDecoded(classContent: String): ClassBody {
        val disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        val psiFactory = KtPsiFactory(environment.project)
        val ktFile = psiFactory.createFile(classContent)

        // Find the package name and the import statements
        val packageName: String? = ktFile.packageFqName.asString().takeIf { it.isNotEmpty() }
        var imports: List<String> = ktFile.importList?.imports?.mapNotNull { it.importPath?.pathStr } ?: emptyList()
        imports = imports.toMutableList()

        val classDeclaration = ktFile.declarations.filterIsInstance<KtClassOrObject>().firstOrNull()
        val className = classDeclaration?.name

        // Get method names and their modifiers
        val methods = mutableMapOf<String, String>()
        classDeclaration?.declarations
            ?.filterIsInstance<KtNamedFunction>()
            ?.forEach { function ->
                val name = function.name ?: return@forEach
                val modifierList = function.modifierList

                val visibility = when {
                    modifierList == null -> "public"
                    modifierList.hasModifier(KtTokens.PRIVATE_KEYWORD) -> "private"
                    modifierList.hasModifier(KtTokens.PROTECTED_KEYWORD) -> "protected"
                    modifierList.hasModifier(KtTokens.PUBLIC_KEYWORD) -> "public"
                    else -> "public"
                }

                methods[name] = visibility
            }

        val hasConstructor = classDeclaration?.let { clazz ->
            val hasPrimary = clazz.primaryConstructor != null
            val hasSecondary = clazz.getBody()?.declarations
                ?.any { it is KtSecondaryConstructor } == true
            hasPrimary || hasSecondary
        } ?: false

        Disposer.dispose(disposable)

        return ClassBody(packageName, imports, methods, getCleanBodyContent(classContent, packageName, imports), hasConstructor)
    }

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

        // 3. Remove unexpected package declarations found in class
        val regexPackageInText = Regex("""\bpackage\b""", RegexOption.MULTILINE)
        val match = regexPackageInText.find(cleanContent)
        val unexpectedPackageLine = match?.value ?: ""
        cleanContent = cleanContent.replace(unexpectedPackageLine, "")

        return cleanContent.trimStart()
    }
}