package com.dhatchina.dhatchinamart.dao.impl;

import com.dhatchina.dhatchinamart.dao.ReviewDAO;
import com.dhatchina.dhatchinamart.model.Review;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAOImpl implements ReviewDAO {

    private static final String COLUMNS =
            "r.id, r.order_id, r.product_id, r.user_id, u.name AS buyer_name, "
                    + "r.rating, r.review_text, r.created_at";

    private final DataSource dataSource;

    public ReviewDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insert(Review review) {
        String sql = "INSERT INTO reviews (order_id, product_id, user_id, rating, review_text) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, review.getOrderId());
            ps.setLong(2, review.getProductId());
            ps.setLong(3, review.getUserId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getReviewText());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for review insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert review", e);
        }
    }

    @Override
    public Optional<Review> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM reviews r JOIN users u ON u.id = r.user_id WHERE r.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find review by id", e);
        }
    }

    @Override
    public List<Review> findByProduct(long productId) {
        String sql = "SELECT " + COLUMNS + " FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.product_id = ? ORDER BY r.created_at DESC, r.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reviews by product", e);
        }
    }

    @Override
    public List<Review> findByOrder(long orderId) {
        String sql = "SELECT " + COLUMNS + " FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.order_id = ? ORDER BY r.created_at DESC, r.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reviews by order", e);
        }
    }

    @Override
    public Optional<Review> findByOrderAndProduct(long orderId, long productId) {
        String sql = "SELECT " + COLUMNS + " FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.order_id = ? AND r.product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find review for order and product", e);
        }
    }

    @Override
    public Optional<Review> findByBuyerAndProduct(long buyerId, long productId) {
        String sql = "SELECT " + COLUMNS + " FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.user_id = ? AND r.product_id = ? ORDER BY r.created_at DESC, r.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buyerId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find review by buyer and product", e);
        }
    }

    @Override
    public long countByProduct(long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count reviews by product", e);
        }
    }

    @Override
    public Optional<BigDecimal> averageRatingForProduct(long productId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal average = rs.getBigDecimal(1);
                    return average == null ? Optional.empty() : Optional.of(average);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute average rating", e);
        }
    }

    @Override
    public Optional<Long> findEligibleDeliveredOrder(long buyerId, long productId) {
        String sql = "SELECT o.id FROM orders o "
                + "JOIN order_items oi ON oi.order_id = o.id "
                + "WHERE o.buyer_id = ? AND o.status = 'DELIVERED' AND oi.product_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.order_id = o.id AND r.product_id = ?) "
                + "ORDER BY o.created_at DESC, o.id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buyerId);
            ps.setLong(2, productId);
            ps.setLong(3, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find eligible delivered order", e);
        }
    }

    private List<Review> mapList(ResultSet rs) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        while (rs.next()) {
            reviews.add(map(rs));
        }
        return reviews;
    }

    private Review map(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setOrderId(rs.getLong("order_id"));
        review.setProductId(rs.getLong("product_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setBuyerName(rs.getString("buyer_name"));
        review.setRating(rs.getInt("rating"));
        review.setReviewText(rs.getString("review_text"));
        review.setCreatedAt(rs.getTimestamp("created_at"));
        return review;
    }
}