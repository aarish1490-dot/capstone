package com.dhatchina.aarishmart.service.sms;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real SMS provider used in production. The endpoint and credentials are
 * injected from environment variables - never hard-coded. It works with any
 * HTTP JSON SMS gateway by customizing the request via env vars:
 *
 * <pre>
 *   SMS_API_URL               - gateway endpoint (required)
 *   SMS_API_KEY               - API key (required)
 *   SMS_API_SECRET            - optional secret
 *   SMS_SENDER_ID             - optional sender id
 *   SMS_PAYLOAD_TEMPLATE      - JSON body template with placeholders:
 *                               {to} {message} {senderId} {apiKey} {apiSecret}
 *                               Default: {"to":"{to}","message":"{message}",
 *                                         "senderId":"{senderId}"}
 *   SMS_AUTH_HEADER           - auth header name, default "Authorization"
 *   SMS_AUTH_VALUE_TEMPLATE   - header value with placeholders, default "Bearer {apiKey}"
 * </pre>
 *
 * Placeholders are substituted with JSON-escaped values. Credentials are NEVER
 * logged.
 */
public class RealSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(RealSmsProvider.class);
    private static final String DEFAULT_PAYLOAD_TEMPLATE =
            "{\"to\":\"{to}\",\"message\":\"{message}\",\"senderId\":\"{senderId}\"}";
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{to\\}|\\{message\\}|\\{senderId\\}|\\{apiKey\\}|\\{apiSecret\\}");

    private final String apiUrl;
    private final String apiKey;
    private final String apiSecret;
    private final String senderId;
    private final String payloadTemplate;
    private final String authHeader;
    private final String authValueTemplate;
    private final HttpClient httpClient;

    public RealSmsProvider(String apiUrl, String apiKey, String apiSecret, String senderId) {
        this(apiUrl, apiKey, apiSecret, senderId,
                DEFAULT_PAYLOAD_TEMPLATE, "Authorization", "Bearer {apiKey}");
    }

    public RealSmsProvider(String apiUrl, String apiKey, String apiSecret, String senderId,
                           String payloadTemplate, String authHeader, String authValueTemplate) {
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Real SMS provider requires SMS_API_URL and SMS_API_KEY "
                            + "environment variables (credentials are never hard-coded).");
        }
        if (payloadTemplate == null || payloadTemplate.isBlank()
                || !isValidJson(payloadTemplate)) {
            throw new IllegalArgumentException(
                    "SMS_PAYLOAD_TEMPLATE must be a valid JSON body template "
                            + "(e.g. {\"to\":\"{to}\",\"message\":\"{message}\"}).");
        }
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret == null ? "" : apiSecret;
        this.senderId = senderId == null ? "" : senderId;
        this.payloadTemplate = payloadTemplate;
        this.authHeader = authHeader == null || authHeader.isBlank() ? "Authorization" : authHeader;
        this.authValueTemplate = authValueTemplate == null || authValueTemplate.isBlank()
                ? "Bearer {apiKey}" : authValueTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    String getApiUrl() {
        return apiUrl;
    }

    @Override
    public boolean send(String mobileNumber, String otp, String message) {
        try {
            String json = buildPayload(mobileNumber, message);
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header(authHeader, render(authValueTemplate, mobileNumber, message))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS delivered via {} (status {})", apiUrl, response.statusCode());
                return true;
            }
            log.error("SMS provider returned status {} for mobile {}", response.statusCode(),
                    mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)));
            return false;
        } catch (Exception e) {
            log.error("SMS delivery failed", e);
            return false;
        }
    }

    /**
     * Builds the JSON body by substituting {to}, {message}, {senderId},
     * {apiKey} and {apiSecret} with JSON-escaped values.
     */
    String buildPayload(String mobileNumber, String message) {
        String body = render(payloadTemplate, mobileNumber, message);
        if (!isValidJson(body)) {
            log.error("SMS payload template produced invalid JSON: {}", body);
            throw new IllegalStateException("Invalid SMS payload template");
        }
        return body;
    }

    private String render(String template, String mobileNumber, String message) {
        Gson gson = new Gson();
        // toJson escapes the value; strip the wrapping quotes so templates
        // can quote placeholders themselves: {"to":"{to}"}
        Map<String, String> values = new HashMap<>();
        values.put("{to}", jsonEscape(gson, mobileNumber));
        values.put("{message}", jsonEscape(gson, message));
        values.put("{senderId}", jsonEscape(gson, senderId));
        values.put("{apiKey}", jsonEscape(gson, apiKey));
        values.put("{apiSecret}", jsonEscape(gson, apiSecret));
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = values.get(matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String jsonEscape(Gson gson, String value) {
        String quoted = gson.toJson(value == null ? "" : value);
        return quoted.substring(1, quoted.length() - 1);
    }

    private static boolean isValidJson(String json) {
        try {
            JsonParser.parseString(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
