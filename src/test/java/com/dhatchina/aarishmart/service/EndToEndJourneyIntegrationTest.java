package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.CartDAO;
import com.dhatchina.aarishmart.dao.OtpDAO;
import com.dhatchina.aarishmart.dao.OrderDAO;
import com.dhatchina.aarishmart.dao.ProductDAO;
import com.dhatchina.aarishmart.dao.ReviewDAO;
import com.dhatchina.aarishmart.dao.UserDAO;
import com.dhatchina.aarishmart.dao.impl.CartDAOImpl;
import com.dhatchina.aarishmart.dao.impl.OtpDAOImpl;
import com.dhatchina.aarishmart.dao.impl.OrderDAOImpl;
import com.dhatchina.aarishmart.dao.impl.ProductDAOImpl;
import com.dhatchina.aarishmart.dao.impl.ReviewDAOImpl;
import com.dhatchina.aarishmart.dao.impl.UserDAOImpl;
import com.dhatchina.aarishmart.dto.CartView;
import com.dhatchina.aarishmart.dto.ProductPage;
import com.dhatchina.aarishmart.dto.RegisterRequest;
import com.dhatchina.aarishmart.dto.SellerOrderView;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.Review;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.sms.MockSmsProvider;
import com.dhatchina.aarishmart.util.TestDb;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full customer journey against a fresh in-memory H2 database: register,
 * login (password and OTP), browse / search / filter the marketplace, product
 * details, cart add + update, checkout, order persistence, seller fulfilment
 * through to DELIVERED, and a posted review that appears on the product.
 * No real SMS, payment or AI services are contacted.
 */
class EndToEndJourneyIntegrationTest {

    private static final String BUYER_MOBILE = "9876511001";
    private static final String SELLER_MOBILE = "9876511002";

    private final DataSource dataSource = TestDb.newDataSource("e2ejourney");

    private final UserDAO userDAO = new UserDAOImpl(dataSource);
    private final ProductDAO productDAO = new ProductDAOImpl(dataSource);
    private final CartDAO cartDAO = new CartDAOImpl(dataSource);
    private final OrderDAO orderDAO = new OrderDAOImpl(dataSource);
    private final ReviewDAO reviewDAO = new ReviewDAOImpl(dataSource);
    private final OtpDAO otpDAO = new OtpDAOImpl(dataSource);

    private final AuthService authService = new AuthService(userDAO);
    private final ProductService productService = new ProductService(productDAO);
    private final CartService cartService = new CartService(cartDAO, productDAO);
    private final OrderService orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
    private final ReviewService reviewService = new ReviewService(reviewDAO, orderDAO, productDAO);
    private final MockSmsProvider mockSmsProvider = new MockSmsProvider();
    private final OtpService otpService = new OtpService(
            otpDAO, new SmsService(mockSmsProvider), 5, 5, 60, true);

