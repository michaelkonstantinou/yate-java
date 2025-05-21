package com.mkonst.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class ConfigYate {

    private static Properties properties;

    public static void initialize() throws IOException {
        properties = new Properties();
        try (FileInputStream in = new FileInputStream(".env")) {
            properties.load(in);
        }
    }

    public static String getString(String name) {
        return properties.getProperty(name);
    }

    public static int getInteger(String name) {
        return Integer.parseInt(properties.getProperty(name));
    }

    public static String[] getArray(String name) {
        String joinedItems = properties.getProperty(name);

        return joinedItems.split(",");
    }

    public static boolean getBoolean(String name) {
        return Boolean.parseBoolean(properties.getProperty(name));
    }
}
