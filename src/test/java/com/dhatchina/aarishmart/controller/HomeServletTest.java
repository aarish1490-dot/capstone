package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.ProductPage;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.CartService;
import com.dhatchina.aarishmart.service.ProductService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeServletTest {

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

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private HomeServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getProductService).thenReturn(productService);
        serviceRegistryMock.when(ServiceRegistry::getCartService).thenReturn(cartService);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(null);
        servlet = new HomeServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    @Test
    void contextRootForwardsToIndexWithFeaturedAndCategories() throws Exception {
        ProductPage featured = new ProductPage(List.of(new Product()), 40L, 1, 8, 5);
        when(productService.browse(null, null, 1, 8)).thenReturn(featured);
        when(productService.categories()).thenReturn(List.of("Books", "Home"));
        when(request.getRequestDispatcher("/index.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("featuredProducts", featured.getProducts());
        verify(request).setAttribute("categories", List.of("Books", "Home"));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void contextRootSetsCartCountForLoggedInUser() throws Exception {
        User user = new User();
        user.setId(7L);
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(user);
        when(productService.browse(null, null, 1, 8)).thenReturn(new ProductPage(List.of(), 0L, 1, 8, 1));
        when(productService.categories()).thenReturn(List.of());
        when(cartService.countItems(7L)).thenReturn(2);
        when(request.getRequestDispatcher("/index.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("cartCount", 2);
        verify(dispatcher).forward(request, response);
    }
}