package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.SellerOrderView;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.User;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerOrderServletTest {

    private static final long SELLER_ID = 3L;
    private static final String ORDERS_JSP = "/WEB-INF/jsp/seller-orders.jsp";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private OrderService orderService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private SellerOrderServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getOrderService).thenReturn(orderService);
        User seller = new User();
        seller.setId(SELLER_ID);
        seller.setRole(User.Role.SELLER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(seller);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestDispatcher(ORDERS_JSP)).thenReturn(dispatcher);
        servlet = new SellerOrderServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    @Test
    void doGetListsOwnedOrders() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders");
        when(orderService.ordersForSeller(SELLER_ID)).thenReturn(
                List.of(new SellerOrderView(new Order(), List.of())));

        servlet.doGet(request, response);

        verify(orderService).ordersForSeller(SELLER_ID);
        verify(request).setAttribute(eq("views"), anyList());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetOnStatusPathIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders/status");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void doPostAdvanceRedirectsWithSuccess() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders/status");
        when(request.getParameter("orderId")).thenReturn("11");

        servlet.doPost(request, response);

        verify(orderService).advanceOrderStatus(SELLER_ID, 11L);
        verify(response).sendRedirect("/seller/orders?msg=advanced");
    }

    @Test
    void doPostWhenOrderDoesNotBelongToSellerIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders/status");
        when(request.getParameter("orderId")).thenReturn("11");
        doThrow(new NotFoundException("Order not found"))
                .when(orderService).advanceOrderStatus(SELLER_ID, 11L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doPostInvalidTransitionRedirectsWithStateMessage() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders/status");
        when(request.getParameter("orderId")).thenReturn("11");
        doThrow(new ValidationException("Order is already delivered"))
                .when(orderService).advanceOrderStatus(SELLER_ID, 11L);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/seller/orders?msg=state");
    }

    @Test
    void doPostInvalidOrderIdIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders/status");
        when(request.getParameter("orderId")).thenReturn("abc");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(orderService, never()).advanceOrderStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void doPostToListPathIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/seller/orders");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(orderService, never()).advanceOrderStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}