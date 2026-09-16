package com.dhatchina.dhatchinamart.util;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.OtpDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OtpDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.service.AdminService;
import com.dhatchina.dhatchinamart.service.AuthService;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.service.OrderService;
import com.dhatchina.dhatchinamart.service.OtpService;
import com.dhatchina.dhatchinamart.service.ProductService;
import com.dhatchina.dhatchinamart.service.RateLimitService;
import com.dhatchina.dhatchinamart.service.SellerService;
import com.dhatchina.dhatchinamart.service.SmsService;
import com.dhatchina.dhatchinamart.service.sms.SmsProvider;
import com.dhatchina.dhatchinamart.service.sms.SmsProviderFactory;

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

        authService = new AuthService(userDAO);
        otpService = new OtpService(otpDAO, new SmsService(createSmsProvider(),
                Integer.parseInt(DbUtil.getEnv("OTP_EXPIRY_MINUTES", "5"))));
        productService = new ProductService(productDAO);
        cartService = new CartService(cartDAO, productDAO);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
        sellerService = new SellerService(productDAO);
        adminService = new AdminService(userDAO, productDAO, orderDAO);
        rateLimitService = new RateLimitService();
    }

    private static SmsProvider createSmsProvider() {
        return SmsProviderFactory.create();
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
}
