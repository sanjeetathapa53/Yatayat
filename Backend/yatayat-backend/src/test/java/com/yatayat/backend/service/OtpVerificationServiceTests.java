package com.yatayat.backend.service;

import com.yatayat.backend.config.OtpProperties;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpVerificationServiceTests {
    @Mock OtpVerificationRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    OtpProperties properties;
    OtpVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new OtpProperties();
        properties.setResendCooldown(Duration.ZERO);
        lenient().when(passwordEncoder.encode(anyString()))
                .thenAnswer(call -> "HASH:" + call.getArgument(0));
        lenient().when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(call -> ("HASH:" + call.getArgument(0)).equals(call.getArgument(1)));
        service = new OtpVerificationService(repository, passwordEncoder, emailService, properties);
    }

    @Test
    void secureSixDigitOtpIsHashedAndNewerOtpInvalidatesPrevious() {
        when(repository.findForUpdate("user@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());
        ArgumentCaptor<OtpVerification> record = ArgumentCaptor.forClass(OtpVerification.class);
        ArgumentCaptor<String> sentOtp = ArgumentCaptor.forClass(String.class);

        service.issue(" User@Example.com ", OtpPurpose.REGISTRATION);

        verify(repository).saveAndFlush(record.capture());
        verify(emailService).sendOtpEmail(eq("user@example.com"), sentOtp.capture());
        assertThat(sentOtp.getValue()).matches("\\d{6}");
        assertThat(record.getValue().getOtpHash()).isEqualTo("HASH:" + sentOtp.getValue());
        assertThat(record.getValue().getOtpHash()).isNotEqualTo(sentOtp.getValue());

        String oldOtp = sentOtp.getValue();
        OtpVerification existing = record.getValue();
        clearInvocations(repository, emailService);
        when(repository.findForUpdate("user@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(existing));
        service.issue("user@example.com", OtpPurpose.REGISTRATION);
        ArgumentCaptor<String> newOtp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("user@example.com"), newOtp.capture());
        assertThat(existing.getOtpHash()).isNotEqualTo("HASH:" + oldOtp);
    }

    @Test
    void incorrectExpiredAndPurposeSeparatedOtpAreRejected() {
        OtpVerification registration = issued(OtpPurpose.REGISTRATION, "123456");
        when(repository.findForUpdate("user@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(registration));
        assertThatThrownBy(() -> service.verify(
                "user@example.com", "654321", OtpPurpose.REGISTRATION))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incorrect OTP");

        registration.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.verify(
                "user@example.com", "123456", OtpPurpose.REGISTRATION))
                .hasMessageContaining("expired");

        when(repository.findForUpdate("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyAndConsume(
                "user@example.com", "123456", OtpPurpose.PASSWORD_RESET))
                .hasMessageContaining("verification is required");
    }

    @Test
    void maximumAttemptsAndResendControlsAreEnforced() {
        OtpVerification verification = issued(OtpPurpose.REGISTRATION, "123456");
        when(repository.findForUpdate("user@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(verification));
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.verify(
                    "user@example.com", "000000", OtpPurpose.REGISTRATION));
        }
        assertThatThrownBy(() -> service.verify(
                "user@example.com", "000000", OtpPurpose.REGISTRATION))
                .hasMessageContaining("Maximum");
        assertThat(verification.getStatus()).isEqualTo(OtpVerificationStatus.LOCKED);

        properties.setResendCooldown(Duration.ofSeconds(60));
        OtpVerification recent = issued(OtpPurpose.PASSWORD_RESET, "123456");
        recent.setLastRequestedAt(LocalDateTime.now());
        when(repository.findForUpdate("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(recent));
        assertThatThrownBy(() -> service.issue(
                "user@example.com", OtpPurpose.PASSWORD_RESET))
                .hasMessageContaining("wait");

        properties.setResendCooldown(Duration.ZERO);
        recent.setRequestCount(properties.getMaximumRequestsPerWindow());
        assertThatThrownBy(() -> service.issue(
                "user@example.com", OtpPurpose.PASSWORD_RESET))
                .hasMessageContaining("request limit");
    }

    @Test
    void verifiedRegistrationIsOneTimeAndMustMatchExactEmail() {
        OtpVerification verification = issued(OtpPurpose.REGISTRATION, "123456");
        when(repository.findForUpdate("user@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(verification));
        service.verify("user@example.com", "123456", OtpPurpose.REGISTRATION);
        assertThat(verification.getStatus()).isEqualTo(OtpVerificationStatus.VERIFIED);
        assertThat(verification.getOtpHash()).isNull();
        service.consumeVerifiedRegistration("user@example.com");
        assertThat(verification.getStatus()).isEqualTo(OtpVerificationStatus.CONSUMED);
        assertThatThrownBy(() -> service.consumeVerifiedRegistration("user@example.com"))
                .hasMessageContaining("already been used");

        when(repository.findForUpdate("other@example.com", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consumeVerifiedRegistration("other@example.com"))
                .hasMessageContaining("verification is required");
    }

    @Test
    void malformedInputsAreControlled() {
        assertThatThrownBy(() -> service.normalizeEmail(null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.normalizeEmail("not-an-email"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.verify(
                "user@example.com", null, OtpPurpose.REGISTRATION))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.verify(
                "user@example.com", "123", OtpPurpose.REGISTRATION))
                .isInstanceOf(ResponseStatusException.class);
    }

    private OtpVerification issued(OtpPurpose purpose, String otp) {
        OtpVerification verification = new OtpVerification();
        verification.setNormalizedEmail("user@example.com");
        verification.setPurpose(purpose);
        verification.setOtpHash("HASH:" + otp);
        verification.setStatus(OtpVerificationStatus.ISSUED);
        verification.setIssuedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setCreatedAt(LocalDateTime.now());
        verification.setUpdatedAt(LocalDateTime.now());
        verification.setRequestWindowStartedAt(LocalDateTime.now());
        return verification;
    }
}
