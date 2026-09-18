package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.AdminStats;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.AdminService;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.dhatchina.dhatchinamart.util.SessionUtil;
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
 * Admin dashboard: platform statistics, user accounts (activate/deactivate),
 * order list and product moderation (list/unlist). Everything under /admin is
 * already guarded by AuthFilter (role ADMIN required), and the CSRF filter
 * enforces the synchronizer token on every POST.
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User current = SessionUtil.getUser(request);
        if (current == null || current.getRole() != User.Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            AdminService adminService = ServiceRegistry.getAdminService();
            AdminStats stats = adminService.getDashboardStats();
            List<User> users = adminService.users();
            List<Order> orders = adminService.orders();
            List<Product> products = adminService.products();
            request.setAttribute("stats", stats);
            request.setAttribute("users", users);
            request.setAttribute("orders", orders);
            request.setAttribute("products", products);
            request.getRequestDispatcher("/WEB-INF/jsp/admin-dashboard.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Failed to load admin dashboard", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        try {
            if ("user-active".equals(action)) {
                handleUserActive(request, response);
            } else if ("product-active".equals(action)) {
                handleProductActive(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NotFoundException e) {
            log.warn("Admin moderation target not found: action={}", action);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ValidationException e) {
            log.warn("Admin moderation rejected: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleUserActive(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        long userId = parseId(request.getParameter("userId"));
        Boolean active = parseBoolean(request.getParameter("active"));
        if (userId <= 0 || active == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        User current = SessionUtil.getUser(request);
        if (current != null && current.getId() == userId) {
            log.warn("Admin {} attempted to deactivate their own account", current.getId());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        AdminService adminService = ServiceRegistry.getAdminService();
        adminService.setUserActive(userId, active);
        log.info("Admin {} set user {} active={}", current == null ? "?" : current.getId(), userId, active);
        response.sendRedirect(request.getContextPath() + "/admin?msg=user");
    }

    private void handleProductActive(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        long productId = parseId(request.getParameter("productId"));
        Boolean active = parseBoolean(request.getParameter("active"));
        if (productId <= 0 || active == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        AdminService adminService = ServiceRegistry.getAdminService();
        adminService.setProductActive(productId, active);
        log.info("Admin set product {} active={}", productId, active);
        response.sendRedirect(request.getContextPath() + "/admin?msg=product");
    }

    private long parseId(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Boolean parseBoolean(String value) {
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        return null;
    }
}