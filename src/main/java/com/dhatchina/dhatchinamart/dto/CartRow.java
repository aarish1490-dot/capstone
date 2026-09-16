package com.dhatchina.dhatchinamart.dto;

import com.dhatchina.dhatchinamart.model.Product;

/**
 * A single cart row loaded together with its product information via a
 * JOIN, avoiding N+1 product lookups when building the cart view.
 * {@code product} is null when the product no longer exists.
 */
public class CartRow {

    private final long productId;
    private final int quantity;
    private final Product product;

    public CartRow(long productId, int quantity, Product product) {
        this.productId = productId;
        this.quantity = quantity;
        this.product = product;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Product getProduct() {
        return product;
    }

    public boolean isAvailable() {
        return product != null;
    }
}