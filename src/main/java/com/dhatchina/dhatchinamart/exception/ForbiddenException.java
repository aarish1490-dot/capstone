package com.dhatchina.dhatchinamart.exception;

/**
 * Thrown when an authenticated user attempts an operation they are not
 * entitled to (e.g. editing another seller's product).
 */
public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(message);
    }
}