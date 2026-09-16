package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDaoIntegrationTest {

    private ProductDAO productDAO;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDb.newDataSource("productdaotest");
        productDAO = new ProductDAOImpl(dataSource);
        userDAO = new UserDAOImpl(dataSource);
    }

    @Test
    void findAllReturnsSeededProducts() {
        List<Product> products = productDAO.find(null, null);
        assertEquals(40, products.size());
    }

    @Test
    void searchByKeywordFiltersProducts() {
        List<Product> products = productDAO.find("headphones", null);
        assertEquals(1, products.size());
        assertEquals("Wireless Bluetooth Headphones", products.get(0).getName());
    }

    @Test
    void filterByCategoryReturnsOnlyThatCategory() {
        List<Product> books = productDAO.find(null, "Books");
        assertEquals(8, books.size());
        assertTrue(books.stream().allMatch(p -> "Books".equals(p.getCategory())));
    }

    @Test
    void searchAndCategoryTogether() {
        List<Product> products = productDAO.find("the", "Books");
        assertEquals(2, products.size(), "keyword matches name or description within the selected category");
        assertTrue(products.stream().anyMatch(p -> "The Pragmatic Programmer".equals(p.getName())));
        assertTrue(products.stream().anyMatch(p -> "The Psychology of Money".equals(p.getName())));
    }

    @Test
    void findByIdReturnsProductWithSellerName() {
        Optional<Product> product = productDAO.findById(1L);
        assertTrue(product.isPresent());
        assertEquals("Wireless Bluetooth Headphones", product.get().getName());
        assertEquals(new BigDecimal("1499.00"), product.get().getPrice());
        assertTrue(product.get().getSellerName() != null && !product.get().getSellerName().isBlank());
    }

    @Test
    void insertPersistsProduct() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Test Widget");
        product.setDescription("A widget");
        product.setPrice(new BigDecimal("99.50"));
        product.setStockQty(7);
        product.setCategory("Home");
        product.setImageUrl("https://example.com/widget.png");

        long id = productDAO.insert(product);

        Optional<Product> found = productDAO.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Test Widget", found.get().getName());
        assertEquals(7, found.get().getStockQty());
    }

    @Test
    void updatePersistsChanges() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Original Name");
        product.setDescription("Old description");
        product.setPrice(new BigDecimal("50.00"));
        product.setStockQty(3);
        product.setCategory("Books");
        long id = productDAO.insert(product);

        product.setId(id);
        product.setName("Updated Name");
        product.setDescription("New description");
        product.setPrice(new BigDecimal("75.25"));
        product.setStockQty(12);
        product.setCategory("Electronics");
        product.setImageUrl("https://example.com/new.png");

        boolean changed = productDAO.update(product);

        assertTrue(changed);
        Optional<Product> found = productDAO.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertEquals("New description", found.get().getDescription());
        assertEquals(new BigDecimal("75.25"), found.get().getPrice());
        assertEquals(12, found.get().getStockQty());
        assertEquals("Electronics", found.get().getCategory());
        assertEquals("https://example.com/new.png", found.get().getImageUrl());
    }

    @Test
    void updateMissingProductReturnsFalse() {
        Product product = new Product();
        product.setId(999999L);
        product.setName("Ghost");
        product.setPrice(new BigDecimal("1.00"));
        product.setStockQty(1);
        product.setCategory("Home");

        assertFalse(productDAO.update(product));
    }

    @Test
    void deleteRemovesProduct() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("To Delete");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQty(1);
        product.setCategory("Home");
        long id = productDAO.insert(product);

        assertTrue(productDAO.delete(id));
        assertFalse(productDAO.findById(id).isPresent());
    }

    @Test
    void deleteMissingProductReturnsFalse() {
        assertFalse(productDAO.delete(999999L));
    }

    @Test
    void findProductsBySeller() {
        List<Product> sellerProducts = productDAO.findBySeller(1L);

        assertTrue(sellerProducts.stream().allMatch(p -> p.getSellerId() == 1L));
        assertFalse(sellerProducts.isEmpty());
    }

    @Test
    void adminAccountHasBcryptHash() {
        Optional<User> admin = userDAO.findByEmail("admin@dhatchinamart.com");
        assertTrue(admin.isPresent());
        assertEquals(User.Role.ADMIN, admin.get().getRole());
        assertTrue(admin.get().getPasswordHash().startsWith("$2"), "seed passwords must be bcrypt hashes");
        assertFalse(admin.get().getPasswordHash().contains("Admin@123"), "plaintext must not be stored");
    }
}
