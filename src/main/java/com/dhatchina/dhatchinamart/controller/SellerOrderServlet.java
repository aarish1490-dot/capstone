package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.SellerOrderView;
import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.OrderService;
import com.dhatchina.dhatchinamart.util.SessionUtil;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Seller console for orders. The CSRF filter and AuthFilter already protect
 * {@code /seller/*} paths, so this servlet only needs to verify that the
 * authenticated seller actually owns the order being advanced.
 */
@WebServlet(urlPatterns = {"/seller/orders", "/seller/orders/status"})
public class SellerOrderServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SellerOrderServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/seller/orders".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = SessionUtil.getUser(request);
        OrderService orderService = ServiceRegistry.getOrderService();
        List<SellerOrderView> views = orderService.ordersForSeller(user.getId());
        request.setAttribute("views", views);
        request.getRequestDispatcher("/WEB-INF/jsp/seller-orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/seller/orders/status".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = SessionUtil.getUser(request);
        long orderId;
        try {
            orderId = Long.parseLong(request.getParameter("orderId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            ServiceRegistry.getOrderService().advanceOrderStatus(user.getId(), orderId);
            response.sendRedirect(request.getContextPath() + "/seller/orders?msg=advanced");
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (AppException e) {
            response.sendRedirect(request.getContextPath() + "/seller/orders?msg=state");
        } catch (Exception e) {
            log.error("Failed to advance order {}", orderId, e);
            response.sendRedirect(request.getContextPath() + "/seller/orders?msg=error");
        }
    }
}