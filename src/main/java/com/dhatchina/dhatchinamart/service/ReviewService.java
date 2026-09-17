package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.ReviewDAO;
import com.dhatchina.dhatchinamart.dto.ReviewStats;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderStatus;
import com.dhatchina.dhatchinamart.model.Review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ReviewService {

    public static final int MAX_REVIEW_LENGTH = 500;

    private final ReviewDAO reviewDAO;
    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;

    public ReviewService(ReviewDAO reviewDAO, OrderDAO orderDAO, ProductDAO productDAO) {
        this.reviewDAO = reviewDAO;
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
    }

    /**
     * Creates a review, tied to the authenticated buyer (never a browser-supplied
     * user id) and to a specific completed order. Every eligibility rule is
     * checked server-side: rating range, text length, product existence, order
     * existence + ownership, delivery status, that the product was part of the
     * order, and that the (order, product) pair has not already been reviewed.
     */
    public Review createReview(long buyerId, long orderId, long productId, int rating, String reviewText) {
        validateRating(rating);
        String text = requireText(reviewText);

        if (productDAO.findById(productId).isEmpty()) {
            throw new NotFoundException("Product not found");
        }
        Order order = orderDAO.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getBuyerId() != buyerId) {
            throw new NotFoundException("Order not found");
        }
        if (!OrderStatus.DELIVERED.name().equals(order.getStatus())) {
            throw new ValidationException("You can review a product only after your order has been delivered");
        }
        boolean purchased = orderDAO.findItemsByOrderId(orderId).stream()
                .anyMatch(item -> item.getProductId() == productId);
        if (!purchased) {
            throw new ValidationException("This product is not part of this order");
        }
        if (reviewDAO.findByOrderAndProduct(orderId, productId).isPresent()) {
            throw new ValidationException("You have already reviewed this product for this order");
        }

        Review review = new Review();
        review.setOrderId(orderId);
        review.setProductId(productId);
        review.setUserId(buyerId);
        review.setRating(rating);
        review.setReviewText(text);
        review.setId(reviewDAO.insert(review));
        return review;
    }

    /**
     * Whether the given order is a valid target for the buyer to leave a review
     * on the given product. Used so a tampered {@code order} query parameter can
     * never pre-select an ineligible order on the product page.
     */
    public boolean isEligibleOrder(long buyerId, long orderId, long productId) {
        Optional<Order> order = orderDAO.findById(orderId);
        if (order.isEmpty() || order.get().getBuyerId() != buyerId
                || !OrderStatus.DELIVERED.name().equals(order.get().getStatus())) {
            return false;
        }
        if (reviewDAO.findByOrderAndProduct(orderId, productId).isPresent()) {
            return false;
        }
        return orderDAO.findItemsByOrderId(orderId).stream()
                .anyMatch(item -> item.getProductId() == productId);
    }

    /**
     * Best-effort default order id for the review form on the product page: the
     * buyer's most recent DELIVERED order containing the product that has not
     * yet been reviewed for that (order, product) pair.
     */
    public Optional<Long> findEligibleOrderId(long buyerId, long productId) {
        return reviewDAO.findEligibleDeliveredOrder(buyerId, productId);
    }

    public List<Review> reviewsForProduct(long productId) {
        return reviewDAO.findByProduct(productId);
    }

    /**
     * Count and average rating for a product's reviews. The average is rounded
     * to one decimal place; it is {@code null} in {@link ReviewStats} while the
     * product has no reviews.
     */
    public ReviewStats statsForProduct(long productId) {
        long count = reviewDAO.countByProduct(productId);
        Optional<BigDecimal> average = reviewDAO.averageRatingForProduct(productId);
        if (count == 0 || average.isEmpty()) {
            return new ReviewStats(count, null);
        }
        return new ReviewStats(count, average.get().setScale(1, RoundingMode.HALF_UP));
    }

    public Optional<Review> findReviewByBuyerAndProduct(long buyerId, long productId) {
        return reviewDAO.findByBuyerAndProduct(buyerId, productId);
    }

    public Optional<Review> findReviewByOrderAndProduct(long orderId, long productId) {
        return reviewDAO.findByOrderAndProduct(orderId, productId);
    }

    /**
     * The set of product ids already reviewed in the given order. Used by the
     * order details page to show "Reviewed" instead of a review link.
     */
    public Set<Long> reviewedProductIdsForOrder(long orderId) {
        Set<Long> productIds = new HashSet<>();
        for (Review review : reviewDAO.findByOrder(orderId)) {
            productIds.add(review.getProductId());
        }
        return productIds;
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5 stars");
        }
    }

    private String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Please share a few words about the product");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_REVIEW_LENGTH) {
            throw new ValidationException("Your review must be " + MAX_REVIEW_LENGTH + " characters or fewer");
        }
        return trimmed;
    }
}