    @Test
    void fullJourneyRegisterToDeliveredReview() {
        User buyer = register("Journey Buyer", BUYER_MOBILE, "BUYER");
        User seller = register("Journey Seller", SELLER_MOBILE, "SELLER");

        // Password login
        User loggedIn = authService.login("journey-buyer@example.com", "Journey@123");
        assertEquals(buyer.getId(), loggedIn.getId());

        // OTP login
        User viaOtp = authService.findUserByMobileNumber(BUYER_MOBILE);
        otpService.sendOtp(viaOtp);
        String code = otpService.getLastDevOtp();
        assertNotNull(code, "dev-mode OTP must be readable in tests");
        otpService.verifyOtp(viaOtp.getId(), code);

        // Browse, search and category filter
        ProductPage all = productService.browse(null, null, 1, 12);
        assertEquals(40, all.getTotalItems(), "seed catalog");
        assertTrue(productService.browse("kalamkari", null).stream()
                        .allMatch(p -> p.getName().toLowerCase().contains("kalamkari")
                                || p.getDescription().toLowerCase().contains("kalamkari")),
                "keyword search matches");
        List<Product> electronics = productService.browse(null, "Electronics");
        assertFalse(electronics.isEmpty());
        assertTrue(electronics.stream().allMatch(p -> "Electronics".equals(p.getCategory())));

        // Product details
        Product product = productService.getById(1L);
        assertEquals(new BigDecimal("599.00"), product.getPrice());

        // Cart add and update
        int stockBefore = productDAO.findById(1L).orElseThrow().getStockQty();
        cartService.addToCart(buyer.getId(), 1L, 2);
        cartService.updateQuantity(buyer.getId(), 1L, 3);
        CartView cart = cartService.getCart(buyer.getId());
        assertEquals(3, cart.getCount());

        // Checkout
        Order order = orderService.placeOrder(buyer.getId());
        assertNotNull(order.getId());
        assertEquals(new BigDecimal("1797.00"), order.getTotalAmount());
        assertTrue(cartService.getCart(buyer.getId()).isEmpty(), "cart cleared after order");
        assertEquals(stockBefore - 3, productDAO.findById(1L).orElseThrow().getStockQty(),
                "stock reduced by ordered quantity");

        // Order visible in history and details
        assertTrue(orderService.ordersForBuyer(buyer.getId()).stream()
                .anyMatch(o -> o.getId() == order.getId()));
        assertEquals(order.getId(), orderService.getOrderForBuyer(order.getId(), buyer.getId()).getId());

        // Seller registers a product, buyer orders it, seller fulfils to DELIVERED
        Product sellerProduct = new Product();
        User sellerUser = userDAO.findById(seller.getId()).orElseThrow();
        assertEquals(sellerUser.getId(), seller.getId());
        Product created = new SellerService(productDAO)
                .createProduct(seller.getId(), "Handmade Notebook", "A lovely notebook",
                        "250.00", "5", "Books", "https://example.com/notebook.jpg");
        assertTrue(created.getId() > 0);

        cartService.addToCart(buyer.getId(), created.getId(), 1);
        Order sellerOrder = orderService.placeOrder(buyer.getId());

        List<SellerOrderView> sellerViews = orderService.ordersForSeller(seller.getId());
        assertTrue(sellerViews.stream().anyMatch(v -> v.getOrder().getId() == sellerOrder.getId()),
                "seller sees the order containing their product");

        orderService.advanceOrderStatus(seller.getId(), sellerOrder.getId());
        orderService.advanceOrderStatus(seller.getId(), sellerOrder.getId());
        orderService.advanceOrderStatus(seller.getId(), sellerOrder.getId());
        assertEquals("DELIVERED", orderDAO.findById(sellerOrder.getId()).orElseThrow().getStatus(),
                "seller advanced PENDING -> CONFIRMED -> SHIPPED -> DELIVERED");
        assertEquals("DELIVERED",
                orderService.getOrderForBuyer(sellerOrder.getId(), buyer.getId()).getStatus(),
                "buyer sees the updated status");

        // Buyer posts a review after delivery; it appears on the product
        Review review = reviewService.createReview(
                buyer.getId(), sellerOrder.getId(), created.getId(), 5, "Beautiful notebook!");
        assertNotNull(review.getId());

        List<Review> reviews = reviewService.reviewsForProduct(created.getId());
        assertEquals(1, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Beautiful notebook!", reviews.get(0).getReviewText());
        assertEquals(1, reviewService.statsForProduct(created.getId()).getCount());
        assertTrue(reviewService.findReviewByBuyerAndProduct(buyer.getId(), created.getId()).isPresent());
        assertTrue(reviewService.findEligibleOrderId(buyer.getId(), created.getId()).isEmpty(),
                "already-reviewed pair is no longer eligible");
    }

    private User register(String name, String mobile, String role) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(name.toLowerCase().replace(" ", "-") + "@example.com");
        request.setMobileNumber(mobile);
        request.setPassword("Journey@123");
        request.setConfirmPassword("Journey@123");
        request.setRole(role);
        return authService.register(request);
    }
}