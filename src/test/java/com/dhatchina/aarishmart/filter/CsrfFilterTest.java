package com.dhatchina.aarishmart.filter;

import com.dhatchina.aarishmart.util.CsrfUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CsrfFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private PrintWriter writer;

    private CsrfFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CsrfFilter();
    }

    private void path(String uri, String contextPath) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn(contextPath);
    }

    private void sessionWithToken(String token) {
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CsrfUtil.SESSION_TOKEN_KEY)).thenReturn(token);
    }

    @Test
    void safeGetRequestsPassThrough() throws Exception {
        path("/products", "");
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(true)).thenReturn(session);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void staticAssetsAreNotCheckedEvenForPost() throws Exception {
        path("/css/style.css", "");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void postWithValidHeaderTokenPasses() throws Exception {
        path("/cart", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getHeader(CsrfUtil.HEADER_NAME)).thenReturn("abc123");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void postWithValidFormFieldTokenPasses() throws Exception {
        path("/login", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("xyz789");
        when(request.getParameter(CsrfUtil.FORM_FIELD)).thenReturn("xyz789");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void postWithMissingTokenForwardsTo403Page() throws Exception {
        path("/checkout", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void cartPostWithMissingTokenIsRejected() throws Exception {
        path("/cart", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postWithMismatchedTokenIsRejected() throws Exception {
        path("/register", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getHeader(CsrfUtil.HEADER_NAME)).thenReturn("wrong");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void formPostToCartWithBadCsrfIsRejected() throws Exception {
        path("/cart", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getParameter(CsrfUtil.FORM_FIELD)).thenReturn("wrong-token");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void ajaxPostWithBadTokenGetsJson403() throws Exception {
        path("/cart", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(writer).write(org.mockito.ArgumentMatchers.contains("\"error\""));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void sellerOrdersGetPassesThrough() throws Exception {
        path("/seller/orders", "");
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(true)).thenReturn(session);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void sellerOrderStatusPostWithoutTokenIsRejected() throws Exception {
        path("/seller/orders/status", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void reviewPostWithoutTokenIsRejected() throws Exception {
        path("/review", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("abc123");
        when(request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void reviewPostWithValidFormTokenPasses() throws Exception {
        path("/review", "");
        when(request.getMethod()).thenReturn("POST");
        sessionWithToken("xyz789");
        when(request.getParameter(CsrfUtil.FORM_FIELD)).thenReturn("xyz789");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenIsExposedAsRequestAttributeForViews() throws Exception {
        path("/products", "");
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(true)).thenReturn(session);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(request).setAttribute(org.mockito.Mockito.eq(CsrfUtil.REQUEST_TOKEN_ATTR), token.capture());
        assertEquals(64, token.getValue().length());
    }
}