package com.mkonst.analysis.java;

import com.mkonst.helpers.YateJavaUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

public class JavaImportsAnalyzer {

    /**
     * Iterates the given currentImports list and checks whether any of the import statements to do reflect to
     * a valid class path within the given repository. Keep the packagePrefix consistent with the repository path
     * in order for the method to work
     */
    public static List<String> getInvalidPackageImports(String repositoryPath, String packagePrefix, List<String> currentImports) {
        List<String> invalidPackageImports = new ArrayList<>();

        for (String importStatement : currentImports) {
            String cleanPackage = importStatement
                    .replace("import ", "")
                    .replace("static", "")
                    .replace(";", "")
                    .strip();

            if (!cleanPackage.endsWith(".*") && cleanPackage.startsWith(packagePrefix)) {

                // Check for invalid imports
                if (!YateJavaUtils.INSTANCE.classPackageExists(repositoryPath, cleanPackage)) {
                    invalidPackageImports.add(importStatement);
                }
            }
        }

        return invalidPackageImports;
    }

    /**
     * The function will find all suggested libraries for import and export them to a json file
     *
     * @param repositoryPath
     * @param targetClass
     */
    public static List<String> getSuggestedImports(String repositoryPath, String targetClass) {
        List<String> suggestedImports = new ArrayList<>();

        // Split the string based on ".", and join all elements except the last one (the class name)
        String[] parts = targetClass.split("\\.");
        String classPackage = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));

        Map<String, Set<String>> classMethodsWithImports = findImportsFromMethodCalls(repositoryPath, classPackage);

        for (Map.Entry<String, Set<String>> entry : classMethodsWithImports.entrySet()) {
            String classQualifyingName = entry.getKey();
            if (classQualifyingName.equals(targetClass)) {
                Set<String> imports = entry.getValue();

                // Results section
                for (String importStatement : imports) {
                    if (importStatement.split("\\.").length > 1) {
                        suggestedImports.add(importStatement);
                    }
                }
            }

        }

        return suggestedImports;
    }

    /**
     * The function checks the code of the class one by one and attempts to find any classes that were perhaps not
     * imported in the header. It then tries to find the possible missing import statement by scanning the repository.
     *
     * @param repositoryPath
     * @param targetClass
     * @return
     */
    public static Set<String> getMissingImportStatements(String repositoryPath, String targetClass) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(repositoryPath);
        launcher.getEnvironment().setNoClasspath(true);
        CtModel model = launcher.buildModel();

        Set<String> usedTypes = new HashSet<>();
        Set<String> importedTypes = new HashSet<>();
        Set<String> wildcardImports = new HashSet<>();

        // Traverse all classes
        for (CtElement elClass : model.getElements(e -> e instanceof CtType<?>)) {
            CtType<?> clazz = (CtType<?>) elClass;
            if (!clazz.getQualifiedName().equals(targetClass)) continue;

            // Collect explicit imports and wildcard imports
            for (CtElement elImport : model.getElements(e -> e instanceof CtImport)) {
                CtImport ctImport = (CtImport) elImport;

                String importRef = ctImport.getReference().toString();
                if (importRef.endsWith(".*")) {
                    wildcardImports.add(importRef.replace(".*", ""));
                } else {
                    importedTypes.add(importRef);
                }
            }

            // Find method calls and add their declaring classes
            for (CtElement elInvocation : clazz.getElements(e -> e instanceof CtInvocation<?>)) {
                CtInvocation<?> invocation = (CtInvocation<?>) elInvocation;
                CtExecutableReference<?> execRef = invocation.getExecutable();
                if (execRef.getDeclaringType() != null) {
                    usedTypes.add(execRef.getDeclaringType().getQualifiedName());
                }
            }

            // Find static field accesses (e.g., Const.ISTORE)
            for (CtElement elFieldAccess : clazz.getElements(e -> e instanceof CtFieldAccess<?>)) {
                CtFieldAccess<?> fieldAccess = (CtFieldAccess<?>) elFieldAccess;
                CtFieldReference<?> fieldRef = fieldAccess.getVariable();
                if (fieldRef.getDeclaringType() != null) {
                    usedTypes.add(fieldRef.getDeclaringType().getQualifiedName());
                }
            }
        }

        // **Detect missing imports**
        Set<String> missingImports = new HashSet<>();
        for (String usedType : usedTypes) {
            if (!importedTypes.contains(usedType)) {
                boolean coveredByWildcard = wildcardImports.stream()
                        .anyMatch(wildcard -> usedType.startsWith(wildcard + "."));

                if (!coveredByWildcard) {
                    missingImports.add(usedType);
                }
            }
        }

        // **Print missing imports**
        System.out.println("🚨 Missing Imports:");
        for (String missing : missingImports) {
            System.out.println("   ➜ " + missing);
        }

        return missingImports;
    }

    /**
     * Returns whether the given type is one of Java's primitive types (boolean, byte etc...)
     */
    private static boolean isPrimitive(String typeName) {
        return typeName.equals("boolean") || typeName.equals("byte") || typeName.equals("char") ||
                typeName.equals("short") || typeName.equals("int") || typeName.equals("long") ||
                typeName.equals("float") || typeName.equals("double") || typeName.equals("<nulltype>");
    }

    /**
     * Finds all classes/libraries that the methods make use of, and exports them to a HashMap
     *
     * @return
     */
    private static Map<String, Set<String>> findImportsFromMethodCalls(String repositoryPath, String classPackage) {
        // Create a Map to store results
        Map<String, Set<String>> classMethods = new HashMap<>();

        Launcher launcher = new Launcher();
        launcher.addInputResource(repositoryPath);

        // Enable Spoon to analyze without full classpath
        launcher.getEnvironment().setNoClasspath(true);

        // Build the Spoon model
        CtModel model = launcher.buildModel();

        // Collect all classes in the repository
        Map<String, String> allClasses = new HashMap<>();
        for (CtType<?> clazz : model.getAllTypes()) {
            allClasses.put(clazz.getSimpleName(), clazz.getQualifiedName());
        }


        // Process each class
        for (CtClass<?> clazz : model.getElements(e -> e instanceof CtClass<?>).stream().map(e -> (CtClass<?>) e).toList()) {
//            System.out.println("🔍 Analyzing class: " + clazz.getQualifiedName());

            // Set to store missing imports
            Set<String> missingImports = new HashSet<>();
            Set<String> missingImportsWithoutPackage = new HashSet<>();

            // Get the compilation unit (file) of the class
            CtCompilationUnit compilationUnit = clazz.getPosition().getCompilationUnit();
            if (compilationUnit == null) continue; // Skip if not found

            // Process each method in the class
            for (CtMethod<?> method : clazz.getMethods()) {
//                System.out.println("   ➜ Checking method: " + method.getSimpleName());

                // Find missing types
                for (CtTypeReference<?> ref : method.getElements(new TypeFilter<>(CtTypeReference.class))) {
                    if (ref.getDeclaration() == null) { // If Spoon couldn't resolve it

                        // Check whether Spoon did not find an import statement with a valid package
                        if (ref.getPackage() != null && !ref.getPackage().getQualifiedName().startsWith("org.") && !ref.getPackage().getQualifiedName().startsWith("com.") && !ref.getPackage().getQualifiedName().startsWith("com.")) {
                            missingImportsWithoutPackage.add(ref.getQualifiedName().split("\\.")[0]);
                        }
                        else if (ref.getPackage() != null && !isPrimitive(ref.getQualifiedName()) && !ref.getPackage().getQualifiedName().equals(classPackage)) {
                            // Excludes primitive types and types from the same package

                            missingImports.add(ref.getQualifiedName());
                        }


                    }
                }

                // Find missing method calls
                for (CtExecutableReference<?> ref : method.getElements(new TypeFilter<>(CtExecutableReference.class))) {
                    if (ref.getDeclaringType() != null && ref.getDeclaringType().getDeclaration() == null) {
                        missingImports.add(ref.getDeclaringType().getQualifiedName());
                    }
                }
            }

            // Append missing imports to the file
            for (String importName : missingImports) {
                if (importName != null && !importName.startsWith("java.lang")) { // Ignore built-in classes
//                    compilationUnit.addImport(importName);
                    classMethods.put(clazz.getQualifiedName(), missingImports);
//                    System.out.println("✅ Added import: import " + importName + ";");

                }
            }

            // Find missing imports without a package
            for (String importName : missingImportsWithoutPackage) {
                if (importName != null && !importName.startsWith("java.lang")) { // Ignore built-in classes
//                    compilationUnit.addImport(importName);
                    // Resolve missing imports
                    if (allClasses.containsKey(importName)) {
                        classMethods.get(clazz.getQualifiedName()).add(allClasses.get(importName));
//                        System.out.println("✅ Missing package import: import " + importName + ";");
                    }

                }
            }
        }

        return classMethods;
    }
}
