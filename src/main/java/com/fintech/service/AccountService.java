package com.fintech.service;

import com.fintech.exception.ResourceNotFoundException;
import com.fintech.model.*;
import com.fintech.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Cacheable(value = "bal", key = "#user.id")
    public BigDecimal getBalance(User user) {
        Account account = accountRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("account not found"));
        return account.getBalance();
    }

    @CacheEvict(value = "bal", key = "#user.id")
    public void evictBalanceCache(User user) {}

    public String getAccountCurrency(User user) { return "USD"; }
}
