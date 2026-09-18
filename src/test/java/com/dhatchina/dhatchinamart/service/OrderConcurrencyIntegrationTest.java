package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.AuthUtil;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integrity: several buyers race to purchase a limited stock.
 * The atomic {@code WHERE stock_qty >= quantity} decrement must prevent
 * overselling - the number of successful orders must never exceed the stock
 * that was available and the final stock must never go negative.
 *
 * <p>Pure database + service stack, no external services.
 */
class OrderConcurrencyIntegrationTest {

    private final DataSource dataSource = TestDb.newDataSource("concurrencytest", 12);

    private final UserDAO userDAO = new UserDAOImpl(dataSource);
    private final ProductDAO productDAO = new ProductDAOImpl(dataSource);
    private final CartDAO cartDAO = new CartDAOImpl(dataSource);
    private final OrderDAO orderDAO = new OrderDAOImpl(dataSource);
    private final CartService cartService = new CartService(cartDAO, productDAO);
    private final OrderService orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);

    @Test
    void raceForSingleUnitSellsAtMostOne() throws Exception {
        long productId = createProduct("Limited Item", 1);

        int buyers = 8;
        List<Long> buyerIds = createBuyers("single", buyers);
        for (long buyerId : buyerIds) {
            cartService.addToCart(buyerId, productId, 1);
        }

        RunResult result = raceCheckout(buyerIds);

        assertEquals(1, result.successes(), buyers + " buyers raced, only one order must win");
        assertEquals(0, productDAO.findById(productId).orElseThrow().getStockQty(),
                "stock must be consumed exactly once, never negative");
        assertTrue(validateWinnerOrders(result.orderIds(), productId),
                "every successful order must hold the product exactly once");
    }

    @Test
    void raceForThreeUnitsSellsExactlyThree() throws Exception {
        long productId = createProduct("Moderate Item", 3);

        int buyers = 6;
        List<Long> buyerIds = createBuyers("moderate", buyers);
        for (long buyerId : buyerIds) {
            cartService.addToCart(buyerId, productId, 1);
        }

        RunResult result = raceCheckout(buyerIds);

        assertEquals(3, result.successes(), "must sell exactly the 3 units available");
        assertEquals(0, productDAO.findById(productId).orElseThrow().getStockQty());
        assertTrue(validateWinnerOrders(result.orderIds(), productId));
    }

    @Test
    void mixedQuantitiesNeverOversellAndConserveStock() throws Exception {
        long productId = createProduct("Contested Item", 2);

        List<Long> buyerIds = createBuyers("mixed", 4);
        cartService.addToCart(buyerIds.get(0), productId, 2);
        for (int i = 1; i < buyerIds.size(); i++) {
            cartService.addToCart(buyerIds.get(i), productId, 1);
        }

        RunResult result = raceCheckout(buyerIds);

        int stockAfter = productDAO.findById(productId).orElseThrow().getStockQty();
        int unitsOrdered = totalUnitsInWinningOrders(result.orderIds());
        assertTrue(unitsOrdered >= 1 && unitsOrdered <= 2,
                "some winner must take the stock, but never more than the " + 2 + " available units");
        assertEquals(2 - unitsOrdered, stockAfter,
                "stock consumed must equal exactly the ordered units (conservation)");
        assertTrue(stockAfter >= 0, "stock must never go negative");
    }

    private long createProduct(String name, int stock) {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName(name);
        product.setDescription("Goal: guarantee stock consistency under concurrent purchase.");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQty(stock);
        product.setCategory("Electronics");
        product.setImageUrl("https://example.com/" + name.toLowerCase().replace(" ", "-") + ".jpg");
        return productDAO.insert(product);
    }

    private List<Long> createBuyers(String prefix, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setName(prefix + " Buyer " + i);
            user.setEmail(prefix + "-" + i + "@example.com");
            user.setMobileNumber(String.format("98765%05d", 1000 + i));
            user.setPasswordHash(AuthUtil.hashPassword("TestPass@123"));
            user.setRole(User.Role.BUYER);
            ids.add(userDAO.insert(user));
        }
        return ids;
    }

    private RunResult raceCheckout(List<Long> buyerIds) throws Exception {
        int n = buyerIds.size();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        AtomicInteger successes = new AtomicInteger();
        ConcurrentLinkedQueue<Long> orderIds = new ConcurrentLinkedQueue<>();

        try {
            for (long buyerId : buyerIds) {
                pool.submit(() -> {
                    try {
                        start.await();
                        Order order = orderService.placeOrder(buyerId);
                        successes.incrementAndGet();
                        orderIds.add(order.getId());
                    } catch (Exception ignored) {
                        // expected for buyers who lose the race
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "checkout race must finish");
        } finally {
            pool.shutdownNow();
        }
        return new RunResult(successes.get(), orderIds);
    }

    private boolean validateWinnerOrders(ConcurrentLinkedQueue<Long> orderIds, long productId) {
        for (long orderId : orderIds) {
            boolean has = orderDAO.findItemsByOrderId(orderId).stream()
                    .anyMatch(item -> item.getProductId() == productId);
            if (!has) {
                return false;
            }
        }
        return true;
    }

    private int totalUnitsInWinningOrders(ConcurrentLinkedQueue<Long> orderIds) {
        return orderIds.stream()
                .map(orderDAO::findItemsByOrderId)
                .flatMap(List::stream)
                .mapToInt(item -> item.getQuantity())
                .sum();
    }

    private record RunResult(int successes, ConcurrentLinkedQueue<Long> orderIds) {
    }
}