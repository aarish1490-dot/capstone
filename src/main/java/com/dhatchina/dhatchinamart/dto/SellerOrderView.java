package com.dhatchina.dhatchinamart.dto;

import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * An order together with only the items belonging to a given seller. Used by
 * the seller console so a seller can act on the part of an order that is
 * theirs without ever seeing another seller's lines or prices beyond their own.
 */
public class SellerOrderView {

    private final Order order;
    private final List<OrderItem> items;

    public SellerOrderView(Order order, List<OrderItem> items) {
        this.order = order;
        this.items = items;
    }

    public Order getOrder() {
        return order;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }
}