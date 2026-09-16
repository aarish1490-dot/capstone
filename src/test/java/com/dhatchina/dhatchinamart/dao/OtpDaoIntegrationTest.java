package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.dao.impl.OtpDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.model.OtpVerification;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.AuthUtil;
import com.dhatchina.dhatchinamart.util.OtpUtil;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpDaoIntegrationTest {

    private final AtomicInteger counter = new AtomicInteger(100);

    private OtpDAO otpDAO;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDb.newDataSource("otpdaotest");
        otpDAO = new OtpDAOImpl(dataSource);
        userDAO = new UserDAOImpl(dataSource);
    }

    private long newUserId() {
        int n = counter.incrementAndGet();
        User user = new User();
        user.setName("Otp User");
        user.setEmail("otp-user-" + n + "@test.com");
        user.setMobileNumber(String.format("98765%05d", n));
        user.setPasswordHash(AuthUtil.hashPassword("Secure@123"));
        user.setRole(User.Role.BUYER);
        return userDAO.insert(user);
    }

    private OtpVerification row(long userId) {
        OtpVerification otp = new OtpVerification();
        otp.setUserId(userId);
        otp.setOtpHash(OtpUtil.hashOtp("483921"));
        otp.setExpiresAt(new Timestamp(System.currentTimeMillis() + 300_000));
        otp.setAttempts(0);
        otp.setVerified(false);
        return otp;
    }

    @Test
    void insertPersistsHashedOtpRow() {
        long userId = newUserId();
        long id = otpDAO.insert(row(userId));

        Optional<OtpVerification> found = otpDAO.findLatestByUserId(userId);
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(OtpUtil.hashOtp("483921"), found.get().getOtpHash());
        assertEquals(0, found.get().getAttempts());
        assertFalse(found.get().isVerified());
        assertTrue(found.get().getCreatedAt() != null);
    }

    @Test
    void findLatestReturnsMostRecentRow() {
        long userId = newUserId();
        otpDAO.insert(row(userId));
        OtpVerification newer = row(userId);
        newer.setOtpHash(OtpUtil.hashOtp("555666"));
        otpDAO.insert(newer);

        Optional<OtpVerification> found = otpDAO.findLatestByUserId(userId);

        assertTrue(found.isPresent());
        assertEquals(OtpUtil.hashOtp("555666"), found.get().getOtpHash());
    }

    @Test
    void markVerifiedSetsOneTimeUseFlag() {
        long userId = newUserId();
        long id = otpDAO.insert(row(userId));

        otpDAO.markVerified(id);

        Optional<OtpVerification> found = otpDAO.findLatestByUserId(userId);
        assertTrue(found.get().isVerified());
    }

    @Test
    void incrementAttemptsCountsVerificationTries() {
        long userId = newUserId();
        long id = otpDAO.insert(row(userId));

        otpDAO.incrementAttempts(id);
        otpDAO.incrementAttempts(id);

        Optional<OtpVerification> found = otpDAO.findLatestByUserId(userId);
        assertEquals(2, found.get().getAttempts());
    }

    @Test
    void invalidateByUserIdInvalidatesAllPendingOtps() {
        long userIdA = newUserId();
        long userIdB = newUserId();
        otpDAO.insert(row(userIdA));
        otpDAO.insert(row(userIdA));
        otpDAO.insert(row(userIdB));

        otpDAO.invalidateByUserId(userIdA);

        assertTrue(otpDAO.findLatestByUserId(userIdA).get().isVerified());
        assertFalse(otpDAO.findLatestByUserId(userIdB).get().isVerified(),
                "other users' OTPs must be untouched");
    }

    @Test
    void noOtpReturnsEmpty() {
        assertTrue(otpDAO.findLatestByUserId(999999L).isEmpty());
    }
}
