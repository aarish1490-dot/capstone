package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.dto.SellerStats;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.OrderItem;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface OrderDAO {

    long insert(Connection connection, Order order);

    void insertItem(Connection connection, OrderItem item);

    List<Order> findByBuyer(long buyerId);

    List<Order> findAll();

    Optional<Order> findById(long id);

    List<OrderItem> findItemsByOrderId(long orderId);

    List<Order> findContainingSeller(long sellerId);

    List<OrderItem> findItemsByOrderForSeller(long orderId, long sellerId);

    boolean belongsToSeller(long orderId, long sellerId);

    boolean updateStatus(long orderId, String status);

    SellerStats findSellerSales(long sellerId);

    long countAll();
}
