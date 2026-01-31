package com.cortex.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load configuration properties.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static Properties properties;

    static {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Unable to find config.properties");
            } else {
                properties.load(input);
                logger.info("Configuration loaded successfully");
            }
        } catch (IOException e) {
            logger.error("Error loading config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static String getIgdbClientId() {
        return getProperty("igdb.client.id");
    }

    public static String getIgdbClientSecret() {
        return getProperty("igdb.client.secret");
    }
}
