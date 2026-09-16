package com.dhatchina.dhatchinamart.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Synchronizer-token pattern CSRF protection.
 *
 * <p>A random token is stored in the session and must be submitted with every
 * state-changing request, either as the {@code _csrf} form field or the
 * {@code X-CSRF-Token} header (used by the cart's AJAX calls). Comparison is
 * constant-time so token values cannot be guessed via timing side channels.
 */
public final class CsrfUtil {

    public static final String SESSION_TOKEN_KEY = "csrfToken";
    public static final String REQUEST_TOKEN_ATTR = "_csrfToken";
    public static final String FORM_FIELD = "_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    /**
     * Returns the CSRF token for the current session, generating and storing a
     * new one on first use. Callers that render forms should set this on the
     * request as an attribute or read it into their JSP directly.
     */
    public static String getToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = peekToken(session);
        if (token == null) {
            token = generateToken();
            session.setAttribute(SESSION_TOKEN_KEY, token);
        }
        return token;
    }

    /**
     * Returns the stored token without creating a session, or null if none.
     */
    public static String peekToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : peekToken(session);
    }

    private static String peekToken(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(SESSION_TOKEN_KEY);
    }

    /**
     * 256-bit random token hex-encoded (64 characters).
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Constant-time comparison of the session token against the submitted token.
     */
    public static boolean isValid(String sessionToken, String submitted) {
        if (sessionToken == null || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sessionToken.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the submitted token (header preferred, form field fallback)
     * against the token stored in the session.
     */
    public static boolean isValidFor(HttpServletRequest request) {
        String sessionToken = peekToken(request);
        if (sessionToken == null) {
            return false;
        }
        String submitted = request.getHeader(HEADER_NAME);
        if (submitted == null || submitted.isBlank()) {
            submitted = request.getParameter(FORM_FIELD);
        }
        return isValid(sessionToken, submitted);
    }
}