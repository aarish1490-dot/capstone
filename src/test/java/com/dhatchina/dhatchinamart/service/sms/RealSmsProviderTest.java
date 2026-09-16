package com.dhatchina.dhatchinamart.service.sms;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealSmsProviderTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> receivedBody = new AtomicReference<>();
    private AtomicReference<String> receivedAuth = new AtomicReference<>();
    private AtomicReference<String> receivedXAuth = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/send", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            receivedBody.set(new String(body, StandardCharsets.UTF_8));
            receivedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            receivedXAuth.set(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
            byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/fail", exchange -> {
            byte[] resp = "{\"status\":\"error\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsJsonPayloadWithBearerAuth() {
        RealSmsProvider provider = new RealSmsProvider(baseUrl + "/send", "key-123", "s3cret", "DHATCHA");

        assertTrue(provider.send("9876543210", "483921", "Your OTP is 483921."));

        assertTrue(receivedBody.get().contains("\"to\":\"9876543210\""));
        assertTrue(receivedBody.get().contains("\"message\":\"Your OTP is 483921.\""));
        assertTrue(receivedBody.get().contains("\"senderId\":\"DHATCHA\""));
        assertEquals("Bearer key-123", receivedAuth.get());
    }

    @Test
    void customTemplateAndAuthHeadersAreUsed() {
        RealSmsProvider provider = new RealSmsProvider(
                baseUrl + "/send", "key-123", "s3cret", "DHATCHA",
                "{\"mobile\":\"{to}\",\"text\":\"{message}\",\"apikey\":\"{apiKey}\"}",
                "X-Auth-Token", "{apiKey}");

        assertTrue(provider.send("9876543210", "483921", "msg"));

        assertTrue(receivedBody.get().contains("\"mobile\":\"9876543210\""));
        assertTrue(receivedBody.get().contains("\"text\":\"msg\""));
        assertTrue(receivedBody.get().contains("\"apikey\":\"key-123\""));
        assertEquals("key-123", receivedXAuth.get());
    }

    @Test
    void messageWithQuotesIsJsonEscaped() {
        RealSmsProvider provider = new RealSmsProvider(baseUrl + "/send", "key-123", "s", "DHATCHA");

        assertTrue(provider.send("9876543210", "483921", "Say \"hi\" \\ now"));

        assertTrue(receivedBody.get().contains("\"message\":\"Say \\\"hi\\\" \\\\ now\""));
    }

    @Test
    void messageContainingPlaceholderTextDoesNotLeakApiKey() {
        RealSmsProvider provider = new RealSmsProvider(baseUrl + "/send", "super-secret-key", "s", "DHATCHA");

        assertTrue(provider.send("9876543210", "483921", "your code {apiKey}"));

        assertTrue(receivedBody.get().contains("your code {apiKey}"),
                "a literal {apiKey} in the message must NOT be substituted");
        assertFalse(receivedBody.get().contains("super-secret-key"));
    }

    @Test
    void non2xxResponseIsReportedAsFailure() {
        RealSmsProvider provider = new RealSmsProvider(baseUrl + "/fail", "key-123", "s", "DHATCHA");

        assertFalse(provider.send("9876543210", "483921", "msg"));
    }

    @Test
    void invalidPayloadTemplateIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RealSmsProvider(baseUrl + "/send", "key-123", "s", "DHATCHA",
                        "not json", "Authorization", "Bearer {apiKey}"));
    }

    @Test
    void missingCredentialsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RealSmsProvider("", "key-123", "s", "DHATCHA"));
        assertThrows(IllegalArgumentException.class,
                () -> new RealSmsProvider(baseUrl + "/send", "", "s", "DHATCHA"));
    }
}
