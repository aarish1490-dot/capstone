package com.dhatchina.dhatchinamart.filter;

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
 * Sets a baseline set of non-breaking security headers on every response
 * (worksheet STEP 13 - security headers).
 *
 * <p>Headers added to <em>every</em> response:</p>
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} - blocks MIME-sniffing so a
 *       browser never reinterprets an HTML response as a script/style.</li>
 *   <li>{@code X-Frame-Options: SAMEORIGIN} - clickjacking protection; the app
 *       never renders an {@code <iframe>} so this cannot break any page.</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin} - the browser
 *       sends full URLs only between same-origin pages relative to the session,
 *       keeping referrer leakage of session ids to a minimum.</li>
 *   <li>{@code Cache-Control: no-store} - released only on private/dynamic pages
 *       (cart, checkout, orders, order details, seller/admin consoles, reviews,
 *       API). Personal data must not survive in the browser or a shared proxy
 *       cache. Static assets (css/js/images) keep default caching so the site is
 *       not slowed down.</li>
 * </ul>
 *
 * <p><strong>Documented limitation (no blind strict CSP):</strong> the pages
 * render inline scripts ({@code window.CONTEXT_PATH} in header.jspf and inline
 * {@code onerror} handlers on product images) and inline styles, so a strict
 * {@code script-src 'self'} Content-Security-Policy would break the UI. A CSP is
 * therefore intentionally NOT enabled here; enabling a policy that breaks the
 * app would be worse than not sending one, and the worksheet explicitly allows
 * documenting a limitation rather than blindly enabling a header.</p>
 *
 * <p>This filter never blocks a request and never weakens {@link AuthFilter} or
 * {@link CsrfFilter}; it only adds response headers (defense-in-depth).</p>
 */
@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            if (isDynamicPath(request)) {
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isDynamicPath(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            String path = ((HttpServletRequest) request).getServletPath();
            if (path == null || path.isEmpty()) {
                path = ((HttpServletRequest) request).getRequestURI();
            }
            if (path != null) {
                return !(path.startsWith("/css") || path.startsWith("/js")
                        || path.startsWith("/images") || path.startsWith("/fonts")
                        || path.matches(".*\\.(css|js|png|jpe?g|gif|svg|webp|ico|woff2?|ttf|eot)(/.*)?"));
            }
        }
        return true;
    }

    @Override
    public void destroy() {
    }
}
