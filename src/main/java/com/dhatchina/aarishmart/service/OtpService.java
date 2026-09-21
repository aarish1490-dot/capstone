package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.OtpDAO;
import com.dhatchina.aarishmart.exception.AppException;
import com.dhatchina.aarishmart.model.OtpVerification;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.util.DbUtil;
import com.dhatchina.aarishmart.util.OtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Generates, stores, sends and verifies one-time passwords.
 *
 * Security rules enforced here:
 * - 6-digit OTP generated with {@link SecureRandom} (never Math.random()).
 * - OTP is stored as a SHA-256 hash, never in plain text.
 * - Expiry (default 5 minutes) and maximum verification attempts (default 5).
 * - One-time use: a used or replaced OTP can never verify again.
 * - Resend cooldown (default 60 seconds) limits OTP request rates.
 */
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_BOUND = 1_000_000;

    private final OtpDAO otpDAO;
    private final SmsService smsService;
    private final int expiryMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;
    private final boolean developmentMode;

    /**
     * The OTP most recently delivered in DEVELOPMENT mode only (mock SMS
     * provider). Never populated when {@code OTP_MODE=production} - see
     * {@link #getLastDevOtp()}.
     */
    private volatile String lastDevOtp;

    public OtpService(OtpDAO otpDAO, SmsService smsService) {
        this(otpDAO, smsService,
                Integer.parseInt(DbUtil.getEnv("OTP_EXPIRY_MINUTES", "5")),
                Integer.parseInt(DbUtil.getEnv("OTP_MAX_ATTEMPTS", "5")),
                Integer.parseInt(DbUtil.getEnv("OTP_RESEND_COOLDOWN_SECONDS", "60")));
    }

    public OtpService(OtpDAO otpDAO, SmsService smsService,
                      int expiryMinutes, int maxAttempts, int resendCooldownSeconds) {
        this(otpDAO, smsService, expiryMinutes, maxAttempts, resendCooldownSeconds,
                !"production".equalsIgnoreCase(DbUtil.getEnv("OTP_MODE", "development")));
    }

    public OtpService(OtpDAO otpDAO, SmsService smsService,
                      int expiryMinutes, int maxAttempts, int resendCooldownSeconds,
                      boolean developmentMode) {
        this.otpDAO = otpDAO;
        this.smsService = smsService;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.developmentMode = developmentMode;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    /**
     * Returns the OTP delivered by the last {@link #sendOtp(User)} call in
     * DEVELOPMENT mode (mock provider), or {@code null} in production mode.
     * UI code may show this value ONLY when {@link #isDevelopmentMode()}.
     */
    public String getLastDevOtp() {
        return lastDevOtp;
    }

    /**
     * Generates a new OTP for the user, invalidates any previous OTP,
     * persists it (hashed) and delivers it through the SMS provider.
     */
    public void sendOtp(User user) {
        long now = System.currentTimeMillis();
        Optional<OtpVerification> latest = otpDAO.findLatestByUserId(user.getId());
        if (latest.isPresent() && !latest.get().isVerified()) {
            long secondsSince = (now - latest.get().getCreatedAt().getTime()) / 1000;
            if (secondsSince < resendCooldownSeconds) {
                long wait = resendCooldownSeconds - secondsSince;
                throw new AppException("Please wait " + wait + " seconds before requesting another OTP.");
            }
        }

        otpDAO.invalidateByUserId(user.getId());

        String otp = generateOtp();
        OtpVerification verification = new OtpVerification();
        verification.setUserId(user.getId());
        verification.setOtpHash(OtpUtil.hashOtp(otp));
        verification.setExpiresAt(new Timestamp(now + expiryMinutes * 60_000L));
        verification.setAttempts(0);
        verification.setVerified(false);
        otpDAO.insert(verification);

        boolean delivered = smsService.sendOtp(user.getMobileNumber(), otp);
        if (!delivered) {
            throw new AppException("Could not send the OTP. Please try again later.");
        }
        if (developmentMode) {
            this.lastDevOtp = otp;
        }
        log.info("OTP issued for user id={}, mobile={}", user.getId(),
                OtpUtil.maskMobile(user.getMobileNumber()));
    }

    /**
     * Verifies the submitted OTP for the user. Throws {@link AppException}
     * with a user-friendly message on any failure.
     */
    public void verifyOtp(long userId, String code) {
        if (code == null || code.isBlank()) {
            throw new AppException("Please enter the OTP.");
        }
        Optional<OtpVerification> latest = otpDAO.findLatestByUserId(userId);
        if (latest.isEmpty()) {
            throw new AppException("No active OTP found. Please request a new one.");
        }
        OtpVerification verification = latest.get();
        if (verification.isVerified()) {
            throw new AppException("This OTP has already been used. Please request a new one.");
        }
        if (verification.getExpiresAt().getTime() < System.currentTimeMillis()) {
            otpDAO.markVerified(verification.getId());
            throw new AppException("OTP has expired. Please request a new one.");
        }
        if (verification.getAttempts() >= maxAttempts) {
            throw new AppException("Too many incorrect attempts. Please request a new OTP.");
        }

        otpDAO.incrementAttempts(verification.getId());
        if (!OtpUtil.constantTimeEquals(OtpUtil.hashOtp(code), verification.getOtpHash())) {
            throw new AppException("Invalid OTP. Please try again.");
        }
        otpDAO.markVerified(verification.getId());
        log.info("OTP verified for user id={}", userId);
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));
    }
}
