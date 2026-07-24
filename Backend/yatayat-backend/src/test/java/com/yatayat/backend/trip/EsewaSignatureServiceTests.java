package com.yatayat.backend.trip;

import com.yatayat.backend.payment.EsewaSignatureService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EsewaSignatureServiceTests {
    private final EsewaSignatureService signatures = new EsewaSignatureService();

    @Test
    void generatedSignatureValidatesAndTamperingFails() {
        String message = "total_amount=100.00,transaction_uuid=TEST-001,product_code=EPAYTEST";
        String signature = signatures.sign(message, "unit-test-secret");

        assertThat(signatures.verify(message, signature, "unit-test-secret")).isTrue();
        assertThat(signatures.verify(message + "0", signature, "unit-test-secret")).isFalse();
        assertThat(signatures.verify(message, "not-base64", "unit-test-secret")).isFalse();
    }
}
