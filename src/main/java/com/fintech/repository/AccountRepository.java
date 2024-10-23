package com.fintech.repository;

import com.fintech.model.Account;
import com.fintech.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.user = :user")
    Optional<Account> findByUserWithLock(@Param("user") User user);
}
