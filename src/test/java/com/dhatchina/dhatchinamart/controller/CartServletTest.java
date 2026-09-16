package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.dhatchina.dhatchinamart.util.SessionUtil;
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

    private void stubAdd(String productId, String quantity) {
        when(request.getParameter("action")).thenReturn("add");
        when(request.getParameter("productId")).thenReturn(productId);
        when(request.getParameter("quantity")).thenReturn(quantity);
        when(request.getParameter("format")).thenReturn(null);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void doPostAddAddsToCartAndRedirects() throws Exception {
        stubAdd("12", "2");

        servlet.doPost(request, response);

        verify(cartService).addToCart(BUYER_ID, 12L, 2);
        verify(response).sendRedirect("/cart");
    }

    @Test
    void doPostAddMissingProductRedirectsWithMessage() throws Exception {
        stubAdd("999", "1");
        org.mockito.Mockito.doThrow(new NotFoundException("Product not found"))
                .when(cartService).addToCart(BUYER_ID, 999L, 1);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=Product not found");
        verify(cartService).addToCart(BUYER_ID, 999L, 1);
    }

    @Test
    void doPostAddInvalidProductIdRedirectsWithMessage() throws Exception {
        stubAdd("abc", "1");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=Invalid product");
    }

    @Test
    void doPostAddOverStockRedirectsWithMessage() throws Exception {
        stubAdd("12", "99");
        org.mockito.Mockito.doThrow(new com.dhatchina.dhatchinamart.exception.ValidationException(
                "Only 5 unit(s) of \"Test Product\" are available in stock"))
                .when(cartService).addToCart(BUYER_ID, 12L, 99);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=Only 5 unit(s) of \"Test Product\" are available in stock");
        verify(cartService).addToCart(BUYER_ID, 12L, 99);
    }

    @Test
    void doPostUnknownActionReturnsBadRequest() throws Exception {
        stubAdd("12", "1");
        when(request.getParameter("action")).thenReturn("rename");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(cartService, never()).addToCart(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }
}