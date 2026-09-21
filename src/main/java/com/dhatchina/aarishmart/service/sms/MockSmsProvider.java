package com.dhatchina.aarishmart.service.sms;

import com.dhatchina.aarishmart.util.OtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DEVELOPMENT-ONLY SMS provider. Used when a real SMS provider is not
 * configured (college MVP / local demo). It never sends a real message;
 * the OTP is written to the application log instead.
 *
 * NEVER enable this as the delivery channel in production.
 */
public class MockSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsProvider.class);

    private volatile String lastOtp;
    private volatile String lastMobileNumber;

    @Override
    public boolean send(String mobileNumber, String otp, String message) {
        this.lastOtp = otp;
        this.lastMobileNumber = mobileNumber;
        log.info("[DEV OTP] Mobile: {}", OtpUtil.maskMobile(mobileNumber));
        log.info("[DEV OTP] OTP: {}", otp);
        log.info("DEVELOPMENT ONLY - mock SMS provider: no real message was sent. "
                + "Configure OTP_MODE=production with a real SMS provider for actual delivery.");
        return true;
    }

    public String getLastOtp() {
        return lastOtp;
    }

    public String getLastMobileNumber() {
        return lastMobileNumber;
    }
}