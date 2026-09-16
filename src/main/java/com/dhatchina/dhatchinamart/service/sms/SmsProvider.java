package com.dhatchina.dhatchinamart.service.sms;

/**
 * Abstraction over the SMS delivery channel so OTP delivery can be
 * swapped between the development mock and a real SMS provider.
 */
public interface SmsProvider {

    /**
     * Sends an SMS message. Implementations must not throw on provider
     * failure; they should return {@code false} so the caller can show
     * a user-friendly message.
     */
    boolean send(String mobileNumber, String otp, String message);
}