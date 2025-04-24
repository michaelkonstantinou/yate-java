package com.mkonst.services;

import com.mkonst.config.ConfigYate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PromptService {

    private static HashMap<String, String> prompts;

    /**
     * Initializes PromptService by reading all files in prompts directory, and storing the content
     * in the static prompts variable for future use. It also replaces the most common variables for each
     * prompt content
     */
    public static void initialize() throws IOException {
        prompts = new HashMap<>();
        Path folderPath = Paths.get(ConfigYate.getString("DIR_PROMPTS"));

        if (!Files.exists(folderPath)) {
            System.out.println("❌ Folder does NOT exist");
        }

        // Iterates all files and reads their content. It also replaces the most common variables with the ones in .env
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path file : stream) {
                byte[] bytes = Files.readAllBytes(file);
                String content = new String(bytes, StandardCharsets.UTF_8);
                content = replaceCommonVariables(content);
                prompts.put(file.getFileName().toString(), content);
            }
        }
    }

    /**
     * Returns the content of a prompt file as is, with ONLY the common variables replaced
     */
    public static String get(String name) {
        return prompts.get(name + ".txt");
    }

    /**
     * Returns the content of a prompt file with the provided variable-values replaced
     */
    public static String get(String name, HashMap<String, String> variables) {
        String content = prompts.get(name + ".txt");

        // Iterating all variables and replacing them in the prompt
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }

        return content;
    }

    private static String replaceCommonVariables(String content) {
        return content.replace("%%TEST_FRAMEWORK%%", ConfigYate.getString("TEST_FRAMEWORK"))
                .replace("%%LANG%%", ConfigYate.getString("LANG"));
    }
}
