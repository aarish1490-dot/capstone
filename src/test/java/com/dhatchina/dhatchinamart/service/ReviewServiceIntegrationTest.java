package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.ReviewDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ReviewDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.dto.ReviewStats;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.Review;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.AuthUtil;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewServiceIntegrationTest {

    private DataSource dataSource;
    private long buyerId;
    private long buyerBId;
    private long sellerAId;
    private ProductDAO productDAO;
    private CartDAO cartDAO;
    private OrderDAO orderDAO;
    private ReviewDAO reviewDAO;
    private OrderService orderService;
    private CartService cartService;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        dataSource = TestDb.newDataSource("reviewservicetest");
        productDAO = new ProductDAOImpl(dataSource);
        cartDAO = new CartDAOImpl(dataSource);
        orderDAO = new OrderDAOImpl(dataSource);
        reviewDAO = new ReviewDAOImpl(dataSource);
        UserDAO userDAO = new UserDAOImpl(dataSource);
        buyerId = createUser("Review Buyer", "review-buyer@test.com", "9876500015", User.Role.BUYER, userDAO);
        buyerBId = createUser("Review Buyer B", "review-buyerb@test.com", "9876500016", User.Role.BUYER, userDAO);
        sellerAId = createUser("Review Seller", "review-seller@test.com", "9876500017", User.Role.SELLER, userDAO);
        cartService = new CartService(cartDAO, productDAO);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
        reviewService = new ReviewService(reviewDAO, orderDAO, productDAO);
    }

    private long createUser(String name, String email, String mobile, User.Role role, UserDAO userDAO) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(AuthUtil.hashPassword("TestPass@123"));
        user.setRole(role);
        return userDAO.insert(user);
    }

    private long createSellerProduct() {
        Product product = new Product();
        product.setSellerId(sellerAId);
        product.setName("Seller Product");
        product.setDescription("A product owned by the seller");
        product.setPrice(new BigDecimal("250.00"));
        product.setStockQty(10);
        product.setCategory("Electronics");
        product.setImageUrl("images/products/test.jpg");
        return productDAO.insert(product);
    }

    private long placeAndDeliverOrder(long buyerId, long productId) {
        cartService.addToCart(buyerId, productId, 2);
        Order placed = orderService.placeOrder(buyerId);
        orderService.advanceOrderStatus(sellerAId, placed.getId());
        orderService.advanceOrderStatus(sellerAId, placed.getId());
        orderService.advanceOrderStatus(sellerAId, placed.getId());
        assertEquals("DELIVERED", orderDAO.findById(placed.getId()).orElseThrow().getStatus());
        return placed.getId();
    }

    private long placeOrderOnly(long buyerId, long productId) {
        cartService.addToCart(buyerId, productId, 2);
        return orderService.placeOrder(buyerId).getId();
    }

    @Test
    void reviewRequiresDeliveredOrder() {
        long productId = createSellerProduct();
        long orderId = placeOrderOnly(buyerId, productId);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(buyerId, orderId, productId, 5, "Great"));

        assertEquals("You can review a product only after your order has been delivered", ex.getMessage());
        assertEquals(0, reviewService.statsForProduct(productId).getCount());
    }

    @Test
    void reviewIsCreatedAndExposedWithBuyerName() {
        long productId = createSellerProduct();
        long orderId = placeAndDeliverOrder(buyerId, productId);

        Review review = reviewService.createReview(buyerId, orderId, productId, 5, "Absolutely loved it!");

        List<Review> reviews = reviewService.reviewsForProduct(productId);
        assertEquals(1, reviews.size());
        assertEquals(review.getId(), reviews.get(0).getId());
        assertEquals("Review Buyer", reviews.get(0).getBuyerName());
        assertTrue(reviewService.findReviewByOrderAndProduct(orderId, productId).isPresent());
        assertTrue(reviewService.findReviewByBuyerAndProduct(buyerId, productId).isPresent());
    }

    @Test
    void statsAverageAcrossBuyers() {
        long productId = createSellerProduct();
        long orderA = placeAndDeliverOrder(buyerId, productId);
        long orderB = placeAndDeliverOrder(buyerBId, productId);
        reviewService.createReview(buyerId, orderA, productId, 5, "Love it");
        reviewService.createReview(buyerBId, orderB, productId, 4, "Good but slow shipping");

        ReviewStats stats = reviewService.statsForProduct(productId);

        assertEquals(2, stats.getCount());
        assertEquals(0, new BigDecimal("4.5").compareTo(stats.getAverage()));
    }

    @Test
    void duplicateReviewForSameOrderIsRejectedAndKeepsSingleCount() {
        long productId = createSellerProduct();
        long orderId = placeAndDeliverOrder(buyerId, productId);
        reviewService.createReview(buyerId, orderId, productId, 5, "First impression");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(buyerId, orderId, productId, 1, "Second attempt"));

        assertEquals("You have already reviewed this product for this order", ex.getMessage());
        assertEquals(1, reviewService.statsForProduct(productId).getCount());
    }

    @Test
    void sameBuyerCanReviewProductFromAnotherDeliveredOrder() {
        long productId = createSellerProduct();
        long orderA = placeAndDeliverOrder(buyerId, productId);
        long orderB = placeAndDeliverOrder(buyerId, productId);
        reviewService.createReview(buyerId, orderA, productId, 3, "First purchase");
        reviewService.createReview(buyerId, orderB, productId, 4, "Second purchase");

        assertEquals(2, reviewService.statsForProduct(productId).getCount());
    }

    @Test
    void buyerCannotReviewAnotherBuyersOrder() {
        long productId = createSellerProduct();
        long orderId = placeAndDeliverOrder(buyerId, productId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.createReview(buyerBId, orderId, productId, 5, "Sneaky review"));

        assertEquals("Order not found", ex.getMessage());
        assertEquals(0, reviewService.statsForProduct(productId).getCount());
    }

    @Test
    void buyerCannotReviewAProductTheyDidNotBuyInThatOrder() {
        long productA = createSellerProduct();
        long productB = createSellerProduct();
        long orderId = placeAndDeliverOrder(buyerId, productA);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> reviewService.createReview(buyerId, orderId, productB, 5, "Never bought this"));

        assertEquals("This product is not part of this order", ex.getMessage());
        assertTrue(reviewService.findEligibleOrderId(buyerId, productB).isEmpty());
    }

    @Test
    void eligibilityChangesBeforeAndAfterReview() {
        long productId = createSellerProduct();
        assertTrue(reviewService.findEligibleOrderId(buyerId, productId).isEmpty());

        long orderId = placeAndDeliverOrder(buyerId, productId);

        assertEquals(Optional.of(orderId), reviewService.findEligibleOrderId(buyerId, productId));
        assertTrue(reviewService.isEligibleOrder(buyerId, orderId, productId));

        reviewService.createReview(buyerId, orderId, productId, 5, "Done");

        assertFalse(reviewService.isEligibleOrder(buyerId, orderId, productId));
        assertTrue(reviewService.findEligibleOrderId(buyerId, productId).isEmpty());
        assertTrue(reviewService.reviewedProductIdsForOrder(orderId).contains(productId));
    }

    @Test
    void otherSellersProductRemainsUnderOriginalSeller() {
        long productId = createSellerProduct();
        long orderId = placeAndDeliverOrder(buyerId, productId);
        reviewService.createReview(buyerId, orderId, productId, 5, "Great quality");

        assertNotNull(reviewService.findReviewByOrderAndProduct(orderId, productId));
    }
}