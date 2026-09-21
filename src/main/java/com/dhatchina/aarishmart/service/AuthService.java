package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.UserDAO;
import com.dhatchina.aarishmart.dto.LoginRequest;
import com.dhatchina.aarishmart.dto.RegisterRequest;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.util.AuthUtil;
import com.dhatchina.aarishmart.util.OtpUtil;
import com.dhatchina.aarishmart.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private static String maskMobile(String mobileNumber) {
        return OtpUtil.maskMobile(mobileNumber);
    }

    public User register(RegisterRequest request) {
        request.validate();
        String password = request.getPassword();
        AuthUtil.validatePassword(password);
        String email = ValidationUtil.requireEmail(request.getEmail());
        String mobileNumber = ValidationUtil.requireIndianMobileNumber(request.getMobileNumber());

        if (userDAO.findByEmail(email).isPresent()) {
            throw new ValidationException("An account with this email already exists");
        }
        if (userDAO.findByMobileNumber(mobileNumber).isPresent()) {
            throw new ValidationException("Mobile number already registered.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setPasswordHash(AuthUtil.hashPassword(password));
        user.setRole(User.Role.SELLER.name().equals(request.getRole()) ? User.Role.SELLER : User.Role.BUYER);
        long id = userDAO.insert(user);
        user.setId(id);
        user.setPasswordHash(null);
        log.info("New user registered: email={}, mobile={}, id={}", email, maskMobile(mobileNumber), id);
        return user;
    }

    /**
     * Finds a user by their registered mobile number (OTP login step 1).
     * The password hash is never returned to the caller.
     */
    public User findUserByMobileNumber(String mobileNumber) {
        String normalized = ValidationUtil.requireIndianMobileNumber(mobileNumber);
        Optional<User> found = userDAO.findByMobileNumber(normalized);
        if (found.isEmpty()) {
            throw new ValidationException("Mobile number is not registered.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new ValidationException("Your account has been deactivated. Contact support.");
        }
        user.setPasswordHash(null);
        return user;
    }

    public User login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.validate();

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> found = userDAO.findByEmail(normalizedEmail);
        if (found.isEmpty() || !AuthUtil.verifyPassword(password, found.get().getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        User user = found.get();
        if (!user.isActive()) {
            throw new ValidationException("Your account has been deactivated");
        }
        user.setPasswordHash(null);
        log.info("User logged in: email={}, id={}, role={}", normalizedEmail, user.getId(), user.getRole());
        return user;
    }
}
