package com.dhatchina.aarishmart.util;

import com.dhatchina.aarishmart.dao.CartDAO;
import com.dhatchina.aarishmart.dao.OrderDAO;
import com.dhatchina.aarishmart.dao.OtpDAO;
import com.dhatchina.aarishmart.dao.ProductDAO;
import com.dhatchina.aarishmart.dao.ReviewDAO;
import com.dhatchina.aarishmart.dao.UserDAO;
import com.dhatchina.aarishmart.dao.impl.CartDAOImpl;
import com.dhatchina.aarishmart.dao.impl.OrderDAOImpl;
import com.dhatchina.aarishmart.dao.impl.OtpDAOImpl;
import com.dhatchina.aarishmart.dao.impl.ProductDAOImpl;
import com.dhatchina.aarishmart.dao.impl.ReviewDAOImpl;
import com.dhatchina.aarishmart.dao.impl.UserDAOImpl;
import com.dhatchina.aarishmart.service.AdminService;
import com.dhatchina.aarishmart.service.AiService;
import com.dhatchina.aarishmart.service.AuthService;
import com.dhatchina.aarishmart.service.CartService;
import com.dhatchina.aarishmart.service.OrderService;
import com.dhatchina.aarishmart.service.OtpService;
import com.dhatchina.aarishmart.service.ProductService;
import com.dhatchina.aarishmart.service.RateLimitService;
import com.dhatchina.aarishmart.service.ReviewService;
import com.dhatchina.aarishmart.service.SellerService;
import com.dhatchina.aarishmart.service.SmsService;
import com.dhatchina.aarishmart.service.ai.AiProvider;
import com.dhatchina.aarishmart.service.ai.AiProviderFactory;
import com.dhatchina.aarishmart.service.sms.SmsProvider;
import com.dhatchina.aarishmart.service.sms.SmsProviderFactory;

import javax.sql.DataSource;

public final class ServiceRegistry {

    private static AuthService authService;
    private static OtpService otpService;
    private static ProductService productService;
    private static CartService cartService;
    private static OrderService orderService;
    private static SellerService sellerService;
    private static AdminService adminService;
    private static RateLimitService rateLimitService;
    private static ReviewService reviewService;
    private static AiService aiService;

    private ServiceRegistry() {
    }

    public static synchronized void init() {
        if (authService != null) {
            return;
        }
        DataSource dataSource = DbUtil.getDataSource();
        UserDAO userDAO = new UserDAOImpl(dataSource);
        ProductDAO productDAO = new ProductDAOImpl(dataSource);
        CartDAO cartDAO = new CartDAOImpl(dataSource);
        OrderDAO orderDAO = new OrderDAOImpl(dataSource);
        OtpDAO otpDAO = new OtpDAOImpl(dataSource);
        ReviewDAO reviewDAO = new ReviewDAOImpl(dataSource);

        authService = new AuthService(userDAO);
        otpService = new OtpService(otpDAO, new SmsService(createSmsProvider(),
                Integer.parseInt(DbUtil.getEnv("OTP_EXPIRY_MINUTES", "5"))));
        productService = new ProductService(productDAO);
        cartService = new CartService(cartDAO, productDAO);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
        sellerService = new SellerService(productDAO);
        adminService = new AdminService(userDAO, productDAO, orderDAO);
        rateLimitService = new RateLimitService();
        reviewService = new ReviewService(reviewDAO, orderDAO, productDAO);
        aiService = new AiService(createAiProvider(), productService, reviewService);
    }

    private static SmsProvider createSmsProvider() {
        return SmsProviderFactory.create();
    }

    private static AiProvider createAiProvider() {
        return AiProviderFactory.create();
    }

    public static synchronized void reset() {
        authService = null;
        otpService = null;
        productService = null;
        cartService = null;
        orderService = null;
        sellerService = null;
        adminService = null;
        rateLimitService = null;
        reviewService = null;
        aiService = null;
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static OtpService getOtpService() {
        return otpService;
    }

    public static ProductService getProductService() {
        return productService;
    }

    public static CartService getCartService() {
        return cartService;
    }

    public static OrderService getOrderService() {
        return orderService;
    }

    public static SellerService getSellerService() {
        return sellerService;
    }

    public static AdminService getAdminService() {
        return adminService;
    }

    public static RateLimitService getRateLimitService() {
        return rateLimitService;
    }

    public static ReviewService getReviewService() {
        return reviewService;
    }

    public static AiService getAiService() {
        return aiService;
    }
}
