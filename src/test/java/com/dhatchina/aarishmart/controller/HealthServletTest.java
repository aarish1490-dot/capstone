package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.util.DbUtil;
import com.dhatchina.aarishmart.util.TestDb;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthServletTest {

    private final StringWriter body = new StringWriter();

    @AfterEach
    void tearDown() {
        DbUtil.close();
    }

    private HttpServletResponse mockResponse() throws Exception {
        body.getBuffer().setLength(0);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        return response;
    }

    private JsonObject callGet(HttpServletResponse response) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        new HealthServlet().doGet(request, response);
        body.flush();
        return JsonParser.parseString(body.toString()).getAsJsonObject();
    }

    @Test
    void reportsUpWhenDatabaseIsReachable() throws Exception {
        DbUtil.init((HikariDataSource) TestDb.newDataSource("health_up"));
        HttpServletResponse response = mockResponse();

        JsonObject json = callGet(response);

        assertTrue(json.get("success").getAsBoolean());
        assertTrue(json.get("error").isJsonNull());
        JsonObject data = json.getAsJsonObject("data");
        assertEquals("aarishmart", data.get("service").getAsString());
        assertEquals("UP", data.get("status").getAsString());
        assertEquals("UP", data.get("database").getAsString());
    }

    @Test
    void returns503WithGenericBodyWhenDatabaseUnavailable() throws Exception {
        HikariDataSource pool = (HikariDataSource) TestDb.newDataSource("health_down");
        DbUtil.init(pool);
        pool.close();
        HttpServletResponse response = mockResponse();

        JsonObject json = callGet(response);

        verify(response).setStatus(503);
        assertFalse(json.get("success").getAsBoolean());
        assertTrue(json.get("data").isJsonNull());
        assertTrue(json.get("error").getAsString().contains("database"));
        assertFalse(json.toString().contains("jdbc:"),
                "the JDBC connection string must never appear in the health response");
    }

    @Test
    void healthEndpointIsReadOnly() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        HttpServletResponse response = mockResponse();

        new HealthServlet().service(request, response);

        verify(response).sendError(eq(405), anyString());
    }
}