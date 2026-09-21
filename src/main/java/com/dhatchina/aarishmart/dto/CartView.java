package com.dhatchina.aarishmart.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartView {

    private final List<CartLine> lines = new ArrayList<>();
    private int unavailableRemoved;
    private int quantityReduced;

    public void addLine(CartLine line) {
        lines.add(line);
    }

    public List<CartLine> getLines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int getCount() {
        return lines.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public BigDecimal getTotal() {
        return lines.stream()
                .map(CartLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void incrementUnavailable() {
        unavailableRemoved++;
    }

    public void incrementReduced() {
        quantityReduced++;
    }

    public int getUnavailableRemoved() {
        return unavailableRemoved;
    }

    public int getQuantityReduced() {
        return quantityReduced;
    }

    public boolean isHasWarnings() {
        return unavailableRemoved > 0 || quantityReduced > 0;
    }
}
