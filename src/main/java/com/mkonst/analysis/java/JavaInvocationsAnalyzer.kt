package com.mkonst.analysis.java

import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import java.util.stream.Collectors

class JavaInvocationsAnalyzer(val repositoryPath: String) {
    private lateinit var model: CtModel

    fun getAllWrongUsagesLog(cutQualifiedName: String): String? {
        loadModel()
        val log: StringBuilder = StringBuilder()

        val wrongMethodCalls: String? = getWrongMethodCallsLog(cutQualifiedName)
        if (wrongMethodCalls !== null) {
            log.append(wrongMethodCalls)
        }

        val wrongMockUsage: String? = getWrongMockUsageLog(cutQualifiedName)
        if (wrongMockUsage !== null) {
            log.append(wrongMockUsage)
        }

        return if (log.isEmpty()) null else log.toString()
    }

    /**
     * Checks for invalid method call invocations, and generates a log report for it (if such invocations exist)
     */
    private fun getWrongMethodCallsLog(cutQualifiedName: String): String? {
        val log: StringBuilder = StringBuilder()
        val methodSignatures: Map<String, List<CtMethod<*>>> = extractMethodSignatures()

        for (clazz in model.getElements<CtElement> { e: CtElement? -> e is CtClass<*> }) {
            val traversedClass = clazz as CtClass<*>

            if (traversedClass.qualifiedName == cutQualifiedName) {
                for (elInvocation in traversedClass.getElements<CtElement> { e: CtElement? -> e is CtInvocation<*> }) {
                    val invocation = elInvocation as CtInvocation<*>
                    log.append(getInvalidInvocations(invocation, methodSignatures))
                }
            }
        }

        return if (log.isEmpty()) null else log.toString()
    }

    /**
     * The function checks the repository (including Tests) for any incorrect invocations of mock objects
     * Returns the findings as a string, in the form of a log output
     */
    private fun getWrongMockUsageLog(cutQualifiedName: String): String? {
        val log: StringBuilder = StringBuilder()

        // Find all method signatures (To check whether any of the mock calls use them)
        val methodSignatures: Map<String, List<CtMethod<*>>> = extractMethodSignatures()

        // Find all test classes
        for (elTestClass in model.getElements<CtElement> { e: CtElement? -> e is CtClass<*> }) {
            val testClass = elTestClass as CtClass<*>

            // Make sure that this applies only to the class under test (typically the test class)
            if (testClass.qualifiedName != cutQualifiedName) {
                continue
            }

            // Iterate all cut methods and search for invalid mock usages
            for (method in testClass.methods) {
                val invocations = method.body
                        .getElements { e: CtInvocation<*>? -> e is CtInvocation<*> }

                for (invocation in invocations) {
                    if (isChainedInvocation(invocation, "when", "thenReturn")) {
                        log.append(findWrongMockCallUsages(invocation, methodSignatures))
                    }
                }
            }
        }

        return if (log.isEmpty()) null else log.toString()
    }

    private fun extractMethodSignatures(): Map<String, MutableList<CtMethod<*>>> {
        val methodSignatures: MutableMap<String, MutableList<CtMethod<*>>> = HashMap()

        for (clazz in model.getElements<CtElement> { e: CtElement? -> e is CtClass<*> }) {
            val traversedClass = clazz as CtClass<*>

            for (method in traversedClass.methods) {
                val key: String = getMethodKey(traversedClass.qualifiedName, method.simpleName)
                methodSignatures.putIfAbsent(key, ArrayList())
                methodSignatures[key]!!.add(method)
            }
        }

        return methodSignatures
    }

    private fun getMethodKey(className: String, methodName: String): String {
        return "$className.$methodName"
    }

    private fun getInvalidInvocations(invocation: CtInvocation<*>, methodSignatures: Map<String, List<CtMethod<*>>>): StringBuilder {
        val log: StringBuilder = StringBuilder()

        // A calledMethod is required to analyze the invocation.
        val calledMethod = invocation.executable ?: return log

        // Get trivial information such as the methodName and the lineNumber
        val methodName = calledMethod.simpleName
        val lineNumber = if (invocation.position.isValidPosition) invocation.position.line else -1
        if (lineNumber < 0) {
            return log
        }

        // Ignore assertion statements
        if (methodName.startsWith("assert")) {
            return log
        }

        // If no declaringType is found, is possible that the method does not exist as no class can be found
        if (calledMethod.declaringType === null) {
            log.appendLine("Line: $lineNumber - Method does not exist: $methodName")

            return log
        }

        val className = calledMethod.declaringType.qualifiedName
        val key: String = getMethodKey(className, methodName)
        val matchingMethods = methodSignatures[key]

        // Extract the actual usage of the method
        val actualMethodCall = invocation.toString()

        if (matchingMethods == null) {
            log.appendLine("Line: $lineNumber - Method does not exist: $className.$methodName")
        } else {
            var valid = false
            for (method in matchingMethods) {
                if (argumentsMatch(method, invocation.arguments)) {
                    valid = true
                    break
                }
            }
            if (!valid) {
                log.appendLine("[Line: $lineNumber] $actualMethodCall - Incorrect arguments")
                log.appendLine("   ➜ Given arguments: " + getArgumentsString(invocation.arguments))
                log.appendLine("   ➜ The correct method signatures are the following:")
                for (method in matchingMethods) {
                    log.appendLine("      ➜ " + getMethodSignature(method))
                }
            }
        }

        return log
    }

