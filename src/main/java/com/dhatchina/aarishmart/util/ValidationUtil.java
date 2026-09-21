package com.dhatchina.aarishmart.util;

import com.dhatchina.aarishmart.exception.ValidationException;

public final class ValidationUtil {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String INDIAN_MOBILE_REGEX = "^[6-9]\\d{9}$";
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PRODUCT_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_URL_LENGTH = 500;

    private ValidationUtil() {
    }

    public static String requireName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new ValidationException(field + " is too long");
        }
        return name.trim();
    }

    public static String requireEmail(String email) {
        if (email == null || !email.trim().matches(EMAIL_REGEX)) {
            throw new ValidationException("A valid email is required");
        }
        if (email.trim().length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Email is too long");
        }
        return email.trim().toLowerCase();
    }

    /**
     * Validates and normalizes a 10-digit Indian mobile number.
     * Leading "+91" / "91" prefixes and non-digit characters are stripped.
     * Returns the normalized 10-digit number.
     */
    public static String requireIndianMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new ValidationException("Mobile number is required");
        }
        String digits = mobileNumber.trim().replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (digits.length() != 10 || !digits.matches(INDIAN_MOBILE_REGEX)) {
            throw new ValidationException("Please enter a valid 10-digit mobile number.");
        }
        return digits;
    }

    public static String requireProductName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Product name is required");
        }
        if (name.trim().length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new ValidationException("Product name is too long");
        }
        return name.trim();
    }

    public static String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException("Value exceeds maximum length");
        }
        return trimmed;
    }

    public static String optionalText(String value) {
        return optionalText(value, MAX_DESCRIPTION_LENGTH);
    }

    public static String optionalUrl(String value) {
        String url = optionalText(value, MAX_URL_LENGTH);
        if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ValidationException("Image URL must start with http:// or https://");
        }
        return url;
    }

    public static int parsePositiveInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new ValidationException(field + " must be a positive number");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid number");
        }
    }

    public static int parseNonNegativeInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new ValidationException(field + " cannot be negative");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid number");
        }
    }

    public static int parseQuantity(String value, String field) {
        int quantity = parsePositiveInt(value, field);
        if (quantity > 99) {
            throw new ValidationException(field + " cannot exceed 99");
        }
        return quantity;
    }

    public static java.math.BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Price must be a valid number");
        }
        try {
            java.math.BigDecimal price = new java.math.BigDecimal(value.trim());
            if (price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Price must be greater than zero");
            }
            if (price.scale() > 2) {
                throw new ValidationException("Price cannot have more than 2 decimal places");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new ValidationException("Price must be a valid number");
        }
    }
}
