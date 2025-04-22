package com.mkonst;

import com.mkonst.config.ConfigYate;
import com.mkonst.services.PromptService;

import java.io.IOException;

public class Main {

    public static void initializeServices() throws IOException {
        ConfigYate.initialize();
        PromptService.initialize();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running YATE (Java)");
        initializeServices();

        System.out.println(PromptService.get("system"));
    }
}