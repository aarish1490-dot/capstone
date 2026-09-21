package com.dhatchina.aarishmart.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window rate limiter with optional lockout support.
 *
 * <p>Each key tracks how many events it has recorded inside the current time
 * window. Once the window limit is exceeded the key can be locked out for a
 * configurable duration (used for repeated login failures). The clock is
 * injectable so tests can control time deterministically.
 *
 * <p>State is process-local only, which is the right trade-off for a
 * single-instance MVP deployment. Prune periodically to bound memory growth.
 */
public final class RateLimiter {

    private final long windowMillis;
    private final long maxEvents;
    private final long lockoutMillis;
    private final LongSupplier clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param windowMillis  fixed-window length in milliseconds
     * @param maxEvents     maximum events allowed per window
     * @param lockoutMillis lockout duration after the limit is hit (0 = none)
     * @param clock         time source for the current timestamp in millis
     */
    public RateLimiter(long windowMillis, long maxEvents, long lockoutMillis, LongSupplier clock) {
        this.windowMillis = windowMillis;
        this.maxEvents = maxEvents;
        this.lockoutMillis = lockoutMillis;
        this.clock = clock;
    }

    public RateLimiter(long windowMillis, long maxEvents, long lockoutMillis) {
        this(windowMillis, maxEvents, lockoutMillis, System::currentTimeMillis);
    }

    /**
     * @return true if the key is currently locked out.
     */
    public boolean isLocked(String key) {
        Bucket bucket = buckets.get(key);
        return bucket != null && bucket.lockoutUntil > clock.getAsLong();
    }

    /**
     * Records one event for the key if it is neither locked out nor over the
     * window limit (e.g. an OTP send / verify request).
     *
     * @return true if the event was counted, false if it should be blocked.
     */
    public boolean allow(String key) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (bucket) {
            if (bucket.lockoutUntil > now) {
                return false;
            }
            if (bucket.windowStart == 0 || expired(bucket, now)) {
                resetWindow(bucket, now);
            }
            if (bucket.windowCount >= maxEvents) {
                return false;
            }
            bucket.windowCount++;
            bucket.lastAccess = now;
            return true;
        }
    }

    /**
     * Records a failed attempt. When the window limit is reached the key is
     * locked out (used for login attempts).
     *
     * @return true if this attempt triggered a lockout.
     */
    public boolean recordFailure(String key) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (bucket) {
            if (bucket.lockoutUntil > now) {
                return false;
            }
            if (bucket.windowStart == 0 || expired(bucket, now)) {
                resetWindow(bucket, now);
            }
            bucket.windowCount++;
            bucket.lastAccess = now;
            if (bucket.windowCount >= maxEvents && lockoutMillis > 0) {
                bucket.lockoutUntil = now + lockoutMillis;
                return true;
            }
            return false;
        }
    }

    /**
     * Clears all recorded state for a key (e.g. after a successful login).
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * Drops state for keys that have not been touched within {@code maxIdleMillis}.
     */
    public void prune(long maxIdleMillis) {
        long cutoff = clock.getAsLong() - maxIdleMillis;
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess < cutoff);
    }

    private boolean expired(Bucket bucket, long now) {
        return now - bucket.windowStart >= windowMillis;
    }

    private void resetWindow(Bucket bucket, long now) {
        bucket.windowStart = now;
        bucket.windowCount = 0;
        bucket.lastAccess = now;
    }

    private static final class Bucket {
        long windowStart;
        long windowCount;
        long lockoutUntil;
        long lastAccess;

        Bucket() {
        }
    }
}