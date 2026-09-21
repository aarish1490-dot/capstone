package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.CartLine;
import com.dhatchina.aarishmart.dto.CartView;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.CartService;
import com.dhatchina.aarishmart.util.ServiceRegistry;
import com.dhatchina.aarishmart.util.SessionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServletTest {

    private static final long BUYER_ID = 2L;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private CartService cartService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private CartServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getCartService).thenReturn(cartService);
        User buyer = new User();
        buyer.setId(BUYER_ID);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        servlet = new CartServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private void stubParams(String action, String productId, String quantity, String format) {
        when(request.getParameter("action")).thenReturn(action);
        when(request.getParameter("productId")).thenReturn(productId);
        when(request.getParameter("quantity")).thenReturn(quantity);
        when(request.getParameter("format")).thenReturn(format);
        when(request.getContextPath()).thenReturn("");
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private CartView viewWith(int quantity) {
        Product product = new Product();
        product.setId(12L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("999.00"));
        product.setStockQty(25);
        CartView view = new CartView();
        view.addLine(new CartLine(product, quantity));
        return view;
    }

    @Test
    void doPostAddAddsToCartAndRedirects() throws Exception {
        stubParams("add", "12", "2", null);

        servlet.doPost(request, response);

        verify(cartService).addToCart(BUYER_ID, 12L, 2);
        verify(response).sendRedirect("/cart");
    }

    @Test
    void doPostAddMissingProductRedirectsWithEncodedMessage() throws Exception {
        stubParams("add", "999", "1", null);
        org.mockito.Mockito.doThrow(new NotFoundException("Product not found"))
                .when(cartService).addToCart(BUYER_ID, 999L, 1);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Product not found"));
        verify(cartService).addToCart(BUYER_ID, 999L, 1);
    }

    @Test
    void doPostAddInvalidProductIdRedirectsWithEncodedMessage() throws Exception {
        stubParams("add", "abc", "1", null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Invalid product"));
    }

    @Test
    void doPostAddInjectionLikeProductIdIsRejected() throws Exception {
        stubParams("add", "'; DROP TABLE cart_items;--", "1", null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Invalid product"));
        verify(cartService, never()).addToCart(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void doPostAddInvalidQuantityRedirectsWithEncodedMessage() throws Exception {
        stubParams("add", "12", "abc", null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Quantity must be a valid number"));
    }

    @Test
    void doPostAddOverStockRedirectsWithEncodedMessage() throws Exception {
        stubParams("add", "12", "99", null);
        org.mockito.Mockito.doThrow(new ValidationException("Only 5 of this item are available."))
                .when(cartService).addToCart(BUYER_ID, 12L, 99);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Only 5 of this item are available."));
        verify(cartService).addToCart(BUYER_ID, 12L, 99);
    }

    @Test
    void doPostUpdateValidRedirects() throws Exception {
        stubParams("update", "12", "4", null);

        servlet.doPost(request, response);

        verify(cartService).updateQuantity(BUYER_ID, 12L, 4);
        verify(response).sendRedirect("/cart");
    }

    @Test
    void doPostUpdateItemNotInCartRedirectsWithEncodedMessage() throws Exception {
        stubParams("update", "12", "4", null);
        org.mockito.Mockito.doThrow(new NotFoundException("Item is not in your cart"))
                .when(cartService).updateQuantity(BUYER_ID, 12L, 4);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Item is not in your cart"));
    }

    @Test
    void doPostUpdateJsonWritesCartSummary() throws Exception {
        stubParams("update", "12", "3", "json");
        when(cartService.getCart(BUYER_ID)).thenReturn(viewWith(3));
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(cartService).updateQuantity(BUYER_ID, 12L, 3);
        String body = sw.toString();
        assertTrue(body.contains("\"success\":true"));
        assertTrue(body.contains("\"count\":3"));
        assertTrue(body.contains("2,997.00"), "total must be formatted as currency");
    }

    @Test
    void doPostRemoveJsonWritesCartSummary() throws Exception {
        stubParams("remove", "12", "0", "json");
        when(cartService.getCart(BUYER_ID)).thenReturn(new CartView());
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(cartService).removeItem(BUYER_ID, 12L);
        assertTrue(sw.toString().contains("\"success\":true"));
        assertTrue(sw.toString().contains("\"count\":0"));
    }

    @Test
    void doPostRemoveNonJsonRedirects() throws Exception {
        stubParams("remove", "12", "0", null);

        servlet.doPost(request, response);

        verify(cartService).removeItem(BUYER_ID, 12L);
        verify(response).sendRedirect("/cart");
    }

    @Test
    void doPostUnknownActionReturnsBadRequest() throws Exception {
        stubParams("rename", "12", "1", null);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(cartService, never()).addToCart(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void doPostMissingProductIdRedirectsWithEncodedMessage() throws Exception {
        stubParams("add", null, "1", null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=" + enc("Invalid product"));
    }
}