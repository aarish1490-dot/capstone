package com.dhatchina.aarishmart.util;

import com.dhatchina.aarishmart.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilTest {

    @Test
    void requireNameRejectsNullBlankAndOversized() {
        assertThrows(ValidationException.class, () -> ValidationUtil.requireName(null, "Name"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireName("   ", "Name"));
        assertThrows(ValidationException.class,
                () -> ValidationUtil.requireName("x".repeat(101), "Name"));
        assertEquals("  Bob  ", "  Bob  ", "input kept for comparison");
        assertEquals("Bob", ValidationUtil.requireName("  Bob  ", "Name"), "name is trimmed");
        assertEquals("x".repeat(100), ValidationUtil.requireName("x".repeat(100), "Name"));
    }

    @Test
    void requireEmailRejectsInvalidValues() {
        assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail(null));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("not-an-email"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("a@b"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("a@b."));
        assertThrows(ValidationException.class,
                () -> ValidationUtil.requireEmail("x".repeat(250) + "@example.com"), "oversized email");
    }

    @Test
    void requireEmailNormalizesToLowercase() {
        assertEquals("buyer@example.com", ValidationUtil.requireEmail("BUYER@Example.COM "));
    }

    @Test
    void mobileNumberRejectsNullBlankAndGarbage() {
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber(null));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber(""));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber("abcdefghij"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber("1234567890"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber("98765432"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireIndianMobileNumber("5678901234"),
                "numbers starting with 5 are not valid Indian mobile numbers");
    }

    @Test
    void mobileNumberNormalizesPrefixesAndSeparators() {
        assertEquals("9876543210", ValidationUtil.requireIndianMobileNumber("9876543210"));
        assertEquals("9876543210", ValidationUtil.requireIndianMobileNumber("+91 98765 43210"));
        assertEquals("9876543210", ValidationUtil.requireIndianMobileNumber("919876543210"));
        assertEquals("9876543210", ValidationUtil.requireIndianMobileNumber("+91-98765-43210"));
    }

    @Test
    void requireProductNameValidatesLength() {
        assertThrows(ValidationException.class, () -> ValidationUtil.requireProductName(null));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireProductName(" "));
        assertThrows(ValidationException.class,
                () -> ValidationUtil.requireProductName("x".repeat(201)));
        assertEquals("Keyboard", ValidationUtil.requireProductName("  Keyboard  "));
    }

    @Test
    void optionalTextTreatsBlankAsAbsentAndEnforcesMaxLength() {
        assertNull(ValidationUtil.optionalText(null, 10));
        assertNull(ValidationUtil.optionalText("   ", 10));
        assertThrows(ValidationException.class, () -> ValidationUtil.optionalText("too long", 5));
        assertEquals("hello", ValidationUtil.optionalText("  hello  ", 10));
    }

    @Test
    void optionalUrlRequiresHttpOrHttpsProtocol() {
        assertNull(ValidationUtil.optionalUrl(null));
        assertThrows(ValidationException.class, () -> ValidationUtil.optionalUrl("ftp://example.com/a.png"));
        assertThrows(ValidationException.class, () -> ValidationUtil.optionalUrl("javascript:alert(1)"));
        assertThrows(ValidationException.class, () -> ValidationUtil.optionalUrl("not-a-url"));
        assertThrows(ValidationException.class, () -> ValidationUtil.optionalUrl("https://" + "x".repeat(500)));
        assertEquals("https://example.com/a.png", ValidationUtil.optionalUrl("https://example.com/a.png"));
        assertEquals("http://example.com/a.png", ValidationUtil.optionalUrl("http://example.com/a.png"));
    }

    @Test
    void parsePositiveIntRejectsNonPositiveAndNonNumericValues() {
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePositiveInt("0", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePositiveInt("-7", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePositiveInt("abc", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePositiveInt(null, "Quantity"));
        assertEquals(5, ValidationUtil.parsePositiveInt("5", "Quantity"));
    }

    @Test
    void parseNonNegativeIntAllowsZeroButRejectsNegative() {
        assertEquals(0, ValidationUtil.parseNonNegativeInt("0", "Stock"));
        assertEquals(3, ValidationUtil.parseNonNegativeInt("3", "Stock"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseNonNegativeInt("-1", "Stock"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseNonNegativeInt("one", "Stock"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseNonNegativeInt(null, "Stock"));
    }

    @Test
    void parseQuantityRejectsOutOfRangeValues() {
        assertEquals(0, 0, "sanity");
        assertThrows(ValidationException.class, () -> ValidationUtil.parseQuantity("0", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseQuantity("-2", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseQuantity("100", "Quantity"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parseQuantity("99999", "Quantity"));
        assertEquals(99, ValidationUtil.parseQuantity("99", "Quantity"));
        assertEquals(1, ValidationUtil.parseQuantity("1", "Quantity"));
    }

    @Test
    void parsePriceRejectsZeroNegativeHugeScaleAndGarbage() {
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice("0"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice("-5.00"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice("10.999"),
                "more than 2 decimal places are rejected");
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice("one-hundred"));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice(""));
        assertThrows(ValidationException.class, () -> ValidationUtil.parsePrice(null),
                "a missing price must be a clean validation error, not an NPE");
    }

    @Test
    void parsePriceAcceptsValidMoneyValues() {
        assertEquals(new BigDecimal("10.50"), ValidationUtil.parsePrice("10.50"));
        assertEquals(new BigDecimal("0.01"), ValidationUtil.parsePrice("0.01"));
        assertEquals(new BigDecimal("1499"), ValidationUtil.parsePrice("1499").stripTrailingZeros());
        assertTrue(new BigDecimal("99999999.99").compareTo(ValidationUtil.parsePrice("99999999.99")) == 0);
    }
}