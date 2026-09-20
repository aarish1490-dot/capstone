package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.dto.SellerStats;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;
import com.dhatchina.dhatchinamart.model.Product;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDaoIntegrationTest {

    private DataSource dataSource;
    private UserDAO userDAO;
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    private long buyerA;
    private long buyerB;
    private long sellerA;
    private long sellerB;

    @BeforeEach
    void setUp() {
        dataSource = TestDb.newDataSource("orderdaotest");
        userDAO = new UserDAOImpl(dataSource);
        productDAO = new ProductDAOImpl(dataSource);
        orderDAO = new OrderDAOImpl(dataSource);
        buyerA = createUser("Buyer A", "buyera@example.com", "9876500002", User.Role.BUYER);
        buyerB = createUser("Buyer B", "buyerb@example.com", "9876500003", User.Role.BUYER);
        sellerA = createUser("Seller A", "sellera@example.com", "9876500004", User.Role.SELLER);
        sellerB = createUser("Seller B", "sellerb@example.com", "9876500005", User.Role.SELLER);
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

    private long createProduct(long sellerId, String name, BigDecimal price) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName(name);
        product.setDescription("Product description");
        product.setPrice(price);
        product.setStockQty(10);
        product.setCategory("Electronics");
        product.setImageUrl("images/products/test.jpg");
        return productDAO.insert(product);
    }

    private long insertOrder(long buyerId, List<OrderItem> items) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Order order = new Order();
            order.setBuyerId(buyerId);
            order.setStatus("PENDING");
            order.setTotalAmount(items.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            long orderId = orderDAO.insert(conn, order);
            for (OrderItem item : items) {
                item.setOrderId(orderId);
                orderDAO.insertItem(conn, item);
            }
            conn.commit();
            return orderId;
        }
    }

    private OrderItem item(long productId, String name, BigDecimal price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(name);
        item.setUnitPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void insertAndFindOrderIncludesBuyerNameAndStatus() throws Exception {
        long orderId = insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1)));

        Optional<Order> found = orderDAO.findById(orderId);

        assertTrue(found.isPresent());
        assertEquals(orderId, found.get().getId());
        assertEquals("Buyer A", found.get().getBuyerName());
        assertEquals("PENDING", found.get().getStatus());
        assertEquals(new BigDecimal("599.00"), found.get().getTotalAmount());
    }

    @Test
    void findItemsByOrderIdIncludesProductImage() throws Exception {
        long orderId = insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 2)));

        List<OrderItem> items = orderDAO.findItemsByOrderId(orderId);

        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getQuantity());
        assertNotNull(items.get(0).getImageUrl(), "order item must carry the product image");
        assertFalse(items.get(0).getImageUrl().isBlank());
    }

    @Test
    void belongsToSellerReflectsProductOwnership() throws Exception {
        long sellerProduct = createProduct(sellerA, "Seller A Product", new BigDecimal("250.00"));
        long orderId = insertOrder(buyerA,
                List.of(item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 1)));

        assertTrue(orderDAO.belongsToSeller(orderId, sellerA));
        assertFalse(orderDAO.belongsToSeller(orderId, sellerB));
        assertFalse(orderDAO.belongsToSeller(orderId, 999_999L));
    }

    @Test
    void findContainingSellerOnlyReturnsOrdersWithTheirProducts() throws Exception {
        long sellerProduct = createProduct(sellerA, "Seller A Product", new BigDecimal("250.00"));
        long orderWithSellerItem = insertOrder(buyerA,
                List.of(item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 1)));
        long orderWithoutSellerItem = insertOrder(buyerA,
                List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1)));

        List<Order> orders = orderDAO.findContainingSeller(sellerA);

        assertTrue(orders.stream().anyMatch(o -> o.getId() == orderWithSellerItem));
        assertTrue(orders.stream().noneMatch(o -> o.getId() == orderWithoutSellerItem));
        assertEquals("Buyer A", orders.get(0).getBuyerName());
    }

    @Test
    void findItemsByOrderForSellerOnlyReturnsTheirLines() throws Exception {
        long sellerProduct = createProduct(sellerA, "Seller A Product", new BigDecimal("250.00"));
        long orderId = insertOrder(buyerA, List.of(
                item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1),
                item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 3)));

        List<OrderItem> sellerLines = orderDAO.findItemsByOrderForSeller(orderId, sellerA);
        List<OrderItem> allLines = orderDAO.findItemsByOrderId(orderId);

        assertEquals(2, allLines.size());
        assertEquals(1, sellerLines.size());
        assertEquals(sellerProduct, sellerLines.get(0).getProductId());
        assertEquals(3, sellerLines.get(0).getQuantity());
    }

    @Test
    void updateStatusPersistsAndReportsRowsAffected() throws Exception {
        long orderId = insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1)));

        assertTrue(orderDAO.updateStatus(orderId, "CONFIRMED"));
        assertEquals("CONFIRMED", orderDAO.findById(orderId).orElseThrow().getStatus());

        assertFalse(orderDAO.updateStatus(999_999L, "DELIVERED"));

        orderDAO.updateStatus(orderId, "SHIPPED");
        orderDAO.updateStatus(orderId, "DELIVERED");
        assertEquals("DELIVERED", orderDAO.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    void ordersAreIsolatedPerBuyer() throws Exception {
        long orderForA = insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1)));
        insertOrder(buyerB, List.of(item(2L, "Coconut Shell Keychain Set", new BigDecimal("199.00"), 1)));

        List<Order> history = orderDAO.findByBuyer(buyerA);

        assertEquals(1, history.size());
        assertEquals(orderForA, history.get(0).getId());
    }

    @Test
    void findAllReturnsOrdersFromAllBuyers() throws Exception {
        long orderForA = insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1)));
        long orderForB = insertOrder(buyerB, List.of(item(2L, "Coconut Shell Keychain Set", new BigDecimal("199.00"), 2)));

        List<Order> all = orderDAO.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(o -> o.getId() == orderForA && "Buyer A".equals(o.getBuyerName())));
        assertTrue(all.stream().anyMatch(o -> o.getId() == orderForB && "Buyer B".equals(o.getBuyerName())));
    }

    @Test
    void findSellerSalesAggregatesOrderCountUnitsAndRevenue() throws Exception {
        long sellerProduct = createProduct(sellerA, "Seller A Product", new BigDecimal("250.00"));
        insertOrder(buyerA, List.of(item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 2)));
        insertOrder(buyerB, List.of(item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 1)));

        SellerStats stats = orderDAO.findSellerSales(sellerA);

        assertEquals(2, stats.getOrderCount(), "one order per checkout");
        assertEquals(3, stats.getUnitsSold());
        assertEquals(new BigDecimal("750.00"), stats.getRevenue());
    }

    @Test
    void findSellerSalesIgnoresOtherSellersAndSeededProducts() throws Exception {
        long sellerProduct = createProduct(sellerA, "Seller A Product", new BigDecimal("250.00"));
        insertOrder(buyerA, List.of(item(1L, "Kalamkari Canvas Tote Bag", new BigDecimal("599.00"), 1),
                item(sellerProduct, "Seller A Product", new BigDecimal("250.00"), 3)));

        SellerStats stats = orderDAO.findSellerSales(sellerA);

        assertEquals(1, stats.getOrderCount());
        assertEquals(3, stats.getUnitsSold(), "only the seller's own lines count, not the seeded product");
        assertEquals(new BigDecimal("750.00"), stats.getRevenue());
    }

    @Test
    void findSellerSalesForSellerWithNoOrdersReturnsZeros() {
        SellerStats stats = orderDAO.findSellerSales(sellerB);

        assertEquals(0, stats.getOrderCount());
        assertEquals(0, stats.getUnitsSold());
        assertEquals(BigDecimal.ZERO, stats.getRevenue());
    }
}