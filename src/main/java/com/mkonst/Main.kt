package com.mkonst

import com.mkonst.config.ConfigYate.getInteger
import com.mkonst.config.ConfigYate.initialize
import com.mkonst.evaluation.EvaluationDataset
import com.mkonst.evaluation.YatePlainRunner
import com.mkonst.helpers.YateConsole.info
import com.mkonst.helpers.YateJavaUtils.countTestMethods
import com.mkonst.helpers.YateUtils.timestamp
import com.mkonst.runners.YateJavaRunner
import com.mkonst.services.PromptService
import com.mkonst.types.TestLevel
import com.mkonst.types.YateResponse
import java.io.IOException

object Main {
    @Throws(IOException::class)
    fun initializeServices() {
        initialize(".env")
        PromptService.initialize()
    }

    fun generateTestForClass(repositoryPath: String?, classPath: String?) {
        val runner = YateJavaRunner(repositoryPath!!, true, null, null)
        runner.generate(classPath!!, TestLevel.METHOD)
        runner.close()
    }

    fun fixOraclesInTest(repositoryPath: String?, testClassPath: String?) {
        val runner = YateJavaRunner(repositoryPath!!, true, null, null)
        runner.fixOracles(testClassPath!!)
        runner.close()
    }

    fun enhanceCoverage(repositoryPath: String?, cut: String?, testClassPath: String?) {
        val runner = YateJavaRunner(repositoryPath!!, true, null, null)
        runner.enhanceCoverage(cut!!, testClassPath!!, null)
        runner.close()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("Running YATE (Java)")
        initializeServices()

        val cut = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/WebSocketStreamClientImpl.java"
        val testClassPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/windward/src/test/java/org/flmelody/util/AntPathMatcherTest.java"
        val repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/"
        //        enhanceCoverage(repositoryPath, cut, testClassPath);
//        generateTestForClass(repositoryPath, cut)
        //        fixOraclesInTest(repositoryPath, testClassPath);
//        System.exit(0)

        ////        CoverageService.INSTANCE.getMissingCoverageForClass(repositoryPath, "com.binance.connector.client.utils.signaturegenerator.RsaSignatureGenerator");
////
////        String outputDir = repositoryPath + "yate-java-tests/";
//        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, null);
//        runner.generate("/Users/michael.konstantinou/Datasets/yate_evaluation/windward/src/main/java/org/flmelody/core/exception/WindwardException.java", TestLevel.CLASS);
//////        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/impl/SpotClientImplTest.java");
//        runner.close();
        val csvFile = "/Users/michael.konstantinou/Projects/yate/output/input_windward_class__gemma3_.csv"

        val dataset = EvaluationDataset(csvFile)

        val recordSize = dataset.records.size
        var index = 0

        // Max repeat failed iterations should only be applicable on class-level testing
        val testLevel: TestLevel = dataset.records[0].testLevel
        val maxRepeatFailedIterations: Int = if (testLevel == TestLevel.METHOD) 1 else getInteger("MAX_REPEAT_FAILED_ITERATIONS")
        val runner = YatePlainRunner(dataset.records[0].repositoryPath, dataset.records[0].outputDir, "gemma3:27b", 5)
//        val runner = YateJavaRunner(dataset.records[0].repositoryPath, true, dataset.records[0].outputDir, null)
        for (record in dataset.records) {
            index += 1

            // Verify that the record has not been executed
            if (record.isExecuted) {
                continue
            }

            var hasFailed = true
            var i = 0
            while (hasFailed && i < maxRepeatFailedIterations) {
                i++

                println("Iterating class (" + index + "/" + recordSize + ") (#" + i + "): " + record.classPath)
                val startTime = System.currentTimeMillis()

                try {
                    val responses: List<YateResponse> = runner.generate(record.classPath, record.testLevel)

                    if (responses.isEmpty()) {
                        hasFailed = true
                        runner.resetNrRequests()

                        continue
                    }

                    // Everything went smoothly, update stats
                    record.isExecuted = true
                    record.requests = runner.getNrRequests()

                    var generatedTests = 0
                    for ((testClassContainer) in responses) {
                        generatedTests += countTestMethods(testClassContainer)
                        record.addGeneratedTests(generatedTests)
                    }
                    if (generatedTests <= 0) {
                        throw Exception("Failed to generate tests although the generation process did not break.")
                    }

                    hasFailed = false
                } catch (e: Exception) {
                    record.errors = e.message
                    hasFailed = true
                }

                val endTime = System.currentTimeMillis()
                record.generationTime = endTime - startTime
                runner.resetNrRequests()
            }

            info("Updating dataset file")
            dataset.saveAs(csvFile)
        }

        runner.close()

        info("Saving a new dataset file by the name: ")
        val newCsvFile = csvFile.replace(".csv", "_results_" + timestamp() + ".csv")
        dataset.saveAs(newCsvFile)
        dataset.printTotals()
    }
}