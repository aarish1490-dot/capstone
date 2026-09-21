package com.dhatchina.aarishmart.dao.impl;

import com.dhatchina.aarishmart.dao.ProductDAO;
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

public class ProductDAOImpl implements ProductDAO {

    private static final String COLUMNS =
            "p.id, p.seller_id, u.name AS seller_name, p.name, p.description, p.price, p.stock_qty, p.category, p.image_url, p.is_active, p.created_at";

    private final DataSource dataSource;

    public ProductDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insert(Product product) {
        String sql = "INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, product.getSellerId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getDescription());
            ps.setBigDecimal(4, product.getPrice());
            ps.setInt(5, product.getStockQty());
            ps.setString(6, product.getCategory());
            ps.setString(7, product.getImageUrl());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for product insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert product", e);
        }
    }

    @Override
    public boolean update(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, stock_qty = ?, "
                + "category = ?, image_url = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getPrice());
            ps.setInt(4, product.getStockQty());
            ps.setString(5, product.getCategory());
            ps.setString(6, product.getImageUrl());
            ps.setLong(7, product.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product", e);
        }
    }

    @Override
    public boolean delete(long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM products p JOIN users u ON u.id = p.seller_id WHERE p.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by id", e);
        }
    }

    @Override
    public Optional<Product> findById(Connection connection, long id) {
        String sql = "SELECT " + COLUMNS + " FROM products p JOIN users u ON u.id = p.seller_id WHERE p.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by id in transaction", e);
        }
    }

    @Override
    public List<Product> find(String keyword, String category) {
        return search(keyword, category, -1, 0);
    }

    @Override
    public List<Product> find(String keyword, String category, int limit, long offset) {
        return search(keyword, category, limit, offset);
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM products p JOIN users u ON u.id = p.seller_id "
                + "ORDER BY p.created_at DESC, p.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all products", e);
        }
    }

    @Override
    public long count(String keyword, String category) {
        String sql = "SELECT COUNT(*) FROM products p "
                + "WHERE (? IS NULL OR LOWER(p.name) LIKE ? ESCAPE '\\' OR LOWER(p.description) LIKE ? ESCAPE '\\') "
                + "AND (? IS NULL OR p.category = ?) "
                + "AND p.stock_qty > 0 "
                + "AND p.is_active = TRUE";
        String pattern = escapeLike(keyword);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern == null ? null : "%" + pattern + "%");
            ps.setString(3, pattern == null ? null : "%" + pattern + "%");
            ps.setString(4, category);
            ps.setString(5, category);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count products", e);
        }
    }

    private List<Product> search(String keyword, String category, int limit, long offset) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM products p JOIN users u ON u.id = p.seller_id ")
                .append("WHERE (? IS NULL OR LOWER(p.name) LIKE ? ESCAPE '\\' OR LOWER(p.description) LIKE ? ESCAPE '\\') ")
                .append("AND (? IS NULL OR p.category = ?) ")
                .append("AND p.stock_qty > 0 ")
                .append("AND p.is_active = TRUE ")
                .append("ORDER BY p.created_at DESC, p.id DESC");
        if (limit >= 0) {
            sql.append(" LIMIT ? OFFSET ?");
        }
        String pattern = escapeLike(keyword);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, pattern);
            ps.setString(2, pattern == null ? null : "%" + pattern + "%");
            ps.setString(3, pattern == null ? null : "%" + pattern + "%");
            ps.setString(4, category);
            ps.setString(5, category);
            if (limit >= 0) {
                ps.setInt(6, limit);
                ps.setLong(7, offset);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search products", e);
        }
    }

    @Override
    public List<String> findCategories() {
        String sql = "SELECT DISTINCT category FROM products ORDER BY category";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
            return categories;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load categories", e);
        }
    }

    @Override
    public List<Product> findBySeller(long sellerId) {
        String sql = "SELECT " + COLUMNS + " FROM products p JOIN users u ON u.id = p.seller_id "
                + "WHERE p.seller_id = ? ORDER BY p.created_at DESC, p.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find products by seller", e);
        }
    }

    @Override
    public boolean updateActive(long id, boolean active) {
        String sql = "UPDATE products SET is_active = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product active flag", e);
        }
    }

    @Override
    public long countAll() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count products", e);
        }
    }

    @Override
    public long countBySeller(long sellerId) {
        String sql = "SELECT COUNT(*) FROM products WHERE seller_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count products by seller", e);
        }
    }

    @Override
    public boolean decrementStock(Connection connection, long productId, int quantity) {
        String sql = "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ? AND stock_qty >= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to decrement stock", e);
        }
    }

    private List<Product> mapList(ResultSet rs) throws SQLException {
        List<Product> products = new ArrayList<>();
        while (rs.next()) {
            products.add(map(rs));
        }
        return products;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setSellerId(rs.getLong("seller_id"));
        product.setSellerName(rs.getString("seller_name"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockQty(rs.getInt("stock_qty"));
        product.setCategory(rs.getString("category"));
        product.setActive(rs.getBoolean("is_active"));
        product.setImageUrl(rs.getString("image_url"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        return product;
    }

    private String escapeLike(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .toLowerCase();
    }
}
