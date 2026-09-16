package com.dhatchina.dhatchinamart.service.sms;

import com.dhatchina.dhatchinamart.util.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the configured SMS provider from environment variables.
 *
 * <pre>
 *   OTP_MODE=production -&gt; requires a real provider (SMS_PROVIDER=real) and
 *                          fails fast at startup if SMS_API_URL / SMS_API_KEY
 *                          are missing - the mock provider is NEVER used in
 *                          production mode.
 *   OTP_MODE=development (default)
 *     SMS_PROVIDER=mock (default) -&gt; MockSmsProvider  (DEVELOPMENT ONLY)
 *     SMS_PROVIDER=real -&gt; RealSmsProvider (validated like production)
 * </pre>
 */
public final class SmsProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(SmsProviderFactory.class);

    private SmsProviderFactory() {
    }

    public static SmsProvider create() {
        boolean production = "production".equalsIgnoreCase(DbUtil.getEnv("OTP_MODE", "development"));
        return create(production,
                DbUtil.getEnv("SMS_PROVIDER", "mock"),
                DbUtil.getEnv("SMS_API_URL", ""),
                DbUtil.getEnv("SMS_API_KEY", ""),
                DbUtil.getEnv("SMS_API_SECRET", ""),
                DbUtil.getEnv("SMS_SENDER_ID", ""),
                DbUtil.getEnv("SMS_PAYLOAD_TEMPLATE",
                        "{\"to\":\"{to}\",\"message\":\"{message}\",\"senderId\":\"{senderId}\"}"),
                DbUtil.getEnv("SMS_AUTH_HEADER", "Authorization"),
                DbUtil.getEnv("SMS_AUTH_VALUE_TEMPLATE", "Bearer {apiKey}"));
    }

    static SmsProvider create(boolean production, String provider, String apiUrl,
                              String apiKey, String apiSecret, String senderId) {
        return create(production, provider, apiUrl, apiKey, apiSecret, senderId,
                "{\"to\":\"{to}\",\"message\":\"{message}\",\"senderId\":\"{senderId}\"}",
                "Authorization", "Bearer {apiKey}");
    }

    static SmsProvider create(boolean production, String provider, String apiUrl,
                              String apiKey, String apiSecret, String senderId,
                              String payloadTemplate, String authHeader, String authValueTemplate) {
        if (production && !"real".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "OTP_MODE=production requires SMS_PROVIDER=real. The mock provider "
                            + "(OTP in logs) must never be used in production.");
        }
        if ("real".equalsIgnoreCase(provider)) {
            if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "SMS_PROVIDER=real requires the SMS_API_URL and SMS_API_KEY "
                                + "environment variables to be set. "
                                + "For local testing use SMS_PROVIDER=mock (development mode).");
            }
            return new RealSmsProvider(apiUrl, apiKey, apiSecret, senderId,
                    payloadTemplate, authHeader, authValueTemplate);
        }
        log.info("Using MockSmsProvider (DEVELOPMENT ONLY) - OTPs are printed to the "
                + "log file and shown on the login page. Configure SMS_PROVIDER=real "
                + "for actual SMS delivery.");
        return new MockSmsProvider();
    }
}
