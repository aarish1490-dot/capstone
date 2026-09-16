package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.exception.AppException;
import com.dhatchina.dhatchinamart.exception.ForbiddenException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.ProductService;
import com.dhatchina.dhatchinamart.service.SellerService;
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
 * Seller product management: dashboard, create, edit and delete. Everything
 * under /seller* is already guarded by AuthFilter (role SELLER required).
 * Ownership for edit/delete is verified server-side against the session user
 * in ProductService - never from a browser-supplied seller id.
 */
@WebServlet(urlPatterns = {"/seller", "/seller/product", "/seller/product/create",
        "/seller/product/edit", "/seller/product/delete"})
public class SellerServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SellerServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        switch (path) {
            case "/seller/product/create":
                showCreateForm(request, response);
                break;
            case "/seller/product/edit":
                showEditForm(request, response);
                break;
            case "/seller":
                handleDashboard(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        switch (path) {
            case "/seller/product/create":
                handleCreateProduct(request, response);
                break;
            case "/seller/product/edit":
                handleUpdateProduct(request, response);
                break;
            case "/seller/product/delete":
                handleDeleteProduct(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        SellerService sellerService = ServiceRegistry.getSellerService();
        List<Product> products = sellerService.productsForSeller(user.getId());
        request.setAttribute("products", products);
        request.setAttribute("productCount", products.size());
        request.getRequestDispatcher("/WEB-INF/jsp/seller-dashboard.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("categories", ServiceRegistry.getProductService().categories());
        request.getRequestDispatcher("/WEB-INF/jsp/create-product.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request);
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = SessionUtil.getUser(request);
        try {
            Product product = ServiceRegistry.getProductService().getOwnedProduct(user.getId(), id);
            request.setAttribute("product", product);
            request.setAttribute("name", product.getName());
            request.setAttribute("description", product.getDescription());
            request.setAttribute("price", product.getPrice() == null ? null : product.getPrice().toPlainString());
            request.setAttribute("stock", product.getStockQty());
            request.setAttribute("category", product.getCategory());
            request.setAttribute("imageUrl", product.getImageUrl());
            request.setAttribute("categories", ServiceRegistry.getProductService().categories());
            request.getRequestDispatcher("/WEB-INF/jsp/edit-product.jsp").forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            log.warn("Seller {} attempted to edit another seller's product {}", user.getId(), id);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to load product {} for edit", id, e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void handleCreateProduct(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String price = request.getParameter("price");
        String stock = request.getParameter("stock");
        String category = request.getParameter("category");
        String imageUrl = request.getParameter("imageUrl");
        try {
            ServiceRegistry.getSellerService().createProduct(
                    user.getId(), name, description, price, stock, category, imageUrl);
            response.sendRedirect(request.getContextPath() + "/seller?msg=created");
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("name", name);
            request.setAttribute("description", description);
            request.setAttribute("price", price);
            request.setAttribute("stock", stock);
            request.setAttribute("category", category);
            request.setAttribute("imageUrl", imageUrl);
            request.setAttribute("categories", ServiceRegistry.getProductService().categories());
            request.getRequestDispatcher("/WEB-INF/jsp/create-product.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Product creation failed", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/create-product.jsp").forward(request, response);
        }
    }

    private void handleUpdateProduct(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request);
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = SessionUtil.getUser(request);
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String price = request.getParameter("price");
        String stock = request.getParameter("stock");
        String category = request.getParameter("category");
        String imageUrl = request.getParameter("imageUrl");
        try {
            ProductService productService = ServiceRegistry.getProductService();
            productService.updateProduct(user.getId(), id, name, description, price, stock, category, imageUrl);
            response.sendRedirect(request.getContextPath() + "/seller?msg=updated");
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            log.warn("Seller {} attempted to update another seller's product {}", user.getId(), id);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("productId", id);
            request.setAttribute("name", name);
            request.setAttribute("description", description);
            request.setAttribute("price", price);
            request.setAttribute("stock", stock);
            request.setAttribute("category", category);
            request.setAttribute("imageUrl", imageUrl);
            request.setAttribute("categories", ServiceRegistry.getProductService().categories());
            request.getRequestDispatcher("/WEB-INF/jsp/edit-product.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Product update failed for id {}", id, e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/edit-product.jsp").forward(request, response);
        }
    }

    private void handleDeleteProduct(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request);
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = SessionUtil.getUser(request);
        try {
            ServiceRegistry.getProductService().deleteProduct(user.getId(), id);
            response.sendRedirect(request.getContextPath() + "/seller?msg=deleted");
        } catch (ForbiddenException e) {
            log.warn("Seller {} attempted to delete another seller's product {}", user.getId(), id);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            log.error("Product deletion failed for id {}", id, e);
            response.sendRedirect(request.getContextPath() + "/seller?err=delete");
        }
    }

    private long parseId(HttpServletRequest request) {
        String idParameter = request.getParameter("id");
        if (idParameter == null || idParameter.isBlank()) {
            return -1;
        }
        try {
            return Long.parseLong(idParameter);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}