    /**
     * The function checks the invocation occurred for mock calls of type when().return()
     * Prints out the wrong usages: Argument mismatch or calling non-existing methods
     * @param thenReturnCall
     * @param methodSignatures
     */
    private fun findWrongMockCallUsages(thenReturnCall: CtInvocation<*>, methodSignatures: Map<String, List<CtMethod<*>>>): StringBuilder {
        val log: StringBuilder = StringBuilder()
        val whenCall = thenReturnCall.target as CtInvocation<*>

        // Ensure when(...) has at least one argument
        if (whenCall.arguments.isEmpty()) {
            // when() has no arguments

            return log
        }


        // Ensure the first argument is a method invocation
        val firstArg: CtElement = whenCall.arguments[0]
        if (firstArg !is CtInvocation<*>) {
            // when() is used with non-method argument

            return log;
        }

        // Check whether the invoked method actually exists
        val lineNumber = if (whenCall.position.isValidPosition) whenCall.position.line else -1

        if (firstArg.executable.declaringType == null) {
            log.append("[Line: $lineNumber] $firstArg - This method does not exist\n")

            return log
        }

        val className: String = firstArg.executable.declaringType.qualifiedName
        val methodName: String = firstArg.executable.simpleName
        val key: String = getMethodKey(className, methodName)
        val matchingMethods = methodSignatures[key]
        if (matchingMethods == null) {
            log.append("[Line: $lineNumber] $firstArg - This method does not exist\n")

            return log
        }

        // Expected type from `when(...)`
        val expectedReturnType = firstArg.type

        // Extract the argument passed to `thenReturn(...)`
        val providedReturnType = thenReturnCall.arguments[0].type

        if (expectedReturnType != null && providedReturnType != null && !providedReturnType.isSubtypeOf(expectedReturnType)) {
            log.append("[Line: $lineNumber] $thenReturnCall - Incorrect mock usage:").append("\n")
            log.append("   ➜ Expected return type: " + expectedReturnType.qualifiedName).append("\n")
            log.append("   ➜ Provided type: " + providedReturnType.qualifiedName).append("\n")
        }

        return log
    }

    /**
     * Returns whether the arguments passed to the method match any method signature
     */
    private fun argumentsMatch(method: CtMethod<*>, arguments: List<CtExpression<*>>): Boolean {
        val expectedParams = method.parameters.stream()
                .map { p: CtParameter<*> -> p.type }.collect(Collectors.toList())

        if (expectedParams.size != arguments.size) return false

        if (!arguments.isEmpty() && arguments[0].toString() == "any()") return true

        for (i in expectedParams.indices) {
            val expectedType = expectedParams[i]
            val providedType = arguments[i].type
            if ((providedType == null || !providedType.isSubtypeOf(expectedType)) && providedType.toString() != "<nulltype>") {
                return false
            }
        }
        return true
    }

    private fun getArgumentsString(arguments: List<CtExpression<*>>): String {
        if (arguments.size == 0) {
            return "()"
        }

        return arguments.stream()
                .map { arg: CtExpression<*> -> if (arg.type != null) arg.type.qualifiedName else "UNKNOWN" }
                .collect(Collectors.joining(", "))
    }

    private fun getMethodSignature(method: CtMethod<*>): String {
        val params = method.parameters.stream()
                .map { p: CtParameter<*> -> p.type.qualifiedName }
                .collect(Collectors.joining(", "))
        return method.simpleName + "(" + params + ")"
    }

    private fun isChainedInvocation(invocation: CtInvocation<*>, outerMethod: String, innerMethod: String): Boolean {
        if (invocation.executable.simpleName != innerMethod) return false

        // Check if the target of `thenReturn()` is an invocation of `when()`
        if (invocation.target is CtInvocation<*>) {
            val whenCall = invocation.target as CtInvocation<*>
            return whenCall.executable.simpleName == outerMethod
        }

        return false
    }

    /**
     * The class is based on Spoon, and Spoon's model needs to be reloaded every time there are changes in the code
     * Since we are interested in recent test changes, the model needs to be loaded before using it
     */
    private fun loadModel() {
        // Initialize Spoon
        val launcher = Launcher()
        launcher.addInputResource(repositoryPath + "src/main")
        launcher.addInputResource(repositoryPath + "src/test")
        launcher.buildModel()

        // Get the Spoon model
        model = launcher.model
    }
}