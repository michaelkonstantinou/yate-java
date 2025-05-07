package com.mkonst.analysis.java

import com.mkonst.types.ClassMethod
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import java.util.function.Consumer
import java.util.function.Predicate

class JavaMethodProvider(val repositoryPath: String) {
    private var model: CtModel
    private val classMethods: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    init {
        // Initialize Spoon
        val launcher = Launcher()
        launcher.addInputResource(repositoryPath + "src/main")
        launcher.buildModel()

        // Get the Spoon model
        model = launcher.model

        findAllMethodBodies()
    }

    fun getMethodBody(classMethod: ClassMethod): String? {
        if (classMethods.isEmpty()) {
            return null
        }

        return classMethods[classMethod.className]?.get(classMethod.methodName)
    }

    private fun findAllMethodBodies() {
        // Find the class
        model.getElements<CtElement> { e: CtElement? -> e is CtClass<*> }
                .stream()
                .map { e: CtElement? -> e as CtClass<*>? } // If a class under test is specified, then query only methods of specific class. Otherwise accept all
                .forEach { c ->
                    if (c === null) {
                        return@forEach
                    }

                    val methodsList = mutableMapOf<String, String>()

                    // Extract methods and their bodies
                    for (method in c.methods) {
                        methodsList[method.simpleName] = method.toString()
                    }

                    classMethods[c.qualifiedName] = methodsList
                }
    }
}