package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartFlowIntegrationTest {

    private static final long HEADPHONES_ID = 1L;
    private static final long SMART_WATCH_ID = 4L;

    private CartService cartService;
    private CartDAO cartDAO;
    private ProductDAO productDAO;
    private long buyerId;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDb.newDataSource("cartflowtest");
        productDAO = new ProductDAOImpl(dataSource);
        cartDAO = new CartDAOImpl(dataSource);
        UserDAO userDAO = new UserDAOImpl(dataSource);

        User buyer = new User();
        buyer.setName("Flow Buyer");
        buyer.setEmail("flowbuyer@example.com");
        buyer.setMobileNumber("9876500004");
        buyer.setPasswordHash("$2a$10$encodedHashValueForTestingOnly");
        buyer.setRole(User.Role.BUYER);
        buyerId = userDAO.insert(buyer);

        cartService = new CartService(cartDAO, productDAO);
    }

    @Test
    void cartLifecycleAddUpdateRemoveReAdd() {
        cartService.addToCart(buyerId, HEADPHONES_ID, 2);

        CartView view = cartService.getCart(buyerId);
        assertFalse(view.isEmpty());
        assertEquals(2, view.getCount());
        assertEquals(new BigDecimal("2998.00"), view.getTotal());
        assertEquals(2, view.getLines().get(0).getQuantity());

        cartService.updateQuantity(buyerId, HEADPHONES_ID, 3);
        view = cartService.getCart(buyerId);
        assertEquals(3, view.getCount());
        assertEquals(new BigDecimal("4497.00"), view.getTotal());

        cartService.addToCart(buyerId, HEADPHONES_ID, 1);
        assertEquals(4, cartService.countItems(buyerId));
        assertEquals(new BigDecimal("5996.00"), cartService.getCart(buyerId).getTotal());

        cartService.removeItem(buyerId, HEADPHONES_ID);
        assertTrue(cartService.getCart(buyerId).isEmpty());
        assertEquals(0, cartService.countItems(buyerId));

        cartService.addToCart(buyerId, HEADPHONES_ID, 1);
        assertEquals(1, cartService.countItems(buyerId));
    }

    @Test
    void combinedCartTotalMixesProductsAndQuantities() {
        cartService.addToCart(buyerId, HEADPHONES_ID, 2);
        cartService.addToCart(buyerId, SMART_WATCH_ID, 1);

        CartView view = cartService.getCart(buyerId);

        assertEquals(3, view.getCount());
        assertEquals(new BigDecimal("2998.00").add(new BigDecimal("2999.00")), view.getTotal());
        assertEquals(2, view.getLines().size());
    }

    @Test
    void secondAddOfSameProductIncrementsQuantity() {
        cartService.addToCart(buyerId, HEADPHONES_ID, 2);
        cartService.addToCart(buyerId, HEADPHONES_ID, 3);

        CartView view = cartService.getCart(buyerId);

        assertEquals(5, view.getCount());
        assertEquals(1, view.getLines().size());
        assertEquals(new BigDecimal("7495.00"), view.getTotal());
    }

    @Test
    void addingBeyondStockIsRejected() {
        cartService.addToCart(buyerId, HEADPHONES_ID, 20);

        assertThrows(ValidationException.class, () -> cartService.addToCart(buyerId, HEADPHONES_ID, 10));
        assertEquals(20, cartService.countItems(buyerId), "rejected add must not change the cart");
    }

    @Test
    void outOfStockOrShrunkStockItemsAreClampedOnRead() {
        cartService.addToCart(buyerId, SMART_WATCH_ID, 5);

        Optional<Product> watch = productDAO.findById(SMART_WATCH_ID);
        assertTrue(watch.isPresent());
        watch.get().setStockQty(2);
        productDAO.update(watch.get());

        CartView view = cartService.getCart(buyerId);

        assertEquals(1, view.getLines().size());
        assertEquals(2, view.getLines().get(0).getQuantity());
        assertEquals(1, view.getQuantityReduced());
        assertEquals(2, cartDAO.findByUserAndProduct(buyerId, SMART_WATCH_ID).orElseThrow().getQuantity(),
                "the clamped quantity must be persisted");
    }

    @Test
    void stockDroppingToZeroRemovesItemFromCart() {
        cartService.addToCart(buyerId, SMART_WATCH_ID, 2);

        Optional<Product> watch = productDAO.findById(SMART_WATCH_ID);
        assertTrue(watch.isPresent());
        watch.get().setStockQty(0);
        productDAO.update(watch.get());

        CartView view = cartService.getCart(buyerId);

        assertTrue(view.isEmpty());
        assertEquals(1, view.getUnavailableRemoved());
        assertTrue(cartDAO.findByUserAndProduct(buyerId, SMART_WATCH_ID).isEmpty(),
                "stale rows must be deleted from the cart");
    }
}