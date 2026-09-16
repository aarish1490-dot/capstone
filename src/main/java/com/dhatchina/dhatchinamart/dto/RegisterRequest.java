package com.dhatchina.dhatchinamart.dto;

import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.ValidationUtil;

public class RegisterRequest {

    private String name;
    private String email;
    private String mobileNumber;
    private String password;
    private String confirmPassword;
    private String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void validate() {
        setName(ValidationUtil.requireName(name, "Name"));
        setEmail(ValidationUtil.requireEmail(email));
        setMobileNumber(ValidationUtil.requireIndianMobileNumber(mobileNumber));
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match");
        }
        if (role != null && !role.equals(User.Role.BUYER.name()) && !role.equals(User.Role.SELLER.name())) {
            throw new ValidationException("Invalid account type");
        }
    }
}
