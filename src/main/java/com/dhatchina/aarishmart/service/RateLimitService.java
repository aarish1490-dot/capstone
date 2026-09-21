package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.util.DbUtil;
import com.dhatchina.aarishmart.util.RateLimiter;

import java.util.function.LongSupplier;

/**
 * Named rate-limit policies guarding the authentication surfaces.
 *
 * <p>Policies and their environment variables (all with sane defaults):
 * <ul>
 *   <li>Login failures  - AUTH_MAX_LOGIN_FAILURES (10), AUTH_WINDOW_MINUTES (15),
 *                         AUTH_LOCKOUT_MINUTES (15)</li>
 *   <li>OTP sends       - OTP_SEND_MAX_PER_WINDOW (10), OTP_WINDOW_MINUTES (15)</li>
 *   <li>OTP verify rate - OTP_VERIFY_MAX_PER_IP (20), OTP_WINDOW_MINUTES (15)</li>
 * </ul>
 */
public final class RateLimitService {

    private static final long MINUTE_MILLIS = 60_000L;

    private static final String ENV_MAX_LOGIN_FAILURES = "AUTH_MAX_LOGIN_FAILURES";
    private static final String ENV_WINDOW_MINUTES = "AUTH_WINDOW_MINUTES";
    private static final String ENV_LOCKOUT_MINUTES = "AUTH_LOCKOUT_MINUTES";
    private static final String ENV_OTP_SEND_MAX = "OTP_SEND_MAX_PER_WINDOW";
    private static final String ENV_OTP_VERIFY_MAX = "OTP_VERIFY_MAX_PER_IP";
    private static final String ENV_OTP_WINDOW_MINUTES = "OTP_WINDOW_MINUTES";

    private static final int DEFAULT_MAX_LOGIN_FAILURES = 10;
    private static final int DEFAULT_WINDOW_MINUTES = 15;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;
    private static final int DEFAULT_OTP_SEND_MAX = 10;
    private static final int DEFAULT_OTP_VERIFY_MAX = 20;

    private final RateLimiter loginFailures;
    private final RateLimiter otpSend;
    private final RateLimiter otpVerify;

    public RateLimitService() {
        this(System::currentTimeMillis);
    }

    /**
     * Package-visible constructor for deterministic tests.
     */
    RateLimitService(LongSupplier clock) {
        long windowMillis = minutes(envInt(ENV_WINDOW_MINUTES, DEFAULT_WINDOW_MINUTES));
        long otpWindowMillis = minutes(envInt(ENV_OTP_WINDOW_MINUTES, DEFAULT_WINDOW_MINUTES));
        this.loginFailures = new RateLimiter(
                windowMillis,
                envInt(ENV_MAX_LOGIN_FAILURES, DEFAULT_MAX_LOGIN_FAILURES),
                minutes(envInt(ENV_LOCKOUT_MINUTES, DEFAULT_LOCKOUT_MINUTES)),
                clock);
        this.otpSend = new RateLimiter(otpWindowMillis, envInt(ENV_OTP_SEND_MAX, DEFAULT_OTP_SEND_MAX), 0, clock);
        this.otpVerify = new RateLimiter(otpWindowMillis, envInt(ENV_OTP_VERIFY_MAX, DEFAULT_OTP_VERIFY_MAX), 0, clock);
    }

    /**
     * @return true if the email is currently locked out of email+password login.
     */
    public boolean isLoginLocked(String email) {
        return email != null && loginFailures.isLocked(email.toLowerCase());
    }

    /**
     * Records a failed email+password login attempt. Locks the account after
     * the configured number of failures within the window.
     */
    public void recordLoginFailure(String email) {
        if (email != null) {
            loginFailures.recordFailure(email.toLowerCase());
        }
    }

    /**
     * Clears login failure tracking after a successful login.
     */
    public void resetLogin(String email) {
        if (email != null) {
            loginFailures.reset(email.toLowerCase());
        }
    }

    /**
     * Checks and records one OTP send for the given mobile number.
     *
     * @return true if the send is allowed.
     */
    public boolean allowOtpSend(String mobileNumber) {
        return mobileNumber != null && otpSend.allow(mobileNumber);
    }

    /**
     * Checks and records one OTP verification attempt from the given client IP.
     *
     * @return true if the attempt is allowed.
     */
    public boolean allowOtpVerify(String ip) {
        return ip != null && otpVerify.allow(ip);
    }

    private static long minutes(long value) {
        return value * MINUTE_MILLIS;
    }

    private static int envInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(DbUtil.getEnv(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}