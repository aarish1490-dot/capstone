package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.CartView;
import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.util.MoneyFormatter;
import com.dhatchina.dhatchinamart.util.SessionUtil;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.dhatchina.dhatchinamart.util.ValidationUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CartServlet.class);

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        CartView cart = ServiceRegistry.getCartService().getCart(user.getId());
        request.setAttribute("cart", cart);
        request.setAttribute("cartCount", cart.getCount());
        request.getRequestDispatcher("/WEB-INF/jsp/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        String action = request.getParameter("action");
        boolean json = "json".equals(request.getParameter("format"));

        try {
            long productId = parseProductId(request);
            if ("add".equals(action)) {
                int quantity = ValidationUtil.parseQuantity(request.getParameter("quantity"), "Quantity");
                ServiceRegistry.getCartService().addToCart(user.getId(), productId, quantity);
                response.sendRedirect(request.getContextPath() + "/cart");
            } else if ("update".equals(action)) {
                int quantity = ValidationUtil.parseQuantity(request.getParameter("quantity"), "Quantity");
                ServiceRegistry.getCartService().updateQuantity(user.getId(), productId, quantity);
                if (json) {
                    writeCartJson(request, response, user);
                } else {
                    response.sendRedirect(request.getContextPath() + "/cart");
                }
            } else if ("remove".equals(action)) {
                ServiceRegistry.getCartService().removeItem(user.getId(), productId);
                if (json) {
                    writeCartJson(request, response, user);
                } else {
                    response.sendRedirect(request.getContextPath() + "/cart");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (AppException e) {
            if (json) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("success", false);
                payload.put("error", e.getMessage());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(payload));
            } else {
                response.sendRedirect(request.getContextPath() + "/cart?msg=" + urlEncode(e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Cart operation failed", e);
            if (json) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("success", false);
                payload.put("error", "Something went wrong. Please try again.");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(gson.toJson(payload));
            } else {
                response.sendRedirect(request.getContextPath() + "/cart?msg=" + urlEncode("Something went wrong. Please try again."));
            }
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void writeCartJson(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        CartService cartService = ServiceRegistry.getCartService();
        CartView cart = cartService.getCart(user.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("count", cart.getCount());
        payload.put("total", MoneyFormatter.format(cart.getTotal()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(payload));
    }

    private long parseProductId(HttpServletRequest request) {
        try {
            return Long.parseLong(request.getParameter("productId"));
        } catch (NumberFormatException e) {
            throw new com.dhatchina.dhatchinamart.exception.ValidationException("Invalid product");
        }
    }
}
