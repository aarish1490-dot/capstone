package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.OrderService;
import com.dhatchina.aarishmart.service.ReviewService;
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

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServletTest {

    private static final long BUYER_ID = 7L;
    private static final String HISTORY_JSP = "/WEB-INF/jsp/order-history.jsp";
    private static final String DETAILS_JSP = "/WEB-INF/jsp/order-details.jsp";
    private static final String SUCCESS_JSP = "/WEB-INF/jsp/order-success.jsp";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private OrderService orderService;

    @Mock
    private ReviewService reviewService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private OrderServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getOrderService).thenReturn(orderService);
        serviceRegistryMock.when(ServiceRegistry::getReviewService).thenReturn(reviewService);
        User buyer = new User();
        buyer.setId(BUYER_ID);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getRequestDispatcher(HISTORY_JSP)).thenReturn(dispatcher);
        when(request.getRequestDispatcher(DETAILS_JSP)).thenReturn(dispatcher);
        when(request.getRequestDispatcher(SUCCESS_JSP)).thenReturn(dispatcher);
        servlet = new OrderServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private Order order(long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }

    @Test
    void historyForwardsWithOrders() throws Exception {
        when(request.getServletPath()).thenReturn("/orders");
        when(orderService.ordersForBuyer(BUYER_ID)).thenReturn(List.of(order(5L)));

        servlet.doGet(request, response);

        verify(orderService).ordersForBuyer(BUYER_ID);
        verify(request).setAttribute(eq("orders"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsForwardsWithOrderAndItems() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn("5");
        Order order = order(5L);
        when(orderService.getOrderForBuyer(5L, BUYER_ID)).thenReturn(order);
        when(orderService.itemsForOrder(5L)).thenReturn(List.of());

        servlet.doGet(request, response);

        verify(request).setAttribute("order", order);
        verify(request).setAttribute(eq("items"), anyList());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void deliveredOrderDetailsSetsReviewedProductIds() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn("5");
        Order order = order(5L);
        order.setStatus("DELIVERED");
        when(orderService.getOrderForBuyer(5L, BUYER_ID)).thenReturn(order);
        when(orderService.itemsForOrder(5L)).thenReturn(List.of());
        when(reviewService.reviewedProductIdsForOrder(5L)).thenReturn(Set.of(3L, 4L));

        servlet.doGet(request, response);

        verify(request).setAttribute("reviewedProductIds", Set.of(3L, 4L));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void ordersNotYetDeliveredSkipReviewedProductIds() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn("5");
        Order order = order(5L);
        order.setStatus("SHIPPED");
        when(orderService.getOrderForBuyer(5L, BUYER_ID)).thenReturn(order);
        when(orderService.itemsForOrder(5L)).thenReturn(List.of());

        servlet.doGet(request, response);

        verify(reviewService, never()).reviewedProductIdsForOrder(5L);
        verify(request, never()).setAttribute(eq("reviewedProductIds"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsWithInvalidIdIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn("abc");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void detailsWithMissingIdIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void detailsOfAnotherBuyersOrderIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/order");
        when(request.getParameter("id")).thenReturn("5");
        when(orderService.getOrderForBuyer(5L, BUYER_ID)).thenThrow(new NotFoundException("Order not found"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void orderSuccessForwardsWithOrder() throws Exception {
        when(request.getServletPath()).thenReturn("/order-success");
        when(request.getParameter("id")).thenReturn("9");
        Order order = order(9L);
        when(orderService.getOrderForBuyer(9L, BUYER_ID)).thenReturn(order);

        servlet.doGet(request, response);

        verify(request).setAttribute("order", order);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void orderSuccessOfAnotherBuyerIs404() throws Exception {
        when(request.getServletPath()).thenReturn("/order-success");
        when(request.getParameter("id")).thenReturn("9");
        when(orderService.getOrderForBuyer(9L, BUYER_ID)).thenThrow(new NotFoundException("Order not found"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }
}