package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.util.DbUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Liveness/readiness endpoint used by the hosting platform and by smoke tests.
 *
 * <p>{@code GET /api/v1/health} returns the application status and a live
 * database connectivity check using the project's standard JSON envelope:
 * <pre>
 *   200: {"success":true,"data":{"service":"aarishmart","status":"UP","database":"UP","time":...},"error":null}
 *   503: {"success":false,"data":null,"error":"database unavailable"}
 * </pre>
 *
 * <p>The endpoint is public and read-only (GET only). It never includes
 * credentials, JDBC connection strings, stack traces or any other internal
 * configuration - only booleans and the service name.
 */
@WebServlet("/api/v1/health")
public class HealthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HealthServlet.class);

    private static final String SERVICE_NAME = "aarishmart";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        boolean databaseUp = isDatabaseUp();

        JsonObject envelope = new JsonObject();
        if (databaseUp) {
            JsonObject data = new JsonObject();
            data.addProperty("service", SERVICE_NAME);
            data.addProperty("status", "UP");
            data.addProperty("database", "UP");
            data.addProperty("time", System.currentTimeMillis());
            envelope.addProperty("success", true);
            envelope.add("data", data);
            envelope.add("error", null);
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            envelope.addProperty("success", false);
            envelope.add("data", null);
            envelope.addProperty("error", "database unavailable");
        }
        response.getWriter().write(envelope.toString());
    }

    private boolean isDatabaseUp() {
        try (Connection connection = DbUtil.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            log.warn("Health check: database connectivity check failed");
            return false;
        }
    }
}