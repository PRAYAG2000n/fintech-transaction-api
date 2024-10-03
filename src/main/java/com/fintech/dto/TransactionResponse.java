package com.fintech.dto;

import com.fintech.model.TransactionStatus;
import com.fintech.model.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private String referenceId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private TransactionType transactionType;
    private String description;
    private BigDecimal fee;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
