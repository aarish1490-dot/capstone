package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.model.Product;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProductDAO {

    long insert(Product product);

    boolean update(Product product);

    boolean delete(long id);

    Optional<Product> findById(long id);

    List<Product> find(String keyword, String category);

    List<String> findCategories();

    List<Product> findBySeller(long sellerId);

    long countAll();

    long countBySeller(long sellerId);

    boolean decrementStock(Connection connection, long productId, int quantity);
}
