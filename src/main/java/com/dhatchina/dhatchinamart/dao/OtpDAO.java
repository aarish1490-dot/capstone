package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.model.OtpVerification;

import java.util.Optional;

public interface OtpDAO {

    long insert(OtpVerification otp);

    Optional<OtpVerification> findLatestByUserId(long userId);

    void markVerified(long id);

    void incrementAttempts(long id);

    void invalidateByUserId(long userId);
}
