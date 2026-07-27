package com.yatayat.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFarePassQrTokenServiceTests {
    private static final String SECRET =
            "test-only-local-fare-pass-secret-at-least-32-characters";

    @Test
    void rawTokenIsUrlSafeAndOnlyItsHashIsStored() {
        LocalFarePassQrTokenService service = new LocalFarePassQrTokenService(SECRET);
        String raw = service.rawToken("YT-LFP-20260727-ABC123");
        String storedHash = service.storedHash("YT-LFP-20260727-ABC123");

        assertThat(raw).hasSizeGreaterThanOrEqualTo(43).doesNotContain("+", "/", "=");
        assertThat(storedHash).hasSize(64).isNotEqualTo(raw);
        assertThat(service.matches(raw, storedHash)).isTrue();
        assertThat(service.matches(raw + "tampered", storedHash)).isFalse();
    }

    @Test
    void tokensAreBoundToTheUniquePassNumber() {
        LocalFarePassQrTokenService service = new LocalFarePassQrTokenService(SECRET);

        assertThat(service.rawToken("YT-LFP-A"))
                .isNotEqualTo(service.rawToken("YT-LFP-B"));
    }

    @Test
    void shortSecretIsRejected() {
        LocalFarePassQrTokenService service = new LocalFarePassQrTokenService("too-short");
        assertThatThrownBy(service::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }
}
