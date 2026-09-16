package com.dhatchina.dhatchinamart.filter;

import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.SessionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private HttpSession session;

    private AuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter();
    }

    private void sessionUser(User user) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionUtil.SESSION_USER)).thenReturn(user);
    }

    private void path(String uri, String contextPath) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn(contextPath);
    }

    private User user(User.Role role) {
        User user = new User();
        user.setId(7L);
        user.setName("Test User");
        user.setRole(role);
        return user;
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/cart");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void anonymousUserRedirectKeepsContextPath() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/dhatchinamart");
        when(request.getRequestURI()).thenReturn("/dhatchinamart/cart");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/dhatchinamart/login");
    }

    @Test
    void buyerCannotAccessSellerArea() throws Exception {
        sessionUser(user(User.Role.BUYER));
        path("/seller", "");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void sellerIsAllowedInSellerArea() throws Exception {
        sessionUser(user(User.Role.SELLER));
        path("/seller/product/edit?id=2", "");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void sellerCannotAccessAdminArea() throws Exception {
        sessionUser(user(User.Role.SELLER));
        path("/admin", "");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void adminIsAllowedInAdminArea() throws Exception {
        sessionUser(user(User.Role.ADMIN));
        path("/admin", "");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void anyAuthenticatedUserCanReachCart() throws Exception {
        sessionUser(user(User.Role.BUYER));
        path("/cart", "");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}