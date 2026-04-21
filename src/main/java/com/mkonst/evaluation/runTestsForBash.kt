package com.mkonst.evaluation

import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateJavaExecution
import com.mkonst.types.DependencyTool

object runTestsForBash {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath: String = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/"
        val packageName = YateCodeUtils.getRootPackage(repositoryPath)

        println(YateJavaExecution.runTestsForErrors(repositoryPath, DependencyTool.MAVEN))
    }
}