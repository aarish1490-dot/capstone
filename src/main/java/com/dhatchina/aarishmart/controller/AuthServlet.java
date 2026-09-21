package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.dto.RegisterRequest;
import com.dhatchina.aarishmart.exception.AppException;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.AuthService;
import com.dhatchina.aarishmart.service.OtpService;
import com.dhatchina.aarishmart.util.OtpUtil;
import com.dhatchina.aarishmart.util.SessionUtil;
import com.dhatchina.aarishmart.util.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Handles the mobile-number based OTP login (primary) plus the
 * existing email + password login (kept for compatibility / recovery),
 * registration and logout.
 */
@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/logout".equals(path)) {
            // Logout is POST-only (see doPost) to prevent cross-site logout.
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        if (SessionUtil.isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        if ("/login".equals(path)) {
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/logout".equals(path)) {
            handleLogout(request, response);
        } else if ("/login".equals(path)) {
            handleLogin(request, response);
        } else {
            handleRegister(request, response);
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String mobileNumber = request.getParameter("mobileNumber");
        if (mobileNumber != null && !mobileNumber.isBlank()) {
            startOtpLogin(request, response);
            return;
        }
        handleEmailPasswordLogin(request, response);
    }

    private void handleEmailPasswordLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (email == null || email.isBlank()) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }
        var rateLimitService = ServiceRegistry.getRateLimitService();
        if (rateLimitService != null && rateLimitService.isLoginLocked(email)) {
            request.setAttribute("error", "Too many failed attempts. Please try again later.");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }
        try {
            User user = ServiceRegistry.getAuthService().login(email, password);
            if (rateLimitService != null) {
                rateLimitService.resetLogin(email);
            }
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(SessionUtil.SESSION_USER, user);
            log.info("Session established for user {}", user.getEmail());
            response.sendRedirect(request.getContextPath() + "/");
        } catch (AppException e) {
            if (rateLimitService != null) {
                rateLimitService.recordLoginFailure(email);
            }
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        } catch (Exception e) {
            if (rateLimitService != null) {
                rateLimitService.recordLoginFailure(email);
            }
            log.error("Login failed unexpectedly", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        }
    }

    /**
     * OTP login step 1: verify the mobile number is registered, generate
     * and send an OTP, then show the OTP verification step.
     */
    private void startOtpLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String mobileNumber = request.getParameter("mobileNumber");
            var rateLimitService = ServiceRegistry.getRateLimitService();
            if (rateLimitService != null && !rateLimitService.allowOtpSend(mobileNumber)) {
                request.setAttribute("error",
                        "Too many OTP requests. Please try again later.");
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            AuthService authService = ServiceRegistry.getAuthService();
            User user = authService.findUserByMobileNumber(mobileNumber);
            OtpService otpService = ServiceRegistry.getOtpService();
            otpService.sendOtp(user);
            showOtpStep(request, user.getMobileNumber(), null);
            exposeDevOtp(request, otpService);
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("OTP request failed unexpectedly", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    static void showOtpStep(HttpServletRequest request, String mobileNumber, String info) {
        request.setAttribute("otpStep", "verify");
        request.setAttribute("mobileNumber", mobileNumber);
        request.setAttribute("maskedMobile", OtpUtil.maskMobile(mobileNumber));
        if (info != null) {
            request.setAttribute("info", info);
        }
    }

    /**
     * In development mode only (mock SMS provider) the OTP is shown on the
     * login page so the flow can be tested locally. In production mode the
     * attribute is never set - the OTP is never displayed in the UI.
     */
    static void exposeDevOtp(HttpServletRequest request, OtpService otpService) {
        if (otpService != null && otpService.isDevelopmentMode()
                && otpService.getLastDevOtp() != null) {
            request.setAttribute("devMode", true);
            request.setAttribute("devOtp", otpService.getLastDevOtp());
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(request.getParameter("name"));
        registerRequest.setEmail(request.getParameter("email"));
        registerRequest.setMobileNumber(request.getParameter("mobileNumber"));
        registerRequest.setPassword(request.getParameter("password"));
        registerRequest.setConfirmPassword(request.getParameter("confirmPassword"));
        registerRequest.setRole(request.getParameter("role"));
        try {
            AuthService authService = ServiceRegistry.getAuthService();
            authService.register(registerRequest);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("name", registerRequest.getName());
            request.setAttribute("email", registerRequest.getEmail());
            request.setAttribute("mobileNumber", registerRequest.getMobileNumber());
            request.setAttribute("role", registerRequest.getRole());
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("Registration failed unexpectedly", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
        }
    }
}
