package com.fintech.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private String requestId;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data)
            .requestId(UUID.randomUUID().toString().substring(0, 8)).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(null, data);
    }

    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder().success(false).error(error)
            .requestId(UUID.randomUUID().toString().substring(0, 8)).timestamp(LocalDateTime.now()).build();
    }
}
