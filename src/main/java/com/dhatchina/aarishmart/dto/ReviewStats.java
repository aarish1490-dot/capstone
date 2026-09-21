package com.dhatchina.aarishmart.dto;

import java.math.BigDecimal;

/**
 * Aggregate rating display information for a product: how many reviews exist
 * and the average rating. {@code average} is {@code null} while there are no
 * reviews; {@code fullStars} is derived for rendering a star row.
 */
public class ReviewStats {

    private final long count;
    private final BigDecimal average;

    public ReviewStats(long count, BigDecimal average) {
        this.count = count;
        this.average = average;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAverage() {
        return average;
    }

    /**
     * Number of full stars to render (rounded to the nearest whole star, from 0
     * to 5). Returns 0 when there are no reviews.
     */
    public int getFullStars() {
        if (average == null) {
            return 0;
        }
        return (int) Math.round(average.doubleValue());
    }
}