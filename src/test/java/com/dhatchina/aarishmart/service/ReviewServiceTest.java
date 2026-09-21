package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.OrderDAO;
import com.dhatchina.aarishmart.dao.ProductDAO;
import com.dhatchina.aarishmart.dao.ReviewDAO;
import com.dhatchina.aarishmart.dto.ReviewStats;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.OrderItem;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    private static final long BUYER_ID = 7L;
    private static final long OTHER_BUYER_ID = 8L;
    private static final long PRODUCT_ID = 1L;
    private static final long ORDER_ID = 9L;

    @Mock
    private ReviewDAO reviewDAO;

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private ProductDAO productDAO;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewDAO, orderDAO, productDAO);
    }

    private Product product() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Wireless Mouse");
        return product;
    }

    private Order order(long id, long buyerId, String status) {
        Order order = new Order();
        order.setId(id);
        order.setBuyerId(buyerId);
        order.setStatus(status);
        return order;
    }

    private OrderItem orderItem(long productId) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10.00"));
        return item;
    }

    private void stubValidContext() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "DELIVERED")));
        when(orderDAO.findItemsByOrderId(ORDER_ID)).thenReturn(List.of(orderItem(PRODUCT_ID)));
        when(reviewDAO.findByOrderAndProduct(ORDER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void createReviewValidatesAndTrimsText() {
        stubValidContext();
        when(reviewDAO.insert(any(Review.class))).thenReturn(99L);

        Review review = reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "  Loved it!  ");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDAO).insert(captor.capture());
        Review saved = captor.getValue();
        assertEquals(ORDER_ID, saved.getOrderId());
        assertEquals(PRODUCT_ID, saved.getProductId());
        assertEquals(BUYER_ID, saved.getUserId());
        assertEquals(5, saved.getRating());
        assertEquals("Loved it!", saved.getReviewText());
        assertEquals(99L, review.getId());
    }

    @Test
    void ratingBelowOneIsRejected() {
        stubValidContext();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 0, "Too low"));
        assertEquals("Rating must be between 1 and 5 stars", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void ratingAboveFiveIsRejected() {
        stubValidContext();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 6, "Too high"));
        assertEquals("Rating must be between 1 and 5 stars", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void emptyReviewTextIsRejected() {
        stubValidContext();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "   "));
        assertEquals("Please share a few words about the product", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void tooLongReviewTextIsRejected() {
        stubValidContext();
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "a".repeat(501)));
        assertEquals("Your review must be 500 characters or fewer", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void missingProductIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Nice"));
        assertEquals("Product not found", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void missingOrderIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Nice"));
        assertEquals("Order not found", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void orderOwnedByAnotherBuyerIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID))
                .thenReturn(Optional.of(order(ORDER_ID, OTHER_BUYER_ID, "DELIVERED")));
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Nice"));
        assertEquals("Order not found", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void orderNotYetDeliveredIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "SHIPPED")));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Nice"));
        assertEquals("You can review a product only after your order has been delivered", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void productNotPartOfOrderIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "DELIVERED")));
        when(orderDAO.findItemsByOrderId(ORDER_ID)).thenReturn(List.of(orderItem(99L)));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Nice"));
        assertEquals("This product is not part of this order", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void duplicateReviewForOrderIsRejected() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "DELIVERED")));
        when(orderDAO.findItemsByOrderId(ORDER_ID)).thenReturn(List.of(orderItem(PRODUCT_ID)));
        when(reviewDAO.findByOrderAndProduct(ORDER_ID, PRODUCT_ID)).thenReturn(Optional.of(new Review()));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(BUYER_ID, ORDER_ID, PRODUCT_ID, 5, "Again"));
        assertEquals("You have already reviewed this product for this order", ex.getMessage());
        verify(reviewDAO, never()).insert(any(Review.class));
    }

    @Test
    void statsWithNoReviewsReturnsZeroCountAndNullAverage() {
        when(reviewDAO.countByProduct(PRODUCT_ID)).thenReturn(0L);
        when(reviewDAO.averageRatingForProduct(PRODUCT_ID)).thenReturn(Optional.empty());

        ReviewStats stats = reviewService.statsForProduct(PRODUCT_ID);

        assertEquals(0, stats.getCount());
        assertNull(stats.getAverage());
        assertEquals(0, stats.getFullStars());
    }

    @Test
    void statsAveragesAndRoundsToNearestFullStar() {
        when(reviewDAO.countByProduct(PRODUCT_ID)).thenReturn(2L);
        when(reviewDAO.averageRatingForProduct(PRODUCT_ID)).thenReturn(Optional.of(new BigDecimal("4.5")));

        ReviewStats stats = reviewService.statsForProduct(PRODUCT_ID);

        assertEquals(2, stats.getCount());
        assertEquals(0, new BigDecimal("4.5").compareTo(stats.getAverage()));
        assertEquals(5, stats.getFullStars());
    }

    @Test
    void statsAverageRoundsDownAtHalfPoint() {
        when(reviewDAO.countByProduct(PRODUCT_ID)).thenReturn(2L);
        when(reviewDAO.averageRatingForProduct(PRODUCT_ID)).thenReturn(Optional.of(new BigDecimal("3.5")));

        assertEquals(4, reviewService.statsForProduct(PRODUCT_ID).getFullStars());
    }

    @Test
    void isEligibleOrderTrueWhenDeliveredOwnedAndUnreviewed() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "DELIVERED")));
        when(orderDAO.findItemsByOrderId(ORDER_ID)).thenReturn(List.of(orderItem(PRODUCT_ID)));
        when(reviewDAO.findByOrderAndProduct(ORDER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        assertTrue(reviewService.isEligibleOrder(BUYER_ID, ORDER_ID, PRODUCT_ID));
    }

    @Test
    void isEligibleOrderFalseWhenNotDelivered() {
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "PENDING")));
        assertFalse(reviewService.isEligibleOrder(BUYER_ID, ORDER_ID, PRODUCT_ID));
    }

    @Test
    void isEligibleOrderFalseWhenOrderBelongsToAnotherBuyer() {
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, OTHER_BUYER_ID, "DELIVERED")));
        assertFalse(reviewService.isEligibleOrder(BUYER_ID, ORDER_ID, PRODUCT_ID));
    }

    @Test
    void isEligibleOrderFalseWhenAlreadyReviewed() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(orderDAO.findById(ORDER_ID)).thenReturn(Optional.of(order(ORDER_ID, BUYER_ID, "DELIVERED")));
        when(orderDAO.findItemsByOrderId(ORDER_ID)).thenReturn(List.of(orderItem(PRODUCT_ID)));
        when(reviewDAO.findByOrderAndProduct(ORDER_ID, PRODUCT_ID)).thenReturn(Optional.of(new Review()));

        assertFalse(reviewService.isEligibleOrder(BUYER_ID, ORDER_ID, PRODUCT_ID));
    }

    @Test
    void findEligibleOrderIdDelegatesToDao() {
        when(reviewDAO.findEligibleDeliveredOrder(BUYER_ID, PRODUCT_ID)).thenReturn(Optional.of(ORDER_ID));

        assertEquals(Optional.of(ORDER_ID), reviewService.findEligibleOrderId(BUYER_ID, PRODUCT_ID));
    }

    @Test
    void reviewedProductIdsForOrderCollectsDistinctProductIds() {
        Review a = new Review();
        a.setProductId(5L);
        Review b = new Review();
        b.setProductId(7L);
        when(reviewDAO.findByOrder(3L)).thenReturn(List.of(a, b));

        assertEquals(Set.of(5L, 7L), reviewService.reviewedProductIdsForOrder(3L));
    }
}