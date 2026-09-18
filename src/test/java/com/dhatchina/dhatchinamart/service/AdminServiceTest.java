package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dto.AdminStats;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private ProductDAO productDAO;

    @Mock
    private OrderDAO orderDAO;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userDAO, productDAO, orderDAO);
    }

    private User buyerUser(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Buyer");
        user.setEmail("buyer@dhatchinamart.com");
        user.setMobileNumber("9876500002");
        user.setRole(User.Role.BUYER);
        return user;
    }

    private User adminUser(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Admin");
        user.setEmail("admin@dhatchinamart.com");
        user.setMobileNumber("9876500001");
        user.setRole(User.Role.ADMIN);
        return user;
    }

    @Test
    void getDashboardStatsAggregatesCounts() {
        when(userDAO.countAll()).thenReturn(10L);
        when(userDAO.countByRole(User.Role.BUYER)).thenReturn(6L);
        when(userDAO.countByRole(User.Role.SELLER)).thenReturn(3L);
        when(productDAO.countAll()).thenReturn(40L);
        when(orderDAO.countAll()).thenReturn(7L);

        AdminStats stats = adminService.getDashboardStats();

        assertEquals(10L, stats.getTotalUsers());
        assertEquals(6L, stats.getTotalBuyers());
        assertEquals(3L, stats.getTotalSellers());
        assertEquals(40L, stats.getTotalProducts());
        assertEquals(7L, stats.getTotalOrders());
    }

    @Test
    void usersDelegatesToUserDao() {
        List<User> expected = List.of(buyerUser(1L));
        when(userDAO.findAll()).thenReturn(expected);

        assertSame(expected, adminService.users());
    }

    @Test
    void ordersDelegatesToOrderDao() {
        List<Order> expected = List.of(new Order());
        when(orderDAO.findAll()).thenReturn(expected);

        assertSame(expected, adminService.orders());
    }

    @Test
    void productsDelegatesToProductDao() {
        List<Product> expected = List.of(new Product());
        when(productDAO.findAll()).thenReturn(expected);

        assertSame(expected, adminService.products());
    }

    @Test
    void setUserActiveDeactivatesBuyer() {
        when(userDAO.findById(5L)).thenReturn(Optional.of(buyerUser(5L)));
        when(userDAO.updateActive(5L, false)).thenReturn(true);

        adminService.setUserActive(5L, false);

        verify(userDAO).updateActive(5L, false);
    }

    @Test
    void setUserActiveReactivatesBuyer() {
        when(userDAO.findById(5L)).thenReturn(Optional.of(buyerUser(5L)));
        when(userDAO.updateActive(5L, true)).thenReturn(true);

        adminService.setUserActive(5L, true);

        verify(userDAO).updateActive(5L, true);
    }

    @Test
    void setUserActiveMissingUserThrowsNotFound() {
        when(userDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminService.setUserActive(999L, false));
    }

    @Test
    void setUserActiveAdminAccountThrowsValidation() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(adminUser(1L)));

        assertThrows(ValidationException.class, () -> adminService.setUserActive(1L, false));
    }

    @Test
    void setUserActiveDaoFailureThrowsNotFound() {
        when(userDAO.findById(5L)).thenReturn(Optional.of(buyerUser(5L)));
        when(userDAO.updateActive(5L, false)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> adminService.setUserActive(5L, false));
    }

    @Test
    void setProductActiveUnlistsProduct() {
        Product product = new Product();
        product.setId(3L);
        when(productDAO.findById(3L)).thenReturn(Optional.of(product));
        when(productDAO.updateActive(3L, false)).thenReturn(true);

        adminService.setProductActive(3L, false);

        verify(productDAO).updateActive(3L, false);
    }

    @Test
    void setProductActiveRelistsProduct() {
        Product product = new Product();
        product.setId(3L);
        when(productDAO.findById(3L)).thenReturn(Optional.of(product));
        when(productDAO.updateActive(3L, true)).thenReturn(true);

        adminService.setProductActive(3L, true);

        verify(productDAO).updateActive(3L, true);
    }

    @Test
    void setProductActiveMissingProductThrowsNotFound() {
        when(productDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminService.setProductActive(999L, false));
    }

    @Test
    void setProductActiveDaoFailureThrowsNotFound() {
        Product product = new Product();
        product.setId(3L);
        when(productDAO.findById(3L)).thenReturn(Optional.of(product));
        when(productDAO.updateActive(3L, false)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> adminService.setProductActive(3L, false));
    }
}