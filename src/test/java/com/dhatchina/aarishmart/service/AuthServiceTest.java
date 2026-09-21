package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.UserDAO;
import com.dhatchina.aarishmart.dto.RegisterRequest;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.User;
import com.dhatchina.aarishmart.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDAO);
    }

    private User buyerUser() {
        User user = new User();
        user.setId(2);
        user.setName("Rahul Sharma");
        user.setEmail("buyer@aarishmart.com");
        user.setMobileNumber("9876543210");
        user.setPasswordHash(AuthUtil.hashPassword("Buyer@123"));
        user.setRole(User.Role.BUYER);
        return user;
    }

    @Test
    void loginWithValidCredentialsReturnsUser() {
        when(userDAO.findByEmail("buyer@aarishmart.com")).thenReturn(Optional.of(buyerUser()));

        User user = authService.login("buyer@aarishmart.com", "Buyer@123");

        assertNotNull(user);
        assertEquals("buyer@aarishmart.com", user.getEmail());
        assertEquals(User.Role.BUYER, user.getRole());
        assertFalse(user.getPasswordHash() != null, "password hash must not be returned to caller");
    }

    @Test
    void loginWithInvalidCredentialsThrows() {
        when(userDAO.findByEmail("buyer@aarishmart.com")).thenReturn(Optional.of(buyerUser()));

        assertThrows(ValidationException.class,
                () -> authService.login("buyer@aarishmart.com", "wrong-password"));
    }

    @Test
    void loginWithUnknownEmailThrows() {
        when(userDAO.findByEmail("nobody@aarishmart.com")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> authService.login("nobody@aarishmart.com", "Anything@123"));
    }

    @Test
    void loginRejectedForDeactivatedUser() {
        User user = buyerUser();
        user.setActive(false);
        when(userDAO.findByEmail("buyer@aarishmart.com")).thenReturn(Optional.of(user));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.login("buyer@aarishmart.com", "Buyer@123"));
        assertEquals("Your account has been deactivated", ex.getMessage());
    }

    @Test
    void findUserByMobileNumberRejectedForDeactivatedUser() {
        User user = buyerUser();
        user.setActive(false);
        when(userDAO.findByMobileNumber("9876543210")).thenReturn(Optional.of(user));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.findUserByMobileNumber("9876543210"));
        assertEquals("Your account has been deactivated. Contact support.", ex.getMessage());
    }

    @Test
    void registerWithDuplicateEmailThrows() {
        when(userDAO.findByEmail("buyer@aarishmart.com")).thenReturn(Optional.of(buyerUser()));

        RegisterRequest request = validRegisterRequest();
        request.setEmail("buyer@aarishmart.com");

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void registerCreatesBuyerWithHashedPassword() {
        when(userDAO.findByEmail("new@aarishmart.com")).thenReturn(Optional.empty());
        when(userDAO.findByMobileNumber("9876500002")).thenReturn(Optional.empty());
        when(userDAO.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, User.class).setId(99L);
            return 99L;
        });

        User user = authService.register(validRegisterRequest());

        assertNotNull(user);
        assertEquals(99L, user.getId());
        assertEquals("new@aarishmart.com", user.getEmail());
        assertEquals("9876500002", user.getMobileNumber());
        assertEquals(User.Role.BUYER, user.getRole());
        verify(userDAO).insert(any(User.class));
    }

    @Test
    void passwordIsStoredHashedNotPlain() {
        when(userDAO.findByEmail("new@aarishmart.com")).thenReturn(Optional.empty());
        when(userDAO.findByMobileNumber("9876500002")).thenReturn(Optional.empty());
        java.util.concurrent.atomic.AtomicReference<String> storedHash =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(userDAO.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            storedHash.set(user.getPasswordHash());
            user.setId(99L);
            return 99L;
        });

        authService.register(validRegisterRequest());

        String hash = storedHash.get();
        assertNotEquals("NewPass@123", hash);
        assertTrue(hash.startsWith("$2"), "stored value must be a bcrypt hash");
        assertTrue(AuthUtil.verifyPassword("NewPass@123", hash));
    }

    @Test
    void registerCreatesSellerWhenRoleProvided() {
        when(userDAO.findByEmail("new@aarishmart.com")).thenReturn(Optional.empty());
        when(userDAO.findByMobileNumber("9876500002")).thenReturn(Optional.empty());
        when(userDAO.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, User.class).setId(99L);
            return 99L;
        });

        RegisterRequest request = validRegisterRequest();
        request.setRole("SELLER");

        User user = authService.register(request);

        assertEquals(User.Role.SELLER, user.getRole());
    }

    @Test
    void registerWithUnknownRoleThrows() {
        RegisterRequest request = validRegisterRequest();
        request.setRole("SUPERUSER");

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void registerWithInvalidMobileThrows() {
        RegisterRequest request = validRegisterRequest();
        request.setMobileNumber("123");

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void registerWithEmptyMobileThrows() {
        RegisterRequest request = validRegisterRequest();
        request.setMobileNumber("");

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void registerWithNonNumericMobileThrows() {
        RegisterRequest request = validRegisterRequest();
        request.setMobileNumber("abcdefghij");

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void registerWithDuplicateMobileThrows() {
        when(userDAO.findByEmail("new@aarishmart.com")).thenReturn(Optional.empty());
        when(userDAO.findByMobileNumber("9876543210")).thenReturn(Optional.of(buyerUser()));

        RegisterRequest request = validRegisterRequest();
        request.setMobileNumber("9876543210");

        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(request));
        assertEquals("Mobile number already registered.", ex.getMessage());
    }

    @Test
    void findUserByMobileNumberReturnsRegisteredUser() {
        when(userDAO.findByMobileNumber("9876543210")).thenReturn(Optional.of(buyerUser()));

        User user = authService.findUserByMobileNumber("9876543210");

        assertNotNull(user);
        assertEquals(2L, user.getId());
        assertEquals("9876543210", user.getMobileNumber());
        assertFalse(user.getPasswordHash() != null, "password hash must not be returned to caller");
    }

    @Test
    void findUserByMobileNumberNormalizesPrefix() {
        when(userDAO.findByMobileNumber("9876543210")).thenReturn(Optional.of(buyerUser()));

        User user = authService.findUserByMobileNumber("+91 98765 43210");

        assertNotNull(user);
        assertEquals("9876543210", user.getMobileNumber());
    }

    @Test
    void findUserByUnregisteredMobileThrows() {
        when(userDAO.findByMobileNumber("9999999999")).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.findUserByMobileNumber("9999999999"));
        assertEquals("Mobile number is not registered.", ex.getMessage());
    }

    @Test
    void findUserByInvalidMobileThrows() {
        assertThrows(ValidationException.class,
                () -> authService.findUserByMobileNumber("0000000000"));
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@aarishmart.com");
        request.setMobileNumber("9876500002");
        request.setPassword("NewPass@123");
        request.setConfirmPassword("NewPass@123");
        return request;
    }
}
