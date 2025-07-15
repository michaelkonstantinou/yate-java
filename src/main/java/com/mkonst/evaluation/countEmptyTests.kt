package com.mkonst.evaluation

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.readText


object CountEmptyTests {

    fun countEmptyTestsInClass(source: String): Int {
        var count = 0
        val parser = JavaParser()
        val cu: CompilationUnit = parser.parse(source).result.orElseThrow()

        cu.findAll(MethodDeclaration::class.java).forEach { method ->
            val name: String = method.getNameAsString()
            if (method.getBody().isPresent()) {
                val body: BlockStmt = method.getBody().get()
                val isEmpty = body.isEmpty

                println(name + ": has body? " + !isEmpty)

                if (isEmpty) {
                    count += 1
                }
            }
        }

        return count

    }

    @JvmStatic
    fun main(args: Array<String>) {
        val path = Paths.get("/Users/michael.konstantinou/Datasets/yate_evaluation/windward-yate-tests-_gpt_4o_mini_plainclass/java")
        var total = 0

        Files.walk(path).use { paths ->
            paths.filter { Files.isRegularFile(it) && (it.fileName.name != ".DS_Store") }
                .forEach {
                    println("File: ${it}")
                    try {
                        total += countEmptyTestsInClass(it.readText())
                    } catch (_: Exception) {}
                }
        }

        println(total)

//        val contents: String = File("/Users/michael.konstantinou/Datasets/yate_evaluation/windward-yate-tests-mistral_7b_plainclass/passing/java/org/flmelody/support/FunctionHelperTest.java").readText()
//        countEmptyTestsInClass(contents)
    }
}