package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.dto.CartRow;
import com.dhatchina.aarishmart.model.CartItem;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface CartDAO {

    Optional<CartItem> findByUserAndProduct(long userId, long productId);

    void insert(CartItem item);

    void updateQuantity(long userId, long productId, int quantity);

    void delete(long userId, long productId);

    void clearForUser(long userId);

    void clearForUser(Connection connection, long userId);

    int countByUser(long userId);

    List<CartItem> findByUserId(long userId);

    List<CartRow> findRowsByUserId(long userId);
}
