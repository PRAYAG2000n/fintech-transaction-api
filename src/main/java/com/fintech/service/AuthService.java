package com.fintech.service;

import com.fintech.dto.*;
import com.fintech.exception.*;
import com.fintech.model.*;
import com.fintech.repository.*;
import com.fintech.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    // TODO: track failed login attempts, lock after 5

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("email already registered");

        User user = User.builder()
            .email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName()).lastName(request.getLastName())
            .role(User.Role.USER).enabled(true).build();
        user = userRepository.save(user);

        Account account = Account.builder()
            .user(user).balance(BigDecimal.valueOf(1000))
            .currency("USD").isActive(true).build();
        accountRepository.save(account);

        log.info("registered {}", user.getEmail());
        return buildAuthPayload(user);
    }

    public AuthResponse login(AuthRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        log.info("login {}", user.getEmail());
        return buildAuthPayload(user);
    }

    private AuthResponse buildAuthPayload(User user) {
        return AuthResponse.builder()
            .accessToken(jwtService.issueToken(user)).refreshToken(jwtService.issueRefreshToken(user))
            .type("Bearer").expiresInMs(jwtService.getExpirationTime())
            .user(toUserInfo(user)).build();
    }

    private UserInfo toUserInfo(User user) {
        return UserInfo.builder()
            .id(user.getId()).email(user.getEmail())
            .firstName(user.getFirstName()).lastName(user.getLastName())
            .accountNumber(user.getAccountNumber()).role(user.getRole().name()).build();
    }
}
