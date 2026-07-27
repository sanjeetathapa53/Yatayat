package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFarePassService {
    private static final DateTimeFormatter PASS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LocalFarePassRepository passRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocalFarePassQrTokenService tokenService;

    public LocalFarePassService(UserRepository userRepository, RouteRepository routeRepository,
                                RouteStopRepository routeStopRepository, WalletRepository walletRepository,
                                WalletTransactionRepository transactionRepository,
                                LocalFarePassRepository passRepository, PasswordEncoder passwordEncoder,
                                LocalFarePassQrTokenService tokenService) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passRepository = passRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LocalFareQuoteResponse quote(String email, LocalFareQuoteRequest request) {
        requirePassenger(email);
        return quote(request.routeId(), request.boardingStopId(), request.destinationStopId());
    }

    @Transactional
    public LocalFarePassResponse purchase(String email, LocalFarePassPurchaseRequest request) {
        User passenger = requirePassenger(email);
        LocalFareQuoteResponse quote = quote(
                request.routeId(), request.boardingStopId(), request.destinationStopId());
        Wallet wallet = walletRepository.findWithLockByUser(passenger)
                .orElseThrow(() -> conflict("Wallet is not active. Please activate your wallet first."));
        verifyPin(wallet, request.walletPin());

        double balance = wallet.getBalance() == null ? 0.0 : wallet.getBalance();
        if (BigDecimal.valueOf(balance).compareTo(quote.fare()) < 0) {
            throw conflict("Insufficient wallet balance.");
        }

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction transaction = new WalletTransaction(
                wallet, "LOCAL_FARE_PAYMENT", quote.fare().doubleValue(), "SUCCESS", "WALLET");
        wallet.setBalance(balance - quote.fare().doubleValue());
        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        LocalFarePass pass = new LocalFarePass();
        pass.setPassNumber(generatePassNumber());
        pass.setPassenger(passenger);
        Route route = routeRepository.findById(quote.routeId()).orElseThrow(this::routeNotFound);
        pass.setRoute(route);
        RouteStop boarding = findStop(route.getId(), quote.boardingStopId());
        RouteStop destination = findStop(route.getId(), quote.destinationStopId());
        pass.setBoardingStop(boarding.getBusStop());
        pass.setDestinationStop(destination.getBusStop());
        pass.setBoardingStopOrder(boarding.getStopOrder());
        pass.setDestinationStopOrder(destination.getStopOrder());
        pass.setBoardingStopName(boarding.getBusStop().getName());
        pass.setDestinationStopName(destination.getBusStop().getName());
        pass.setFare(quote.fare());
        pass.setStatus(LocalFarePassStatus.VALID);
        pass.setWalletTransaction(transaction);
        pass.setIssuedAt(now);
        pass.setValidFrom(now);
        pass.setValidUntil(now.plusHours(24));
        pass.setQrTokenHash(tokenService.storedHash(pass.getPassNumber()));

        try {
            return toResponse(passRepository.saveAndFlush(pass));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Local fare pass could not be issued. Please try again.");
        }
    }

    @Transactional
    public List<LocalFarePassResponse> list(String email) {
        User passenger = requirePassenger(email);
        LocalDateTime now = LocalDateTime.now();
        List<LocalFarePass> passes = passRepository.findByPassengerOrderByIssuedAtDesc(passenger);
        passes.forEach(pass -> expire(pass, now));
        return passes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public LocalFarePassResponse details(String email, String passNumber) {
        User passenger = requirePassenger(email);
        LocalFarePass pass = passRepository.findByPassNumberAndPassenger(
                        cleanPassNumber(passNumber), passenger)
                .orElseThrow(this::passNotFound);
        expire(pass, LocalDateTime.now());
        return toResponse(pass);
    }

    private LocalFareQuoteResponse quote(Long routeId, Long boardingStopId, Long destinationStopId) {
        if (routeId == null || boardingStopId == null || destinationStopId == null) {
            throw badRequest("Route, boarding stop and destination stop are required.");
        }
        Route route = routeRepository.findByIdAndStatusAndTripType(
                        routeId, RouteStatus.ACTIVE, TripType.LOCAL)
                .orElseThrow(this::routeNotFound);
        RouteStop boarding = findStop(routeId, boardingStopId);
        RouteStop destination = findStop(routeId, destinationStopId);
        if (destination.getStopOrder() <= boarding.getStopOrder()) {
            throw badRequest("Destination must come after the boarding stop.");
        }
        BigDecimal fare = destination.getCumulativeFare().subtract(boarding.getCumulativeFare());
        if (fare.signum() <= 0) throw conflict("Fare is not available for this route segment.");
        return new LocalFareQuoteResponse(
                route.getId(), route.getCode(), route.getName(),
                boarding.getBusStop().getId(), boarding.getBusStop().getName(), boarding.getStopOrder(),
                destination.getBusStop().getId(), destination.getBusStop().getName(), destination.getStopOrder(),
                fare
        );
    }

    private RouteStop findStop(Long routeId, Long busStopId) {
        return routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(routeId).stream()
                .filter(stop -> stop.getBusStop().isActive())
                .filter(stop -> stop.getBusStop().getId().equals(busStopId))
                .findFirst()
                .orElseThrow(() -> badRequest("Selected stop does not belong to this active route."));
    }

    private void verifyPin(Wallet wallet, String pin) {
        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            throw conflict("Wallet is not active. Please activate your wallet first.");
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw badRequest("Wallet PIN must be 4 digits.");
        }
        if (!passwordEncoder.matches(pin, wallet.getWalletPin())) {
            throw badRequest("Incorrect wallet PIN.");
        }
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated passenger not found."));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Passenger access is required.");
        }
        return user;
    }

    private void expire(LocalFarePass pass, LocalDateTime now) {
        if (pass.getStatus() == LocalFarePassStatus.VALID && now.isAfter(pass.getValidUntil())) {
            pass.setStatus(LocalFarePassStatus.EXPIRED);
            passRepository.save(pass);
        }
    }

    private LocalFarePassResponse toResponse(LocalFarePass pass) {
        return new LocalFarePassResponse(
                pass.getPassNumber(), pass.getRoute().getId(), pass.getRoute().getCode(),
                pass.getRoute().getName(), pass.getBoardingStop().getId(), pass.getBoardingStopName(),
                pass.getDestinationStop().getId(), pass.getDestinationStopName(), pass.getFare(),
                pass.getStatus().name(), pass.getIssuedAt(), pass.getValidFrom(), pass.getValidUntil(),
                pass.getUsedAt(), qrPayload(pass)
        );
    }

    private String qrPayload(LocalFarePass pass) {
        return """
                {"version":1,"type":"LOCAL_FARE_PASS","passNumber":"%s","token":"%s"}
                """.formatted(pass.getPassNumber(), tokenService.rawToken(pass.getPassNumber())).trim();
    }

    private String generatePassNumber() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String value = "YT-LFP-" + LocalDate.now().format(PASS_DATE) + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            if (!passRepository.existsByPassNumber(value)) return value;
        }
        throw conflict("Local fare-pass number could not be generated.");
    }

    private String cleanPassNumber(String value) {
        if (value == null || value.isBlank()) throw passNotFound();
        return value.trim();
    }

    private ResponseStatusException routeNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Active local route not found.");
    }
    private ResponseStatusException passNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Local fare pass not found.");
    }
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
