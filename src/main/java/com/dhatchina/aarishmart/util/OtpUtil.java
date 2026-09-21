package com.dhatchina.aarishmart.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class OtpUtil {

    private static final int MASK_KEEP = 4;
    private static final String MASK_CHAR = "*";

    private OtpUtil() {
    }

    /**
     * SHA-256 hex digest of the OTP. Used so the OTP is never stored
     * in plain text in the database.
     */
    public static String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison to avoid leaking OTP length/content via timing.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Masks a mobile number so only the last 4 digits are visible,
     * e.g. 9876543210 -> ******3210.
     */
    public static String maskMobile(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return "";
        }
        String digits = mobileNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        if (digits.length() <= MASK_KEEP) {
            return MASK_CHAR.repeat(digits.length());
        }
        String tail = digits.substring(digits.length() - MASK_KEEP);
        return MASK_CHAR.repeat(digits.length() - MASK_KEEP) + tail;
    }
}
