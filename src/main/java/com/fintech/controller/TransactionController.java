package com.fintech.controller;

import com.fintech.dto.*;
import com.fintech.model.User;
import com.fintech.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {

        String ip = resolveIp(httpRequest);
        TransactionResponse response = transactionService.createTransaction(user, request, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("transaction created", response));
    }

    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<ApiResponse<TransactionResponse>>> createTransactionAsync(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveIp(httpRequest);
        return transactionService.createTransactionAsync(user, request, ip)
            .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("transaction created", response)));
    }

    @GetMapping("/{referenceId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable String referenceId,
            @AuthenticationPrincipal User user) {

        TransactionResponse response = transactionService.getTransaction(referenceId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getUserTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        PagedResponse<TransactionResponse> response = transactionService.getUserTransactions(user, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
