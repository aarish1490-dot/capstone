package com.dhatchina.dhatchinamart.exception;

/**
 * Thrown when a client exceeds a rate limit, e.g. too many AI chat messages
 * within a short window. Carries a user-facing friendly message.
 */
public class RateLimitException extends AppException {

    public RateLimitException(String message) {
        super(message);
    }
}