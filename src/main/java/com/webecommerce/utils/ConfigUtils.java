package com.webecommerce.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                System.err.println("Không tìm thấy file application.properties");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
