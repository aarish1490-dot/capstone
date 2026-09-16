package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dto.CartRow;
import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.CartItem;
import com.dhatchina.dhatchinamart.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final long BUYER_ID = 2L;
    private static final long PRODUCT_ID = 1L;

    @Mock
    private CartDAO cartDAO;

    @Mock
    private ProductDAO productDAO;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartDAO, productDAO);
    }

    private Product productWithStock(int stock) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQty(stock);
        return product;
    }

    @Test
    void addToCartInsertsNewItemWhenNotPresent() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(10)));
        when(cartDAO.findByUserAndProduct(BUYER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        cartService.addToCart(BUYER_ID, PRODUCT_ID, 2);

        verify(cartDAO).insert(org.mockito.ArgumentMatchers.argThat(item ->
                item.getUserId() == BUYER_ID && item.getProductId() == PRODUCT_ID && item.getQuantity() == 2));
    }

    @Test
    void addToCartIncrementsExistingItemQuantity() {
        CartItem existing = new CartItem();
        existing.setUserId(BUYER_ID);
        existing.setProductId(PRODUCT_ID);
        existing.setQuantity(1);
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(10)));
        when(cartDAO.findByUserAndProduct(BUYER_ID, PRODUCT_ID)).thenReturn(Optional.of(existing));

        cartService.addToCart(BUYER_ID, PRODUCT_ID, 2);

        verify(cartDAO).updateQuantity(BUYER_ID, PRODUCT_ID, 3);
    }

    @Test
    void addToCartBeyondStockThrows() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(2)));

        assertThrows(ValidationException.class, () -> cartService.addToCart(BUYER_ID, PRODUCT_ID, 3));
    }

    @Test
    void addToCartForMissingProductThrowsNotFound() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cartService.addToCart(BUYER_ID, PRODUCT_ID, 1));
    }

    @Test
    void updateQuantityBelowOneThrows() {
        assertThrows(ValidationException.class, () -> cartService.updateQuantity(BUYER_ID, PRODUCT_ID, 0));
    }

    @Test
    void updateQuantityBeyondStockThrows() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(5)));
        when(cartDAO.findByUserAndProduct(BUYER_ID, PRODUCT_ID)).thenReturn(Optional.of(new CartItem()));

        assertThrows(ValidationException.class, () -> cartService.updateQuantity(BUYER_ID, PRODUCT_ID, 6));
    }

    @Test
    void updateQuantityValidDelegatesToDao() {
        when(productDAO.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(10)));
        when(cartDAO.findByUserAndProduct(BUYER_ID, PRODUCT_ID)).thenReturn(Optional.of(new CartItem()));

        cartService.updateQuantity(BUYER_ID, PRODUCT_ID, 4);

        verify(cartDAO).updateQuantity(BUYER_ID, PRODUCT_ID, 4);
    }

    @Test
    void getCartReturnsEmptyViewForEmptyCart() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(Collections.emptyList());

        CartView view = cartService.getCart(BUYER_ID);

        assertTrue(view.isEmpty());
        assertEquals(0, view.getCount());
        assertEquals(BigDecimal.ZERO, view.getTotal());
        assertFalse(view.isHasWarnings());
    }

    @Test
    void getCartBuildsLinesAndTotals() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(List.of(
                new CartRow(PRODUCT_ID, 2, productWithStock(10)),
                new CartRow(2L, 1, productWithPrice(200L))
        ));

        CartView view = cartService.getCart(BUYER_ID);

        assertFalse(view.isEmpty());
        assertEquals(3, view.getCount());
        assertEquals(new BigDecimal("300.00"), view.getTotal());
        assertFalse(view.isHasWarnings());
    }

    @Test
    void getCartDropsRowsWhoseProductNoLongerExists() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(List.of(
                new CartRow(PRODUCT_ID, 2, null)));

        CartView view = cartService.getCart(BUYER_ID);

        assertTrue(view.isEmpty());
        assertEquals(1, view.getUnavailableRemoved());
        assertTrue(view.isHasWarnings());
        verify(cartDAO).delete(BUYER_ID, PRODUCT_ID);
    }

    @Test
    void getCartDropsRowsWhenProductIsOutOfStock() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(List.of(
                new CartRow(PRODUCT_ID, 2, productWithStock(0))));

        CartView view = cartService.getCart(BUYER_ID);

        assertTrue(view.isEmpty());
        assertEquals(1, view.getUnavailableRemoved());
        assertTrue(view.isHasWarnings());
        verify(cartDAO).delete(BUYER_ID, PRODUCT_ID);
    }

    @Test
    void getCartClampsQuantityToAvailableStock() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(List.of(
                new CartRow(PRODUCT_ID, 10, productWithStock(5))));

        CartView view = cartService.getCart(BUYER_ID);

        assertEquals(1, view.getLines().size());
        assertEquals(5, view.getLines().get(0).getQuantity());
        assertEquals(1, view.getQuantityReduced());
        assertTrue(view.isHasWarnings());
        verify(cartDAO).updateQuantity(BUYER_ID, PRODUCT_ID, 5);
    }

    @Test
    void getCartLeavesHealthyRowsUnchanged() {
        when(cartDAO.findRowsByUserId(BUYER_ID)).thenReturn(List.of(
                new CartRow(PRODUCT_ID, 3, productWithStock(10))));

        CartView view = cartService.getCart(BUYER_ID);

        assertEquals(3, view.getLines().get(0).getQuantity());
        assertFalse(view.isHasWarnings());
    }

    private Product productWithPrice(long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Test Product " + id);
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQty(10);
        return product;
    }
}
