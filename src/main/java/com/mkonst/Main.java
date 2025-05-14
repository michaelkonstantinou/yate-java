package com.mkonst;

import com.mkonst.config.ConfigYate;
import com.mkonst.evaluation.EvaluationDataset;
import com.mkonst.evaluation.EvaluationDatasetRecord;
import com.mkonst.helpers.YateConsole;
import com.mkonst.helpers.YateJavaUtils;
import com.mkonst.helpers.YateUtils;
import com.mkonst.runners.YateJavaRunner;
import com.mkonst.services.PromptService;
import com.mkonst.types.TestErrorType;
import com.mkonst.types.TestLevel;
import com.mkonst.types.YateResponse;

import java.io.IOException;

public class Main {

    public static void initializeServices() throws IOException {
        ConfigYate.initialize();
        PromptService.initialize();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running YATE (Java)");
        initializeServices();

        String repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/";
        String outputDir = repositoryPath + "yate-java-tests/";
        YateJavaRunner runner = new YateJavaRunner(repositoryPath, false, null);
        runner.generate("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/utils/signaturegenerator/RsaSignatureGenerator.java", TestLevel.CLASS);
//        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/impl/SpotClientImplTest.java");
        runner.close();
        System.exit(0);

        String csvFile = "/Users/michael.konstantinou/Projects/yate/output/input_binance-connector-java-2.0.0.csv";

        EvaluationDataset dataset = new EvaluationDataset(csvFile);
        runner = new YateJavaRunner(dataset.getRecords().get(0).getRepositoryPath(), true, dataset.getRecords().get(0).getOutputDir());
        for(EvaluationDatasetRecord record: dataset.getRecords()) {

            // Verify that the record has not been executed
            if (record.isExecuted()) {
                continue;
            }

            boolean hasFailed = true;
            int i = 0;
            while (hasFailed && i < ConfigYate.getInteger("MAX_REPEAT_FAILED_ITERATIONS")) {
                i++;

                System.out.println("Iterating class (#" + i + "): " + record.getClassPath());
                var startTime = System.currentTimeMillis();

                try {
                    YateResponse response = runner.generate(record.getClassPath(), record.getTestLevel());

                    if (response == null) {
                        hasFailed = true;
                        runner.resetNrRequests();

                        continue;
                    }

                    // Everything went smoothly, update stats
                    record.setExecuted(true);
                    record.setRequests(runner.getNrRequests());
                    record.setGeneratedTests(YateJavaUtils.INSTANCE.countTestMethods(response.getTestClassContainer()));
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


    }
}