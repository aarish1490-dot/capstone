package com.dhatchina.dhatchinamart.service.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsProviderFactoryTest {

    @Test
    void developmentModeDefaultsToMock() {
        SmsProvider provider = SmsProviderFactory.create(false, null, "", "", "", "");

        assertTrue(provider instanceof MockSmsProvider);
    }

    @Test
    void developmentModeWithExplicitMock() {
        SmsProvider provider = SmsProviderFactory.create(false, "mock", "", "", "", "");

        assertTrue(provider instanceof MockSmsProvider);
    }

    @Test
    void developmentModeWithRealProviderConfig() {
        SmsProvider provider = SmsProviderFactory.create(
                false, "real", "https://sms.example.com/v1/send", "key-123", "secret", "DHATCHA");

        assertTrue(provider instanceof RealSmsProvider);
    }

    @Test
    void realProviderWithoutCredentialsThrows() {
        assertThrows(IllegalStateException.class,
                () -> SmsProviderFactory.create(false, "real", "", "", "", ""));
        assertThrows(IllegalStateException.class,
                () -> SmsProviderFactory.create(false, "real", "https://sms.example.com", "", "", ""));
    }

    @Test
    void productionModeRefusesMock() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SmsProviderFactory.create(true, "mock", "", "", "", ""));
        assertTrue(ex.getMessage().contains("SMS_PROVIDER=real"),
                "production mode must refuse the mock provider with a clear message");
    }

    @Test
    void productionModeWithoutCredentialsThrows() {
        assertThrows(IllegalStateException.class,
                () -> SmsProviderFactory.create(true, "real", "", "", "", ""));
    }

    @Test
    void productionModeWithRealProviderWorks() {
        SmsProvider provider = SmsProviderFactory.create(
                true, "real", "https://sms.example.com/v1/send", "key-123", "secret", "DHATCHA");

        assertTrue(provider instanceof RealSmsProvider);
        assertEquals("https://sms.example.com/v1/send",
                ((RealSmsProvider) provider).getApiUrl());
    }
}
