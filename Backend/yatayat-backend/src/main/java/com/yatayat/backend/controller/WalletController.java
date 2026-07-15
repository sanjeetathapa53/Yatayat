package com.yatayat.backend.controller;

import com.yatayat.backend.dto.TopUpRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.WalletTransaction;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.repository.WalletRepository;
import com.yatayat.backend.repository.WalletTransactionRepository;
import org.springframework.web.bind.annotation.*;
import com.yatayat.backend.dto.WalletPaymentRequest;
import com.yatayat.backend.dto.WalletPinRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public WalletController(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;

    }

    @GetMapping("/balance/{userId}")
    public Object getBalance(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "User not found";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        return wallet.getBalance();
    }

    @PostMapping("/topup")
    public String topUp(@RequestBody TopUpRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            return "User not found";
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return "Invalid amount";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction(
                wallet,
                "TOPUP",
                request.getAmount(),
                "SUCCESS",
                request.getPaymentMethod()
        );

        transactionRepository.save(transaction);

        return "Wallet topped up successfully";
    }

    @GetMapping("/history/{userId}")
    public Object getHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "User not found";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        List<WalletTransaction> transactions =
                transactionRepository.findByWalletOrderByTransactionDateDesc(wallet);

        return transactions;
    }

    @PostMapping("/create-pin")
    public String createWalletPin(@RequestBody WalletPinRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            return "User not found";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getWalletPin() != null && !wallet.getWalletPin().isBlank()) {
            return "Wallet PIN already set";
        }

        wallet.setWalletPin(passwordEncoder.encode(request.getWalletPin()));
        walletRepository.save(wallet);

        return "Wallet PIN created";
    }

    @PostMapping("/verify-pin")
    public String verifyWalletPin(@RequestBody WalletPinRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            return "User not found";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            return "Wallet PIN not set";
        }

        if (!passwordEncoder.matches(request.getWalletPin(), wallet.getWalletPin())) {
            return "Incorrect wallet PIN";
        }

        return "Wallet PIN verified";
    }

    @GetMapping("/pin-status/{userId}")
    public String getWalletPinStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "User not found";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            return "PIN_NOT_SET";
        }

        return "PIN_SET";
    }

    @PostMapping("/pay")
    public String payFromWallet(@RequestBody WalletPaymentRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            return "User not found";
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return "Invalid amount";
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getBalance() < request.getAmount()) {
            return "Insufficient wallet balance";
        }

        wallet.setBalance(wallet.getBalance() - request.getAmount());
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction(
                wallet,
                "TICKET_PAYMENT",
                request.getAmount(),
                "SUCCESS",
                "WALLET"
        );

        transactionRepository.save(transaction);

        return "Payment successful";
    }
}
