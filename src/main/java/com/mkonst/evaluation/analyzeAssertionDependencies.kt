package com.mkonst.evaluation

import com.mkonst.analysis.MethodCallGraph
import com.mkonst.helpers.YateCodeUtils
import com.mkonst.services.MethodCallGraphProvider
import com.mkonst.types.ClassMethod

object AnalyzeAssertionDependencies {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath: String = "/Users/michael.konstantinou/Datasets/yate_evaluation/Windward/"
        val packageName = YateCodeUtils.getRootPackage(repositoryPath)
        val mcg: MethodCallGraph = MethodCallGraphProvider.getNewMethodCallGraph(repositoryPath, packageName)

        var nrAllCalls = 0
        var allMethods: MutableSet<ClassMethod> = mutableSetOf()
        var counter = 0
        val classMethodsWithOutsideCalls: MutableSet<ClassMethod> = mutableSetOf()

        for (edge in mcg.graph.edgeSet()) {
            nrAllCalls += 1

            val source = mcg.graph.getEdgeSource(edge)
            val target = mcg.graph.getEdgeTarget(edge)

            allMethods.add(source)
            if (source.className != target.className) {
                counter += 1
                classMethodsWithOutsideCalls.add(source)
            }
        }

        println("# all calls: $nrAllCalls")
        println("# outside calls: $counter")
        println("# all methods with calls: ${allMethods.size}")
        println("# methods with outside calls: ${classMethodsWithOutsideCalls.size}")
    }
}