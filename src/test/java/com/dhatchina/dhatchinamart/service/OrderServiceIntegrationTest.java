package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.dto.SellerOrderView;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.AuthUtil;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceIntegrationTest {

    private DataSource dataSource;
    private long buyerId;
    private long buyerBId;
    private long sellerAId;
    private ProductDAO productDAO;
    private CartDAO cartDAO;
    private OrderDAO orderDAO;
    private UserDAO userDAO;
    private OrderService orderService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        dataSource = TestDb.newDataSource("ordertest");
        productDAO = new ProductDAOImpl(dataSource);
        cartDAO = new CartDAOImpl(dataSource);
        orderDAO = new OrderDAOImpl(dataSource);
        userDAO = new UserDAOImpl(dataSource);
        buyerId = createUser("Test Buyer", "test-buyer@test.com", "9876500002", User.Role.BUYER);
        buyerBId = createUser("Other Buyer", "other-buyer@test.com", "9876500003", User.Role.BUYER);
        sellerAId = createUser("Test Seller", "test-seller@test.com", "9876500004", User.Role.SELLER);
        cartService = new CartService(cartDAO, productDAO);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
    }

    private long createUser(String name, String email, String mobile, User.Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(AuthUtil.hashPassword("TestPass@123"));
        user.setRole(role);
        return userDAO.insert(user);
    }

    private long createProduct(long sellerId) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName("Seller Product");
        product.setDescription("A product owned by the seller");
        product.setPrice(new BigDecimal("250.00"));
        product.setStockQty(10);
        product.setCategory("Electronics");
        product.setImageUrl("images/products/test.jpg");
        return productDAO.insert(product);
    }

    @Test
    void placeOrderPersistsOrderClearsCartAndDecrementsStock() {
        cartService.addToCart(buyerId, 1L, 2);

        Order order = orderService.placeOrder(buyerId);

        assertNotNull(order);
        assertTrue(order.getId() > 0, "order id must be generated");
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("1198.00"), order.getTotalAmount(),
                "total must be 2 x 599.00");

        Product product = productDAO.findById(1L).orElseThrow();
        assertEquals(22, product.getStockQty(), "stock must be reduced by 2");

        assertEquals(0, cartService.getCart(buyerId).getCount(), "cart must be cleared");

        List<OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals(new BigDecimal("599.00"), items.get(0).getUnitPrice());
    }

    @Test
    void placeOrderComputesTotalAcrossMultipleItems() {
        cartService.addToCart(buyerId, 1L, 2);
        cartService.addToCart(buyerId, 3L, 1);

        Order order = orderService.placeOrder(buyerId);

        assertEquals(new BigDecimal("1347.00"), order.getTotalAmount(),
                "total must be 2 x 599.00 + 1 x 149.00");
        assertEquals(2, orderDAO.findItemsByOrderId(order.getId()).size());
    }

    @Test
    void placeOrderWithEmptyCartThrows() {
        assertThrows(ValidationException.class, () -> orderService.placeOrder(buyerId));
    }

    @Test
    void orderAppearsInBuyerHistory() {
        cartService.addToCart(buyerId, 1L, 1);
        Order placed = orderService.placeOrder(buyerId);

        List<Order> history = orderService.ordersForBuyer(buyerId);
        assertTrue(history.stream().anyMatch(o -> o.getId() == placed.getId()),
                "placed order must appear in buyer order history");
    }

    @Test
    void orderTotalMatchesCartViewTotal() {
        cartService.addToCart(buyerId, 5L, 3);
        CartView cart = cartService.getCart(buyerId);

        Order order = orderService.placeOrder(buyerId);

        assertEquals(cart.getTotal(), order.getTotalAmount());
    }

    @Test
    void orderTotalUsesCurrentLiveProductPrice() {
        cartService.addToCart(buyerId, 5L, 3);
        Product speaker = productDAO.findById(5L).orElseThrow();
        speaker.setPrice(new BigDecimal("999.00"));
        productDAO.update(speaker);

        Order order = orderService.placeOrder(buyerId);

        assertEquals(new BigDecimal("2997.00"), order.getTotalAmount(),
                "the order total must be recomputed from the current product price in the transaction");
    }

    @Test
    void insufficientStockAtOrderTimeRollsBackEverything() {
        cartService.addToCart(buyerId, 2L, 5);
        Product keyboard = productDAO.findById(2L).orElseThrow();
        keyboard.setStockQty(2);
        productDAO.update(keyboard);

        assertThrows(ValidationException.class, () -> orderService.placeOrder(buyerId));

        long orderCount = orderDAO.countAll();
        assertEquals(5, orderCount, "only the seeded demo orders exist");
        assertEquals(5, cartDAO.findByUserId(buyerId).get(0).getQuantity(),
                "cart rows must be kept intact after a rollback");
        assertEquals(2, productDAO.findById(2L).orElseThrow().getStockQty(), "stock must be untouched");
    }

    @Test
    void failureAfterOrderAndFirstDecrementRollsBackEverything() {
        final int[] decrementCalls = {0};
        ProductDAO exploding = new ProductDAOImpl(dataSource) {
            @Override
            public boolean decrementStock(Connection connection, long productId, int quantity) {
                decrementCalls[0]++;
                if (decrementCalls[0] == 2) {
                    throw new RuntimeException("simulated database failure during decrement");
                }
                return super.decrementStock(connection, productId, quantity);
            }
        };
        OrderService service = new OrderService(dataSource, orderDAO, cartDAO, exploding);

        cartService.addToCart(buyerId, 1L, 1);
        cartService.addToCart(buyerId, 2L, 1);
        int initialStock1 = productDAO.findById(1L).orElseThrow().getStockQty();
        int initialStock2 = productDAO.findById(2L).orElseThrow().getStockQty();

        assertThrows(RuntimeException.class, () -> service.placeOrder(buyerId));

        assertEquals(5, orderDAO.countAll(), "the partially written order must be rolled back; only the 5 seeded demo orders remain");
        assertEquals(0, orderDAO.findItemsByOrderId(1L).size(), "stale order items must not remain");
        assertEquals(initialStock1, productDAO.findById(1L).orElseThrow().getStockQty(),
                "the first stock decrement (inside the failed transaction) must be rolled back");
        assertEquals(initialStock2, productDAO.findById(2L).orElseThrow().getStockQty(),
                "stock of the product that failed to decrement must be untouched");
        assertFalse(cartDAO.findByUserId(buyerId).isEmpty(),
                "the cart must be kept intact after any failed order");
        assertEquals(1, cartDAO.findByUserId(buyerId).stream()
                        .filter(item -> item.getProductId() == 1L).findFirst().orElseThrow().getQuantity(),
                "cart quantity must survive a rollback");
    }

    @Test
    void duplicateSubmitCreatesOnlyOneOrder() {
        cartService.addToCart(buyerId, 1L, 1);
        orderService.placeOrder(buyerId);

        ValidationException ex = assertThrows(ValidationException.class, () -> orderService.placeOrder(buyerId));

        assertEquals("Your cart is empty", ex.getMessage());
        assertEquals(6, orderDAO.countAll(), "only the first submit (plus the 5 seeded demo orders) must persist");
    }

    @Test
    void orderIsOnlyVisibleToItsOwner() {
        cartService.addToCart(buyerId, 1L, 1);
        Order placed = orderService.placeOrder(buyerId);

        assertNotNull(orderService.getOrderForBuyer(placed.getId(), buyerId));
        assertThrows(NotFoundException.class, () -> orderService.getOrderForBuyer(placed.getId(), buyerBId));
        assertThrows(NotFoundException.class, () -> orderService.getOrderForBuyer(999_999L, buyerId));
    }

    @Test
    void sellerAdvancesOrderThroughFullLifecycle() {
        long sellerProductId = createProduct(sellerAId);
        cartService.addToCart(buyerId, 1L, 1);
        cartService.addToCart(buyerId, sellerProductId, 2);
        Order placed = orderService.placeOrder(buyerId);

        orderService.advanceOrderStatus(sellerAId, placed.getId());
        assertEquals("CONFIRMED", orderDAO.findById(placed.getId()).orElseThrow().getStatus());

        orderService.advanceOrderStatus(sellerAId, placed.getId());
        assertEquals("SHIPPED", orderDAO.findById(placed.getId()).orElseThrow().getStatus());

        orderService.advanceOrderStatus(sellerAId, placed.getId());
        assertEquals("DELIVERED", orderDAO.findById(placed.getId()).orElseThrow().getStatus());

        assertThrows(ValidationException.class,
                () -> orderService.advanceOrderStatus(sellerAId, placed.getId()),
                "a delivered order cannot advance further");
    }

    @Test
    void unrelatedSellerCannotAdvanceOrSeeAnotherSellersOrder() {
        long sellerProductId = createProduct(sellerAId);
        cartService.addToCart(buyerId, sellerProductId, 1);
        Order placed = orderService.placeOrder(buyerId);

        assertThrows(NotFoundException.class,
                () -> orderService.advanceOrderStatus(buyerBId, placed.getId()),
                "a seller without items in the order gets the same 404 as a missing order");

        assertEquals("PENDING", orderDAO.findById(placed.getId()).orElseThrow().getStatus());
        assertTrue(orderService.ordersForSeller(buyerBId).isEmpty());
    }

    @Test
    void sellerSeesOnlyOrdersContainingTheirProductsWithTheirLinesOnly() {
        long sellerProductId = createProduct(sellerAId);
        cartService.addToCart(buyerId, 1L, 1);
        cartService.addToCart(buyerId, sellerProductId, 2);
        Order placed = orderService.placeOrder(buyerId);

        List<SellerOrderView> views = orderService.ordersForSeller(sellerAId);

        assertEquals(1, views.size());
        assertEquals(placed.getId(), views.get(0).getOrder().getId());
        assertEquals(1, views.get(0).getItems().size(), "only the seller's own line must be attached");
        assertEquals(sellerProductId, views.get(0).getItems().get(0).getProductId());
        assertEquals(new BigDecimal("500.00"), views.get(0).getSubtotal(), "seller subtotal = 2 x 250.00");
    }
}