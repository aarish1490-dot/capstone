package com.dhatchina.dhatchinamart.dao.impl;

import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAOImpl implements OrderDAO {

    private final DataSource dataSource;

    public OrderDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insert(Connection connection, Order order) {
        String sql = "INSERT INTO orders (buyer_id, status, total_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getBuyerId());
            ps.setString(2, order.getStatus());
            ps.setBigDecimal(3, order.getTotalAmount());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for order insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order", e);
        }
    }

    @Override
    public void insertItem(Connection connection, OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order item", e);
        }
    }

    @Override
    public List<Order> findByBuyer(long buyerId) {
        String sql = "SELECT o.id, o.buyer_id, u.name AS buyer_name, o.status, o.total_amount, o.created_at "
                + "FROM orders o JOIN users u ON u.id = o.buyer_id "
                + "WHERE o.buyer_id = ? ORDER BY o.created_at DESC, o.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapOrders(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load orders for buyer", e);
        }
    }

    @Override
    public Optional<Order> findById(long id) {
        String sql = "SELECT o.id, o.buyer_id, u.name AS buyer_name, o.status, o.total_amount, o.created_at "
                + "FROM orders o JOIN users u ON u.id = o.buyer_id WHERE o.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOrder(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load order by id", e);
        }
    }

    @Override
    public List<OrderItem> findItemsByOrderId(long orderId) {
        String sql = "SELECT oi.id, oi.order_id, oi.product_id, p.name AS product_name, "
                + "p.image_url AS product_image, oi.quantity, oi.unit_price, oi.created_at "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = ? ORDER BY oi.id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapItems(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load order items", e);
        }
    }

    @Override
    public List<Order> findContainingSeller(long sellerId) {
        String sql = "SELECT DISTINCT o.id, o.buyer_id, u.name AS buyer_name, o.status, o.total_amount, o.created_at "
                + "FROM orders o "
                + "JOIN order_items oi ON oi.order_id = o.id "
                + "JOIN products p ON p.id = oi.product_id "
                + "JOIN users u ON u.id = o.buyer_id "
                + "WHERE p.seller_id = ? ORDER BY o.created_at DESC, o.id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapOrders(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load orders containing seller products", e);
        }
    }

    @Override
    public List<OrderItem> findItemsByOrderForSeller(long orderId, long sellerId) {
        String sql = "SELECT oi.id, oi.order_id, oi.product_id, p.name AS product_name, "
                + "p.image_url AS product_image, oi.quantity, oi.unit_price, oi.created_at "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.order_id = ? AND p.seller_id = ? ORDER BY oi.id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapItems(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load seller-specific order items", e);
        }
    }

    @Override
    public boolean belongsToSeller(long orderId, long sellerId) {
        String sql = "SELECT COUNT(*) FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.order_id = ? AND p.seller_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check order seller ownership", e);
        }
    }

    @Override
    public boolean updateStatus(long orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, orderId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order status", e);
        }
    }

    @Override
    public long countAll() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count orders", e);
        }
    }

    private List<Order> mapOrders(ResultSet rs) throws SQLException {
        List<Order> orders = new ArrayList<>();
        while (rs.next()) {
            orders.add(mapOrder(rs));
        }
        return orders;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setBuyerId(rs.getLong("buyer_id"));
        order.setBuyerName(rs.getString("buyer_name"));
        order.setStatus(rs.getString("status"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }

    private List<OrderItem> mapItems(ResultSet rs) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        while (rs.next()) {
            OrderItem item = new OrderItem();
            item.setId(rs.getLong("id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setProductId(rs.getLong("product_id"));
            item.setProductName(rs.getString("product_name"));
            item.setImageUrl(rs.getString("product_image"));
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unit_price"));
            item.setCreatedAt(rs.getTimestamp("created_at"));
            items.add(item);
        }
        return items;
    }
}
