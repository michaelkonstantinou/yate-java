package com.mkonst.evaluation

import com.mkonst.helpers.YateIO
import com.mkonst.services.ErrorService
import com.mkonst.services.PiTestService
import com.mkonst.types.DependencyTool
import com.mkonst.types.coverage.MutationScore

object RemoveNonCompilingTests {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/ConfigMe/"
        val errorService = ErrorService(repositoryPath)
        val filepathsByImports = errorService.findNonCompilingClasses(DependencyTool.MAVEN)
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