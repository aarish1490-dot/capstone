package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.exception.AppException;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.ReviewService;
import com.dhatchina.aarishmart.util.ServiceRegistry;
import com.dhatchina.aarishmart.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Buyer review submission. The buyer identity always comes from the session;
 * the order and product ids from the form are re-validated against the
 * database (ownership, delivery status, purchase, no existing review) in the
 * service layer. CSRF is enforced globally by {@code CsrfFilter}.
 */
@WebServlet("/review")
public class ReviewServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ReviewServlet.class);

    private static final String PRODUCT_JSP = "/WEB-INF/jsp/product-details.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/products");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.getUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (user.getRole() != User.Role.BUYER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long productId;
        try {
            productId = Long.parseLong(request.getParameter("productId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        try {
            long orderId = parsePositiveLong(request.getParameter("orderId"), "Invalid order");
            int rating = parseRating(request.getParameter("rating"));
            ServiceRegistry.getReviewService()
                    .createReview(user.getId(), orderId, productId, rating, request.getParameter("reviewText"));
            response.sendRedirect(request.getContextPath() + "/product?id=" + productId + "&reviewed=1");
        } catch (AppException e) {
            forwardDetails(request, response, user, productId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to submit review", e);
            forwardDetails(request, response, user, productId, "Something went wrong. Please try again.");
        }
    }

    private long parsePositiveLong(String raw, String message) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(message);
        }
    }

    private int parseRating(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException("Rating must be between 1 and 5 stars");
        }
    }

    private void forwardDetails(HttpServletRequest request, HttpServletResponse response, User user,
                                long productId, String errorMessage) throws ServletException, IOException {
        try {
            Product product = ServiceRegistry.getProductService().getById(productId);
            ReviewService reviewService = ServiceRegistry.getReviewService();
            request.setAttribute("product", product);
            request.setAttribute("cartCount", cartCount(user));
            request.setAttribute("reviews", reviewService.reviewsForProduct(productId));
            request.setAttribute("reviewStats", reviewService.statsForProduct(productId));
            request.setAttribute("eligibleOrderId",
                    reviewService.findEligibleOrderId(user.getId(), productId).orElse(null));
            request.setAttribute("myReview",
                    reviewService.findReviewByBuyerAndProduct(user.getId(), productId).orElse(null));
            request.setAttribute("error", errorMessage);
            request.getRequestDispatcher(PRODUCT_JSP).forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private Integer cartCount(User user) {
        if (user == null) {
            return null;
        }
        return ServiceRegistry.getCartService().countItems(user.getId());
    }
}