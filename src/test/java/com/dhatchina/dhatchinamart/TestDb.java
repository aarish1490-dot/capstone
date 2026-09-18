package com.dhatchina.dhatchinamart.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDb {

    private TestDb() {
    }

    public static DataSource newDataSource(String name) {
        return newDataSource(name, 5);
    }

    public static DataSource newDataSource(String name, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(poolSize);
        HikariDataSource dataSource = new HikariDataSource(config);
        runScript(dataSource, "db/schema.sql");
        runScript(dataSource, "db/seed.sql");
        return dataSource;
    }

    private static void runScript(DataSource dataSource, String resource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             InputStream in = TestDb.class.getClassLoader().getResourceAsStream(resource);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    stmt.execute(current.toString());
                    current.setLength(0);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run script " + resource, e);
        }
    }
}
