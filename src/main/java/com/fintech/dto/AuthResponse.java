package com.fintech.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String type;
    private Long expiresInMs;
    private UserInfo user;
}
