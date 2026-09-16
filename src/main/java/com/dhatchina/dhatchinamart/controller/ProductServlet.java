package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.ProductPage;
import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.model.Product;
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
import java.util.List;

@WebServlet(urlPatterns = {"/products", "/product"})
public class ProductServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ProductServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/product".equals(request.getServletPath())) {
            handleDetails(request, response);
        } else {
            handleBrowse(request, response);
        }
    }

    private void handleBrowse(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("q");
        String category = request.getParameter("category");
        int page = parsePage(request.getParameter("page"));
        try {
            ProductService productService = ServiceRegistry.getProductService();
            ProductPage pageData = productService.browse(keyword, category, page, ProductService.DEFAULT_PAGE_SIZE);
            request.setAttribute("products", pageData.getProducts());
            request.setAttribute("page", pageData);
            request.setAttribute("categories", productService.categories());
            request.setAttribute("keyword", keyword);
            request.setAttribute("selectedCategory", category);
            request.setAttribute("cartCount", cartCount(request));
            request.getRequestDispatcher("/WEB-INF/jsp/products.jsp").forward(request, response);
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Failed to browse products", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void handleDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id;
        try {
            id = Long.parseLong(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            Product product = ServiceRegistry.getProductService().getById(id);
            request.setAttribute("product", product);
            request.setAttribute("cartCount", cartCount(request));
            request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp").forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to load product {}", id, e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
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
