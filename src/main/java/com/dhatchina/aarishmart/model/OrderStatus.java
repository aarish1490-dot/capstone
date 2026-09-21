package com.dhatchina.aarishmart.model;

/**
 * The supported order lifecycle. Orders always advance forward one step at a
 * time: PENDING &rarr; CONFIRMED &rarr; SHIPPED &rarr; DELIVERED. The status is
 * persisted as a VARCHAR in the {@code orders} table; this enum gives the
 * lifecycle its rules and a single source of truth for allowed transitions.
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED;

    private static final OrderStatus[] VALUES = values();

    /**
     * Returns the next legal status in the lifecycle, or {@code null} when the
     * current status is terminal (DELIVERED).
     */
    public OrderStatus next() {
        return ordinal() < VALUES.length - 1 ? VALUES[ordinal() + 1] : null;
    }

    public static OrderStatus from(String value) {
        for (OrderStatus status : VALUES) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + value);
    }
}