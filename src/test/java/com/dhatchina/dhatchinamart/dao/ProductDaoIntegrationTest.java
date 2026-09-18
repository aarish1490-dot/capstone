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

    @Test
    void pagedFindRespectsLimitAndOffset() {
        List<Product> first = productDAO.find(null, null, 4, 0);
        List<Product> second = productDAO.find(null, null, 4, 4);

        assertEquals(4, first.size());
        assertEquals(4, second.size());
        List<Long> firstIds = first.stream().map(Product::getId).toList();
        List<Long> secondIds = second.stream().map(Product::getId).toList();
        for (int i = 1; i < firstIds.size(); i++) {
            assertTrue(firstIds.get(i - 1) > firstIds.get(i), "products must be ordered newest first");
        }
        assertTrue(secondIds.get(0) < firstIds.get(0), "offset page must come after the first page");
    }

    @Test
    void pagedFindCombinedWithFilters() {
        List<Product> products = productDAO.find("the", "Books", 1, 0);

        assertEquals(1, products.size());
        String name = products.get(0).getName();
        assertTrue("The Pragmatic Programmer".equals(name) || "The Psychology of Money".equals(name),
                "expected one of the two matching books but was: " + name);
    }

    @Test
    void pagedFindBeyondEndReturnsEmpty() {
        assertTrue(productDAO.find(null, null, 4, 1000).isEmpty());
    }

    @Test
    void countWithNoFiltersMatchesAllProducts() {
        assertEquals(40, productDAO.count(null, null));
    }

    @Test
    void countWithKeywordFilters() {
        assertEquals(1, productDAO.count("headphones", null));
    }

    @Test
    void countWithCategoryFilters() {
        assertEquals(8, productDAO.count(null, "Books"));
    }

    @Test
    void countWithCombinedFilters() {
        assertEquals(2, productDAO.count("the", "Books"));
    }

    @Test
    void keywordSearchTrimsAndIgnoresCase() {
        List<Product> products = productDAO.find("  HEADPHONES  ", null);

        assertEquals(1, products.size());
        assertEquals("Wireless Bluetooth Headphones", products.get(0).getName());
    }

    @Test
    void sqlInjectionLikeKeywordCannotAffectTable() {
        List<Product> products = productDAO.find("'; DROP TABLE products;--", null);

        assertTrue(products.isEmpty());
        assertEquals(40, productDAO.countAll(), "products table must remain intact");
    }

    @Test
    void likeWildcardsAreTreatedAsLiterals() {
        assertTrue(productDAO.find("%", null).isEmpty(), "a literal percent sign matches nothing");
        assertTrue(productDAO.find("_", null).isEmpty(), "a literal underscore matches nothing");
    }

    @Test
    void marketplaceQueriesHideOutOfStockProducts() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Out Of Stock Widget");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQty(0);
        product.setCategory("Home");
        long id = productDAO.insert(product);

        assertEquals(40, productDAO.count(null, null), "out-of-stock products must not be counted");
        assertTrue(productDAO.count("Out Of Stock Widget", null) == 0);
        assertFalse(productDAO.find(null, null).stream().anyMatch(p -> p.getId() == id));
        assertFalse(productDAO.find("Widget", null).stream().anyMatch(p -> p.getId() == id));
    }

    @Test
    void findByIdAndBySellerStillReturnOutOfStockProducts() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Restock Me Later");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQty(0);
        product.setCategory("Home");
        long id = productDAO.insert(product);

        Optional<Product> found = productDAO.findById(id);
        assertTrue(found.isPresent(), "sellers must still be able to load an out-of-stock product");
        assertTrue(productDAO.findBySeller(1L).stream().anyMatch(p -> p.getId() == id),
                "seller dashboard must still list out-of-stock products");
    }

    @Test
    void findAllIncludesActiveAndUnlistedProducts() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Under Moderation");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQty(10);
        product.setCategory("Home");
        long id = productDAO.insert(product);
        assertTrue(productDAO.updateActive(id, false));

        List<Product> all = productDAO.findAll();

        assertTrue(all.stream().anyMatch(p -> p.getId() == id && !p.isActive()),
                "admin moderation must see unlisted products");
        assertTrue(all.size() == productDAO.countAll());
    }

    @Test
    void marketplaceQueriesHideUnlistedProducts() {
        Product product = new Product();
        product.setSellerId(1L);
        product.setName("Hidden From Buyers");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQty(10);
        product.setCategory("Home");
        long id = productDAO.insert(product);
        assertTrue(productDAO.updateActive(id, false));

        assertEquals(40, productDAO.count(null, null), "unlisted products must not be counted");
        assertEquals(0, productDAO.count("Hidden From Buyers", null));
        assertFalse(productDAO.find(null, null).stream().anyMatch(p -> p.getId() == id));
        assertTrue(productDAO.findById(id).isPresent(),
                "detail load used by sellers/admin must still see the unlisted product");
        assertTrue(productDAO.findBySeller(1L).stream().anyMatch(p -> p.getId() == id),
                "seller dashboard must still list unlisted products");
    }

    @Test
    void updateActiveMissingProductReturnsFalse() {
        assertFalse(productDAO.updateActive(999_999L, false));
    }
}
