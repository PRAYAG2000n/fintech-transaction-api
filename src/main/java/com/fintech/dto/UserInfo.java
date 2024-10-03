package com.fintech.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String accountNumber;
    private String role;
}
