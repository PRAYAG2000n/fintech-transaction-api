package com.fintech.repository;

import com.fintech.model.Transaction;
import com.fintech.model.TransactionStatus;
import com.fintech.model.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReferenceId(String referenceId);

    Page<Transaction> findBySenderOrReceiver(User sender, User receiver, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sender_id = :senderId AND status = 'COMPLETED' AND created_at >= :since", nativeQuery = true)
    BigDecimal getDailySpend(@Param("senderId") UUID senderId, @Param("since") LocalDateTime since);

    long countBySenderAndCreatedAtAfter(User sender, LocalDateTime since);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime before);
}
