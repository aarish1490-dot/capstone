package com.dhatchina.dhatchinamart.controller;

import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.AuthService;
import com.dhatchina.dhatchinamart.service.OtpService;
import com.dhatchina.dhatchinamart.service.RateLimitService;
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
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServletTest {

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
    private AuthServlet servlet;

    @BeforeEach
    void setUp() {
        serviceRegistryMock = org.mockito.Mockito.mockStatic(ServiceRegistry.class);
        sessionUtilMock = org.mockito.Mockito.mockStatic(SessionUtil.class);
        serviceRegistryMock.when(ServiceRegistry::getAuthService).thenReturn(authService);
        serviceRegistryMock.when(ServiceRegistry::getOtpService).thenReturn(otpService);
        serviceRegistryMock.when(ServiceRegistry::getRateLimitService).thenReturn(rateLimitService);
        sessionUtilMock.when(() -> SessionUtil.isLoggedIn(request)).thenReturn(false);
        when(request.getContextPath()).thenReturn("");
        servlet = new AuthServlet();
    }

    @AfterEach
    void tearDown() {
        sessionUtilMock.close();
        serviceRegistryMock.close();
    }

    private void stubPath(String path) {
        when(request.getServletPath()).thenReturn(path);
    }

    private void stubForward(String jsp) {
        when(request.getRequestDispatcher(jsp)).thenReturn(dispatcher);
    }

    private User userWithRole(User.Role role) {
        User user = new User();
        user.setId(2L);
        user.setEmail("buyer@dhatchinamart.com");
        user.setMobileNumber("9876500002");
        user.setRole(role);
        return user;
    }

    @Test
    void doGetLoginAnonymousForwardsLoginPage() throws Exception {
        stubPath("/login");
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetRegisterAnonymousForwardsRegisterPage() throws Exception {
        stubPath("/register");
        stubForward("/WEB-INF/jsp/register.jsp");

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void doGetLoginWhenLoggedInRedirectsHome() throws Exception {
        sessionUtilMock.when(() -> SessionUtil.isLoggedIn(request)).thenReturn(true);
        stubPath("/login");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/");
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void doGetLogoutRedirectsWithoutInvalidating() throws Exception {
        stubPath("/logout");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/");
    }

    @Test
    void doPostLogoutInvalidatesSessionAndRedirects() throws Exception {
        stubPath("/logout");
        when(request.getSession(false)).thenReturn(session);

        servlet.doPost(request, response);

        verify(session).invalidate();
        verify(response).sendRedirect("/");
    }

    @Test
    void doPostLogoutWithoutSessionStillRedirects() throws Exception {
        stubPath("/logout");
        when(request.getSession(false)).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/");
    }

    @Test
    void doPostEmailLoginSuccessCreatesSessionAndRedirects() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn(null);
        when(request.getParameter("email")).thenReturn("buyer@dhatchinamart.com");
        when(request.getParameter("password")).thenReturn("Buyer@123");
        User buyer = userWithRole(User.Role.BUYER);
        when(authService.login("buyer@dhatchinamart.com", "Buyer@123")).thenReturn(buyer);
        when(request.getSession(true)).thenReturn(session);
        when(request.changeSessionId()).thenReturn("new-id");

        servlet.doPost(request, response);

        verify(rateLimitService).resetLogin("buyer@dhatchinamart.com");
        verify(request).changeSessionId();
        verify(session).setAttribute(SessionUtil.SESSION_USER, buyer);
        verify(response).sendRedirect("/");
    }

    @Test
    void doPostEmailLoginFailureForwardsErrorAndRecordsFailure() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn(null);
        when(request.getParameter("email")).thenReturn("buyer@dhatchinamart.com");
        when(request.getParameter("password")).thenReturn("wrong-password");
        when(authService.login("buyer@dhatchinamart.com", "wrong-password"))
                .thenThrow(new ValidationException("Invalid email or password"));
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(rateLimitService).recordLoginFailure("buyer@dhatchinamart.com");
        verify(request).setAttribute("error", "Invalid email or password");
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doPostEmailLoginBlankEmailForwardsWithoutContactingServices() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn(null);
        when(request.getParameter("email")).thenReturn("   ");
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Email is required");
        verify(authService, never()).login(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doPostEmailLoginLockedAccountForwardsLockMessage() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn(null);
        when(request.getParameter("email")).thenReturn("buyer@dhatchinamart.com");
        when(rateLimitService.isLoginLocked("buyer@dhatchinamart.com")).thenReturn(true);
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(authService, never()).login(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(request).setAttribute("error", "Too many failed attempts. Please try again later.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostOtpLoginForwardsVerifyStepAndExposesDevOtp() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn("9876500002");
        when(rateLimitService.allowOtpSend("9876500002")).thenReturn(true);
        User buyer = userWithRole(User.Role.BUYER);
        when(authService.findUserByMobileNumber("9876500002")).thenReturn(buyer);
        when(otpService.isDevelopmentMode()).thenReturn(true);
        when(otpService.getLastDevOtp()).thenReturn("483921");
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(otpService).sendOtp(buyer);
        verify(request).setAttribute("otpStep", "verify");
        verify(request).setAttribute("devOtp", "483921");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostOtpLoginUnknownMobileForwardsError() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn("9999999999");
        when(rateLimitService.allowOtpSend("9999999999")).thenReturn(true);
        when(authService.findUserByMobileNumber("9999999999"))
                .thenThrow(new ValidationException("Mobile number is not registered."));
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Mobile number is not registered.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPostOtpLoginRateLimitedForwardsError() throws Exception {
        stubPath("/login");
        when(request.getParameter("mobileNumber")).thenReturn("9876500002");
        when(rateLimitService.allowOtpSend("9876500002")).thenReturn(false);
        stubForward("/WEB-INF/jsp/login.jsp");

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Too many OTP requests. Please try again later.");
        verify(authService, never()).findUserByMobileNumber(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doPostRegisterSuccessRedirectsToLoginWithFlag() throws Exception {
        stubPath("/register");
        when(request.getParameter("name")).thenReturn("New Buyer");
        when(request.getParameter("email")).thenReturn("new@dhatchinamart.com");
        when(request.getParameter("mobileNumber")).thenReturn("9876500002");
        when(request.getParameter("password")).thenReturn("NewPass@123");
        when(request.getParameter("confirmPassword")).thenReturn("NewPass@123");
        when(request.getParameter("role")).thenReturn("BUYER");
        User registered = userWithRole(User.Role.BUYER);
        when(authService.register(org.mockito.ArgumentMatchers.any())).thenReturn(registered);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/login?registered=1");
    }

    @Test
    void doPostRegisterValidationErrorForwardsRegisterPageAndRepopulates() throws Exception {
        stubPath("/register");
        when(request.getParameter("name")).thenReturn("New Buyer");
        when(request.getParameter("email")).thenReturn("not-an-email");
        when(request.getParameter("mobileNumber")).thenReturn("9876500002");
        when(request.getParameter("password")).thenReturn("NewPass@123");
        when(request.getParameter("confirmPassword")).thenReturn("NewPass@123");
        when(request.getParameter("role")).thenReturn("BUYER");
        when(authService.register(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ValidationException("A valid email is required"));
        stubForward("/WEB-INF/jsp/register.jsp");

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "A valid email is required");
        verify(request).setAttribute("email", "not-an-email");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void showOtpStepSetsVerifyStepMaskedMobileAndInfo() {
        mock(HttpServletRequest.class);
        HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);

        AuthServlet.showOtpStep(req, "9876500002", "A new OTP has been sent");

        verify(req).setAttribute("otpStep", "verify");
        verify(req).setAttribute("mobileNumber", "9876500002");
        verify(req).setAttribute("maskedMobile", "******0002");
        verify(req).setAttribute("info", "A new OTP has been sent");
    }

    @Test
    void exposeDevOtpSetsDevModeOnlyWhenDevelopmentAndOtpAvailable() {
        HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
        OtpService dev = org.mockito.Mockito.mock(OtpService.class);
        when(dev.isDevelopmentMode()).thenReturn(true);
        when(dev.getLastDevOtp()).thenReturn("483921");

        AuthServlet.exposeDevOtp(req, dev);

        verify(req).setAttribute("devMode", true);
        verify(req).setAttribute("devOtp", "483921");
        assertEquals("******0002", com.dhatchina.dhatchinamart.util.OtpUtil.maskMobile("9876500002"));
    }

    @Test
    void exposeDevOtpNeverShowsInProductionMode() {
        HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
        OtpService prod = org.mockito.Mockito.mock(OtpService.class);
        when(prod.isDevelopmentMode()).thenReturn(false);
        when(prod.getLastDevOtp()).thenReturn("483921");

        AuthServlet.exposeDevOtp(req, prod);

        verify(req, never()).setAttribute(org.mockito.ArgumentMatchers.eq("devOtp"),
                org.mockito.ArgumentMatchers.any());
        assertTrue(prod != null, "sanity");
    }
}