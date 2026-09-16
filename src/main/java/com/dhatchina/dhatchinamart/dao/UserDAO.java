package com.dhatchina.dhatchinamart.dao;

import com.dhatchina.dhatchinamart.model.User;

import java.util.Optional;

public interface UserDAO {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findById(long id);

    long insert(User user);

    long countAll();

    long countByRole(User.Role role);
}
