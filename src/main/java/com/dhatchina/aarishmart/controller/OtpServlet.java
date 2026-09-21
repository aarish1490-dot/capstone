package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.exception.AppException;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.AuthService;
import com.dhatchina.aarishmart.service.OtpService;
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
 * OTP login step 2 endpoints:
 * /otp/send    - (re)issue an OTP for a registered mobile number
 * /otp/verify  - check the submitted OTP and create the session
 * /otp/resend  - resend after the cooldown (60s)
 */
@WebServlet(urlPatterns = {"/otp/send", "/otp/verify", "/otp/resend"})
public class OtpServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OtpServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        switch (path) {
            case "/otp/send":
                handleSend(request, response, null);
                break;
            case "/otp/resend":
                handleSend(request, response, "A new OTP has been sent to your mobile number.");
                break;
            case "/otp/verify":
                handleVerify(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleSend(HttpServletRequest request, HttpServletResponse response, String info)
            throws ServletException, IOException {
        String mobileNumber = request.getParameter("mobileNumber");
        try {
            var rateLimitService = ServiceRegistry.getRateLimitService();
            if (rateLimitService != null && !rateLimitService.allowOtpSend(mobileNumber)) {
                request.setAttribute("error", "Too many OTP requests. Please try again later.");
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            AuthService authService = ServiceRegistry.getAuthService();
            User user = authService.findUserByMobileNumber(mobileNumber);
            OtpService otpService = ServiceRegistry.getOtpService();
            otpService.sendOtp(user);
            AuthServlet.showOtpStep(request, user.getMobileNumber(), info);
            AuthServlet.exposeDevOtp(request, otpService);
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("OTP send failed unexpectedly", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    private void handleVerify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String mobileNumber = request.getParameter("mobileNumber");
        String code = request.getParameter("otpCode");
        try {
            var rateLimitService = ServiceRegistry.getRateLimitService();
            if (rateLimitService != null && !rateLimitService.allowOtpVerify(SessionUtil.clientIp(request))) {
                request.setAttribute("error", "Too many verification attempts. Please try again later.");
                request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
                return;
            }
            AuthService authService = ServiceRegistry.getAuthService();
            OtpService otpService = ServiceRegistry.getOtpService();
            User user = authService.findUserByMobileNumber(mobileNumber);
            otpService.verifyOtp(user.getId(), code);

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(SessionUtil.SESSION_USER, user);
            log.info("Session established for user id={} role={} via OTP", user.getId(), user.getRole());
            response.sendRedirect(request.getContextPath() + homeFor(user.getRole()));
        } catch (AppException e) {
            AuthServlet.showOtpStep(request, mobileNumber, null);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        } catch (Exception e) {
            log.error("OTP verification failed unexpectedly", e);
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        }
    }

    private String homeFor(User.Role role) {
        if (role == User.Role.SELLER) {
            return "/seller";
        }
        if (role == User.Role.ADMIN) {
            return "/admin";
        }
        return "/";
    }
}
