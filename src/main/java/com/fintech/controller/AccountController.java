package com.fintech.controller;

import com.fintech.dto.ApiResponse;
import com.fintech.model.User;
import com.fintech.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(@AuthenticationPrincipal User user) {
        BigDecimal balance = accountService.getBalance(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("acctNo", user.getAccountNumber(), "balance", balance, "currency", "USD")));
    }
}
