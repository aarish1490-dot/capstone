package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.exception.RateLimitException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.service.AiService;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JSON endpoint for the AI shopping assistant.
 *
 * <p>{@code POST /api/chat} with body {@code {"message":"..."}} returns:
 * <pre>
 *   success: {"success":true,"data":{"reply":"..."},"error":null}
 *   error:   {"success":false,"data":null,"error":"..."}
 * </pre>
 *
 * <p>Guests may chat (the assistant only answers public marketplace topics),
 * but every caller needs a session, which CSRF protection already guarantees.
 * Per-session rate limiting is enforced by {@link AiService}.
 */
@WebServlet("/api/chat")
public class AiServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AiServlet.class);

    private static final int SC_TOO_MANY_REQUESTS = 429;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String message = readMessage(req);
            if (message == null || message.isBlank()) {
                throw new ValidationException("Please enter a message.");
            }
            message = message.trim();
            String reply = ServiceRegistry.getAiService().chat(req.getSession(true), message);
            writeSuccess(resp, reply);
        } catch (ValidationException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, e.getMessage());
        } catch (RateLimitException e) {
            resp.setStatus(SC_TOO_MANY_REQUESTS);
            writeError(resp, e.getMessage());
        } catch (Exception e) {
            log.error("AI chat request failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Something went wrong. Please try again.");
        }
    }

    private String readMessage(HttpServletRequest req) throws IOException {
        String body;
        try (var reader = req.getReader()) {
            body = reader.lines().reduce("", (a, line) -> a.isEmpty() ? line : a + "\n" + line);
        }
        if (body == null || body.isBlank()) {
            throw new ValidationException("Please enter a message.");
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return json.has("message") && !json.get("message").isJsonNull()
                    ? json.get("message").getAsString()
                    : null;
        } catch (JsonSyntaxException | IllegalStateException | UnsupportedOperationException e) {
            throw new ValidationException("Please enter a valid message.");
        }
    }

    private void writeSuccess(HttpServletResponse resp, String reply) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("reply", reply);
        JsonObject envelope = new JsonObject();
        envelope.addProperty("success", true);
        envelope.add("data", data);
        envelope.add("error", null);
        resp.getWriter().write(envelope.toString());
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("success", false);
        envelope.add("data", null);
        envelope.addProperty("error", message);
        resp.getWriter().write(envelope.toString());
    }
}