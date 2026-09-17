package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ReviewDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.Review;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewDaoIntegrationTest {

    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private DataSource dataSource;
    private UserDAO userDAO;
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    private ReviewDAO reviewDAO;
    private long buyerA;
    private long buyerB;
    private long sellerA;

    @BeforeEach
    void setUp() {
        dataSource = TestDb.newDataSource("reviewdaotest");
        userDAO = new UserDAOImpl(dataSource);
        productDAO = new ProductDAOImpl(dataSource);
        orderDAO = new OrderDAOImpl(dataSource);
        reviewDAO = new ReviewDAOImpl(dataSource);
        buyerA = createUser("Buyer A", "reviewbuyera@example.com", "9876500012", User.Role.BUYER);
        buyerB = createUser("Buyer B", "reviewbuyerb@example.com", "9876500013", User.Role.BUYER);
        sellerA = createUser("Seller A", "reviewsellera@example.com", "9876500014", User.Role.SELLER);
    }

    private long createUser(String name, String email, String mobile, User.Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash("$2a$10$encodedHashValueForTestingOnly");
        user.setRole(role);
        return userDAO.insert(user);
    }

    private long createProduct(String name) {
        Product product = new Product();
        product.setSellerId(sellerA);
        product.setName(name);
        product.setDescription("Product description");
        product.setPrice(PRICE);
        product.setStockQty(10);
        product.setCategory("Electronics");
        product.setImageUrl("images/products/test.jpg");
        return productDAO.insert(product);
    }

    private long insertOrder(long buyerId, long productId, String status) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Order order = new Order();
            order.setBuyerId(buyerId);
            order.setStatus(status);
            order.setTotalAmount(PRICE);
            long orderId = orderDAO.insert(conn, order);
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(productId);
            item.setQuantity(1);
            item.setUnitPrice(PRICE);
            orderDAO.insertItem(conn, item);
            conn.commit();
            return orderId;
        }
    }

    private Review review(long orderId, long productId, long userId, int rating, String text) {
        Review review = new Review();
        review.setOrderId(orderId);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setReviewText(text);
        return review;
    }

    @Test
    void insertAndFindByIdPopulatesAllReviewFields() throws Exception {
        long product = createProduct("Reviewable Product");
        long order = insertOrder(buyerA, product, "DELIVERED");
        long id = reviewDAO.insert(review(order, product, buyerA, 5, "Fantastic!"));

        Optional<Review> found = reviewDAO.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(order, found.get().getOrderId());
        assertEquals(product, found.get().getProductId());
        assertEquals(buyerA, found.get().getUserId());
        assertEquals(5, found.get().getRating());
        assertEquals("Fantastic!", found.get().getReviewText());
        assertEquals("Buyer A", found.get().getBuyerName());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void findByProductReturnsOnlyReviewsForThatProduct() throws Exception {
        long productA = createProduct("Product A");
        long productB = createProduct("Product B");
        long orderA = insertOrder(buyerA, productA, "DELIVERED");
        long orderB = insertOrder(buyerA, productB, "DELIVERED");
        reviewDAO.insert(review(orderA, productA, buyerA, 5, "A is great"));
        reviewDAO.insert(review(orderB, productB, buyerA, 4, "B is okay"));

        List<Review> reviews = reviewDAO.findByProduct(productA);

        assertEquals(1, reviews.size());
        assertEquals(productA, reviews.get(0).getProductId());
        assertEquals("A is great", reviews.get(0).getReviewText());
    }

    @Test
    void findByOrderAndProductFindsTheExactPair() throws Exception {
        long product = createProduct("Reviewable Product");
        long orderA = insertOrder(buyerA, product, "DELIVERED");
        long orderB = insertOrder(buyerB, product, "DELIVERED");
        reviewDAO.insert(review(orderA, product, buyerA, 5, "From A"));
        reviewDAO.insert(review(orderB, product, buyerB, 3, "From B"));

        Optional<Review> forOrderA = reviewDAO.findByOrderAndProduct(orderA, product);
        Optional<Review> forOtherOrder = reviewDAO.findByOrderAndProduct(orderB, product);

        assertTrue(forOrderA.isPresent());
        assertEquals("From A", forOrderA.get().getReviewText());
        assertEquals("From B", forOtherOrder.get().getReviewText());
    }

    @Test
    void findByBuyerAndProductFindsTheBuyersReview() throws Exception {
        long product = createProduct("Reviewable Product");
        long orderA = insertOrder(buyerA, product, "DELIVERED");
        long orderB = insertOrder(buyerB, product, "DELIVERED");
        reviewDAO.insert(review(orderA, product, buyerA, 5, "A's words"));

        Optional<Review> forBuyerA = reviewDAO.findByBuyerAndProduct(buyerA, product);

        assertTrue(forBuyerA.isPresent());
        assertEquals("A's words", forBuyerA.get().getReviewText());
        assertTrue(reviewDAO.findByBuyerAndProduct(buyerB, product).isEmpty());
    }

    @Test
    void countAndAverageRatingAreCorrectAcrossBuyers() throws Exception {
        long product = createProduct("Reviewable Product");
        long orderA = insertOrder(buyerA, product, "DELIVERED");
        long orderB = insertOrder(buyerB, product, "DELIVERED");
        reviewDAO.insert(review(orderA, product, buyerA, 5, "Best"));
        reviewDAO.insert(review(orderB, product, buyerB, 4, "Good"));

        assertEquals(2, reviewDAO.countByProduct(product));
        Optional<BigDecimal> average = reviewDAO.averageRatingForProduct(product);
        assertTrue(average.isPresent());
        assertEquals(0, new BigDecimal("4.5").compareTo(average.get()));
        assertTrue(reviewDAO.averageRatingForProduct(999_999L).isEmpty());
    }

    @Test
    void duplicateReviewForSameOrderAndProductIsRejected() throws Exception {
        long product = createProduct("Reviewable Product");
        long order = insertOrder(buyerA, product, "DELIVERED");
        reviewDAO.insert(review(order, product, buyerA, 5, "First"));

        assertThrows(RuntimeException.class,
                () -> reviewDAO.insert(review(order, product, buyerA, 4, "Second")));

        assertEquals(1, reviewDAO.countByProduct(product));
    }

    @Test
    void sameProductCanBeReviewedFromDifferentOrders() throws Exception {
        long product = createProduct("Reviewable Product");
        long orderA = insertOrder(buyerA, product, "DELIVERED");
        long orderB = insertOrder(buyerA, product, "DELIVERED");
        reviewDAO.insert(review(orderA, product, buyerA, 5, "Order one"));
        reviewDAO.insert(review(orderB, product, buyerA, 3, "Order two"));

        assertEquals(2, reviewDAO.countByProduct(product));
    }

    @Test
    void reviewsAreIsolatedPerBuyer() throws Exception {
        long product = createProduct("Reviewable Product");
        long orderA = insertOrder(buyerA, product, "DELIVERED");
        long orderB = insertOrder(buyerB, product, "DELIVERED");
        long reviewA = reviewDAO.insert(review(orderA, product, buyerA, 5, "A's words"));
        long reviewB = reviewDAO.insert(review(orderB, product, buyerB, 2, "B's words"));

        List<Review> reviews = reviewDAO.findByProduct(product);

        assertEquals(2, reviews.size());
        assertTrue(reviews.stream().anyMatch(r -> r.getId() == reviewA && "Buyer A".equals(r.getBuyerName())));
        assertTrue(reviews.stream().anyMatch(r -> r.getId() == reviewB && "Buyer B".equals(r.getBuyerName())));
    }

    @Test
    void findEligibleDeliveredOrderOnlyReturnsDeliveredUnreviewedOrders() throws Exception {
        long product = createProduct("Reviewable Product");
        insertOrder(buyerA, product, "PENDING");

        assertTrue(reviewDAO.findEligibleDeliveredOrder(buyerA, product).isEmpty(),
                "a pending order is not eligible for review");

        long deliveredOrder = insertOrder(buyerA, product, "DELIVERED");

        Optional<Long> eligible = reviewDAO.findEligibleDeliveredOrder(buyerA, product);
        assertTrue(eligible.isPresent());
        assertEquals(deliveredOrder, eligible.get());
    }

    @Test
    void findEligibleDeliveredOrderEmptyWhenAlreadyReviewed() throws Exception {
        long product = createProduct("Reviewable Product");
        long deliveredOrder = insertOrder(buyerA, product, "DELIVERED");
        reviewDAO.insert(review(deliveredOrder, product, buyerA, 5, "Done"));

        assertTrue(reviewDAO.findEligibleDeliveredOrder(buyerA, product).isEmpty());
    }

    @Test
    void findEligibleDeliveredOrderIsIsolatedPerBuyerAndProduct() throws Exception {
        long productA = createProduct("Product A");
        long productB = createProduct("Product B");
        long orderA = insertOrder(buyerA, productA, "DELIVERED");
        insertOrder(buyerB, productB, "DELIVERED");

        assertEquals(Optional.of(orderA), reviewDAO.findEligibleDeliveredOrder(buyerA, productA));
        assertTrue(reviewDAO.findEligibleDeliveredOrder(buyerA, productB).isEmpty(),
                "buyer A never bought product B");
        assertTrue(reviewDAO.findEligibleDeliveredOrder(buyerB, productA).isEmpty(),
                "the delivered order belongs to buyer B");
    }
}