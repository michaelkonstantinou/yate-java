package com.mkonst.services;

import com.mkonst.analysis.MethodCallGraph;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.Launcher;

public class MethodCallGraphService {

    /**
     * The function is responsible for analyzing the given repository and generating
     * a new MethodCallGraph instance.
     * Throws an exception if an error occurred during the analysis
     */
    public static MethodCallGraph getNewMethodCallGraph(String repositoryPath, String packageName) {
        MethodCallGraph methodCallGraph = new MethodCallGraph();

        // Make sure repository path ends in a /
        if (! repositoryPath.endsWith("/")) {
            repositoryPath = repositoryPath + "/";
        }

        // Initialize a new Spoon code analysis model
        Launcher launcher = new Launcher();
        launcher.addInputResource(repositoryPath + "src/main");
        launcher.buildModel();
        CtModel model = launcher.getModel();

        // Iterate all methods and find all method invocations
        for (CtType<?> type : model.getAllTypes()) {
            for (CtMethod<?> method : type.getMethods()) {
                String caller = type.getQualifiedName() + "#" + method.getSimpleName();

                // For each method invocation append a new edge to the MethodCallGraph instance
                for (CtElement invocation : method.getElements(e -> e instanceof CtInvocation)) {
                    CtInvocation<?> invokedInstance = (CtInvocation<?>) invocation;

                    try {
                        String calleePackageClass = invokedInstance.getExecutable().getDeclaringType().getQualifiedName();

                        // Filter only the classes that belong to the repository (exclude java generic libraries)
                        if (calleePackageClass.contains(packageName)) {
                            String callee = calleePackageClass + "#" + invokedInstance.getExecutable().getSimpleName();
                            methodCallGraph.addEdge(caller, callee);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return methodCallGraph;
    }
}
