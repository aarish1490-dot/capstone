package com.dhatchina.aarishmart.dto;

import com.dhatchina.aarishmart.model.Product;

import java.math.BigDecimal;

public class CartLine {

    private final Product product;
    private final int quantity;

    public CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
