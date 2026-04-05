package com.jarvis.api;

import com.google.gson.*;
import com.jarvis.Configuration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Gemini API client for generating commands from natural language.
 * Handles API communication, error handling, and response parsing.
 */
public class GeminiClient {
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String PROMPT_TEMPLATE = """
            You are a secure Linux command generator for Jarvis Terminal.
            Your task is to convert natural language requests into single-line Linux commands.
            
            CRITICAL RULES:
            1. Return ONLY the command, nothing else
            2. Do NOT include explanations or comments
            3. Do NOT suggest dangerous commands
            4. Never use: sudo, su, rm -rf, shutdown, reboot, mkfs, dd, chmod 777, chown
            5. Commands must be 200 characters or less
            6. Use safe alternatives when possible
            
            User request: %s
            Command:""";

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String apiEndpoint;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    public GeminiClient(Configuration config) throws IllegalStateException {
        config.validateConfiguration();

        this.apiKey = config.getApiKey();
        this.apiEndpoint = config.getApiEndpoint();
        this.model = config.getModel();
        this.temperature = config.getApiTemperature();
        this.maxTokens = config.getMaxTokens();
        int timeoutSeconds = config.getApiTimeout();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        logger.info("Gemini API client initialized with model: {}", model);
    }

    /**
     * Generate a Linux command from natural language input
     */
    public String generateCommand(String userInput) throws IOException {
        if (userInput == null || userInput.isEmpty()) {
            throw new IllegalArgumentException("User input cannot be empty");
        }

        String prompt = String.format(PROMPT_TEMPLATE, sanitizePrompt(userInput));
        logger.debug("Sending API request with prompt length: {}", prompt.length());

        try {
            String response = callGeminiAPI(prompt);
            logger.debug("API response: {}", response);
            return response.trim();
        } catch (IOException e) {
            logger.error("Gemini API request failed: {}", e.getMessage());
            throw new IOException("Failed to generate command: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            throw new IOException("Failed to generate command: " + e.getMessage(), e);
        }
    }

    private String callGeminiAPI(String prompt) throws IOException {
        // Detect format based on endpoint
        boolean isGenerateText = apiEndpoint.contains("generateText");
        
        JsonObject requestBody;
        
        if (isGenerateText) {
            // Text-Bison format (older API)
            JsonObject promptObj = new JsonObject();
            promptObj.addProperty("text", prompt);
            
            requestBody = new JsonObject();
            requestBody.add("prompt", promptObj);
            requestBody.addProperty("temperature", temperature);
            requestBody.addProperty("candidate_count", 1);
            requestBody.addProperty("max_output_tokens", maxTokens);
        } else {
            // Gemini format (newer API - generateContent)
            JsonObject content = new JsonObject();
            
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            
            content.add("parts", parts);
            
            JsonArray contents = new JsonArray();
            contents.add(content);

            requestBody = new JsonObject();
            requestBody.add("contents", contents);
            
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", temperature);
            generationConfig.addProperty("maxOutputTokens", maxTokens);
            requestBody.add("generationConfig", generationConfig);
        }

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        // Gemini API uses key as query parameter
        String urlWithKey = apiEndpoint;
        if (!apiEndpoint.contains("?")) {
            urlWithKey = apiEndpoint + "?key=" + apiKey;
        }

        Request request = new Request.Builder()
                .url(urlWithKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                logger.error("API error {} : {}", response.code(), errorBody);
                throw new IOException(formatApiError(response.code(), errorBody));
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Parse response based on endpoint type
            if (isGenerateText) {
                // Text-Bison response format
                return jsonResponse
                        .getAsJsonArray("candidates")
                        .get(0)
                        .getAsJsonObject()
                        .get("output")
                        .getAsString();
            } else {
                // Gemini response format: candidates[0].content.parts[0].text
                return jsonResponse
                        .getAsJsonArray("candidates")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString();
            }
        }
    }

    private String formatApiError(int statusCode, String errorBody) {
        String apiMessage = extractApiMessage(errorBody);

        if (statusCode == 429) {
            return "Gemini quota exceeded. Check API billing/quota and retry later.";
        }

        if (statusCode == 404) {
            return "Gemini model/endpoint not found. Update MODEL and API_ENDPOINT in config.properties.";
        }

        if (statusCode == 401 || statusCode == 403) {
            return "Gemini API key is invalid or lacks permission.";
        }

        if (apiMessage != null && !apiMessage.isBlank()) {
            return "Gemini API error " + statusCode + ": " + apiMessage;
        }

        return "Gemini API error " + statusCode;
    }

    private String extractApiMessage(String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return null;
        }

        try {
            JsonObject root = JsonParser.parseString(errorBody).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject err = root.getAsJsonObject("error");
                if (err.has("message") && !err.get("message").isJsonNull()) {
                    return err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // Ignore parse failures and fall back to generic messages.
        }

        return null;
    }

    private String sanitizePrompt(String input) {
        // Remove potential prompt injection attempts
        return input
                .replace("\n", " ")
                .replace("\"", "")
                .substring(0, Math.min(input.length(), 500));
    }

    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        logger.info("Gemini API client closed");
    }
}
