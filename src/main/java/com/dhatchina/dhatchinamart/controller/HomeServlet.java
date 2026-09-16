package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.ProductPage;
import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.service.ProductService;
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

/**
 * Serves the marketplace landing page at the context root. Because this
 * servlet is mapped to the empty string pattern (""), it answers requests
 * for "/" and the welcome-file list is not consulted for the root request.
 */
@WebServlet(urlPatterns = {""})
public class HomeServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HomeServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ProductService productService = ServiceRegistry.getProductService();
            ProductPage featured = productService.browse(null, null, 1, 8);
            request.setAttribute("featuredProducts", featured.getProducts());
            request.setAttribute("categories", productService.categories());
            request.setAttribute("cartCount", cartCount(request));
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Failed to load home page", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private Integer cartCount(HttpServletRequest request) {
        User user = SessionUtil.getUser(request);
        if (user == null) {
            return null;
        }
        CartService cartService = ServiceRegistry.getCartService();
        return cartService.countItems(user.getId());
    }
}