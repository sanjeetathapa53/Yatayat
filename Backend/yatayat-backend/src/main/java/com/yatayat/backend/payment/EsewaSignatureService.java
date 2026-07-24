package com.yatayat.backend.payment;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class EsewaSignatureService {
    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String message, String secretKey) {
        if (message == null || secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("eSewa signature input is invalid.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate eSewa signature.", exception);
        }
    }

    public boolean verify(String message, String suppliedSignature, String secretKey) {
        if (suppliedSignature == null || suppliedSignature.isBlank()) return false;
        try {
            byte[] expected = Base64.getDecoder().decode(sign(message, secretKey));
            byte[] supplied = Base64.getDecoder().decode(suppliedSignature.trim());
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
