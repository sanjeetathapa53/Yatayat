package com.yatayat.backend.service;

import com.yatayat.backend.config.OtpProperties;
import com.yatayat.backend.entity.OtpPurpose;
import com.yatayat.backend.entity.OtpVerification;
import com.yatayat.backend.entity.OtpVerificationStatus;
import com.yatayat.backend.repository.OtpVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class OtpVerificationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{6}$");
    private final SecureRandom secureRandom = new SecureRandom();
    private final OtpVerificationRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpProperties properties;

    public OtpVerificationService(OtpVerificationRepository repository,
                                  PasswordEncoder passwordEncoder,
                                  EmailService emailService,
                                  OtpProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void issue(String rawEmail, OtpPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        LocalDateTime now = LocalDateTime.now();
        OtpVerification verification = repository.findForUpdate(email, purpose)
                .orElseGet(() -> newVerification(email, purpose, now));

        if (verification.getLastRequestedAt() != null
                && now.isBefore(verification.getLastRequestedAt()
                .plus(properties.getResendCooldown()))) {
            throw failure(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait before requesting another OTP.");
        }
        if (verification.getRequestWindowStartedAt() == null
                || !now.isBefore(verification.getRequestWindowStartedAt()
                .plus(properties.getRequestWindow()))) {
            verification.setRequestWindowStartedAt(now);
            verification.setRequestCount(0);
        }
        if (verification.getRequestCount() >= properties.getMaximumRequestsPerWindow()) {
            throw failure(HttpStatus.TOO_MANY_REQUESTS,
                    "OTP request limit exceeded. Please try again later.");
        }

        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setStatus(OtpVerificationStatus.ISSUED);
        verification.setIssuedAt(now);
        verification.setExpiresAt(now.plus(properties.getExpiry()));
        verification.setVerifiedAt(null);
        verification.setConsumedAt(null);
        verification.setAttemptCount(0);
        verification.setLastRequestedAt(now);
        verification.setRequestCount(verification.getRequestCount() + 1);
        verification.setUpdatedAt(now);
        repository.saveAndFlush(verification);
        emailService.sendOtpEmail(email, otp);
    }

    @Transactional
    public void verify(String rawEmail, String rawOtp, OtpPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        String otp = validateOtp(rawOtp);
        OtpVerification verification = locked(email, purpose);
        LocalDateTime now = LocalDateTime.now();
        requireUsableIssuedState(verification, now);

        if (!passwordEncoder.matches(otp, verification.getOtpHash())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            verification.setUpdatedAt(now);
            if (verification.getAttemptCount() >= properties.getMaximumAttempts()) {
                verification.setStatus(OtpVerificationStatus.LOCKED);
                repository.saveAndFlush(verification);
                throw failure(HttpStatus.TOO_MANY_REQUESTS,
                        "Maximum OTP verification attempts exceeded.");
            }
            repository.saveAndFlush(verification);
            throw failure(HttpStatus.BAD_REQUEST, "Incorrect OTP.");
        }

        verification.setStatus(OtpVerificationStatus.VERIFIED);
        verification.setVerifiedAt(now);
        verification.setOtpHash(null);
        verification.setUpdatedAt(now);
        repository.saveAndFlush(verification);
    }

    @Transactional
    public void consumeVerifiedRegistration(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        OtpVerification verification = locked(email, OtpPurpose.REGISTRATION);
        LocalDateTime now = LocalDateTime.now();
        if (verification.getStatus() == OtpVerificationStatus.CONSUMED) {
            throw failure(HttpStatus.CONFLICT, "Email verification has already been used.");
        }
        if (verification.getStatus() != OtpVerificationStatus.VERIFIED
                || verification.getVerifiedAt() == null) {
            throw failure(HttpStatus.FORBIDDEN,
                    "Verify the registration OTP before creating an account.");
        }
        if (verification.getExpiresAt() == null || !now.isBefore(verification.getExpiresAt())) {
            throw failure(HttpStatus.GONE, "OTP verification has expired.");
        }
        consume(verification, now);
    }

    @Transactional
    public void verifyAndConsume(String rawEmail, String rawOtp, OtpPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        String otp = validateOtp(rawOtp);
        OtpVerification verification = locked(email, purpose);
        LocalDateTime now = LocalDateTime.now();
        requireUsableIssuedState(verification, now);
        if (!passwordEncoder.matches(otp, verification.getOtpHash())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            verification.setUpdatedAt(now);
            if (verification.getAttemptCount() >= properties.getMaximumAttempts()) {
                verification.setStatus(OtpVerificationStatus.LOCKED);
                repository.saveAndFlush(verification);
                throw failure(HttpStatus.TOO_MANY_REQUESTS,
                        "Maximum OTP verification attempts exceeded.");
            }
            repository.saveAndFlush(verification);
            throw failure(HttpStatus.BAD_REQUEST, "Incorrect OTP.");
        }
        consume(verification, now);
    }

    public String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw failure(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw failure(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }
        return email;
    }

    private String validateOtp(String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) {
            throw failure(HttpStatus.BAD_REQUEST, "OTP is required.");
        }
        String otp = rawOtp.trim();
        if (!OTP_PATTERN.matcher(otp).matches()) {
            throw failure(HttpStatus.BAD_REQUEST, "OTP must contain exactly 6 digits.");
        }
        return otp;
    }

    private OtpVerification locked(String email, OtpPurpose purpose) {
        return repository.findForUpdate(email, purpose)
                .orElseThrow(() -> failure(HttpStatus.FORBIDDEN,
                        "OTP verification is required."));
    }

    private void requireUsableIssuedState(OtpVerification verification, LocalDateTime now) {
        if (verification.getStatus() == OtpVerificationStatus.CONSUMED) {
            throw failure(HttpStatus.CONFLICT, "OTP has already been used.");
        }
        if (verification.getStatus() == OtpVerificationStatus.LOCKED
                || verification.getAttemptCount() >= properties.getMaximumAttempts()) {
            throw failure(HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP verification attempts exceeded.");
        }
        if (verification.getStatus() != OtpVerificationStatus.ISSUED
                || verification.getOtpHash() == null) {
            throw failure(HttpStatus.CONFLICT, "OTP has already been verified.");
        }
        if (verification.getExpiresAt() == null || !now.isBefore(verification.getExpiresAt())) {
            throw failure(HttpStatus.GONE, "OTP has expired. Request a new OTP.");
        }
    }

    private OtpVerification newVerification(String email, OtpPurpose purpose, LocalDateTime now) {
        OtpVerification verification = new OtpVerification();
        verification.setNormalizedEmail(email);
        verification.setPurpose(purpose);
        verification.setCreatedAt(now);
        verification.setRequestWindowStartedAt(now);
        return verification;
    }

    private void consume(OtpVerification verification, LocalDateTime now) {
        verification.setStatus(OtpVerificationStatus.CONSUMED);
        verification.setConsumedAt(now);
        verification.setOtpHash(null);
        verification.setUpdatedAt(now);
        repository.saveAndFlush(verification);
    }

    private ResponseStatusException failure(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
