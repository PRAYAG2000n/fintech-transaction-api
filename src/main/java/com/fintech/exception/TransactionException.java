package com.fintech.exception;

public class TransactionException extends RuntimeException {

    private final ErrorCode code;

    public enum ErrorCode { INVALID, INSUFFICIENT_FUNDS, LIMIT_EXCEEDED, SELF_TRANSFER }

    public TransactionException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() { return code; }
}
