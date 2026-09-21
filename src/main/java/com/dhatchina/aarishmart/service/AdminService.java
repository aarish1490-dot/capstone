package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.OrderDAO;
import com.dhatchina.aarishmart.dao.ProductDAO;
import com.dhatchina.aarishmart.dao.UserDAO;
import com.dhatchina.aarishmart.dto.AdminStats;
import com.dhatchina.aarishmart.exception.NotFoundException;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Order;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.model.User;

import java.util.List;

public class AdminService {

    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;

    public AdminService(UserDAO userDAO, ProductDAO productDAO, OrderDAO orderDAO) {
        this.userDAO = userDAO;
        this.productDAO = productDAO;
        this.orderDAO = orderDAO;
    }

    public AdminStats getDashboardStats() {
        AdminStats stats = new AdminStats();
        stats.setTotalUsers(userDAO.countAll());
        stats.setTotalBuyers(userDAO.countByRole(User.Role.BUYER));
        stats.setTotalSellers(userDAO.countByRole(User.Role.SELLER));
        stats.setTotalProducts(productDAO.countAll());
        stats.setTotalOrders(orderDAO.countAll());
        return stats;
    }

    public List<User> users() {
        return userDAO.findAll();
    }

    public List<Order> orders() {
        return orderDAO.findAll();
    }

    public List<Product> products() {
        return productDAO.findAll();
    }

    /**
     * Activates or deactivates a user account. Admin accounts can never be
     * deactivated (from anywhere) so the platform always has at least one
     * administrator who can undo a deactivation.
     */
    public void setUserActive(long userId, boolean active) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new ValidationException("Admin accounts cannot be deactivated");
        }
        if (!userDAO.updateActive(userId, active)) {
            throw new NotFoundException("User not found");
        }
    }

    /**
     * Lists or unlists a product in the marketplace. Sellers and the admin
     * can still load it.
     */
    public void setProductActive(long productId, boolean active) {
        if (productDAO.findById(productId).isEmpty()) {
            throw new NotFoundException("Product not found");
        }
        if (!productDAO.updateActive(productId, active)) {
            throw new NotFoundException("Product not found");
        }
    }
}