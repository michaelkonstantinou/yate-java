package com.mkonst.runners

import com.mkonst.analysis.ClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.config.ConfigYate.getInteger
import com.mkonst.evaluation.RequestsCounter
import com.mkonst.evaluation.ablation.RawUnitGenerator
import com.mkonst.helpers.*
import com.mkonst.providers.ClassContainerProvider
import com.mkonst.services.CoverageService
import com.mkonst.services.ErrorService
import com.mkonst.types.*
import com.mkonst.types.coverage.MethodCoverage
import java.io.File

class YateRawRunner(
    protected val repositoryPath: String,
    private val modelName: String,
    val lang: ProgramLangType = ProgramLangType.JAVA,
    private val outputDirectory: String? = null,
    ) {
    // Identify whether a pom.xml file is present
    // The purpose of this process is to check whether the repository is using maven or gradle
    protected var dependencyTool: DependencyTool = YateUtils.getDependencyTool(repositoryPath)
    protected var packageName: String
    private val rawUnitGenerator = RawUnitGenerator(modelName)

    init {
        println("The given repository is using ${dependencyTool.name}")

        packageName = YateCodeUtils.getRootPackage(repositoryPath)
        println("The package name of the repository under test is: $packageName")
    }

    /**
     * Given the absolute path of the class under test, the method will generate a number of test cases
     * depending on the TestLevel provided
     */
    fun generate(classPath: String) {
        val container = ClassContainerProvider.getFromFile(classPath, lang)
        val newClassName = container.className + "Test"
        val response = rawUnitGenerator.generateForClass(container)
        val testClassPath = classPath.replace("src/main", "src/test")
            .replace("${container.className}.java", "${newClassName}.java")

        if (response.codeContent != null) {
            YateIO.writeFile(testClassPath, response.codeContent!!)
        }
    }

}