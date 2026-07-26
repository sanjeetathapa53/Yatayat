package com.yatayat.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class WalletTopUpService {
    private static final String ESEWA_SIGNED_FIELDS =
            "total_amount,transaction_uuid,product_code";
    private static final Set<String> REQUIRED_CALLBACK_FIELDS = Set.of(
            "transaction_code", "status", "total_amount", "transaction_uuid",
            "product_code", "signed_field_names");
    private static final Set<String> ALLOWED_CALLBACK_FIELDS = REQUIRED_CALLBACK_FIELDS;
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository users;
    private final WalletRepository wallets;
    private final WalletTopUpRepository topUps;
    private final WalletTransactionRepository transactions;
    private final KhaltiGateway khalti;
    private final KhaltiProperties khaltiProperties;
    private final EsewaGateway esewa;
    private final EsewaProperties esewaProperties;
    private final EsewaSignatureService signatures;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final BigDecimal minimumAmount;
    private final BigDecimal maximumAmount;

    public WalletTopUpService(
            UserRepository users, WalletRepository wallets, WalletTopUpRepository topUps,
            WalletTransactionRepository transactions, KhaltiGateway khalti,
            KhaltiProperties khaltiProperties, EsewaGateway esewa,
            EsewaProperties esewaProperties, EsewaSignatureService signatures,
            ObjectMapper objectMapper, NotificationService notificationService,
            @Value("${yatayat.wallet.topup.minimum-amount:100.00}") BigDecimal minimumAmount,
            @Value("${yatayat.wallet.topup.maximum-amount:50000.00}") BigDecimal maximumAmount) {
        this.users = users;
        this.wallets = wallets;
        this.topUps = topUps;
        this.transactions = transactions;
        this.khalti = khalti;
        this.khaltiProperties = khaltiProperties;
        this.esewa = esewa;
        this.esewaProperties = esewaProperties;
        this.signatures = signatures;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.minimumAmount = minimumAmount;
        this.maximumAmount = maximumAmount;
    }

    @Transactional
    public WalletTopUpInitiationResponse initiateKhalti(
            String email, CreateWalletTopUpRequest request) {
        requireProvider(khaltiProperties.isEnabled(), "Khalti");
        User passenger = requirePassenger(email);
        Wallet wallet = requireActiveWallet(passenger);
        BigDecimal amount = validateAmount(request);
        WalletTopUp existing = reusable(wallet, PaymentMethod.KHALTI, amount);
        if (existing != null && validKhaltiCheckout(existing)) return response(existing, Map.of());

        String reference = generateReference();
        long paisa = toPaisa(amount);
        String returnUrl = UriComponentsBuilder.fromUriString(khaltiProperties.getFrontendBaseUrl())
                .path("/wallet/topup/khalti/callback")
                .queryParam("topUpReference", reference).build().encode().toUriString();
        KhaltiGateway.InitiationResult result = khalti.initiate(new KhaltiGateway.InitiationRequest(
                returnUrl, khaltiProperties.getFrontendBaseUrl(), paisa, reference,
                "Yatayat Wallet Top-up", new KhaltiGateway.CustomerInfo(
                passenger.getFullName(), passenger.getEmail(), passenger.getPhone())));
        if (result == null || blank(result.pidx()) || !validKhaltiPaymentUrl(result.paymentUrl())) {
            throw badGateway("Khalti returned an invalid initiation response.");
        }
        WalletTopUp topUp = newTopUp(wallet, passenger, amount, PaymentMethod.KHALTI, reference);
        topUp.setProviderPaymentId(result.pidx());
        topUp.setProviderPaymentUrl(result.paymentUrl());
        topUp.setProviderExpiresAt(khaltiExpiry(result));
        topUps.saveAndFlush(topUp);
        return response(topUp, Map.of());
    }

    @Transactional
    public WalletTopUpInitiationResponse initiateEsewa(
            String email, CreateWalletTopUpRequest request) {
        requireProvider(esewaProperties.isEnabled(), "eSewa");
        User passenger = requirePassenger(email);
        Wallet wallet = requireActiveWallet(passenger);
        BigDecimal amount = validateAmount(request);
        WalletTopUp existing = reusable(wallet, PaymentMethod.ESEWA, amount);
        if (existing != null && existing.getProviderExpiresAt() != null
                && existing.getProviderExpiresAt().isAfter(LocalDateTime.now())) {
            return response(existing, esewaFields(existing));
        }
        String reference = generateReference();
        WalletTopUp topUp = newTopUp(wallet, passenger, amount, PaymentMethod.ESEWA, reference);
        topUp.setProviderPaymentId(generateEsewaUuid());
        topUp.setProviderPaymentUrl(esewaProperties.getFormUrl());
        topUp.setProviderExpiresAt(LocalDateTime.now().plusMinutes(30));
        topUps.saveAndFlush(topUp);
        return response(topUp, esewaFields(topUp));
    }

    @Transactional
    public WalletTopUpVerificationResponse verifyKhalti(
            String email, String reference, KhaltiPaymentVerificationRequest request) {
        requireProvider(khaltiProperties.isEnabled(), "Khalti");
        User passenger = requirePassenger(email);
        if (request == null || blank(request.pidx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khalti pidx is required.");
        }
        WalletTopUp topUp = ownedLocked(reference, passenger, PaymentMethod.KHALTI);
        if (topUp.getStatus() == PaymentStatus.SUCCESS) return verifiedResponse(topUp);
        if (!request.pidx().trim().equals(topUp.getProviderPaymentId())) {
            throw conflict("Khalti pidx does not match this wallet top-up.");
        }
        KhaltiGateway.LookupResult lookup = khalti.lookup(topUp.getProviderPaymentId());
        if (lookup == null || blank(lookup.pidx()) || blank(lookup.status())) {
            throw badGateway("Khalti returned an invalid lookup response.");
        }
        if (!topUp.getProviderPaymentId().equals(lookup.pidx())) {
            throw conflict("Khalti lookup pidx does not match the stored wallet top-up.");
        }
        if (Boolean.TRUE.equals(lookup.refunded())) {
            return nonSuccessful(topUp, PaymentStatus.REFUNDED, "Khalti reports this top-up as refunded.");
        }
        if (!"Completed".equals(lookup.status().trim())) {
            return switch (lookup.status().trim()) {
                case "Pending" -> nonSuccessful(topUp, PaymentStatus.PENDING, "Khalti top-up is pending.");
                case "Initiated" -> nonSuccessful(topUp, PaymentStatus.INITIATED, "Khalti top-up is still initiated.");
                case "User canceled" -> nonSuccessful(topUp, PaymentStatus.CANCELLED, "Khalti top-up was cancelled.");
                case "Expired" -> nonSuccessful(topUp, PaymentStatus.EXPIRED, "Khalti top-up expired.");
                case "Refunded" -> nonSuccessful(topUp, PaymentStatus.REFUNDED, "Khalti top-up was refunded.");
                default -> nonSuccessful(topUp, PaymentStatus.FAILED, "Khalti top-up was not completed.");
            };
        }
        if (lookup.totalAmount() == null || lookup.totalAmount() != toPaisa(topUp.getAmount())) {
            throw conflict("Khalti amount does not match the wallet top-up.");
        }
        if (blank(lookup.transactionId())) {
            throw conflict("Khalti did not return a transaction ID.");
        }
        return credit(topUp, lookup.transactionId());
    }

    @Transactional
    public WalletTopUpVerificationResponse verifyEsewa(
            String email, String reference, EsewaPaymentVerificationRequest request) {
        requireProvider(esewaProperties.isEnabled(), "eSewa");
        User passenger = requirePassenger(email);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eSewa verification information is required.");
        }
        VerifiedEsewaCallback callback = blank(request.data())
                ? null : verifyEsewaCallback(request.data().trim());
        String uuid = callback == null ? required(request.transactionUuid(),
                "eSewa transaction UUID is required when callback data is absent.")
                : callback.transactionUuid();
        WalletTopUp topUp = ownedLocked(reference, passenger, PaymentMethod.ESEWA);
        if (topUp.getStatus() == PaymentStatus.SUCCESS) return verifiedResponse(topUp);
        if (!uuid.equals(topUp.getProviderPaymentId())) {
            throw conflict("eSewa transaction UUID does not match this wallet top-up.");
        }
        if (callback != null && callback.amount().compareTo(topUp.getAmount()) != 0) {
            throw conflict("eSewa callback amount does not match the wallet top-up.");
        }
        if (callback != null && !"COMPLETE".equals(callback.status())) {
            throw conflict("eSewa callback does not report a completed payment.");
        }
        EsewaGateway.StatusResult lookup = esewa.lookup(
                esewaProperties.getProductCode().trim(), formatAmount(topUp.getAmount()), uuid);
        validateEsewaLookup(topUp, lookup);
        String status = lookup.status().trim().toUpperCase(Locale.ROOT);
        if (!"COMPLETE".equals(status)) {
            return switch (status) {
                case "PENDING", "AMBIGUOUS", "AMBIGIOUS" ->
                        nonSuccessful(topUp, PaymentStatus.PENDING, "eSewa top-up is pending.");
                case "CANCELED", "CANCELLED" ->
                        nonSuccessful(topUp, PaymentStatus.CANCELLED, "eSewa top-up was cancelled.");
                case "FULL_REFUND", "PARTIAL_REFUND", "REFUNDED" ->
                        nonSuccessful(topUp, PaymentStatus.REFUNDED, "eSewa top-up was refunded.");
                case "NOT_FOUND", "EXPIRED" ->
                        nonSuccessful(topUp, PaymentStatus.EXPIRED, "eSewa top-up was not found or expired.");
                default -> nonSuccessful(topUp, PaymentStatus.FAILED, "eSewa top-up was not completed.");
            };
        }
        if (callback == null) {
            throw conflict("Signed eSewa callback data is required to credit a wallet.");
        }
        if (blank(lookup.referenceId())) {
            throw conflict("eSewa did not return a provider reference ID.");
        }
        return credit(topUp, lookup.referenceId());
    }

    @Transactional
    public WalletTopUpVerificationResponse details(String email, String reference) {
        User passenger = requirePassenger(email);
        WalletTopUp topUp = topUps.findByTopUpReferenceAndPassengerId(
                        cleanReference(reference), passenger.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet top-up not found."));
        return verifiedResponse(topUp);
    }

    private WalletTopUpVerificationResponse credit(WalletTopUp topUp, String providerTransactionId) {
        if (topUp.getStatus() == PaymentStatus.SUCCESS) return verifiedResponse(topUp);
        Wallet wallet = wallets.findWithLockByUser(topUp.getPassenger())
                .orElseThrow(() -> conflict("Wallet not found."));
        requireActivated(wallet);
        if (topUp.getWalletTransaction() != null) {
            throw conflict("Wallet top-up was already applied.");
        }
        double balance = wallet.getBalance() == null ? 0.0 : wallet.getBalance();
        wallet.setBalance(balance + topUp.getAmount().doubleValue());
        LocalDateTime now = LocalDateTime.now();
        WalletTransaction transaction = transactions.save(new WalletTransaction(
                wallet, "TOPUP", topUp.getAmount().doubleValue(), "SUCCESS",
                topUp.getPaymentMethod().name()));
        topUp.setWallet(wallet);
        topUp.setWalletTransaction(transaction);
        topUp.setProviderTransactionId(providerTransactionId);
        topUp.setStatus(PaymentStatus.SUCCESS);
        topUp.setVerifiedAt(now);
        topUp.setCreditedAt(now);
        topUp.setFailureReason(null);
        wallets.save(wallet);
        topUps.saveAndFlush(topUp);
        notificationService.walletTopUpSuccessful(topUp);
        return verifiedResponse(topUp);
    }

    private WalletTopUpVerificationResponse nonSuccessful(
            WalletTopUp topUp, PaymentStatus status, String message) {
        topUp.setStatus(status);
        topUp.setFailureReason(message);
        topUps.saveAndFlush(topUp);
        return verifiedResponse(topUp);
    }

    private WalletTopUp reusable(Wallet wallet, PaymentMethod method, BigDecimal amount) {
        return topUps.findFirstByWalletAndPaymentMethodAndAmountAndStatusOrderByCreatedAtDesc(
                wallet, method, amount, PaymentStatus.INITIATED).orElse(null);
    }

    private WalletTopUp newTopUp(Wallet wallet, User passenger, BigDecimal amount,
                                 PaymentMethod method, String reference) {
        WalletTopUp topUp = new WalletTopUp();
        topUp.setWallet(wallet);
        topUp.setPassenger(passenger);
        topUp.setAmount(amount);
        topUp.setPaymentMethod(method);
        topUp.setStatus(PaymentStatus.INITIATED);
        topUp.setTopUpReference(reference);
        topUp.setInitiatedAt(LocalDateTime.now());
        return topUp;
    }

    private Map<String, String> esewaFields(WalletTopUp topUp) {
        String amount = formatAmount(topUp.getAmount());
        String uuid = topUp.getProviderPaymentId();
        String productCode = esewaProperties.getProductCode().trim();
        String message = "total_amount=" + amount + ",transaction_uuid=" + uuid
                + ",product_code=" + productCode;
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", amount);
        fields.put("tax_amount", "0");
        fields.put("total_amount", amount);
        fields.put("transaction_uuid", uuid);
        fields.put("product_code", productCode);
        fields.put("product_service_charge", "0");
        fields.put("product_delivery_charge", "0");
        fields.put("success_url", esewaCallback("success", topUp, false));
        fields.put("failure_url", esewaCallback("failure", topUp, true));
        fields.put("signed_field_names", ESEWA_SIGNED_FIELDS);
        fields.put("signature", signatures.sign(message, esewaProperties.getSecretKey()));
        return Collections.unmodifiableMap(fields);
    }

    private String esewaCallback(String outcome, WalletTopUp topUp, boolean includeUuid) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(esewaProperties.getFrontendBaseUrl())
                .path("/wallet/topup/esewa/callback")
                .pathSegment(outcome, topUp.getTopUpReference());
        if (includeUuid) builder.pathSegment(topUp.getProviderPaymentId());
        return builder.build().encode().toUriString();
    }

    private WalletTopUpInitiationResponse response(WalletTopUp topUp, Map<String, String> fields) {
        boolean esewaMethod = topUp.getPaymentMethod() == PaymentMethod.ESEWA;
        return new WalletTopUpInitiationResponse(
                topUp.getTopUpReference(), topUp.getPaymentMethod().name(), topUp.getAmount(),
                topUp.getStatus().name(), topUp.getInitiatedAt(), topUp.getProviderExpiresAt(),
                esewaMethod ? null : topUp.getProviderPaymentUrl(),
                esewaMethod ? esewaProperties.getFormUrl() : null, fields);
    }

    private WalletTopUpVerificationResponse verifiedResponse(WalletTopUp topUp) {
        Double balance = topUp.getWallet() == null ? null : topUp.getWallet().getBalance();
        boolean credited = topUp.getStatus() == PaymentStatus.SUCCESS
                && topUp.getWalletTransaction() != null;
        String message = credited ? "Wallet top-up verified and credited."
                : topUp.getFailureReason();
        return new WalletTopUpVerificationResponse(
                topUp.getTopUpReference(), topUp.getPaymentMethod().name(), topUp.getAmount(),
                topUp.getStatus().name(), credited, balance,
                topUp.getProviderTransactionId(), topUp.getVerifiedAt(), message);
    }

    private VerifiedEsewaCallback verifyEsewaCallback(String encoded) {
        if (encoded.length() > 16_384) throw conflict("eSewa callback data is too large.");
        try {
            JsonNode root = objectMapper.readTree(new String(
                    Base64.getDecoder().decode(encoded.replace(' ', '+')), StandardCharsets.UTF_8));
            String signedValue = requiredText(root, "signed_field_names");
            String[] names = Arrays.stream(signedValue.split(",")).map(String::trim)
                    .toArray(String[]::new);
            Set<String> nameSet = new HashSet<>(Arrays.asList(names));
            if (!nameSet.containsAll(REQUIRED_CALLBACK_FIELDS)
                    || nameSet.size() != names.length
                    || Arrays.stream(names).anyMatch(name -> !ALLOWED_CALLBACK_FIELDS.contains(name))) {
                throw conflict("eSewa callback signed fields are invalid.");
            }
            String canonical = Arrays.stream(names)
                    .map(name -> name + "=" + requiredText(root, name))
                    .reduce((left, right) -> left + "," + right).orElseThrow();
            if (!signatures.verify(canonical, requiredText(root, "signature"),
                    esewaProperties.getSecretKey())) {
                throw conflict("eSewa callback signature is invalid.");
            }
            String productCode = requiredText(root, "product_code");
            if (!esewaProperties.getProductCode().trim().equals(productCode)) {
                throw conflict("eSewa callback product code does not match.");
            }
            return new VerifiedEsewaCallback(requiredText(root, "transaction_uuid"),
                    requiredText(root, "status").toUpperCase(Locale.ROOT),
                    new BigDecimal(requiredText(root, "total_amount").replace(",", "")));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException | java.io.IOException exception) {
            throw conflict("eSewa callback data is invalid.");
        }
    }

    private void validateEsewaLookup(WalletTopUp topUp, EsewaGateway.StatusResult lookup) {
        if (lookup == null || blank(lookup.status())) {
            throw badGateway("eSewa returned an invalid status response.");
        }
        if (!blank(lookup.transactionUuid())
                && !topUp.getProviderPaymentId().equals(lookup.transactionUuid().trim())) {
            throw conflict("eSewa status transaction UUID does not match.");
        }
        if (!blank(lookup.productCode())
                && !esewaProperties.getProductCode().trim().equals(lookup.productCode().trim())) {
            throw conflict("eSewa status product code does not match.");
        }
        if (lookup.totalAmount() == null || lookup.totalAmount().compareTo(topUp.getAmount()) != 0) {
            throw conflict("eSewa status amount does not match the wallet top-up.");
        }
    }

    private User requirePassenger(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found."));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passenger access is required.");
        }
        return user;
    }

    private Wallet requireActiveWallet(User user) {
        Wallet wallet = wallets.findWithLockByUser(user)
                .orElseThrow(() -> conflict("Activate your wallet before topping up."));
        requireActivated(wallet);
        return wallet;
    }

    private void requireActivated(Wallet wallet) {
        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            throw conflict("Activate your wallet before topping up.");
        }
    }

    private WalletTopUp ownedLocked(String reference, User passenger, PaymentMethod method) {
        WalletTopUp topUp = topUps.findOwnedWithLock(cleanReference(reference), passenger.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wallet top-up not found."));
        if (topUp.getPaymentMethod() != method) {
            throw conflict("Wallet top-up provider does not match.");
        }
        return topUp;
    }

    private BigDecimal validateAmount(CreateWalletTopUpRequest request) {
        if (request == null || request.amount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up amount is required.");
        }
        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Top-up amount supports at most two decimal places.");
        }
        if (amount.signum() <= 0 || amount.compareTo(minimumAmount) < 0
                || amount.compareTo(maximumAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Top-up amount must be between NPR " + minimumAmount.toPlainString()
                            + " and NPR " + maximumAmount.toPlainString() + ".");
        }
        return amount;
    }

    private long toPaisa(BigDecimal amount) {
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw conflict("Top-up amount cannot be represented exactly in paisa.");
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String generateReference() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String value = "WTU-" + LocalDate.now().format(REFERENCE_DATE) + "-"
                    + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
            if (!topUps.existsByTopUpReference(value)) return value;
        }
        throw conflict("Wallet top-up reference could not be generated.");
    }

    private String generateEsewaUuid() {
        return LocalDate.now().format(REFERENCE_DATE) + "-"
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private LocalDateTime khaltiExpiry(KhaltiGateway.InitiationResult result) {
        if (!blank(result.expiresAt())) {
            try {
                return OffsetDateTime.parse(result.expiresAt())
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (DateTimeParseException ignored) { }
        }
        return LocalDateTime.now().plusSeconds(
                result.expiresIn() == null ? 1800 : Math.max(60, result.expiresIn()));
    }

    private boolean validKhaltiCheckout(WalletTopUp topUp) {
        return !blank(topUp.getProviderPaymentId())
                && validKhaltiPaymentUrl(topUp.getProviderPaymentUrl())
                && topUp.getProviderExpiresAt() != null
                && topUp.getProviderExpiresAt().isAfter(LocalDateTime.now());
    }

    private boolean validKhaltiPaymentUrl(String value) {
        if (blank(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "test-pay.khalti.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String cleanReference(String reference) {
        if (blank(reference)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet top-up not found.");
        }
        return reference.trim();
    }

    private String required(String value, String message) {
        if (blank(value)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return value.trim();
    }

    private String requiredText(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing callback field.");
        }
        return value.asText().trim();
    }

    private void requireProvider(boolean enabled, String provider) {
        if (!enabled) throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, provider + " payments are not configured.");
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException badGateway(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private record VerifiedEsewaCallback(
            String transactionUuid, String status, BigDecimal amount) {}
}
