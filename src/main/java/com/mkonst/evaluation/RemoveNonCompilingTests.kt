package com.mkonst.evaluation

import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateIO
import com.mkonst.services.ErrorService
import com.mkonst.services.PiTestService
import com.mkonst.types.DependencyTool
import com.mkonst.types.coverage.MutationScore

object RemoveNonCompilingTests {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/chesslib/"
        val errorService = ErrorService(repositoryPath)
        val (filepathsByMethods, filepathsByImports) = errorService.findNonCompilingClassesRegex(DependencyTool.MAVEN)

        // Remove invalid methods
        var totalRemovedMethods = 0
        for ((testClassPath, invalidMethods) in filepathsByMethods) {
            totalRemovedMethods += invalidMethods.size
            var content: String = YateIO.readFile(testClassPath)

            for (method in invalidMethods) {
                content = content.replace(method, "")
            }

            YateConsole.debug("Removing #${invalidMethods.size} methods in file $testClassPath")
            YateIO.writeFile(testClassPath, content)
        }

        var nrIncorrectImports = 0
        var nrIncorrectFiles = 0

        for ((testClass, imports) in filepathsByImports) {
            nrIncorrectImports += imports.size
            nrIncorrectFiles += 1
            YateIO.deleteFile(testClass)
        }

        println("Removed imports: $nrIncorrectImports")
        println("Removed files: $nrIncorrectFiles")
    }
}