package com.dhatchina.aarishmart.controller;

import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.service.AuthService;
import com.dhatchina.aarishmart.service.OtpService;
import com.dhatchina.aarishmart.service.RateLimitService;
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
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServletTest {

    private static final String MOBILE = "9876500002";
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private AuthService authService;

    @Mock
    private OtpService otpService;

    @Mock
    private RateLimitService rateLimitService;

    private MockedStatic<ServiceRegistry> serviceRegistryMock;
    private MockedStatic<SessionUtil> sessionUtilMock;
    private OtpServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getAuthService).thenReturn(authService);
        serviceRegistryMock.when(ServiceRegistry::getOtpService).thenReturn(otpService);
        serviceRegistryMock.when(ServiceRegistry::getRateLimitService).thenReturn(rateLimitService);
        sessionUtilMock.when(() -> SessionUtil.clientIp(request)).thenReturn(CLIENT_IP);
        when(request.getContextPath()).thenReturn("");
        servlet = new OtpServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private User buyer() {
        User user = new User();
        user.setId(2L);
        user.setEmail("buyer@aarishmart.com");
        user.setMobileNumber(MOBILE);
        user.setRole(User.Role.BUYER);
        return user;
    }

    private void stubLoginForward() {
        when(request.getRequestDispatcher("/WEB-INF/jsp/login.jsp")).thenReturn(dispatcher);
    }

    @Test
    void otpSendSuccessForwardsVerifyStep() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/send");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(rateLimitService.allowOtpSend(MOBILE)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(buyer());
        when(otpService.isDevelopmentMode()).thenReturn(true);
        when(otpService.getLastDevOtp()).thenReturn("483921");
        stubLoginForward();

        servlet.doPost(request, response);

        verify(otpService).sendOtp(org.mockito.ArgumentMatchers.any(User.class));
        verify(request).setAttribute("otpStep", "verify");
        verify(request).setAttribute("devOtp", "483921");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void otpSendRateLimitedForwardsErrorWithoutGeneratingOtp() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/send");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(rateLimitService.allowOtpSend(MOBILE)).thenReturn(false);
        stubLoginForward();

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Too many OTP requests. Please try again later.");
        verify(authService, never()).findUserByMobileNumber(org.mockito.ArgumentMatchers.anyString());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void otpSendUnknownMobileForwardsError() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/send");
        when(request.getParameter("mobileNumber")).thenReturn("9999999999");
        when(rateLimitService.allowOtpSend("9999999999")).thenReturn(true);
        when(authService.findUserByMobileNumber("9999999999"))
                .thenThrow(new ValidationException("Mobile number is not registered."));
        stubLoginForward();

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Mobile number is not registered.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void otpResendAttachesInfoMessage() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/resend");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(rateLimitService.allowOtpSend(MOBILE)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(buyer());
        stubLoginForward();

        servlet.doPost(request, response);

        verify(request).setAttribute("info", "A new OTP has been sent to your mobile number.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void otpVerifySuccessBuyerRedirectsHome() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/verify");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(request.getParameter("otpCode")).thenReturn("483921");
        when(rateLimitService.allowOtpVerify(CLIENT_IP)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(buyer());
        stubLoginForward();
        when(request.getSession(true)).thenReturn(session);
        when(request.changeSessionId()).thenReturn("rotated-id");

        servlet.doPost(request, response);

        verify(otpService).verifyOtp(2L, "483921");
        verify(request).changeSessionId();
        verify(session).setAttribute(
                org.mockito.ArgumentMatchers.eq(SessionUtil.SESSION_USER),
                org.mockito.ArgumentMatchers.any(User.class));
        verify(response).sendRedirect("/");
    }

    @Test
    void otpVerifySuccessSellerRedirectsSellerDashboard() throws Exception {
        User seller = buyer();
        seller.setRole(User.Role.SELLER);
        when(request.getServletPath()).thenReturn("/otp/verify");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(request.getParameter("otpCode")).thenReturn("483921");
        when(rateLimitService.allowOtpVerify(CLIENT_IP)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(seller);
        when(request.getSession(true)).thenReturn(session);
        when(request.changeSessionId()).thenReturn("rotated-id");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/seller");
    }

    @Test
    void otpVerifySuccessAdminRedirectsAdminDashboard() throws Exception {
        User admin = buyer();
        admin.setRole(User.Role.ADMIN);
        when(request.getServletPath()).thenReturn("/otp/verify");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(request.getParameter("otpCode")).thenReturn("483921");
        when(rateLimitService.allowOtpVerify(CLIENT_IP)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(admin);
        when(request.getSession(true)).thenReturn(session);
        when(request.changeSessionId()).thenReturn("rotated-id");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/admin");
    }

    @Test
    void otpVerifyWrongCodeForwardsErrorAndKeepsVerifyStep() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/verify");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(request.getParameter("otpCode")).thenReturn("000000");
        when(rateLimitService.allowOtpVerify(CLIENT_IP)).thenReturn(true);
        when(authService.findUserByMobileNumber(MOBILE)).thenReturn(buyer());
        org.mockito.Mockito.doThrow(new ValidationException("Invalid OTP. Please try again."))
                .when(otpService).verifyOtp(2L, "000000");
        stubLoginForward();

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Invalid OTP. Please try again.");
        verify(request).setAttribute("otpStep", "verify");
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void otpVerifyRateLimitedForwardsError() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/verify");
        when(request.getParameter("mobileNumber")).thenReturn(MOBILE);
        when(request.getParameter("otpCode")).thenReturn("483921");
        when(rateLimitService.allowOtpVerify(CLIENT_IP)).thenReturn(false);
        stubLoginForward();

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Too many verification attempts. Please try again later.");
        verify(authService, never()).findUserByMobileNumber(org.mockito.ArgumentMatchers.anyString());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void unknownPathReturns404() throws Exception {
        when(request.getServletPath()).thenReturn("/otp/unknown");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}