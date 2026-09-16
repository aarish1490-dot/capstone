package com.dhatchina.dhatchinamart.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DbUtil {

    private static final String DEFAULT_URL = "jdbc:h2:file:~/dhatchinamart;AUTO_SERVER=TRUE";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "";

    private static HikariDataSource dataSource;

    private DbUtil() {
    }

    public static void init() {
        if (dataSource != null) {
            return;
        }
        String url = getEnv("DHAT_DB_URL", DEFAULT_URL);
        String user = getEnv("DHAT_DB_USER", DEFAULT_USER);
        String password = getEnv("DHAT_DB_PASSWORD", DEFAULT_PASSWORD);
        int maxPool = Integer.parseInt(getEnv("DHAT_DB_POOL_MAX", "10"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(15000);
        config.setDriverClassName("org.h2.Driver");
        dataSource = new HikariDataSource(config);
    }

    public static void init(DataSource testDataSource) {
        if (dataSource != null) {
            dataSource.close();
        }
        dataSource = (HikariDataSource) testDataSource;
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public static boolean isInitialized() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND UPPER(TABLE_NAME) = 'USERS'")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void runScript(String classpathResource) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String statement : splitStatements(classpathResource)) {
                stmt.execute(statement);
            }
        } catch (SQLException | IOException e) {
            throw new AppSqlException("Failed to execute script " + classpathResource, e);
        }
    }

    private static List<String> splitStatements(String classpathResource) throws IOException {
        List<String> statements = new ArrayList<>();
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath: " + classpathResource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(current.toString());
                    current.setLength(0);
                }
            }
        }
        return statements;
    }

    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = EnvFileLoader.get(key);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static class AppSqlException extends RuntimeException {
        AppSqlException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
