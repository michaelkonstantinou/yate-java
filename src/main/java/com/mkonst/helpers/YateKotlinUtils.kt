package com.mkonst.helpers

import com.mkonst.analysis.ClassContainer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

object YateKotlinUtils {

    /**
     * Reads the given class-path and scans the code content to find,
     * and return the number of methods that contain the @Test annotation
     *
     * Makes use of function countTestMethodsForContent
     */
    fun countTestMethods(classPath: String): Int {
        return countTestMethodsForContent(YateIO.readFile(classPath))
    }

    /**
     * Scans the code content of a given class container,
     * and returns the number of methods that contain the @Test annotation
     *
     * Makes use of function countTestMethodsForContent
     */
    fun countTestMethods(classContainer: ClassContainer): Int {
        return countTestMethodsForContent(classContainer.getCompleteContent())
    }

    /**
     * Uses Kotlin PSI to parse the class' content and find all methods that have the @Test annotation
     */
    fun countTestMethodsForContent(classContent: String): Int {
        val disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        val psiFactory = KtPsiFactory(environment.project)
        val ktFile = psiFactory.createFile(classContent)
        val allFunctions = ktFile.collectDescendantsOfType<KtNamedFunction>()

        val testAnnotatedCount = allFunctions.count { function ->
            function.annotationEntries.any { annotation ->
                annotation.shortName?.asString() == "Test"
            }
        }

        Disposer.dispose(disposable)
        return testAnnotatedCount
    }
}