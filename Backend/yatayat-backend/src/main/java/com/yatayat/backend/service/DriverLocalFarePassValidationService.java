package com.yatayat.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.DriverLocalFarePassValidationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverLocalFarePassValidationService {
    public static final int MAX_QR_PAYLOAD_LENGTH = 2048;

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final LocalServiceRunRepository runRepository;
    private final LocalFarePassRepository passRepository;
    private final LocalFarePassQrTokenService tokenService;
    private final NotificationService notificationService;

    public DriverLocalFarePassValidationService(
            ObjectMapper objectMapper, UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverOperatorAssociationRepository associationRepository,
            LocalServiceRunRepository runRepository, LocalFarePassRepository passRepository,
            LocalFarePassQrTokenService tokenService, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.associationRepository = associationRepository;
        this.runRepository = runRepository;
        this.passRepository = passRepository;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
    }

    @Transactional(dontRollbackOn = LocalPassValidationException.class)
    public DriverLocalFarePassValidationResponse validate(String email, String qrPayload) {
        DriverProfile driver = requireApprovedDriver(email);
        ParsedQr parsed = parse(qrPayload);
        LocalFarePass pass = passRepository.findByPassNumberForValidation(parsed.passNumber())
                .orElseThrow(() -> validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "This local fare pass could not be verified."));

        if (!tokenService.matches(parsed.token(), pass.getQrTokenHash())) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "This local fare pass could not be verified.");
        }
        if (pass.getStatus() == LocalFarePassStatus.USED) {
            throw validation(HttpStatus.CONFLICT, "ALREADY_USED",
                    "This local fare pass has already been used.");
        }
        if (pass.getStatus() == LocalFarePassStatus.CANCELLED) {
            throw validation(HttpStatus.CONFLICT, "CANCELLED",
                    "This local fare pass has been cancelled.");
        }
        if (pass.getStatus() == LocalFarePassStatus.EXPIRED) {
            throw validation(HttpStatus.CONFLICT, "EXPIRED",
                    "This local fare pass has expired.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(pass.getValidFrom())) {
            throw validation(HttpStatus.CONFLICT, "NOT_YET_VALID",
                    "This local fare pass is not valid yet.");
        }
        if (now.isAfter(pass.getValidUntil())) {
            pass.setStatus(LocalFarePassStatus.EXPIRED);
            passRepository.saveAndFlush(pass);
            throw validation(HttpStatus.CONFLICT, "EXPIRED",
                    "This local fare pass has expired.");
        }

        List<LocalServiceRun> activeRuns = runRepository
                .findByDriverAndStatusOrderByActualStartedAtDesc(
                        driver, LocalServiceRunStatus.IN_SERVICE);
        if (activeRuns.size() != 1) {
            throw validation(HttpStatus.CONFLICT, "NO_ACTIVE_LOCAL_SERVICE",
                    activeRuns.isEmpty()
                            ? "Start your assigned local service before scanning fare passes."
                            : "More than one active local service was found.");
        }
        LocalServiceRun run = activeRuns.get(0);
        requireAssociation(driver, run);
        if (!run.getRoute().getId().equals(pass.getRoute().getId())) {
            throw validation(HttpStatus.CONFLICT, "WRONG_ROUTE",
                    "This fare pass is for a different local route.");
        }

        pass.setStatus(LocalFarePassStatus.USED);
        pass.setUsedAt(now);
        pass.setValidatedByDriverProfile(driver);
        pass.setValidatedLocalServiceRun(run);
        passRepository.save(pass);
        notificationService.localFarePassUsed(pass);
        return response("VALID", "Local fare confirmed.", pass);
    }

    private DriverProfile requireApprovedDriver(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated driver not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver access is required.");
        }
        DriverProfile driver = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Approved driver profile is required."));
        if (driver.getVerificationStatus() != DriverVerificationStatus.APPROVED
                || driver.isLicenseExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Approved active driver profile is required.");
        }
        return driver;
    }

    private void requireAssociation(DriverProfile driver, LocalServiceRun run) {
        associationRepository.findByDriverAndOperator(driver, run.getOperator())
                .filter(value -> value.getStatus() == DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active operator association is required for this local service."));
    }

    private ParsedQr parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR", "QR payload is required.");
        }
        if (payload.length() > MAX_QR_PAYLOAD_LENGTH) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR", "QR payload is too large.");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.path("version").asInt(-1) != 1) {
                throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "Unsupported local fare-pass QR version.");
            }
            if (!"LOCAL_FARE_PASS".equals(text(root, "type"))) {
                throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "Unsupported QR type.");
            }
            String passNumber = text(root, "passNumber");
            String token = text(root, "token");
            if (passNumber.isBlank() || token.isBlank()) {
                throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "This local fare pass is missing required data.");
            }
            return new ParsedQr(passNumber, token);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "This local fare pass could not be read.");
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private DriverLocalFarePassValidationResponse response(
            String result, String message, LocalFarePass pass) {
        return new DriverLocalFarePassValidationResponse(
                result, message, pass.getPassNumber(), pass.getPassenger().getFullName(),
                pass.getBoardingStopName(), pass.getDestinationStopName(), pass.getFare(),
                pass.getUsedAt(), pass.getValidatedLocalServiceRun().getId()
        );
    }

    private LocalPassValidationException validation(
            HttpStatus status, String result, String message) {
        return new LocalPassValidationException(status, result + "|" + message);
    }

    private record ParsedQr(String passNumber, String token) {}

    static final class LocalPassValidationException extends ResponseStatusException {
        LocalPassValidationException(HttpStatus status, String reason) {
            super(status, reason);
        }
    }
}
