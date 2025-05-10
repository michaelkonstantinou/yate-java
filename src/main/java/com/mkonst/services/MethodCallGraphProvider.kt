package com.mkonst.services

import com.mkonst.analysis.MethodCallGraph
import spoon.Launcher
import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtElement

object MethodCallGraphProvider {
    /**
     * The function is responsible for analyzing the given repository and generating
     * a new MethodCallGraph instance.
     * Throws an exception if an error occurred during the analysis
     */
    fun getNewMethodCallGraph(repositoryPath: String, packageName: String?): MethodCallGraph {
        var repositoryPath = repositoryPath
        val methodCallGraph = MethodCallGraph()

        // Make sure repository path ends in a /
        if (!repositoryPath.endsWith("/")) {
            repositoryPath = "$repositoryPath/"
        }

        // Initialize a new Spoon code analysis model
        val launcher = Launcher()
        launcher.addInputResource(repositoryPath + "src/main")
        launcher.buildModel()
        val model = launcher.model

        // Iterate all methods and find all method invocations
        for (type in model.allTypes) {
            for (method in type.methods) {
                val caller = type.qualifiedName + "#" + method.simpleName

                // For each method invocation append a new edge to the MethodCallGraph instance
                for (invocation in method.getElements<CtElement> { e: CtElement? -> e is CtInvocation<*> }) {
                    val invokedInstance = invocation as CtInvocation<*>

                    try {
                        val calleePackageClass = invokedInstance.executable.declaringType.qualifiedName

                        // Filter only the classes that belong to the repository (exclude java generic libraries)
                        if (calleePackageClass.contains(packageName!!)) {
                            val callee = calleePackageClass + "#" + invokedInstance.executable.simpleName
                            methodCallGraph.addEdge(caller, callee)
                        }
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        return methodCallGraph
    }
}
