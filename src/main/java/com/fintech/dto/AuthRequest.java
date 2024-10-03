package com.fintech.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "email cannot be blank")
    @Email
    private String email;

    @NotBlank(message = "password cannot be blank")
    private String password;
}
