package com.dhatchina.dhatchinamart.dao.impl;

import com.dhatchina.dhatchinamart.dao.OtpDAO;
import com.dhatchina.dhatchinamart.model.OtpVerification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class OtpDAOImpl implements OtpDAO {

    private static final String COLUMNS =
            "SELECT id, user_id, otp_hash, expires_at, attempts, verified, created_at FROM otp_verifications";

    private final DataSource dataSource;

    public OtpDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insert(OtpVerification otp) {
        String sql = "INSERT INTO otp_verifications (user_id, otp_hash, expires_at, attempts, verified) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, otp.getUserId());
            ps.setString(2, otp.getOtpHash());
            ps.setTimestamp(3, otp.getExpiresAt());
            ps.setInt(4, otp.getAttempts());
            ps.setBoolean(5, otp.isVerified());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for otp insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert otp verification", e);
        }
    }

    @Override
    public Optional<OtpVerification> findLatestByUserId(long userId) {
        String sql = COLUMNS + " WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find latest otp verification", e);
        }
    }

    @Override
    public void markVerified(long id) {
        update("UPDATE otp_verifications SET verified = TRUE WHERE id = ?", id);
    }

    @Override
    public void incrementAttempts(long id) {
        update("UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = ?", id);
    }

    @Override
    public void invalidateByUserId(long userId) {
        String sql = "UPDATE otp_verifications SET verified = TRUE WHERE user_id = ? AND verified = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to invalidate otp verifications", e);
        }
    }

    private void update(String sql, long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update otp verification", e);
        }
    }

    private OtpVerification map(ResultSet rs) throws SQLException {
        OtpVerification otp = new OtpVerification();
        otp.setId(rs.getLong("id"));
        otp.setUserId(rs.getLong("user_id"));
        otp.setOtpHash(rs.getString("otp_hash"));
        otp.setExpiresAt(rs.getTimestamp("expires_at"));
        otp.setAttempts(rs.getInt("attempts"));
        otp.setVerified(rs.getBoolean("verified"));
        otp.setCreatedAt(rs.getTimestamp("created_at"));
        return otp;
    }
}
