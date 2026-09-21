package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.SellerStats;
import com.dhatchina.aarishmart.exception.ForbiddenException;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.OrderService;
import com.dhatchina.aarishmart.service.ProductService;
import com.dhatchina.aarishmart.service.SellerService;
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
import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private SellerService sellerService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private SellerServlet servlet;

    private final User sellerUser = createUser(10L, "Seller One", User.Role.SELLER);

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getSellerService).thenReturn(sellerService);
        serviceRegistryMock.when(ServiceRegistry::getProductService).thenReturn(productService);
        serviceRegistryMock.when(ServiceRegistry::getOrderService).thenReturn(orderService);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(sellerUser);
        servlet = new SellerServlet();
    }

    @AfterEach
    void tearDown() {
        serviceRegistryMock.close();
        sessionUtilMock.close();
    }

    private User createUser(long id, String name, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    private Product sampleProduct(long id) {
        Product product = new Product();
        product.setId(id);
        product.setSellerId(10L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("299.99"));
        product.setStockQty(25);
        product.setCategory("Home");
        product.setImageUrl("https://example.com/img.jpg");
        return product;
    }

    private void stubRoute(String servletPath) {
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void doGetDashboardForwardsToSellerDashboard() throws Exception {
        stubRoute("/seller");
        when(sellerService.productsForSeller(10L)).thenReturn(Collections.emptyList());
        when(orderService.salesStats(10L)).thenReturn(new SellerStats());
        when(request.getRequestDispatcher("/WEB-INF/jsp/seller-dashboard.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("products"), any());
        verify(request).setAttribute(eq("productCount"), eq(0));
        verify(request).setAttribute(eq("sales"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetCreateFormForwardsToCreateJsp() throws Exception {
        stubRoute("/seller/product/create");
        when(productService.categories()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("/WEB-INF/jsp/create-product.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("categories", Collections.emptyList());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetEditOwnedProductForwardsToEditJsp() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("1");
        when(productService.getOwnedProduct(10L, 1L)).thenReturn(sampleProduct(1L));
        when(productService.categories()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("/WEB-INF/jsp/edit-product.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetEditMissingProductSends404() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("999");
        when(productService.getOwnedProduct(10L, 999L)).thenThrow(new NotFoundException("not found"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doGetEditOtherSellersProductSends403() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("2");
        when(productService.getOwnedProduct(10L, 2L)).thenThrow(new ForbiddenException("no"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doGetEditInvalidIdSends404() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("abc");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPostCreateSuccessRedirects() throws Exception {
        stubRoute("/seller/product/create");
        when(request.getParameter("name")).thenReturn("New Product");
        when(request.getParameter("description")).thenReturn("desc");
        when(request.getParameter("price")).thenReturn("499.00");
        when(request.getParameter("stock")).thenReturn("10");
        when(request.getParameter("category")).thenReturn("Home");
        when(request.getParameter("imageUrl")).thenReturn("https://example.com/a.jpg");

        servlet.doPost(request, response);

        verify(sellerService).createProduct(eq(10L), any(), any(), any(), any(), any(), any());
        verify(response).sendRedirect("/seller?msg=created");
    }

    @Test
    void doPostCreateValidationFailureForwardsWithError() throws Exception {
        stubRoute("/seller/product/create");
        when(request.getParameter("name")).thenReturn("");
        when(request.getParameter("price")).thenReturn("0");
        when(productService.categories()).thenReturn(Collections.emptyList());
        when(sellerService.createProduct(eq(10L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ValidationException("Product name is required"));
        when(request.getRequestDispatcher("/WEB-INF/jsp/create-product.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Product name is required");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostEditSuccessRedirects() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("1");
        when(request.getParameter("name")).thenReturn("Updated Name");
        when(request.getParameter("description")).thenReturn("desc");
        when(request.getParameter("price")).thenReturn("499.00");
        when(request.getParameter("stock")).thenReturn("10");
        when(request.getParameter("category")).thenReturn("Home");
        when(request.getParameter("imageUrl")).thenReturn("https://example.com/b.jpg");

        servlet.doPost(request, response);

        verify(productService).updateProduct(eq(10L), eq(1L), any(), any(), any(), any(), any(), any());
        verify(response).sendRedirect("/seller?msg=updated");
    }

    @Test
    void doPostEditValidationFailureForwardsWithError() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("1");
        when(request.getParameter("name")).thenReturn("Good Name");
        when(request.getParameter("price")).thenReturn("-5.00");
        when(productService.categories()).thenReturn(Collections.emptyList());
        when(productService.updateProduct(eq(10L), eq(1L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ValidationException("Price must be greater than zero"));
        when(request.getRequestDispatcher("/WEB-INF/jsp/edit-product.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Price must be greater than zero");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostEditMissingProductSends404() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("999");
        when(request.getParameter("name")).thenReturn("A");
        when(request.getParameter("price")).thenReturn("10.00");
        when(productService.updateProduct(eq(10L), eq(999L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new NotFoundException("not found"));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPostEditForbiddenSends403() throws Exception {
        stubRoute("/seller/product/edit");
        when(request.getParameter("id")).thenReturn("2");
        when(request.getParameter("name")).thenReturn("A");
        when(request.getParameter("price")).thenReturn("10.00");
        when(productService.updateProduct(eq(10L), eq(2L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("no"));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doPostDeleteSuccessRedirects() throws Exception {
        stubRoute("/seller/product/delete");
        when(request.getParameter("id")).thenReturn("1");

        servlet.doPost(request, response);

        verify(productService).deleteProduct(10L, 1L);
        verify(response).sendRedirect("/seller?msg=deleted");
    }

    @Test
    void doPostDeleteForbiddenSends403() throws Exception {
        stubRoute("/seller/product/delete");
        when(request.getParameter("id")).thenReturn("2");
        org.mockito.Mockito.doThrow(new ForbiddenException("no"))
                .when(productService).deleteProduct(10L, 2L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doPostDeleteMissingProductSends404() throws Exception {
        stubRoute("/seller/product/delete");
        when(request.getParameter("id")).thenReturn("999");
        org.mockito.Mockito.doThrow(new NotFoundException("not found"))
                .when(productService).deleteProduct(10L, 999L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPostUnrecognizedPathSends404() throws Exception {
        stubRoute("/seller/unknown");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}