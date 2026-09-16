package com.dhatchina.dhatchinamart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal .env file loader (12-factor style). Searches, in order:
 * the working directory, ${catalina.base} (Tomcat), and the user home.
 *
 * Rules:
 * - Lines like {@code KEY=value}; {@code #} comments and blank lines ignored.
 * - Values may be single- or double-quoted.
 * - Real environment variables ALWAYS win over .env values.
 * - The file is loaded lazily once. Values are NEVER logged.
 */
public final class EnvFileLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvFileLoader.class);

    private static volatile Map<String, String> values;
    private static volatile boolean loaded;

    private EnvFileLoader() {
    }

    public static String get(String key) {
        loadIfNeeded();
        return values.get(key);
    }

    private static void loadIfNeeded() {
        if (loaded) {
            return;
        }
        synchronized (EnvFileLoader.class) {
            if (loaded) {
                return;
            }
            values = new HashMap<>();
            Path path = findEnvFile();
            if (path != null) {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String raw : lines) {
                        String line = raw.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        if (eq <= 0) {
                            continue;
                        }
                        String key = line.substring(0, eq).trim();
                        String value = stripQuotes(line.substring(eq + 1).trim());
                        if (!key.isEmpty() && !value.isEmpty()) {
                            values.put(key, value);
                        }
                    }
                    log.info("Loaded {} setting(s) from {}", values.size(), path.toAbsolutePath());
                } catch (IOException e) {
                    log.warn("Could not read .env file at {}: {}", path.toAbsolutePath(), e.getMessage());
                }
            }
            loaded = true;
        }
    }

    private static Path findEnvFile() {
        String catalinaBase = System.getProperty("catalina.base");
        String[] candidates = {
                System.getProperty("user.dir"),
                catalinaBase,
                System.getProperty("user.home")
        };
        for (String dir : candidates) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            Path candidate = Paths.get(dir, ".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Test hook: clears the cached values so a new .env can be picked up.
     */
    static synchronized void reset() {
        loaded = false;
        values = null;
    }
}
