package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private ProductDAO productDAO;

    private SellerService sellerService;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService(productDAO);
    }

    private static final long SELLER_ID = 10L;

    @Test
    void createProductValidReturnsProductWithId() {
        when(productDAO.insert(any(Product.class))).thenReturn(42L);

        Product product = sellerService.createProduct(
                SELLER_ID, "Test Product", "A description", "499.00", "10", "Home",
                "https://example.com/image.jpg");

        assertNotNull(product);
        assertEquals(42L, product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals(SELLER_ID, product.getSellerId());
        verify(productDAO).insert(any(Product.class));
    }

    @Test
    void createWithBlankNameThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "", "desc", "100.00", "1", "Home", ""));
    }

    @Test
    void createWithNullNameThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, null, "desc", "100.00", "1", "Home", ""));
    }

    @Test
    void createWithZeroPriceThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "0", "1", "Home", ""));
    }

    @Test
    void createWithNegativePriceThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "-50.00", "1", "Home", ""));
    }

    @Test
    void createWithNegativeStockThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "100.00", "-5", "Home", ""));
    }

    @Test
    void createWithBlankCategoryThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "100.00", "1", "", ""));
    }

    @Test
    void createWithNullCategoryThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "100.00", "1", null, ""));
    }

    @Test
    void createWithJavascriptImageUrlThrows() {
        assertThrows(ValidationException.class,
                () -> sellerService.createProduct(SELLER_ID, "Name", "desc", "100.00", "1", "Home",
                        "javascript:alert(1)"));
    }

    @Test
    void createWithNullableDescriptionSucceeds() {
        when(productDAO.insert(any(Product.class))).thenReturn(1L);

        Product product = sellerService.createProduct(
                SELLER_ID, "Name", null, "100.00", "1", "Home", "");

        assertNotNull(product);
        verify(productDAO).insert(any(Product.class));
    }

    @Test
    void createWithHttpImageUrlSucceeds() {
        when(productDAO.insert(any(Product.class))).thenReturn(1L);

        Product product = sellerService.createProduct(
                SELLER_ID, "Name", "desc", "100.00", "1", "Home",
                "https://example.com/img.png");

        assertNotNull(product);
        assertEquals("https://example.com/img.png", product.getImageUrl());
    }
}