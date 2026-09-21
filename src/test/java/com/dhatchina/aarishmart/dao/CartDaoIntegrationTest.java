package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.dao.impl.CartDAOImpl;
import com.dhatchina.aarishmart.dao.impl.ProductDAOImpl;
import com.dhatchina.aarishmart.dao.impl.UserDAOImpl;
import com.dhatchina.aarishmart.dto.CartRow;
import com.dhatchina.aarishmart.model.CartItem;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartDaoIntegrationTest {

    private static final long SEED_PRODUCT_TOTE = 1L;

    private CartDAO cartDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private long buyerA;
    private long buyerB;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDb.newDataSource("cartdaotest");
        cartDAO = new CartDAOImpl(dataSource);
        productDAO = new ProductDAOImpl(dataSource);
        userDAO = new UserDAOImpl(dataSource);
        buyerA = createBuyer("Buyer A", "buyera@example.com", "9876500002");
        buyerB = createBuyer("Buyer B", "buyerb@example.com", "9876500003");
    }

    private long createBuyer(String name, String email, String mobile) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash("$2a$10$encodedHashValueForTestingOnly");
        user.setRole(User.Role.BUYER);
        return userDAO.insert(user);
    }

    private void addItem(long userId, long productId, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        cartDAO.insert(item);
    }

    @Test
    void findRowsByUserIdReturnsJoinedProductInformation() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);

        List<CartRow> rows = cartDAO.findRowsByUserId(buyerA);

        assertEquals(1, rows.size());
        CartRow row = rows.get(0);
        assertEquals(SEED_PRODUCT_TOTE, row.getProductId());
        assertEquals(2, row.getQuantity());
        assertNotNull(row.getProduct());
        assertEquals("Kalamkari Canvas Tote Bag", row.getProduct().getName());
        assertEquals(new BigDecimal("599.00"), row.getProduct().getPrice());
        assertNotNull(row.getProduct().getSellerName());
        assertFalse(row.getProduct().getSellerName().isBlank());
    }

    @Test
    void findRowsSeparatesCartsByUser() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);
        addItem(buyerA, 2L, 1);

        assertTrue(cartDAO.findByUserId(buyerB).isEmpty());
        assertTrue(cartDAO.findRowsByUserId(buyerB).isEmpty());
        assertEquals(2, cartDAO.findRowsByUserId(buyerA).size());
    }

    @Test
    void insertIsFoundByUserAndProduct() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 3);

        Optional<CartItem> found = cartDAO.findByUserAndProduct(buyerA, SEED_PRODUCT_TOTE);

        assertTrue(found.isPresent());
        assertEquals(3, found.get().getQuantity());
        assertEquals(buyerA, found.get().getUserId());
    }

    @Test
    void missingItemInOtherUsersCartReturnsEmpty() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 1);

        assertTrue(cartDAO.findByUserAndProduct(buyerB, SEED_PRODUCT_TOTE).isEmpty());
    }

    @Test
    void updateQuantityPersists() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 1);

        cartDAO.updateQuantity(buyerA, SEED_PRODUCT_TOTE, 7);

        Optional<CartItem> found = cartDAO.findByUserAndProduct(buyerA, SEED_PRODUCT_TOTE);
        assertTrue(found.isPresent());
        assertEquals(7, found.get().getQuantity());
    }

    @Test
    void deleteRemovesOnlyTheTargetUsersItem() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);
        addItem(buyerB, SEED_PRODUCT_TOTE, 2);

        cartDAO.delete(buyerA, SEED_PRODUCT_TOTE);

        assertTrue(cartDAO.findByUserAndProduct(buyerA, SEED_PRODUCT_TOTE).isEmpty());
        assertTrue(cartDAO.findByUserAndProduct(buyerB, SEED_PRODUCT_TOTE).isPresent());
    }

    @Test
    void clearForUserRemovesAllOfThatUsersItems() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);
        addItem(buyerA, 2L, 1);
        addItem(buyerB, SEED_PRODUCT_TOTE, 1);

        cartDAO.clearForUser(buyerA);

        assertEquals(0, cartDAO.countByUser(buyerA));
        assertEquals(1, cartDAO.countByUser(buyerB));
    }

    @Test
    void countByUserSumsQuantities() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);
        addItem(buyerA, 2L, 3);

        assertEquals(5, cartDAO.countByUser(buyerA));
    }

    @Test
    void countByUserIsZeroForOperatorOnEmptyCart() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);

        assertEquals(0, cartDAO.countByUser(buyerB));
    }

    @Test
    void injectionLikeIdsCannotAffectOtherRows() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 2);

        assertTrue(cartDAO.findByUserAndProduct(buyerA, Long.MAX_VALUE).isEmpty());
        assertTrue(cartDAO.findByUserAndProduct(buyerA, -1L).isEmpty());
        cartDAO.updateQuantity(buyerA, -1L, 5);
        cartDAO.delete(buyerA, -1L);

        assertEquals(2, cartDAO.findRowsByUserId(buyerA).get(0).getQuantity());
    }

    @Test
    void productInfoIncludesStockAndSellerName() {
        addItem(buyerA, SEED_PRODUCT_TOTE, 1);

        CartRow row = cartDAO.findRowsByUserId(buyerA).get(0);

        assertNotNull(row.getProduct());
        assertEquals(24, row.getProduct().getStockQty());
        assertEquals("Platform Admin", row.getProduct().getSellerName());
    }
}