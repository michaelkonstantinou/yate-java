package com.mkonst;

import com.mkonst.config.ConfigYate;
import com.mkonst.evaluation.EvaluationDataset;
import com.mkonst.evaluation.EvaluationDatasetRecord;
import com.mkonst.helpers.YateCodeUtils;
import com.mkonst.models.ChatOpenAIModel;
import com.mkonst.runners.YateJavaRunner;
import com.mkonst.services.ErrorService;
import com.mkonst.services.PromptService;
import com.mkonst.types.TestLevel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main {

    public static void initializeServices() throws IOException {
        ConfigYate.initialize();
        PromptService.initialize();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running YATE (Java)");
        initializeServices();

        String csvFile = "/Users/michael.konstantinou/Library/Application Support/JetBrains/IntelliJIdea2023.3/scratches/input.csv";
        EvaluationDataset dataset = new EvaluationDataset(csvFile);
        YateJavaRunner runner = new YateJavaRunner(dataset.getRecords().get(0).getRepositoryPath(), true, dataset.getRecords().get(0).getOutputDir());

        // test
        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/utils/signaturegenerator/RsaSignatureGenerator.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/utils/signaturegenerator/RsaSignatureGeneratorTest.java");
        runner.close();
        System.exit(0);

        for(EvaluationDatasetRecord record: dataset.getRecords()) {
            System.out.println("Iterating class: " + record.getClassPath());
            var startTime = System.currentTimeMillis();

            try {
                runner.generate(record.getClassPath(), record.getClassLevel());
                record.setExecuted(true);
                record.setRequests(runner.getNrRequests());
            } catch (Exception e) {
                record.setErrors(e.getMessage());
            }

            var endTime = System.currentTimeMillis();
            record.setGenerationTime(endTime - startTime);
            runner.resetNrRequests();
        }

        runner.close();
        dataset.saveAs("output_test.csv");

//        String repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/";
//        String outputDir = repositoryPath + "yate-java-tests/";
//        YateJavaRunner runner = new YateJavaRunner(repositoryPath, true, outputDir);
//        runner.generate("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/utils/signaturegenerator/RsaSignatureGenerator.java", TestLevel.CLASS);
////        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/impl/SpotClientImplTest.java");
//        runner.close();
    }
}