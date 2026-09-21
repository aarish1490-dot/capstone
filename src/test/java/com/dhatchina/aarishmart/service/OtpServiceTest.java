package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.OtpDAO;
import com.dhatchina.aarishmart.exception.AppException;
import com.dhatchina.aarishmart.model.OtpVerification;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.sms.MockSmsProvider;
import com.dhatchina.aarishmart.service.sms.SmsProvider;
import com.dhatchina.aarishmart.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final long USER_ID = 42L;
    private static final String MOBILE = "9876543210";

    @Mock
    private OtpDAO otpDAO;

    private MockSmsProvider mockSmsProvider;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        mockSmsProvider = new MockSmsProvider();
        otpService = new OtpService(otpDAO, new SmsService(mockSmsProvider), 5, 5, 60);
    }

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Test User");
        user.setEmail("test@aarishmart.com");
        user.setMobileNumber(MOBILE);
        return user;
    }

    private OtpVerification otpRow(long createdAtAgoMillis, long expiresInMillis,
                                   int attempts, boolean verified, String otpHash) {
        OtpVerification row = new OtpVerification();
        row.setId(7L);
        row.setUserId(USER_ID);
        row.setOtpHash(otpHash);
        row.setExpiresAt(new Timestamp(System.currentTimeMillis() + expiresInMillis));
        row.setAttempts(attempts);
        row.setVerified(verified);
        row.setCreatedAt(new Timestamp(System.currentTimeMillis() - createdAtAgoMillis));
        return row;
    }

    @Test
    void sendOtpGeneratesSecureSixDigitOtp() {
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        otpService.sendOtp(user());

        assertTrue(mockSmsProvider.getLastOtp().matches("\\d{6}"),
                "OTP must be exactly 6 digits");
        assertEquals(MOBILE, mockSmsProvider.getLastMobileNumber());
        verify(otpDAO).invalidateByUserId(USER_ID);
    }

    @Test
    void otpIsStoredHashedNotPlain() {
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        otpService.sendOtp(user());

        String sentOtp = mockSmsProvider.getLastOtp();
        verify(otpDAO).insert(any(OtpVerification.class));
        // capture what was actually passed to the DAO
        java.util.concurrent.atomic.AtomicReference<OtpVerification> stored =
                new java.util.concurrent.atomic.AtomicReference<>();
        verify(otpDAO).insert(org.mockito.ArgumentMatchers.argThat(v -> {
            stored.set(v);
            return true;
        }));
        OtpVerification row = stored.get();
        assertNotEquals(sentOtp, row.getOtpHash(), "plain OTP must never be persisted");
        assertEquals(64, row.getOtpHash().length(), "hash must be SHA-256 hex");
        assertEquals(OtpUtil.hashOtp(sentOtp), row.getOtpHash());
    }

    @Test
    void otpExpiresAfterConfiguredWindow() {
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());
        otpService.sendOtp(user());

        java.util.concurrent.atomic.AtomicReference<OtpVerification> stored =
                new java.util.concurrent.atomic.AtomicReference<>();
        verify(otpDAO).insert(org.mockito.ArgumentMatchers.argThat(v -> {
            stored.set(v);
            return true;
        }));
        long delta = stored.get().getExpiresAt().getTime() - System.currentTimeMillis();
        assertTrue(delta > 4 * 60_000 && delta <= 5 * 60_000, "expiry must be ~5 minutes");
    }

    @Test
    void correctOtpVerifiesAndIsMarkedUsed() {
        String otp = "483921";
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, 5 * 60_000, 0, false, OtpUtil.hashOtp(otp))));

        otpService.verifyOtp(USER_ID, otp);

        verify(otpDAO).incrementAttempts(7L);
        verify(otpDAO).markVerified(7L);
    }

    @Test
    void incorrectOtpIsRejected() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, 5 * 60_000, 0, false, OtpUtil.hashOtp("111111"))));

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, "000000"));

        assertEquals("Invalid OTP. Please try again.", ex.getMessage());
        verify(otpDAO).incrementAttempts(7L);
        verify(otpDAO, never()).markVerified(7L);
    }

    @Test
    void expiredOtpIsRejected() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, -1, 0, false, OtpUtil.hashOtp("483921"))));

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, "483921"));

        assertTrue(ex.getMessage().contains("expired"));
        verify(otpDAO).markVerified(7L);
    }

    @Test
    void usedOtpCannotBeReused() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, 5 * 60_000, 1, true, OtpUtil.hashOtp("483921"))));

        assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, "483921"));
        verify(otpDAO, never()).incrementAttempts(anyLong());
    }

    @Test
    void moreThanMaxAttemptsAreBlocked() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, 5 * 60_000, 5, false, OtpUtil.hashOtp("483921"))));

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, "483921"));

        assertTrue(ex.getMessage().contains("Too many incorrect attempts"));
        verify(otpDAO, never()).incrementAttempts(anyLong());
    }

    @Test
    void resendBeforeCooldownIsBlocked() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(10_000, 5 * 60_000, 0, false, OtpUtil.hashOtp("483921"))));

        AppException ex = assertThrows(AppException.class, () -> otpService.sendOtp(user()));

        assertTrue(ex.getMessage().contains("wait"));
        verify(otpDAO, never()).insert(any(OtpVerification.class));
    }

    @Test
    void resendAfterCooldownIsAllowed() {
        when(otpDAO.findLatestByUserId(USER_ID))
                .thenReturn(Optional.of(otpRow(90_000, 5 * 60_000, 0, false, OtpUtil.hashOtp("483921"))));

        otpService.sendOtp(user());

        verify(otpDAO).insert(any(OtpVerification.class));
    }

    @Test
    void newOtpInvalidatesPreviousOtp() {
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        otpService.sendOtp(user());
        String first = mockSmsProvider.getLastOtp();
        otpService.sendOtp(user());
        String second = mockSmsProvider.getLastOtp();

        verify(otpDAO, org.mockito.Mockito.times(2)).invalidateByUserId(USER_ID);
        assertNotEquals(first, second, "each send must produce a fresh OTP");
    }

    @Test
    void verifyWithEmptyCodeThrows() {
        assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, ""));
        assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, null));
    }

    @Test
    void verifyWithNoActiveOtpThrows() {
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> otpService.verifyOtp(USER_ID, "483921"));
    }

    @Test
    void smsProviderFailureRaisesFriendlyError() {
        SmsProvider failingProvider = (mobileNumber, otp, message) -> false;
        otpService = new OtpService(otpDAO, new SmsService(failingProvider), 5, 5, 60);
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> otpService.sendOtp(user()));

        assertTrue(ex.getMessage().contains("Could not send the OTP"));
    }

    @Test
    void mockProviderDeliversAndTracksLastOtp() {
        assertTrue(mockSmsProvider.send(MOBILE, "483921", "msg"));

        assertEquals("483921", mockSmsProvider.getLastOtp());
        assertEquals(MOBILE, mockSmsProvider.getLastMobileNumber());
    }

    @Test
    void developmentModeExposesLastDevOtp() {
        otpService = new OtpService(otpDAO, new SmsService(mockSmsProvider), 5, 5, 60, true);
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        otpService.sendOtp(user());

        assertTrue(otpService.isDevelopmentMode());
        assertEquals(mockSmsProvider.getLastOtp(), otpService.getLastDevOtp());
    }

    @Test
    void productionModeNeverExposesOtp() {
        otpService = new OtpService(otpDAO, new SmsService(mockSmsProvider), 5, 5, 60, false);
        when(otpDAO.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        otpService.sendOtp(user());

        assertTrue(!otpService.isDevelopmentMode());
        assertEquals(null, otpService.getLastDevOtp());
    }
}
