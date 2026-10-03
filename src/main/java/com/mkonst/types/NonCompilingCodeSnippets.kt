package com.mkonst.types

/**
 * The data structure is being used to return a group of non-compiling code snippets that must be removed
 * Such code-snippets can be invalid tests, import statements, field declarations or static classes.
 *
 * It is only used by the plugin, so the plugin is aware of what must be removed in order for the code to compile
 */
data class NonCompilingCodeSnippets(
    val testsByTestClass: Map<String, MutableSet<String>>,
    val importsByTestClass: Map<String, MutableSet<String>>,
    val staticClassesByTestClass: Map<String, MutableSet<String>>,
    val invalidFieldDeclarationsByTestClass: Map<String, MutableSet<String>>
)
