package com.yatayat.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketQrTokenServiceTests {
    private static final String SECRET =
            "test-only-ticket-qr-secret-at-least-32-characters";

    @Test
    void newTokenIsRawInPayloadButOnlyHashMatchesStoredValue() {
        TicketQrTokenService service = new TicketQrTokenService(SECRET);
        String ticketNumber = "YT-TKT-SECURE-001";
        String raw = service.rawToken(ticketNumber);
        String storedHash = service.storedHash(ticketNumber);

        assertThat(raw).isNotEqualTo(storedHash);
        assertThat(storedHash).hasSize(64);
        assertThat(service.payloadToken(ticketNumber, storedHash)).isEqualTo(raw);
        assertThat(service.matches(ticketNumber, raw, storedHash)).isTrue();
        assertThat(service.matches(ticketNumber, raw + "altered", storedHash)).isFalse();
        assertThat(service.matches(ticketNumber, storedHash, storedHash)).isFalse();
    }

    @Test
    void legacyDirectStoredValueComparisonIsCompatibilityOnly() {
        TicketQrTokenService service = new TicketQrTokenService(SECRET);
        String ticketNumber = "YT-TKT-LEGACY-001";
        String legacyStoredValue = "a".repeat(64);

        assertThat(service.payloadToken(ticketNumber, legacyStoredValue))
                .isEqualTo(legacyStoredValue);
        assertThat(service.matches(ticketNumber, legacyStoredValue, legacyStoredValue)).isTrue();
    }

    @Test
    void shortSecretIsRejected() {
        TicketQrTokenService service = new TicketQrTokenService("too-short");
        assertThatThrownBy(service::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }
}
