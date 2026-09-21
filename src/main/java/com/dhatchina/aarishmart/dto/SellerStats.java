package com.dhatchina.aarishmart.dto;

import java.math.BigDecimal;

/**
 * Aggregated sales figures for a seller's own line items across all orders:
 * how many orders contained one of their products, how many units were sold
 * and the revenue generated (quantity x unit price).
 */
public class SellerStats {

    private long orderCount;
    private long unitsSold;
    private BigDecimal revenue = BigDecimal.ZERO;

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}