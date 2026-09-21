package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.OrderItem;
import com.dhatchina.aarishmart.model.OrderStatus;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.OrderService;
import com.dhatchina.aarishmart.util.SessionUtil;
import com.dhatchina.aarishmart.util.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/orders", "/order", "/order-success"})
public class OrderServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OrderServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/orders".equals(path)) {
            handleHistory(request, response);
        } else if ("/order-success".equals(path)) {
            handleSuccess(request, response);
        } else {
            handleDetails(request, response);
        }
    }

    private void handleHistory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        List<Order> orders = ServiceRegistry.getOrderService().ordersForBuyer(user.getId());
        request.setAttribute("orders", orders);
        request.getRequestDispatcher("/WEB-INF/jsp/order-history.jsp").forward(request, response);
    }

    private void handleSuccess(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        try {
            long id = parseId(request);
            Order order = ServiceRegistry.getOrderService().getOrderForBuyer(id, user.getId());
            request.setAttribute("order", order);
            request.getRequestDispatcher("/WEB-INF/jsp/order-success.jsp").forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        try {
            long id = parseId(request);
            OrderService orderService = ServiceRegistry.getOrderService();
            Order order = orderService.getOrderForBuyer(id, user.getId());
            List<OrderItem> items = orderService.itemsForOrder(id);
            request.setAttribute("order", order);
            request.setAttribute("items", items);
            if (OrderStatus.DELIVERED.name().equals(order.getStatus())) {
                request.setAttribute("reviewedProductIds",
                        ServiceRegistry.getReviewService().reviewedProductIdsForOrder(id));
            }
            request.getRequestDispatcher("/WEB-INF/jsp/order-details.jsp").forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to load order", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private long parseId(HttpServletRequest request) {
        String raw = request.getParameter("id");
        if (raw == null) {
            throw new NotFoundException("Order not found");
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Order not found");
        }
    }
}