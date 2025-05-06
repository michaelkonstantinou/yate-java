package com.mkonst;

import com.mkonst.config.ConfigYate;
import com.mkonst.models.ChatOpenAIModel;
import com.mkonst.runners.YateJavaRunner;
import com.mkonst.services.PromptService;
import com.mkonst.types.TestLevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void initializeServices() throws IOException {
        ConfigYate.initialize();
        PromptService.initialize();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running YATE (Java)");
        initializeServices();

        String repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/";
        YateJavaRunner runner = new YateJavaRunner(repositoryPath, false, "com.binance.connector");
//        runner.generate("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", TestLevel.CLASS);
        runner.fix("/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/main/java/com/binance/connector/client/impl/SpotClientImpl.java", "/Users/michael.konstantinou/Datasets/yate_evaluation/binance-connector-java-2.0.0/src/test/java/com/binance/connector/client/impl/SpotClientImplTest.java");
        runner.close();
//        ChatOpenAIModel aimodel = new ChatOpenAIModel(null);
//        List<String> prompts = new ArrayList<String>();
//        prompts.add("Hello ChatGPT!");
//        prompts.add("Can you write tests in kotlin?");
//        System.out.println(aimodel.ask(prompts, PromptService.get("system"), null));
//        aimodel.closeConnection();


    }
}