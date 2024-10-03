package com.fintech.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "receiver account is required")
    @JsonProperty("receiver_account")
    private String receiverAccount;

    @NotNull
    @DecimalMin(value = "1.00", message = "minimum transfer amount is 1.00")
    @DecimalMax(value = "1000000")
    private BigDecimal amount;

    @NotBlank @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private TransactionType transactionType;

    @Size(max = 500)
    private String description;

    private String idempotencyKey;

    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
}
