package com.dhatchina.aarishmart.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private AtomicLong clock;
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(1_000_000L);
        limiter = new RateLimiter(10_000L, 3, 10_000L, clock::get);
    }

    @Test
    void allowCountsUpToWindowLimitThenBlocks() {
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertFalse(limiter.allow("key"), "fourth event inside the window must be blocked");
    }

    @Test
    void windowResetsAfterWindowElapses() {
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertFalse(limiter.allow("key"));

        clock.set(clock.get() + 10_001L);

        assertTrue(limiter.allow("key"), "new window must allow requests again");
    }

    @Test
    void recordFailureLocksOutAfterLimit() {
        assertFalse(limiter.recordFailure("email@example.com"));
        assertFalse(limiter.recordFailure("email@example.com"));
        assertTrue(limiter.recordFailure("email@example.com"), "third failure triggers lockout");
        assertTrue(limiter.isLocked("email@example.com"));
    }

    @Test
    void lockoutExpiresAfterDuration() {
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");
        assertTrue(limiter.isLocked("email@example.com"));

        clock.set(clock.get() + 10_001L);

        assertFalse(limiter.isLocked("email@example.com"));
    }

    @Test
    void lockedKeyIsBlockedEvenInsideWindow() {
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");

        assertFalse(limiter.allow("email@example.com"));
    }

    @Test
    void resetClearsState() {
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");
        limiter.recordFailure("email@example.com");
        assertTrue(limiter.isLocked("email@example.com"));

        limiter.reset("email@example.com");

        assertFalse(limiter.isLocked("email@example.com"));
        assertTrue(limiter.allow("email@example.com"));
    }

    @Test
    void keysAreTrackedIndependently() {
        assertTrue(limiter.allow("a"));
        assertTrue(limiter.allow("b"));
        assertFalse(limiter.isLocked("a"));
        assertFalse(limiter.isLocked("b"));
    }

    @Test
    void pruneRemovesIdleKeys() {
        limiter.allow("stale");
        clock.set(clock.get() + 60_000L);
        limiter.allow("fresh");

        limiter.prune(30_000L);

        assertFalse(limiter.isLocked("stale"));
        assertTrue(limiter.allow("fresh"));
    }
}