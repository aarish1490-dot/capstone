package com.dhatchina.aarishmart.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvFileLoaderTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void backupUserDir() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        EnvFileLoader.reset();
    }

    @AfterEach
    void restoreUserDir() {
        System.setProperty("user.dir", originalUserDir);
        EnvFileLoader.reset();
    }

    @Test
    void loadsKeyValuePairsIgnoringCommentsAndBlanks() throws IOException {
        Files.writeString(tempDir.resolve(".env"),
                "# SMS config\n\nSMS_API_URL=https://sms.example.com/v1/send\n"
                        + "SMS_API_KEY=key-123\nOTP_MODE=production\n");

        assertEquals("https://sms.example.com/v1/send", EnvFileLoader.get("SMS_API_URL"));
        assertEquals("key-123", EnvFileLoader.get("SMS_API_KEY"));
        assertEquals("production", EnvFileLoader.get("OTP_MODE"));
    }

    @Test
    void stripsQuotesAndTrimsWhitespace() throws IOException {
        Files.writeString(tempDir.resolve(".env"),
                "SMS_API_KEY = 'key-123'\nSMS_SENDER_ID=\"DHATCHA\"\n");

        assertEquals("key-123", EnvFileLoader.get("SMS_API_KEY"));
        assertEquals("DHATCHA", EnvFileLoader.get("SMS_SENDER_ID"));
    }

    @Test
    void missingFileReturnsNull() {
        assertNull(EnvFileLoader.get("SMS_API_KEY"));
    }

    @Test
    void missingKeyReturnsNull() throws IOException {
        Files.writeString(tempDir.resolve(".env"), "SMS_API_KEY=key-123\n");

        assertNull(EnvFileLoader.get("SMS_API_SECRET"));
    }
}
