package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.ReviewStats;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.CartService;
import com.dhatchina.aarishmart.service.ProductService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServletTest {

    private static final long BUYER_ID = 7L;
    private static final String PRODUCT_JSP = "/WEB-INF/jsp/product-details.jsp";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private ReviewServlet servlet;
    private User buyer;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getReviewService).thenReturn(reviewService);
        serviceRegistryMock.when(ServiceRegistry::getProductService).thenReturn(productService);
        serviceRegistryMock.when(ServiceRegistry::getCartService).thenReturn(cartService);
        buyer = new User();
        buyer.setId(BUYER_ID);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestDispatcher(PRODUCT_JSP)).thenReturn(dispatcher);
        servlet = new ReviewServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private void stubProductDetails(long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Wireless Mouse");
        when(productService.getById(productId)).thenReturn(product);
        when(reviewService.reviewsForProduct(productId)).thenReturn(List.of());
        when(reviewService.statsForProduct(productId)).thenReturn(new ReviewStats(0, null));
        when(reviewService.findEligibleOrderId(BUYER_ID, productId)).thenReturn(Optional.empty());
        when(reviewService.findReviewByBuyerAndProduct(BUYER_ID, productId)).thenReturn(Optional.empty());
        when(cartService.countItems(BUYER_ID)).thenReturn(0);
    }

    @Test
    void successfulSubmissionRedirectsToProductPage() throws Exception {
        when(request.getParameter("productId")).thenReturn("5");
        when(request.getParameter("orderId")).thenReturn("9");
        when(request.getParameter("rating")).thenReturn("4");
        when(request.getParameter("reviewText")).thenReturn("Nice product!");

        servlet.doPost(request, response);

        verify(reviewService).createReview(BUYER_ID, 9L, 5L, 4, "Nice product!");
        verify(response).sendRedirect("/product?id=5&reviewed=1");
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/login");
        verify(reviewService, never()).createReview(anyLong(), anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void sellerIsRejectedWithForbidden() throws Exception {
        User seller = new User();
        seller.setRole(User.Role.SELLER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(seller);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(reviewService, never()).createReview(anyLong(), anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void invalidProductIdRedirectsToProducts() throws Exception {
        when(request.getParameter("productId")).thenReturn("abc");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/products");
        verify(reviewService, never()).createReview(anyLong(), anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void invalidOrderIdForwardsErrorToProductPage() throws Exception {
        when(request.getParameter("productId")).thenReturn("5");
        when(request.getParameter("orderId")).thenReturn("abc");
        stubProductDetails(5L);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Invalid order");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void invalidRatingForwardsErrorToProductPage() throws Exception {
        when(request.getParameter("productId")).thenReturn("5");
        when(request.getParameter("orderId")).thenReturn("9");
        when(request.getParameter("rating")).thenReturn("abc");
        stubProductDetails(5L);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Rating must be between 1 and 5 stars");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void ownershipViolationForwardsErrorWithoutSendingServerError() throws Exception {
        when(request.getParameter("productId")).thenReturn("5");
        when(request.getParameter("orderId")).thenReturn("9");
        when(request.getParameter("rating")).thenReturn("5");
        when(request.getParameter("reviewText")).thenReturn("Nice");
        stubProductDetails(5L);
        when(reviewService.createReview(BUYER_ID, 9L, 5L, 5, "Nice"))
                .thenThrow(new NotFoundException("Order not found"));

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Order not found");
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void unexpectedFailureForwardsGenericErrorInsteadOf500() throws Exception {
        when(request.getParameter("productId")).thenReturn("5");
        when(request.getParameter("orderId")).thenReturn("9");
        when(request.getParameter("rating")).thenReturn("5");
        when(request.getParameter("reviewText")).thenReturn("Nice");
        stubProductDetails(5L);
        when(reviewService.createReview(BUYER_ID, 9L, 5L, 5, "Nice"))
                .thenThrow(new RuntimeException("database down"));

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Something went wrong. Please try again.");
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void getRedirectsToProducts() throws Exception {
        servlet.doGet(request, response);

        verify(response).sendRedirect("/products");
    }
}