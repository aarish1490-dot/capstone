package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.service.sms.SmsProvider;

/**
 * Sends OTP messages through the configured {@link SmsProvider}.
 * Delivery concerns are isolated here so database logic never
 * talks to an SMS API directly.
 */
public class SmsService {

    private final SmsProvider provider;
    private final int expiryMinutes;

    public SmsService(SmsProvider provider) {
        this(provider, 5);
    }

    public SmsService(SmsProvider provider, int expiryMinutes) {
        this.provider = provider;
        this.expiryMinutes = expiryMinutes;
    }

    public boolean sendOtp(String mobileNumber, String otp) {
        String message = "Your DhatchinaMart verification OTP is " + otp
                + ". It expires in " + expiryMinutes + " minutes. Do not share it with anyone.";
        return provider.send(mobileNumber, otp, message);
    }
}
