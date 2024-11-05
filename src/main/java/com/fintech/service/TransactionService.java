package com.fintech.service;

import com.fintech.dto.*;
import com.fintech.exception.*;
import com.fintech.model.*;
import com.fintech.repository.*;
import com.fintech.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;
    private final EncryptionService encryptionService;
    private final AccountService accountService;

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    @Transactional
    public TransactionResponse createTransaction(User sender, TransactionRequest request, String ipAddress) {
        log.info("txn {} -> {}", sender.getAccountNumber(), request.getReceiverAccount());

        User receiver = userRepo.findByAccountNumber(request.getReceiverAccount())
            .orElseThrow(() -> new ResourceNotFoundException("receiver account not found"));

        if (sender.getId().equals(receiver.getId()))
            throw new TransactionException(TransactionException.ErrorCode.SELF_TRANSFER, "cannot transfer to yourself");

        if (request.getAmount().compareTo(BigDecimal.ONE) < 0)
            throw new TransactionException(TransactionException.ErrorCode.INVALID, "minimum transfer is $1.00");

        checkDailyLimit(sender, request.getAmount());

        BigDecimal fee = calculateFee(request.getAmount(), request.getTransactionType());
        BigDecimal totalAmount = request.getAmount().add(fee);

        Account senderAcct = accountRepo.findByUserWithLock(sender)
            .orElseThrow(() -> new ResourceNotFoundException("sender account not found"));
        Account receiverAcct = accountRepo.findByUserWithLock(receiver)
            .orElseThrow(() -> new ResourceNotFoundException("receiver account not found"));

        if (senderAcct.getBalance().compareTo(totalAmount) < 0)
            throw new TransactionException(TransactionException.ErrorCode.INSUFFICIENT_FUNDS, "insufficient balance");

        log.debug("pre-transfer: sender={}, receiver={}", senderAcct.getBalance(), receiverAcct.getBalance());

        Transaction transaction = Transaction.builder()
            .sender(sender).receiver(receiver).amount(request.getAmount())
            .currency(request.getCurrency()).transactionType(request.getTransactionType())
            .description(request.getDescription()).fee(fee)
            .status(TransactionStatus.PROCESSING).ipAddress(ipAddress).build();

        if (request.getCardNumber() != null) {
            transaction.setEncryptedCardNumber(encryptionService.encrypt(request.getCardNumber()));
            transaction.setCardLastFour(encryptionService.lastFour(request.getCardNumber()));
        }

        transaction = transactionRepo.save(transaction);

        try {
            senderAcct.setBalance(senderAcct.getBalance().subtract(totalAmount));
            receiverAcct.setBalance(receiverAcct.getBalance().add(request.getAmount()));
            accountRepo.save(senderAcct);
            accountRepo.save(receiverAcct);
            accountService.evictBalanceCache(sender);
            accountService.evictBalanceCache(receiver);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            log.info("completed {}", transaction.getReferenceId());
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            log.warn("concurrent modification on {}", transaction.getReferenceId());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("concurrent update conflict");
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            log.error("failed {}: {}", transaction.getReferenceId(), e.getMessage());
        }

        return toDto(transactionRepo.save(transaction));
    }

    @Async
    public CompletableFuture<TransactionResponse> createTransactionAsync(User sender, TransactionRequest request, String ip) {
        return CompletableFuture.completedFuture(createTransaction(sender, request, ip));
    }

    public TransactionResponse getTransaction(String referenceId, User user) {
        Transaction txn = transactionRepo.findByReferenceId(referenceId)
            .orElseThrow(() -> new ResourceNotFoundException("transaction not found"));

        if (!txn.getSender().getId().equals(user.getId()) && !txn.getReceiver().getId().equals(user.getId()))
            throw new ResourceNotFoundException("transaction not found");

        return toDto(txn);
    }

    public PagedResponse<TransactionResponse> getUserTransactions(User user, int page, int size) {
        Page<Transaction> txns = transactionRepo.findBySenderOrReceiver(user, user, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.<TransactionResponse>builder()
            .content(txns.getContent().stream().map(this::toDto).toList())
            .page(page).size(size).totalElements(txns.getTotalElements())
            .totalPages(txns.getTotalPages()).last(txns.isLast()).build();
    }

    private BigDecimal calculateFee(BigDecimal amount, TransactionType type) {
        if (type == TransactionType.DEPOSIT) return BigDecimal.ZERO;
        return amount.multiply(FEE_RATE);
    }

    private void checkDailyLimit(User user, BigDecimal amount) {
        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        BigDecimal spent = transactionRepo.getDailySpend(user.getId(), dayStart);
        if (spent.add(amount).compareTo(DAILY_LIMIT) > 0)
            throw new TransactionException(TransactionException.ErrorCode.LIMIT_EXCEEDED, "daily limit exceeded");
    }

    private TransactionResponse toDto(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId()).referenceId(t.getReferenceId())
            .senderAccount(t.getSender().getAccountNumber())
            .receiverAccount(t.getReceiver().getAccountNumber())
            .amount(t.getAmount()).currency(t.getCurrency())
            .status(t.getStatus()).transactionType(t.getTransactionType())
            .description(t.getDescription()).fee(t.getFee())
            .createdAt(t.getCreatedAt()).completedAt(t.getCompletedAt()).build();
    }
}
