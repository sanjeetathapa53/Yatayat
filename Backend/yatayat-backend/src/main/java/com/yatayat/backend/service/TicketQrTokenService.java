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
public class TicketQrTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final String secret;

    public TicketQrTokenService(@Value("${yatayat.tickets.qr-secret:}") String secret) {
        this.secret = secret == null ? "" : secret.trim();
    }

    @PostConstruct
    void validateConfiguration() {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "Ticket QR secret must contain at least 32 characters.");
        }
    }

    public String rawToken(String ticketNumber) {
        if (ticketNumber == null || ticketNumber.isBlank()) {
            throw new IllegalArgumentException("Ticket number is required.");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(ticketNumber.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate secure QR token.", exception);
        }
    }

    public String storedHash(String ticketNumber) {
        return sha256(rawToken(ticketNumber));
    }

    public String payloadToken(String ticketNumber, String storedHash) {
        String rawToken = rawToken(ticketNumber);
        return constantTimeEquals(sha256(rawToken), storedHash)
                ? rawToken
                : storedHash;
    }

    public boolean matches(String ticketNumber, String presentedToken, String storedHash) {
        if (presentedToken == null || storedHash == null) return false;
        if (constantTimeEquals(sha256(presentedToken), storedHash)) return true;

        // Legacy compatibility: tickets issued before secure raw-token storage put the
        // stored hash itself in the QR payload. New tickets never use this format.
        return !constantTimeEquals(storedHash(ticketNumber), storedHash)
                && constantTimeEquals(presentedToken, storedHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash QR token.", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
