package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dto.SellerOrderView;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.CartItem;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;
import com.dhatchina.dhatchinamart.model.OrderStatus;
import com.dhatchina.dhatchinamart.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DataSource dataSource;
    private final OrderDAO orderDAO;
    private final CartDAO cartDAO;
    private final ProductDAO productDAO;

    public OrderService(DataSource dataSource, OrderDAO orderDAO, CartDAO cartDAO, ProductDAO productDAO) {
        this.dataSource = dataSource;
        this.orderDAO = orderDAO;
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
    }

    /**
     * Places an order for the buyer's current cart. The order, its items, the
     * stock decrements and the cart clear all happen inside a single
     * transaction: either everything commits or the transaction rolls back. All
     * prices and the total are recomputed from the products table inside the
     * transaction &mdash; the browser/cart figures are never trusted.
     */
    public Order placeOrder(long buyerId) {
        List<CartItem> cartItems = cartDAO.findByUserId(buyerId);
        if (cartItems.isEmpty()) {
            throw new ValidationException("Your cart is empty");
        }

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.PENDING.name());

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<Long, Product> products = new LinkedHashMap<>();
                List<OrderItem> orderItems = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;

                for (CartItem item : cartItems) {
                    Product product = productDAO.findById(conn, item.getProductId())
                            .orElseThrow(() -> new ValidationException("A product in your cart is no longer available"));
                    products.put(item.getProductId(), product);
                    if (product.getStockQty() < item.getQuantity()) {
                        throw new ValidationException(
                                "Only " + product.getStockQty() + " unit(s) of \"" + product.getName()
                                        + "\" are available in stock");
                    }
                    total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(item.getProductId());
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setUnitPrice(product.getPrice());
                    orderItems.add(orderItem);
                }
                order.setTotalAmount(total);

                long orderId = orderDAO.insert(conn, order);
                order.setId(orderId);
                for (OrderItem orderItem : orderItems) {
                    orderItem.setOrderId(orderId);
                    orderDAO.insertItem(conn, orderItem);
                    if (!productDAO.decrementStock(conn, orderItem.getProductId(), orderItem.getQuantity())) {
                        Product product = products.get(orderItem.getProductId());
                        String name = product != null ? product.getName() : "product in your cart";
                        throw new ValidationException("\"" + name + "\" is currently out of stock.");
                    }
                }
                cartDAO.clearForUser(conn, buyerId);
                conn.commit();
                log.info("Order placed: id={}, buyerId={}, total={}", orderId, buyerId, total);
                return order;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new RuntimeException("Failed to place order", e);
            } catch (RuntimeException e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to place order", e);
        }
    }

    public List<Order> ordersForBuyer(long buyerId) {
        return orderDAO.findByBuyer(buyerId);
    }

    public Order getOrderForBuyer(long orderId, long buyerId) {
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getBuyerId() != buyerId) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    public List<OrderItem> itemsForOrder(long orderId) {
        return orderDAO.findItemsByOrderId(orderId);
    }

    /**
     * Every order that contains at least one product belonging to the seller,
     * with only the seller's own line items attached.
     */
    public List<SellerOrderView> ordersForSeller(long sellerId) {
        List<SellerOrderView> views = new ArrayList<>();
        for (Order order : orderDAO.findContainingSeller(sellerId)) {
            views.add(new SellerOrderView(order, orderDAO.findItemsByOrderForSeller(order.getId(), sellerId)));
        }
        return views;
    }

    /**
     * Advances an order one step through the lifecycle. Only a seller whose
     * products are part of the order may update it; a seller without any items
     * in the order gets the same 404 as a non-existent order, and an invalid or
     * terminal transition is rejected.
     */
    public void advanceOrderStatus(long sellerId, long orderId) {
        if (!orderDAO.belongsToSeller(orderId, sellerId)) {
            throw new NotFoundException("Order not found");
        }
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        OrderStatus current;
        try {
            current = OrderStatus.from(order.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Order is in an unknown state");
        }
        OrderStatus next = current.next();
        if (next == null) {
            throw new ValidationException("Order is already " + current.name().toLowerCase());
        }
        orderDAO.updateStatus(orderId, next.name());
        log.info("Order {} advanced from {} to {} by seller {}", orderId, current, next, sellerId);
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            log.warn("Rollback failed", ex);
        }
    }
}