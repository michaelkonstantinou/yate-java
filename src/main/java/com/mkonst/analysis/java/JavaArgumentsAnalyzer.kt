package com.mkonst.analysis.java

import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtType

class JavaArgumentsAnalyzer(val repositoryPath: String, val packageName: String) {
    private var model: CtModel

    init {
        // Initialize Spoon
        val launcher = Launcher()
        launcher.addInputResource(repositoryPath + "src/main")
        launcher.buildModel()

        // Get the Spoon model
        model = launcher.model
    }

    fun getClassesInArgumentsLog(cut: String): String? {
        val resultsLog: StringBuilder = StringBuilder()

        // Store the classes that are relevant for the results log (so the model knows what to instantiate)
        val relevantClasses: MutableSet<String> = mutableSetOf()

        val type = getClassTypeInstance(cut)
        if (type is CtClass<*>) {

            // Extract relevant classes from constructors
            for (constructor in type.constructors) {
                for (param in constructor.parameters) {
                    val paramQualifiedName = param.type.toString()
                    if (paramQualifiedName.startsWith(packageName)) {
                        relevantClasses.add(paramQualifiedName)
                        resultsLog.append("One of the class' constructors makes use of the following object: $paramQualifiedName\n")
                    }
                }
            }

            // Extract relevant classes from methods
            for (method in type.methods) {
                for (param in method.parameters) {
                    val paramQualifiedName = param.type.toString()
                    if (paramQualifiedName.startsWith(packageName)) {
                        relevantClasses.add(paramQualifiedName)
                        resultsLog.append("One of the class' methods makes use of the following object: $paramQualifiedName\n")
                    }
                }
            }

            // Now get the constructors for each relevant class and print them to the console
            resultsLog.append(getConstructorImplementationsLog(relevantClasses))
        }

        return if (resultsLog.isEmpty()) null else resultsLog.toString()
    }

    private fun getClassTypeInstance(qualifiedName: String): CtType<*> {
        return model.allTypes.stream()
                .filter { t: CtType<*> -> t.qualifiedName == qualifiedName }
                .findFirst()
                .orElse(null)
    }

    /**
     * Generates a log text message that contains the constructor implementations of the relevantClasses.
     * In case a relevantClass given is an interface, then the same process is repeated for every class that
     * implements the interface.
     */
    private fun getConstructorImplementationsLog(relevantClasses: Set<String>): String {
        val log: StringBuilder = StringBuilder()

        for (relevantClass in relevantClasses) {
            val relevantClassType = getClassTypeInstance(relevantClass)

            if (relevantClassType is CtClass<*>) {
                log.append("Object ${relevantClassType.getQualifiedName()} can be instantiated with one of the following constructors (or mocked if the types are not primitive)\n")

                // Extract relevant classes from constructors
                for (constructor in relevantClassType.constructors) {
                    log.append(constructor.signature).append(constructor.body).append("\n")
                }
            } else if (relevantClassType.isInterface) {
                log.append("${relevantClassType.qualifiedName} is an interface and the following classes implement it\n")

                // Find all the class implementations of the interfaces and repeat the process for it
                val classImplementations = getInterfaceImplementations(relevantClassType.qualifiedName)
                log.append(getConstructorImplementationsLog(classImplementations))
            }
        }

        return log.toString()
    }

    /**
     * Finds all classes that implement the given interface and returns them as a Set<String>
     */
    private fun getInterfaceImplementations(qualifiedName: String): Set<String> {
        val classImplementations: MutableSet<String> = HashSet()

        // Loop through all classes in the model
        for (type in model.allTypes) {
            if (type is CtClass<*>) { // Ensure it's a class (not another interface)
                val clazz = type

                // Check if the class implements the target interface
                for (ref in clazz.superInterfaces) {
                    if (ref.qualifiedName == qualifiedName) {
                        println("  - " + clazz.qualifiedName)
                        classImplementations.add(clazz.qualifiedName)
                    }
                }
            }
        }

        return classImplementations
    }

}