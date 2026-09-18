package com.dhatchina.dhatchinamart.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Real AI provider backed by the Google Gemini {@code generateContent} REST
 * API. The API key is injected from the {@code GEMINI_API_KEY} environment
 * variable - never hard-coded - and is never logged.
 *
 * <p>Failures (non-2xx status, malformed responses, timeouts, IO errors) are
 * reported as an empty Optional so the application can fall back gracefully
 * instead of breaking the chat feature.
 */
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
    private static final String DEFAULT_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final String apiUrl;
    private final String apiKey;
    private final long timeoutMillis;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiAiProvider(String apiKey, String model, long timeoutMillis) {
        this(DEFAULT_ENDPOINT.replace("{model}", model), apiKey, timeoutMillis,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeoutMillis))
                        .build());
    }

    /**
     * Package-visible constructor with an injectable endpoint and HttpClient
     * so unit tests can run against a local HTTP server.
     */
    GeminiAiProvider(String apiUrl, String apiKey, long timeoutMillis, HttpClient httpClient) {
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini AI provider requires an endpoint and GEMINI_API_KEY "
                            + "environment variable (credentials are never hard-coded).");
        }
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.timeoutMillis = timeoutMillis;
        this.httpClient = httpClient;
    }

    String getApiUrl() {
        return apiUrl;
    }

    @Override
    public Optional<String> complete(String systemPrompt, List<ChatMessage> conversation) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .header(API_KEY_HEADER, apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(systemPrompt, conversation)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI provider returned status {}", response.statusCode());
                return Optional.empty();
            }
            Optional<String> reply = parseReply(response.body());
            if (reply.isEmpty()) {
                log.error("AI provider returned a response without usable text");
            }
            return reply;
        } catch (Exception e) {
            log.error("AI provider request failed", e);
            return Optional.empty();
        }
    }

    /**
     * Builds the Gemini request body: a system instruction plus the user/model
     * conversation turns.
     */
    String buildPayload(String systemPrompt, List<ChatMessage> conversation) {
        JsonObject root = new JsonObject();
        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", partsArray(systemPrompt));
        root.add("systemInstruction", systemInstruction);

        JsonArray contents = new JsonArray();
        for (ChatMessage turn : conversation) {
            JsonObject item = new JsonObject();
            item.addProperty("role", ChatMessage.ROLE_USER.equals(turn.getRole()) ? "user" : "model");
            item.add("parts", partsArray(turn.getContent()));
            contents.add(item);
        }
        root.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 512);
        generationConfig.addProperty("temperature", 0.4);
        root.add("generationConfig", generationConfig);
        return gson.toJson(root);
    }

    private JsonArray partsArray(String text) {
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        return parts;
    }

    /**
     * Extracts {@code candidates[0].content.parts[0].text} from a Gemini
     * response. Returns empty if the shape is unexpected.
     */
    static Optional<String> parseReply(String responseBody) {
        try {
            JsonArray candidates = JsonParser.parseString(responseBody)
                    .getAsJsonObject()
                    .getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return Optional.empty();
            }
            JsonArray parts = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) {
                return Optional.empty();
            }
            String text = parts.get(0).getAsJsonObject().getAsJsonPrimitive("text").getAsString();
            return text == null || text.isBlank() ? Optional.empty() : Optional.of(text.trim());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}