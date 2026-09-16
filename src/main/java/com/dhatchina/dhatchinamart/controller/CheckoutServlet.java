package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.CartService;
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

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (requireBuyer(request, response)) {
            return;
        }
        User user = SessionUtil.getUser(request);
        CartView cart = ServiceRegistry.getCartService().getCart(user.getId());
        if (cart.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart?msg=empty");
            return;
        }
        request.setAttribute("cart", cart);
        request.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (requireBuyer(request, response)) {
            return;
        }
        User user = SessionUtil.getUser(request);
        OrderService orderService = ServiceRegistry.getOrderService();
        try {
            long orderId = orderService.placeOrder(user.getId()).getId();
            response.sendRedirect(request.getContextPath() + "/order-success?id=" + orderId);
        } catch (AppException e) {
            CartView cart = ServiceRegistry.getCartService().getCart(user.getId());
            if (cart.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/cart?msg=empty");
                return;
            }
            request.setAttribute("cart", cart);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Checkout failed", e);
            response.sendRedirect(request.getContextPath() + "/cart?msg=error");
        }
    }

    /**
     * Returns true when the request was already answered (403 or redirect).
     */
    private boolean requireBuyer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = SessionUtil.getUser(request);
        if (user == null || user.getRole() != User.Role.BUYER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return true;
        }
        return false;
    }
}