package com.fintech.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class EncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    private SecretKey secretKey;
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 128;
    private final SecureRandom rng = new SecureRandom();

    @PostConstruct
    public void init() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        this.secretKey = new SecretKeySpec(hash, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) throw new IllegalArgumentException("nothing to encrypt");
        try {
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("encrypt failed", e);
            throw new RuntimeException("encrypt failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_LEN];
            byte[] encrypted = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            System.arraycopy(combined, IV_LEN, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("decrypt failed", e);
            throw new RuntimeException("decrypt failed", e);
        }
    }

    public String lastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
