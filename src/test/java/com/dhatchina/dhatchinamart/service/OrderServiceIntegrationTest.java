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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceIntegrationTest {

    private DataSource dataSource;
    private long buyerId;
    private ProductDAO productDAO;
    private CartDAO cartDAO;
    private OrderDAO orderDAO;
    private OrderService orderService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        dataSource = TestDb.newDataSource("ordertest");
        productDAO = new ProductDAOImpl(dataSource);
        cartDAO = new CartDAOImpl(dataSource);
        orderDAO = new OrderDAOImpl(dataSource);
        UserDAO userDAO = new UserDAOImpl(dataSource);
        User buyer = new User();
        buyer.setName("Test Buyer");
        buyer.setEmail("test-buyer@test.com");
        buyer.setMobileNumber("9876500002");
        buyer.setPasswordHash(AuthUtil.hashPassword("TestPass@123"));
        buyer.setRole(User.Role.BUYER);
        buyerId = userDAO.insert(buyer);
        cartService = new CartService(cartDAO, productDAO);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
    }

    @Test
    void placeOrderPersistsOrderClearsCartAndDecrementsStock() {
        cartService.addToCart(buyerId, 1L, 2);

        Order order = orderService.placeOrder(buyerId);

        assertNotNull(order);
        assertTrue(order.getId() > 0, "order id must be generated");
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("2998.00"), order.getTotalAmount(),
                "total must be 2 x 1499.00");

        Product product = productDAO.findById(1L).orElseThrow();
        assertEquals(23, product.getStockQty(), "stock must be reduced by 2");

        assertEquals(0, cartService.getCart(buyerId).getCount(), "cart must be cleared");

        List<OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals(new BigDecimal("1499.00"), items.get(0).getUnitPrice());
    }

    @Test
    void placeOrderComputesTotalAcrossMultipleItems() {
        cartService.addToCart(buyerId, 1L, 2);
        cartService.addToCart(buyerId, 3L, 1);

        Order order = orderService.placeOrder(buyerId);

        assertEquals(new BigDecimal("3797.00"), order.getTotalAmount(),
                "total must be 2 x 1499.00 + 1 x 799.00");
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
}
