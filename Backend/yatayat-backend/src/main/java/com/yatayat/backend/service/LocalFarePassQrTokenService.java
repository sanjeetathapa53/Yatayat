package com.yatayat.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class LocalFarePassQrTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final String secret;

    public LocalFarePassQrTokenService(
            @Value("${yatayat.local-fare-pass.qr-secret:${yatayat.tickets.qr-secret:}}") String secret) {
        this.secret = secret == null ? "" : secret.trim();
    }

    @PostConstruct
    void validateConfiguration() {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "Local fare-pass QR secret must contain at least 32 characters.");
        }
    }

    public String rawToken(String passNumber) {
        if (passNumber == null || passNumber.isBlank()) {
            throw new IllegalArgumentException("Pass number is required.");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(passNumber.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate secure local fare-pass token.", exception);
        }
    }

    public String storedHash(String passNumber) {
        return sha256(rawToken(passNumber));
    }

    public boolean matches(String presentedToken, String storedHash) {
        return presentedToken != null && storedHash != null
                && constantTimeEquals(sha256(presentedToken), storedHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash local fare-pass token.", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
