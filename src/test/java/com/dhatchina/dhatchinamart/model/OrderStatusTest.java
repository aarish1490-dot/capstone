package com.dhatchina.dhatchinamart.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTest {

    @Test
    void lifecycleAdvancesOneStepAtATime() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.PENDING.next());
        assertEquals(OrderStatus.SHIPPED, OrderStatus.CONFIRMED.next());
        assertEquals(OrderStatus.DELIVERED, OrderStatus.SHIPPED.next());
    }

    @Test
    void deliveredIsTerminal() {
        assertNull(OrderStatus.DELIVERED.next());
    }

    @Test
    void fromIsCaseInsensitive() {
        assertEquals(OrderStatus.PENDING, OrderStatus.from("pending"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.from("ShiPpEd"));
    }

    @Test
    void fromRejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.from("FULFILLED"));
    }
}