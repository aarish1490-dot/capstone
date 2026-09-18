package com.dhatchina.dhatchinamart.service.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAiProviderTest {

    private static final String SYSTEM_PROMPT = "You are the DhatchinaMart assistant.";
    private static final String REPLY_BODY =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Sure! Check the Electronics category.\"}]}}]}";

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> receivedBody = new AtomicReference<>();
    private AtomicReference<String> receivedKey = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reply", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            receivedBody.set(new String(body, StandardCharsets.UTF_8));
            receivedKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            respond(exchange, 200, "\n" + REPLY_BODY);
        });
        server.createContext("/fail", exchange -> {
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 500, "{\"error\":\"boom\"}");
        });
        server.createContext("/malformed", exchange -> {
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 200, "{\"unexpected\":true}");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private GeminiAiProvider providerTo(String path) {
        return new GeminiAiProvider(baseUrl + path, "secret-key-123", 5_000L, HttpClient.newHttpClient());
    }

    @Test
    void sendsKeyHeaderAndReturnsReplyText() {
        GeminiAiProvider provider = providerTo("/reply");

        Optional<String> reply = provider.complete(SYSTEM_PROMPT, conversation("Suggest something"));

        assertTrue(reply.isPresent());
        assertEquals("Sure! Check the Electronics category.", reply.get());
        assertEquals("secret-key-123", receivedKey.get());
    }

    @Test
    void requestBodyContainsSystemPromptConversationAndNoApiKey() {
        GeminiAiProvider provider = providerTo("/reply");

        provider.complete(SYSTEM_PROMPT, conversation("Show me books"));

        assertTrue(receivedBody.get().contains(SYSTEM_PROMPT));
        assertTrue(receivedBody.get().contains("Show me books"));
        assertTrue(receivedBody.get().contains("\"role\":\"user\""));
        assertFalse(receivedBody.get().contains("secret-key-123"),
                "the API key must never be placed in the request body");
    }

    @Test
    void non2xxStatusReturnsEmpty() {
        GeminiAiProvider provider = providerTo("/fail");

        assertTrue(provider.complete(SYSTEM_PROMPT, conversation("hi")).isEmpty());
    }

    @Test
    void malformedResponseReturnsEmpty() {
        GeminiAiProvider provider = providerTo("/malformed");

        assertTrue(provider.complete(SYSTEM_PROMPT, conversation("hi")).isEmpty());
    }

    @Test
    void missingCredentialsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new GeminiAiProvider(baseUrl + "/reply", "", 5_000L, HttpClient.newHttpClient()));
    }

    @Test
    void missingEndpointRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new GeminiAiProvider("", "secret-key-123", 5_000L, HttpClient.newHttpClient()));
    }

    @Test
    void parseReplyExtractsFirstCandidateText() {
        Optional<String> text = GeminiAiProvider.parseReply(REPLY_BODY);

        assertTrue(text.isPresent());
        assertEquals("Sure! Check the Electronics category.", text.get());
    }

    @Test
    void parseReplyHandlesEmptyAndBlankText() {
        assertTrue(GeminiAiProvider.parseReply("{}").isEmpty());
        assertTrue(GeminiAiProvider.parseReply("{\"candidates\":[]}").isEmpty());
        assertTrue(GeminiAiProvider.parseReply(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"  \"}]}}]}").isEmpty());
        assertTrue(GeminiAiProvider.parseReply("not json at all").isEmpty());
    }

    private List<ChatMessage> conversation(String message) {
        return List.of(new ChatMessage(ChatMessage.ROLE_USER, message));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}