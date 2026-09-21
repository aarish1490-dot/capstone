package com.dhatchina.aarishmart.dao.impl;

import com.dhatchina.aarishmart.dao.CartDAO;
import com.dhatchina.aarishmart.dto.CartRow;
import com.dhatchina.aarishmart.model.CartItem;
import com.dhatchina.aarishmart.model.Product;

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

public class CartDAOImpl implements CartDAO {

    private final DataSource dataSource;

    public CartDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<CartItem> findByUserAndProduct(long userId, long productId) {
        String sql = "SELECT id, user_id, product_id, quantity, created_at FROM cart_items "
                + "WHERE user_id = ? AND product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find cart item", e);
        }
    }

    @Override
    public void insert(CartItem item) {
        String sql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getUserId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert cart item", e);
        }
    }

    @Override
    public void updateQuantity(long userId, long productId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, userId);
            ps.setLong(3, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update cart item quantity", e);
        }
    }

    @Override
    public void delete(long userId, long productId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete cart item", e);
        }
    }

    @Override
    public void clearForUser(long userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear cart", e);
        }
    }

    @Override
    public void clearForUser(Connection connection, long userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear cart in transaction", e);
        }
    }

    @Override
    public int countByUser(long userId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count cart items", e);
        }
    }

    @Override
    public List<CartItem> findByUserId(long userId) {
        String sql = "SELECT id, user_id, product_id, quantity, created_at FROM cart_items "
                + "WHERE user_id = ? ORDER BY created_at, id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CartItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(map(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load cart items", e);
        }
    }

    @Override
    public List<CartRow> findRowsByUserId(long userId) {
        String sql = "SELECT ci.product_id, ci.quantity, "
                + "p.id AS p_id, p.seller_id AS p_seller_id, u.name AS p_seller_name, "
                + "p.name AS p_name, p.description AS p_description, p.price AS p_price, "
                + "p.stock_qty AS p_stock_qty, p.category AS p_category, "
                + "p.image_url AS p_image_url, p.created_at AS p_created_at "
                + "FROM cart_items ci "
                + "LEFT JOIN products p ON p.id = ci.product_id "
                + "LEFT JOIN users u ON u.id = p.seller_id "
                + "WHERE ci.user_id = ? ORDER BY ci.created_at, ci.id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CartRow> rows = new ArrayList<>();
                while (rs.next()) {
                    long productId = rs.getLong("product_id");
                    int quantity = rs.getInt("quantity");
                    long pId = rs.getLong("p_id");
                    rows.add(rs.wasNull()
                            ? new CartRow(productId, quantity, null)
                            : new CartRow(productId, quantity, mapProduct(rs)));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load cart rows with products", e);
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("p_id"));
        product.setSellerId(rs.getLong("p_seller_id"));
        product.setSellerName(rs.getString("p_seller_name"));
        product.setName(rs.getString("p_name"));
        product.setDescription(rs.getString("p_description"));
        BigDecimal price = rs.getBigDecimal("p_price");
        product.setPrice(price == null ? BigDecimal.ZERO : price);
        product.setStockQty(rs.getInt("p_stock_qty"));
        product.setCategory(rs.getString("p_category"));
        product.setImageUrl(rs.getString("p_image_url"));
        product.setCreatedAt(rs.getTimestamp("p_created_at"));
        return product;
    }

    private CartItem map(ResultSet rs) throws SQLException {
        CartItem item = new CartItem();
        item.setId(rs.getLong("id"));
        item.setUserId(rs.getLong("user_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setCreatedAt(rs.getTimestamp("created_at"));
        return item;
    }
}
