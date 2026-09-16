package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.exception.ForbiddenException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductDAO productDAO;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productDAO);
    }

    @Test
    void getByIdReturnsProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Wireless Bluetooth Headphones");
        product.setPrice(new BigDecimal("1299.00"));
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        Product found = productService.getById(1L);

        assertEquals(1L, found.getId());
        assertEquals("Wireless Bluetooth Headphones", found.getName());
    }

    @Test
    void getByIdForMissingProductThrows() {
        when(productDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getById(999L));
    }

    @Test
    void browseDelegatesSearchAndCategoryToDao() {
        when(productDAO.find("head", "Electronics")).thenReturn(List.of(new Product()));

        List<Product> products = productService.browse("head", "Electronics");

        assertEquals(1, products.size());
        verify(productDAO).find("head", "Electronics");
    }

    private Product sampleOwnedProduct(long id, long sellerId) {
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setName("Wireless Headphones");
        product.setDescription("Bluetooth over-ear headphones");
        product.setPrice(new BigDecimal("1499.00"));
        product.setStockQty(15);
        product.setCategory("Electronics");
        product.setImageUrl("https://example.com/headphones.jpg");
        return product;
    }

    @Test
    void getOwnedProductReturnsForOwner() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        Product found = productService.getOwnedProduct(10L, 1L);

        assertEquals(1L, found.getId());
        assertEquals(10L, found.getSellerId());
    }

    @Test
    void getOwnedProductThrowsForOtherSeller() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> productService.getOwnedProduct(11L, 1L));
    }

    @Test
    void getOwnedProductThrowsForMissing() {
        when(productDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> productService.getOwnedProduct(10L, 999L));
    }

    @Test
    void updateProductDelegatesToDao() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));
        when(productDAO.update(any(Product.class))).thenReturn(true);

        productService.updateProduct(10L, 1L, "New Name", "New desc", "2500.00", "20", "Electronics", "https://example.com/new.jpg");

        verify(productDAO).update(any(Product.class));
    }

    @Test
    void updateOtherSellersProductThrowsForbidden() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> productService.updateProduct(11L, 1L, "Name", "", "100.00", "5", "Home", ""));
    }

    @Test
    void updateInvalidNameThrowsValidation() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> productService.updateProduct(10L, 1L, "", "", "100.00", "5", "Home", ""));
    }

    @Test
    void updateInvalidPriceThrowsValidation() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> productService.updateProduct(10L, 1L, "Name", "", "-5.00", "5", "Home", ""));
    }

    @Test
    void updateInvalidStockThrowsValidation() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> productService.updateProduct(10L, 1L, "Name", "", "100.00", "-1", "Home", ""));
    }

    @Test
    void updateBlankCategoryThrowsValidation() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> productService.updateProduct(10L, 1L, "Name", "", "100.00", "5", "", ""));
    }

    @Test
    void updateDangerousImageUrlThrowsValidation() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> productService.updateProduct(10L, 1L, "Name", "", "100.00", "5", "Home", "javascript:alert(1)"));
    }

    @Test
    void deleteOwnedProductCallsDao() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));
        when(productDAO.delete(1L)).thenReturn(true);

        productService.deleteProduct(10L, 1L);

        verify(productDAO).delete(1L);
    }

    @Test
    void deleteOtherSellersProductThrowsForbidden() {
        Product product = sampleOwnedProduct(1L, 10L);
        when(productDAO.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> productService.deleteProduct(11L, 1L));
    }

    @Test
    void deleteMissingProductThrowsNotFound() {
        when(productDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> productService.deleteProduct(10L, 999L));
    }
}
