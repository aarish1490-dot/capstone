package com.dhatchina.dhatchinamart.filter;

import com.dhatchina.dhatchinamart.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Enforces the synchronizer-token CSRF check on every state-changing request
 * (POST / PUT / PATCH / DELETE) for the whole application.
 *
 * <p>Static assets are skipped. The token is always exposed as the
 * {@code _csrfToken} request attribute so JSPs can render it as a hidden field
 * or meta tag (header fragment). AJAX callers that reply with 403 receive a
 * JSON error body; regular form posts are forwarded to the error-403 page.
 */
@WebFilter(urlPatterns = {"/*"})
public class CsrfFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

    private static final String PATH_REFIX_CSS = "/css/";
    private static final String PATH_PREFIX_JS = "/js/";
    private static final String PATH_PREFIX_IMAGES = "/images/";
    private static final String PATH_FAVICON = "/favicon.ico";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Always expose the token to views so every form / meta tag can use it.
        httpRequest.setAttribute(CsrfUtil.REQUEST_TOKEN_ATTR, CsrfUtil.getToken(httpRequest));

        if (!isSafeMethod(httpRequest.getMethod())) {
            if (!CsrfUtil.isValidFor(httpRequest)) {
                log.warn("Rejected {} {} with missing or invalid CSRF token, remote={}",
                        httpRequest.getMethod(), path, httpRequest.getRemoteAddr());
                reject(httpRequest, httpResponse);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isStaticResource(String path) {
        return path.startsWith(PATH_REFIX_CSS)
                || path.startsWith(PATH_PREFIX_JS)
                || path.startsWith(PATH_PREFIX_IMAGES)
                || path.equals(PATH_FAVICON);
    }

    private static boolean isSafeMethod(String method) {
        return "GET".equals(method)
                || "HEAD".equals(method)
                || "OPTIONS".equals(method)
                || "TRACE".equals(method);
    }

    private static boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private static void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Your session is no longer valid. Please refresh the page and try again.\"}");
            return;
        }
        request.setAttribute("error",
                "Your session is no longer valid. Please refresh the page and try again.");
        request.getRequestDispatcher("/WEB-INF/jsp/error-403.jsp").forward(request, response);
    }

    @Override
    public void destroy() {
    }
}