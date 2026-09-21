package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.ProductPage;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private ReviewService reviewService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private ProductServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getProductService).thenReturn(productService);
        serviceRegistryMock.when(ServiceRegistry::getCartService).thenReturn(cartService);
        serviceRegistryMock.when(ServiceRegistry::getReviewService).thenReturn(reviewService);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(null);
        servlet = new ProductServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private void stubBrowse(String page) {
        when(request.getServletPath()).thenReturn("/products");
        when(request.getParameter("q")).thenReturn(null);
        when(request.getParameter("category")).thenReturn(null);
        when(request.getParameter("page")).thenReturn(page);
        when(request.getRequestDispatcher("/WEB-INF/jsp/products.jsp")).thenReturn(dispatcher);
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        return product;
    }

    @Test
    void browseForwardsWithPageMetadata() throws Exception {
        ProductPage pageData = new ProductPage(List.of(sampleProduct()), 40L, 1, 12, 4);
        stubBrowse(null);
        when(productService.browse(null, null, 1, 12)).thenReturn(pageData);
        when(productService.categories()).thenReturn(List.of("Books", "Home"));

        servlet.doGet(request, response);

        verify(request).setAttribute("products", pageData.getProducts());
        verify(request).setAttribute("page", pageData);
        verify(request).setAttribute("categories", List.of("Books", "Home"));
        verify(request).setAttribute(eq("keyword"), isNull());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void browseInvalidPageParameterDefaultsToFirstPage() throws Exception {
        ProductPage pageData = new ProductPage(List.of(sampleProduct()), 1L, 1, 12, 1);
        stubBrowse("abc");
        when(productService.browse(null, null, 1, 12)).thenReturn(pageData);
        when(productService.categories()).thenReturn(List.of());

        servlet.doGet(request, response);

        verify(productService).browse(null, null, 1, 12);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void browseNegativePageParameterDefaultsToFirstPage() throws Exception {
        ProductPage pageData = new ProductPage(List.of(sampleProduct()), 1L, 1, 12, 1);
        stubBrowse("-5");
        when(productService.browse(null, null, 1, 12)).thenReturn(pageData);
        when(productService.categories()).thenReturn(List.of());

        servlet.doGet(request, response);

        verify(productService).browse(null, null, 1, 12);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void browseSetsCartCountForLoggedInUser() throws Exception {
        User user = new User();
        user.setId(7L);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(user);
        ProductPage pageData = new ProductPage(List.of(sampleProduct()), 1L, 1, 12, 1);
        stubBrowse(null);
        when(productService.browse(null, null, 1, 12)).thenReturn(pageData);
        when(productService.categories()).thenReturn(List.of());
        when(cartService.countItems(7L)).thenReturn(3);

        servlet.doGet(request, response);

        verify(request).setAttribute("cartCount", 3);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsValidProductForwards() throws Exception {
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        Product product = sampleProduct();
        when(productService.getById(5L)).thenReturn(product);
        when(request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("product", product);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsMissingProductReturns404() throws Exception {
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        when(productService.getById(5L)).thenThrow(new NotFoundException("Product not found"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void detailsInvalidIdReturns404() throws Exception {
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("abc");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void detailsSetsReviewDataForAnonymousGuests() throws Exception {
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        Product product = sampleProduct();
        when(productService.getById(5L)).thenReturn(product);
        when(reviewService.reviewsForProduct(5L)).thenReturn(List.of());
        when(reviewService.statsForProduct(5L)).thenReturn(new ReviewStats(0, null));
        when(request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("product", product);
        verify(request).setAttribute(eq("reviews"), any());
        verify(request).setAttribute(eq("reviewStats"), any(ReviewStats.class));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsForBuyerWithEligibleOrderSetsFormOrderId() throws Exception {
        User buyer = new User();
        buyer.setId(7L);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        when(request.getParameter("order")).thenReturn(null);
        when(productService.getById(5L)).thenReturn(sampleProduct());
        when(reviewService.reviewsForProduct(5L)).thenReturn(List.of());
        when(reviewService.statsForProduct(5L)).thenReturn(new ReviewStats(0, null));
        when(reviewService.findEligibleOrderId(7L, 5L)).thenReturn(Optional.of(9L));
        when(reviewService.findReviewByBuyerAndProduct(7L, 5L)).thenReturn(Optional.empty());
        when(request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp")).thenReturn(dispatcher);
        when(cartService.countItems(7L)).thenReturn(0);

        servlet.doGet(request, response);

        verify(request).setAttribute("eligibleOrderId", 9L);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsHonoursOrderParamProvidedContext() throws Exception {
        User buyer = new User();
        buyer.setId(7L);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        when(request.getParameter("order")).thenReturn("12");
        when(productService.getById(5L)).thenReturn(sampleProduct());
        when(reviewService.reviewsForProduct(5L)).thenReturn(List.of());
        when(reviewService.statsForProduct(5L)).thenReturn(new ReviewStats(0, null));
        when(reviewService.isEligibleOrder(7L, 12L, 5L)).thenReturn(true);
        when(reviewService.findEligibleOrderId(7L, 5L)).thenReturn(Optional.of(9L));
        when(reviewService.findReviewByBuyerAndProduct(7L, 5L)).thenReturn(Optional.empty());
        when(request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp")).thenReturn(dispatcher);
        when(cartService.countItems(7L)).thenReturn(0);

        servlet.doGet(request, response);

        verify(request).setAttribute("eligibleOrderId", 12L);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailsForBuyerAlreadyReviewedHidesForm() throws Exception {
        User buyer = new User();
        buyer.setId(7L);
        buyer.setRole(User.Role.BUYER);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(buyer);
        when(request.getServletPath()).thenReturn("/product");
        when(request.getParameter("id")).thenReturn("5");
        when(productService.getById(5L)).thenReturn(sampleProduct());
        when(reviewService.reviewsForProduct(5L)).thenReturn(List.of());
        when(reviewService.statsForProduct(5L)).thenReturn(new ReviewStats(1, java.math.BigDecimal.valueOf(4L)));
        when(reviewService.findEligibleOrderId(7L, 5L)).thenReturn(Optional.empty());
        when(request.getRequestDispatcher("/WEB-INF/jsp/product-details.jsp")).thenReturn(dispatcher);
        when(cartService.countItems(7L)).thenReturn(0);

        servlet.doGet(request, response);

        verify(request).setAttribute("eligibleOrderId", null);
        verify(dispatcher).forward(request, response);
    }
}