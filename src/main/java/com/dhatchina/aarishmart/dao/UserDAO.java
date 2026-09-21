package com.dhatchina.aarishmart.dao;

import com.dhatchina.aarishmart.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findById(long id);

    List<User> findAll();

    long insert(User user);

    boolean updateActive(long id, boolean active);

    long countAll();

    long countByRole(User.Role role);
}
