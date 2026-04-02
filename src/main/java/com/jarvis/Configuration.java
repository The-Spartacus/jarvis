package com.jarvis;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager for Jarvis Terminal.
 * Updated for Google Gemini 1.5 Pro integration.
 */
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    // Standard Linux path and local fallback
    private static final String CONFIG_FILE = "/opt/jarvis/config.properties";
    private static final String LOCAL_CONFIG_FILE = "config/config.properties";

    private final Properties properties;

    public Configuration() throws IOException {
        this.properties = new Properties();
        loadConfiguration();
    }

    private void loadConfiguration() throws IOException {
        try {
            Path configPath = null;

            String sysPropPath = System.getProperty("config.path");
            if (sysPropPath != null) {
                configPath = Paths.get(sysPropPath);
            }

            if (configPath == null || !Files.exists(configPath)) {
                configPath = Paths.get(CONFIG_FILE);
            }
            if (!Files.exists(configPath)) {
                configPath = Paths.get(LOCAL_CONFIG_FILE);
            }

            if (!Files.exists(configPath)) {
                try {
                    java.net.URL url = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
                    if (url != null) {
                        File f = new File(url.toURI());
                        Path jarDir = f.toPath().getParent();
                        if (jarDir != null) {
                            Path jarConfig = jarDir.resolve("config.properties");
                            if (Files.exists(jarConfig)) {
                                configPath = jarConfig;
                            } else if (jarDir.getParent() != null) {
                                Path grandParentConfig = jarDir.getParent().resolve(LOCAL_CONFIG_FILE);
                                if (Files.exists(grandParentConfig)) {
                                    configPath = grandParentConfig;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Could not resolve JAR path", e);
                }
            }

            if (configPath != null && Files.exists(configPath)) {
                try (InputStream is = Files.newInputStream(configPath)) {
                    properties.load(is);
                }
                logger.info("Loaded configuration from {}", configPath);
            } else {
                logger.warn("No configuration file found, using built-in Gemini defaults");
            }
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            throw new IOException("Failed to load configuration", e);
        }
    }

    /**
     * Updated to check for GOOGLE_API_KEY specifically.
     */
    public String getApiKey() {
        // Checks GOOGLE_API_KEY first, falls back to OPENAI_API_KEY if you haven't
        // renamed it yet
        String key = getProperty("GOOGLE_API_KEY", "");
        if (key.isEmpty()) {
            key = getProperty("OPENAI_API_KEY", "");
        }
        return key;
    }

    /**
     * Default changed to Gemini 1.5 Pro :generateContent endpoint
     */
    public String getApiEndpoint() {
        return getProperty("API_ENDPOINT",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent");
    }

    /**
     * Default model updated to gemini-1.5-pro
     */
    public String getModel() {
        return getProperty("MODEL", "gemini-1.5-pro");
    }

    /**
     * Increased default timeout to 30s. Gemini Pro models take longer to process
     * than smaller models like GPT-4o-mini.
     */
    public int getApiTimeout() {
        return getIntProperty("API_TIMEOUT_SECONDS", 30);
    }

    /**
     * Increased default max tokens to 1000 to allow for full code responses.
     */
    public int getMaxTokens() {
        return getIntProperty("MAX_TOKENS", 1000);
    }

    public String getDockerImage() {
        return getProperty("DOCKER_IMAGE", "ubuntu:22.04");
    }

    public String getDockerContainerName() {
        return getProperty("DOCKER_CONTAINER_NAME", "jarvis-sandbox");
    }

    public int getCommandTimeout() {
        return getIntProperty("COMMAND_TIMEOUT_SECONDS", 10);
    }

    public int getMaxCommandLength() {
        return getIntProperty("MAX_COMMAND_LENGTH", 500);
    }

    public int getMaxOutputLength() {
        return getIntProperty("MAX_OUTPUT_LENGTH", 5000);
    }

    public String getLogsDirectory() {
        return getProperty("LOGS_DIR", "./logs");
    }

    public String getPluginDirectory() {
        return getProperty("PLUGIN_DIR", "./plugins");
    }

    public boolean isVoiceEnabled() {
        return getBooleanProperty("VOICE_ENABLED", true);
    }

    public double getApiTemperature() {
        return getDoubleProperty("API_TEMPERATURE", 0.2);
    }

    // Helper methods
    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer for property {}, using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private double getDoubleProperty(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid double for property {}, using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    public void validateConfiguration() throws IllegalStateException {
        if (getApiKey().isEmpty()) {
            throw new IllegalStateException("Google API key not configured. Set GOOGLE_API_KEY in config.properties");
        }
    }
}