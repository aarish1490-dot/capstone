package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.model.Review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ReviewDAO {

    long insert(Review review);

    Optional<Review> findById(long id);

    List<Review> findByProduct(long productId);

    List<Review> findByOrder(long orderId);

    Optional<Review> findByOrderAndProduct(long orderId, long productId);

    Optional<Review> findByBuyerAndProduct(long buyerId, long productId);

    long countByProduct(long productId);

    Optional<BigDecimal> averageRatingForProduct(long productId);

    /**
     * The id of the buyer's most recent DELIVERED order that contains the
     * product and has not yet been reviewed for that (order, product) pair, if
     * such an order exists. Used to pre-fill the review form on the product
     * page; {@link ReviewService} re-validates everything on submission.
     */
    Optional<Long> findEligibleDeliveredOrder(long buyerId, long productId);
}