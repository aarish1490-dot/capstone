package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.exception.RateLimitException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.service.AiService;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private AiService aiService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private StringWriter output;
    private AiServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        serviceRegistryMock.when(ServiceRegistry::getAiService).thenReturn(aiService);
        when(request.getSession(true)).thenReturn(session);
        output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        servlet = new AiServlet();
    }

    @AfterEach
    void tearDown() {
        serviceRegistryMock.close();
    }

    private void bodyJson(String body) throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    }

    private JsonObject parseResponse() {
        return JsonParser.parseString(output.toString()).getAsJsonObject();
    }

    @Test
    void successfulReplyUsesTheExpectedEnvelope() throws Exception {
        when(aiService.chat(session, "hello")).thenReturn("Hi! What can I help you with?");
        bodyJson("{\"message\":\"hello\"}");

        servlet.doPost(request, response);

        JsonObject json = parseResponse();
        assertEquals(true, json.get("success").getAsBoolean());
        assertTrue(json.has("data"));
        assertEquals("Hi! What can I help you with?", json.getAsJsonObject("data").get("reply").getAsString());
        assertTrue(json.get("error").isJsonNull());
        verify(response).setContentType("application/json;charset=UTF-8");
    }

    @Test
    void messageIsTrimmedServerSide() throws Exception {
        when(aiService.chat(session, "hello")).thenReturn("Hi!");
        bodyJson("{\"message\":\"   hello   \"}");

        servlet.doPost(request, response);

        assertEquals(true, parseResponse().get("success").getAsBoolean());
        verify(aiService).chat(session, "hello");
    }

    @Test
    void blankMessageReturns400() throws Exception {
        when(aiService.chat(session, "x")).thenReturn("reply");
        bodyJson("{\"message\":\"   \"}");

        servlet.doPost(request, response);

        verify(response).setStatus(400);
        JsonObject json = parseResponse();
        assertFalse(json.get("success").getAsBoolean());
        assertTrue(json.get("data").isJsonNull());
        assertTrue(json.get("error").getAsString().contains("message"));
    }

    @Test
    void emptyBodyReturns400() throws Exception {
        bodyJson("");

        servlet.doPost(request, response);

        verify(response).setStatus(400);
        assertTrue(parseResponse().get("error").getAsString().contains("message"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        bodyJson("this is not json");

        servlet.doPost(request, response);

        verify(response).setStatus(400);
        JsonObject json = parseResponse();
        assertFalse(json.get("success").getAsBoolean());
        assertTrue(json.get("error").getAsString().contains("valid message"));
    }

    @Test
    void missingMessageFieldReturns400() throws Exception {
        bodyJson("{\"other\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(400);
    }

    @Test
    void rateLimitReturns429() throws Exception {
        when(aiService.chat(session, "fast")).thenThrow(
                new RateLimitException("slow down"));
        bodyJson("{\"message\":\"fast\"}");

        servlet.doPost(request, response);

        verify(response).setStatus(429);
        JsonObject json = parseResponse();
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("slow down", json.get("error").getAsString());
    }

    @Test
    void unexpectedFailureReturns500WithGenericMessage() throws Exception {
        when(aiService.chat(session, "boom")).thenThrow(
                new RuntimeException("provider exploded"));
        bodyJson("{\"message\":\"boom\"}");

        servlet.doPost(request, response);

        verify(response).setStatus(500);
        JsonObject json = parseResponse();
        assertFalse(json.get("success").getAsBoolean());
        assertFalse(json.get("error").getAsString().contains("exploded"),
                "internal error details must never leak to the client");
        assertEquals("Something went wrong. Please try again.", json.get("error").getAsString());
    }

    @Test
    void suspiciousInputIsHandledAsPlainText() throws Exception {
        when(aiService.chat(session,
                "<script>alert('xss')</script>")).thenReturn("That's not a product question.");
        bodyJson("{\"message\":\"<script>alert('xss')</script>\"}");

        servlet.doPost(request, response);

        assertEquals(true, parseResponse().get("success").getAsBoolean());
        verify(aiService).chat(session, "<script>alert('xss')</script>");
    }
}