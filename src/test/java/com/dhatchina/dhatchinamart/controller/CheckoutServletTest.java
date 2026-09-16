package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.CartLine;
import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.service.OrderService;
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

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckoutServletTest {

    private static final long BUYER_ID = 2L;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private CheckoutServlet servlet;
    private User buyer;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getCartService).thenReturn(cartService);
        serviceRegistryMock.when(ServiceRegistry::getOrderService).thenReturn(orderService);
        buyer = new User();
        buyer.setId(BUYER_ID);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp")).thenReturn(dispatcher);
        servlet = new CheckoutServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private CartView cartWith(int quantity) {
        Product product = new Product();
        product.setId(4L);
        product.setName("Smart Watch");
        product.setPrice(new BigDecimal("2999.00"));
        CartView view = new CartView();
        view.addLine(new CartLine(product, quantity));
        return view;
    }

    @Test
    void doPostSuccessRedirectsToOrderSuccess() throws Exception {
        when(cartService.getCart(BUYER_ID)).thenReturn(cartWith(1));
        Order order = new Order();
        order.setId(42L);
        when(orderService.placeOrder(BUYER_ID)).thenReturn(order);

        servlet.doPost(request, response);

        verify(orderService).placeOrder(BUYER_ID);
        verify(response).sendRedirect("/order-success?id=42");
    }

    @Test
    void doPostValidationErrorRendersCheckoutWithMessage() throws Exception {
        CartView cart = cartWith(2);
        when(cartService.getCart(BUYER_ID)).thenReturn(cart);
        doThrow(new ValidationException("Only 2 unit(s) of \"Smart Watch\" are available in stock"))
                .when(orderService).placeOrder(BUYER_ID);

        servlet.doPost(request, response);

        verify(request).setAttribute("cart", cart);
        verify(request).setAttribute("error", "Only 2 unit(s) of \"Smart Watch\" are available in stock");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostWhenCartBecameEmptyRedirectsToCart() throws Exception {
        when(cartService.getCart(BUYER_ID)).thenReturn(new CartView());
        doThrow(new ValidationException("Your cart is empty")).when(orderService).placeOrder(BUYER_ID);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=empty");
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void doPostUnexpectedErrorRedirectsToErrorMessage() throws Exception {
        when(cartService.getCart(BUYER_ID)).thenReturn(cartWith(1));
        doThrow(new IllegalStateException("db down")).when(orderService).placeOrder(BUYER_ID);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/cart?msg=error");
    }

    @Test
    void doPostNonBuyerIsForbidden() throws Exception {
        User seller = new User();
        seller.setId(9L);
        seller.setRole(User.Role.SELLER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(seller);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(orderService, never()).placeOrder(anyLong());
    }

    @Test
    void doGetWithEmptyCartRedirectsToCart() throws Exception {
        when(cartService.getCart(BUYER_ID)).thenReturn(new CartView());

        servlet.doGet(request, response);

        verify(response).sendRedirect("/cart?msg=empty");
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void doGetWithCartForwardsToCheckout() throws Exception {
        CartView cart = cartWith(3);
        when(cartService.getCart(BUYER_ID)).thenReturn(cart);

        servlet.doGet(request, response);

        verify(request).setAttribute("cart", cart);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetNonBuyerIsForbidden() throws Exception {
        User seller = new User();
        seller.setId(9L);
        seller.setRole(User.Role.SELLER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(seller);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(dispatcher, never()).forward(request, response);
    }
}