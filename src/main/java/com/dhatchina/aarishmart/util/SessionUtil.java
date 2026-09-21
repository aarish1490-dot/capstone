package com.dhatchina.aarishmart.util;

import com.dhatchina.aarishmart.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class SessionUtil {

    public static final String SESSION_USER = "user";

    private SessionUtil() {
    }

    public static User getUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(SESSION_USER);
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getUser(request) != null;
    }

    /**
     * Best-effort real client IP, honouring the X-Forwarded-For header when the
     * app runs behind a proxy. Returns the first hop so spoofed headers cannot
     * override a direct connection.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            if (!first.isBlank()) {
                return first.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
