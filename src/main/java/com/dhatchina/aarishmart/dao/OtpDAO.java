package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.model.OtpVerification;

import java.util.Optional;

public interface OtpDAO {

    long insert(OtpVerification otp);

    Optional<OtpVerification> findLatestByUserId(long userId);

    void markVerified(long id);

    void incrementAttempts(long id);

    void invalidateByUserId(long userId);
}
