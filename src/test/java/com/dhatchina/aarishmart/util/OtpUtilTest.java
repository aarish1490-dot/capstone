package com.dhatchina.aarishmart.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpUtilTest {

    @Test
    void hashOtpIsDeterministicSha256Hex() {
        String hash = OtpUtil.hashOtp("483921");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(hash, OtpUtil.hashOtp("483921"));
        assertNotEquals("483921", hash, "plain OTP must never equal its hash");
    }

    @Test
    void hashOtpDiffersBetweenValues() {
        assertNotEquals(OtpUtil.hashOtp("483921"), OtpUtil.hashOtp("483922"));
    }

    @Test
    void constantTimeEqualsComparesSecurely() {
        assertTrue(OtpUtil.constantTimeEquals("483921", "483921"));
        assertFalse(OtpUtil.constantTimeEquals("483921", "483922"));
        assertFalse(OtpUtil.constantTimeEquals(null, "483921"));
        assertFalse(OtpUtil.constantTimeEquals("483921", null));
    }

    @Test
    void maskMobileKeepsOnlyLastFourDigits() {
        assertEquals("******3210", OtpUtil.maskMobile("9876543210"));
        assertEquals("*8765", OtpUtil.maskMobile("98765"));
        assertEquals("****", OtpUtil.maskMobile("1234"));
        assertEquals("", OtpUtil.maskMobile(null));
        assertEquals("", OtpUtil.maskMobile(""));
    }
}
