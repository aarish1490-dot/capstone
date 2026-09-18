package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.util.AuthUtil;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoIntegrationTest {

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDb.newDataSource("userdaotest");
        userDAO = new UserDAOImpl(dataSource);
    }

    private User newUser(String email, String mobile) {
        User user = new User();
        user.setName("Integration User");
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(AuthUtil.hashPassword("Secure@123"));
        user.setRole(User.Role.BUYER);
        return user;
    }

    @Test
    void findByMobileNumberReturnsSeededAdmin() {
        Optional<User> admin = userDAO.findByMobileNumber("9876500001");

        assertTrue(admin.isPresent());
        assertEquals("admin@dhatchinamart.com", admin.get().getEmail());
        assertEquals(User.Role.ADMIN, admin.get().getRole());
        assertEquals("9876500001", admin.get().getMobileNumber());
    }

    @Test
    void findByMobileNumberUnknownReturnsEmpty() {
        assertTrue(userDAO.findByMobileNumber("9999999999").isEmpty());
    }

    @Test
    void insertPersistsMobileNumber() {
        long id = userDAO.insert(newUser("newuser@dhatchinamart.com", "9876500002"));

        Optional<User> found = userDAO.findByMobileNumber("9876500002");
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("newuser@dhatchinamart.com", found.get().getEmail());
        assertEquals("9876500002", found.get().getMobileNumber());
    }

    @Test
    void duplicateMobileNumberIsRejectedByUniqueConstraint() {
        userDAO.insert(newUser("first@dhatchinamart.com", "9876500003"));

        assertThrows(RuntimeException.class,
                () -> userDAO.insert(newUser("second@dhatchinamart.com", "9876500003")));
    }

    @Test
    void duplicateEmailIsRejectedByUniqueConstraint() {
        userDAO.insert(newUser("dup@dhatchinamart.com", "9876500004"));

        assertThrows(RuntimeException.class,
                () -> userDAO.insert(newUser("dup@dhatchinamart.com", "9876500005")));
    }

    @Test
    void findAllReturnsSeededAndInsertedUsers() {
        long id = userDAO.insert(newUser("listed@dhatchinamart.com", "9876500006"));

        List<User> users = userDAO.findAll();

        assertTrue(users.stream().anyMatch(u -> u.getId() == id));
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("admin@dhatchinamart.com")));
        assertTrue(users.stream().allMatch(u -> u.isActive()), "freshly inserted users are active by default");
    }

    @Test
    void updateActivePersistsAndResets() {
        long id = userDAO.insert(newUser("toggle@dhatchinamart.com", "9876500007"));

        assertTrue(userDAO.updateActive(id, false));
        assertFalse(userDAO.findById(id).orElseThrow().isActive());
        assertTrue(userDAO.findById(id).orElseThrow().getEmail().equals("toggle@dhatchinamart.com"));

        assertTrue(userDAO.updateActive(id, true));
        assertTrue(userDAO.findById(id).orElseThrow().isActive());
    }

    @Test
    void updateActiveMissingUserReturnsFalse() {
        assertFalse(userDAO.updateActive(999_999L, false));
    }
}
