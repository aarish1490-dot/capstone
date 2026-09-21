package com.dhatchina.aarishmart.util;

import com.dhatchina.aarishmart.exception.ValidationException;
import org.mindrot.jbcrypt.BCrypt;

public final class AuthUtil {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private AuthUtil() {
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean verifyPassword(String plainPassword, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hash);
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }
}
