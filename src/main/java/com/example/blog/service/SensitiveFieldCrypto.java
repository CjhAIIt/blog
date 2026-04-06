package com.example.blog.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SensitiveFieldCrypto {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static SecretKeySpec secretKey;

    @Value("${app.security.field-encryption-secret}")
    private String secret;

    @PostConstruct
    public void init() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化敏感字段加密失败", e);
        }
    }

    public static String encrypt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    public static String decrypt(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段解密失败", e);
        }
    }
}
