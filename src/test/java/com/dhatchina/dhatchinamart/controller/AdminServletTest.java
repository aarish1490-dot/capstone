package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.dto.AdminStats;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.AdminService;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import com.dhatchina.dhatchinamart.util.SessionUtil;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private AdminService adminService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private AdminServlet servlet;

    private final User adminUser = createUser(7L, "Platform Admin", User.Role.ADMIN);

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getAdminService).thenReturn(adminService);
        when(request.getContextPath()).thenReturn("");
        servlet = new AdminServlet();
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

    @Test
    void doGetLoadsListsAndForwardsToAdminDashboard() throws Exception {
        when(adminService.getDashboardStats()).thenReturn(new AdminStats());
        when(adminService.users()).thenReturn(Collections.emptyList());
        when(adminService.orders()).thenReturn(Collections.emptyList());
        when(adminService.products()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("/WEB-INF/jsp/admin-dashboard.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("stats"), any(AdminStats.class));
        verify(request).setAttribute(eq("users"), any());
        verify(request).setAttribute(eq("orders"), any());
        verify(request).setAttribute(eq("products"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostDeactivateUserRedirects() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("5");
        when(request.getParameter("active")).thenReturn("false");

        servlet.doPost(request, response);

        verify(adminService).setUserActive(5L, false);
        verify(response).sendRedirect("/admin?msg=user");
    }

    @Test
    void doPostActivateUserRedirects() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("5");
        when(request.getParameter("active")).thenReturn("true");

        servlet.doPost(request, response);

        verify(adminService).setUserActive(5L, true);
        verify(response).sendRedirect("/admin?msg=user");
    }

    @Test
    void doPostSelfDeactivationSends403() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("7");
        when(request.getParameter("active")).thenReturn("false");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doPostMissingUserSends404() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("999");
        when(request.getParameter("active")).thenReturn("false");
        org.mockito.Mockito.doThrow(new NotFoundException("User not found"))
                .when(adminService).setUserActive(999L, false);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPostDeactivatingAdminAccountSends400() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("1");
        when(request.getParameter("active")).thenReturn("false");
        org.mockito.Mockito.doThrow(new ValidationException("Admin accounts cannot be deactivated"))
                .when(adminService).setUserActive(1L, false);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doPostInvalidBooleanSends400() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.getUser(request)).thenReturn(adminUser);
        when(request.getParameter("action")).thenReturn("user-active");
        when(request.getParameter("userId")).thenReturn("5");
        when(request.getParameter("active")).thenReturn("maybe");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doPostUnlistProductRedirects() throws Exception {
        when(request.getParameter("action")).thenReturn("product-active");
        when(request.getParameter("productId")).thenReturn("3");
        when(request.getParameter("active")).thenReturn("false");

        servlet.doPost(request, response);

        verify(adminService).setProductActive(3L, false);
        verify(response).sendRedirect("/admin?msg=product");
    }

    @Test
    void doPostRelistProductRedirects() throws Exception {
        when(request.getParameter("action")).thenReturn("product-active");
        when(request.getParameter("productId")).thenReturn("3");
        when(request.getParameter("active")).thenReturn("true");

        servlet.doPost(request, response);

        verify(adminService).setProductActive(3L, true);
        verify(response).sendRedirect("/admin?msg=product");
    }

    @Test
    void doPostMissingProductSends404() throws Exception {
        when(request.getParameter("action")).thenReturn("product-active");
        when(request.getParameter("productId")).thenReturn("999");
        when(request.getParameter("active")).thenReturn("false");
        org.mockito.Mockito.doThrow(new NotFoundException("Product not found"))
                .when(adminService).setProductActive(999L, false);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPostUnknownActionSends404() throws Exception {
        when(request.getParameter("action")).thenReturn("explode");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}