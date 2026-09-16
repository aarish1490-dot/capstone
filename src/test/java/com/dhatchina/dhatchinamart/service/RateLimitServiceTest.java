package com.dhatchina.dhatchinamart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private AtomicLong clock;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(1_000_000L);
        rateLimitService = new RateLimitService(clock::get);
    }

    @Test
    void loginLocksAfterConfiguredFailures() {
        assertFalse(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));
        assertFalse(rateLimitService.isLoginLocked("BUYER@DHATCHINAMART.COM"));

        for (int i = 0; i < 10; i++) {
            rateLimitService.recordLoginFailure("buyer@dhatchinamart.com");
        }

        assertTrue(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));
        assertTrue(rateLimitService.isLoginLocked("BUYER@DHATCHINAMART.COM"), "lockout must be case-insensitive");
    }

    @Test
    void failedAttemptsBelowLimitDoNotLock() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.recordLoginFailure("buyer@dhatchinamart.com");
        }

        assertFalse(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));
    }

    @Test
    void successfulLoginResetsLockout() {
        for (int i = 0; i < 10; i++) {
            rateLimitService.recordLoginFailure("buyer@dhatchinamart.com");
        }
        assertTrue(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));

        rateLimitService.resetLogin("buyer@dhatchinamart.com");

        assertFalse(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));
    }

    @Test
    void lockoutExpiresAfterWindow() {
        for (int i = 0; i < 10; i++) {
            rateLimitService.recordLoginFailure("buyer@dhatchinamart.com");
        }
        assertTrue(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));

        clock.set(clock.get() + 15 * 60_000L + 1L);

        assertFalse(rateLimitService.isLoginLocked("buyer@dhatchinamart.com"));
    }

    @Test
    void otpSendLimitedPerWindow() {
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.allowOtpSend("9876543210"));
        }
        assertFalse(rateLimitService.allowOtpSend("9876543210"), "eleventh OTP send must be blocked");
        assertTrue(rateLimitService.allowOtpSend("9999999999"), "other numbers must be unaffected");
    }

    @Test
    void otpVerifyLimitedPerIp() {
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.allowOtpVerify("203.0.113.7"));
        }
        assertFalse(rateLimitService.allowOtpVerify("203.0.113.7"));
        assertTrue(rateLimitService.allowOtpVerify("203.0.113.8"));
    }
}