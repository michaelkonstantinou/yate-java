package com.mkonst;

import com.mkonst.components.YateUnitGenerator;
import com.mkonst.config.ConfigYate;
import com.mkonst.evaluation.EvaluationDataset;
import com.mkonst.evaluation.EvaluationDatasetRecord;
import com.mkonst.helpers.YateConsole;
import com.mkonst.helpers.YateJavaUtils;
import com.mkonst.helpers.YateUtils;
import com.mkonst.runners.YateJavaRunner;
import com.mkonst.services.CoverageService;
import com.mkonst.services.PromptService;
import com.mkonst.types.TestLevel;
import com.mkonst.types.YateResponse;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void initializeServices() throws IOException {
        ConfigYate.initialize(".env");
        PromptService.initialize();
    }

    public static void generateTestForClass(String repositoryPath, String classPath) {
        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, null);
        runner.generate(classPath, TestLevel.CLASS);
        runner.close();
    }

    public static void fixOraclesInTest(String repositoryPath, String testClassPath) {
        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, null);
        runner.fixOracles(testClassPath);
        runner.close();
    }

    public static void enhanceCoverage(String repositoryPath, String cut, String testClassPath) {
        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, null);
        runner.enhanceCoverage(cut, testClassPath);
        runner.close();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running YATE (Java)");
        initializeServices();
//        ConfigYate.setValue("MODEL", "llama3");

        String cut = "/Users/michael.konstantinou/Datasets/yate_evaluation/event-ruler/src/main/software/amazon/event/ruler/ACFinder.java";
        String testClassPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/windward/src/test/java/org/flmelody/util/AntPathMatcherTest.java";
        String repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/event-ruler/";
//        enhanceCoverage(repositoryPath, cut, testClassPath);
//        generateTestForClass(repositoryPath, cut);
//        fixOraclesInTest(repositoryPath, testClassPath);
//        System.exit(0);
////        CoverageService.INSTANCE.getMissingCoverageForClass(repositoryPath, "com.binance.connector.client.utils.signaturegenerator.RsaSignatureGenerator");
////
////        String outputDir = repositoryPath + "yate-java-tests/";
//        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, null);
//        runner.generate("/Users/michael.konstantinou/Datasets/yate_evaluation/windward/src/main/java/org/flmelody/core/exception/WindwardException.java", TestLevel.CLASS);
//////        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/impl/SpotClientImplTest.java");
//        runner.close();

        String csvFile = "/Users/michael.konstantinou/Projects/yate/output/input_event-ruler_class.csv";

        EvaluationDataset dataset = new EvaluationDataset(csvFile);

        int recordSize = dataset.getRecords().size();
        int index = 0;
        YateJavaRunner runner = new YateJavaRunner(dataset.getRecords().get(0).getRepositoryPath(), true, dataset.getRecords().get(0).getOutputDir());
        for(EvaluationDatasetRecord record: dataset.getRecords()) {
            index += 1;

            // Verify that the record has not been executed
            if (record.isExecuted()) {
                continue;
            }

            boolean hasFailed = true;
            int i = 0;
            while (hasFailed && i < ConfigYate.getInteger("MAX_REPEAT_FAILED_ITERATIONS")) {
                i++;

                System.out.println("Iterating class (" + index + "/" + recordSize + " (#" + i + "): " + record.getClassPath());
                var startTime = System.currentTimeMillis();

                try {
                    List<YateResponse> responses = runner.generate(record.getClassPath(), record.getTestLevel());

                    if (responses.isEmpty()) {
                        hasFailed = true;
                        runner.resetNrRequests();

                        continue;
                    }

                    // Everything went smoothly, update stats
                    record.setExecuted(true);
                    record.setRequests(runner.getNrRequests());

                    for (YateResponse response: responses) {
                        int generatedTests = YateJavaUtils.INSTANCE.countTestMethods(response.getTestClassContainer());
                        if (generatedTests <= 0) {
                            throw new Exception("Failed to generate tests. Re-run");
                        }
                        record.addGeneratedTests(generatedTests);
                    }
                    hasFailed = false;
                } catch (Exception e) {
                    record.setErrors(e.getMessage());
                    hasFailed = true;
                }

                var endTime = System.currentTimeMillis();
                record.setGenerationTime(endTime - startTime);
                runner.resetNrRequests();
            }

            YateConsole.INSTANCE.info("Updating dataset file");
            dataset.saveAs(csvFile);
        }

        runner.close();

        YateConsole.INSTANCE.info("Saving a new dataset file by the name: ");
        String newCsvFile = csvFile.replace(".csv", "_results_" + YateUtils.INSTANCE.timestamp() + ".csv");
        dataset.saveAs(newCsvFile);
        dataset.printTotals();

    }
}