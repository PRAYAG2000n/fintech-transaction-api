package com.fintech.repository;

import com.fintech.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAccountNumber(String accountNumber);
    boolean existsByEmail(String email);
}
