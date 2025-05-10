package com.mkonst.analysis.java

import com.mkonst.helpers.YateJavaUtils.classPackageExists
import spoon.Launcher
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import spoon.reflect.reference.CtExecutableReference
import spoon.reflect.reference.CtTypeReference
import spoon.reflect.visitor.filter.TypeFilter

class JavaImportsAnalyzer(val repositoryPath: String, val packageName: String) {

    /**
     * Iterates the given currentImports list and checks whether any of the import statements to do reflect to
     * a valid class path within the given repository. Keep the packagePrefix consistent with the repository path
     * in order for the method to work
     */
    fun getInvalidPackageImports(
        currentImports: List<String>
    ): MutableList<String> {
        val invalidPackageImports: MutableList<String> = ArrayList()

        for (importStatement in currentImports) {
            val cleanPackage = importStatement
                .replace("import ", "")
                .replace("static", "")
                .replace(";", "")
                .trim()

            if (!cleanPackage.endsWith(".*") && cleanPackage.startsWith(packageName)) {
                // Check for invalid imports

                if (!classPackageExists(repositoryPath, cleanPackage)) {
                    invalidPackageImports.add(importStatement)
                }
            }
        }

        return invalidPackageImports
    }

    /**
     * The function will find all suggested libraries for import and export them to a json file
     *
     * @param targetClass
     */
    fun getSuggestedImports(targetClass: String): MutableList<String> {
        val suggestedImports: MutableList<String> = mutableListOf()

        // Split the string based on ".", and join all elements except the last one (the class name)
        val parts = targetClass.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val classPackage = java.lang.String.join(".", *parts.copyOf(parts.size - 1))

        val classMethodsWithImports = findImportsFromMethodCalls(classPackage)

        for ((classQualifyingName, imports) in classMethodsWithImports) {
            if (classQualifyingName == targetClass) {
                // Results section
                for (importStatement in imports) {
                    if (importStatement.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1) {
                        suggestedImports.add(importStatement)
                    }
                }
            }
        }

        return suggestedImports
    }

    /**
     * Returns whether the given type is one of Java's primitive types (boolean, byte etc...)
     */
    private fun isPrimitive(typeName: String): Boolean {
        return typeName == "boolean" || typeName == "byte" || typeName == "char" || typeName == "short" || typeName == "int" || typeName == "long" || typeName == "float" || typeName == "double" || typeName == "<nulltype>"
    }

    /**
     * Finds all classes/libraries that the methods make use of, and exports them to a HashMap
     *
     * @return
     */
    private fun findImportsFromMethodCalls(
        classPackage: String
    ): Map<String, MutableSet<String>> {
        // Create a Map to store results
        val classMethods: MutableMap<String, MutableSet<String>> = HashMap()

        val launcher = Launcher()
        launcher.addInputResource(repositoryPath)

        // Enable Spoon to analyze without full classpath
        launcher.environment.noClasspath = true

        // Build the Spoon model
        val model = launcher.buildModel()

        // Collect all classes in the repository
        val allClasses: MutableMap<String, String> = HashMap()
        for (clazz in model.allTypes) {
            allClasses[clazz.simpleName] = clazz.qualifiedName
        }


        // Process each class
        for (clazz in model.getElements<CtElement> { e: CtElement? -> e is CtClass<*> }.stream()
            .map { e: CtElement? -> e as CtClass<*>? }
            .toList()) {
//            System.out.println("🔍 Analyzing class: " + clazz.getQualifiedName());

            // Set to store missing imports

            val missingImports: MutableSet<String> = HashSet()
            val missingImportsWithoutPackage: MutableSet<String> = HashSet()

            // Get the compilation unit (file) of the class
            // Skip if not found
            val compilationUnit = clazz?.position?.compilationUnit ?: continue

            // Process each method in the class
            for (method in clazz.methods) {

                // Find missing types
                for (ref in method.getElements<CtTypeReference<*>>(
                    TypeFilter<CtTypeReference<*>>(
                        CtTypeReference::class.java
                    )
                )) {
                    if (ref.declaration == null) { // If Spoon couldn't resolve it

                        // Check whether Spoon did not find an import statement with a valid package
                        if (ref.getPackage() != null && !ref.getPackage().qualifiedName.startsWith("org.") && !ref.getPackage().qualifiedName.startsWith(
                                "com."
                            ) && !ref.getPackage().qualifiedName.startsWith("com.")
                        ) {
                            missingImportsWithoutPackage.add(
                                ref.qualifiedName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[0])
                        } else if (ref.getPackage() != null && !isPrimitive(ref.qualifiedName) && ref.getPackage().qualifiedName != classPackage) {

                            // Excludes primitive types and types from the same package
                            missingImports.add(ref.qualifiedName)
                        }
                    }
                }

                // Find missing method calls
                for (ref in method.getElements<CtExecutableReference<*>>(
                    TypeFilter<CtExecutableReference<*>>(
                        CtExecutableReference::class.java
                    )
                )) {
                    if (ref.declaringType != null && ref.declaringType.declaration == null) {
                        missingImports.add(ref.declaringType.qualifiedName)
                    }
                }
            }

            // Append missing imports to the file
            for (importName in missingImports) {
                if (!importName.startsWith("java.lang")) { // Ignore built-in classes
                    classMethods[clazz.qualifiedName] = missingImports
                }
            }

            // Find missing imports without a package
            for (importName in missingImportsWithoutPackage) {
                if (!importName.startsWith("java.lang")) { // Ignore built-in classes

                    // Resolve missing imports
                    if (allClasses.containsKey(importName) && allClasses[importName] !== null) {
                        classMethods[clazz.qualifiedName]!!.add(allClasses[importName]!!)
                    }
                }
            }
        }

        return classMethods
    }
}