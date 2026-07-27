package com.yatayat.backend.controller;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.WalletTransaction;
import com.yatayat.backend.repository.WalletRepository;
import com.yatayat.backend.repository.WalletTransactionRepository;
import org.springframework.web.bind.annotation.*;
import com.yatayat.backend.dto.WalletPinRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import com.yatayat.backend.service.AuthenticatedUserService;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserService authenticatedUserService;

    public WalletController(
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserService = authenticatedUserService;

    }

    @GetMapping("/balance/{userId}")
    public Object getBalance(@PathVariable Long userId, Authentication authentication) {
        User user = authenticatedUserService.requireOwnedUser(authentication, userId);

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        return wallet.getBalance();
    }

    @GetMapping("/history/{userId}")
    public Object getHistory(@PathVariable Long userId, Authentication authentication) {
        User user = authenticatedUserService.requireOwnedUser(authentication, userId);

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        List<WalletTransaction> transactions =
                transactionRepository.findByWalletOrderByTransactionDateDesc(wallet);

        return transactions;
    }

    @PostMapping("/create-pin")
    public String createWalletPin(@RequestBody WalletPinRequest request, Authentication authentication) {
        User user = authenticatedUserService.requireOwnedUser(authentication, request.getUserId());

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
    public String verifyWalletPin(@RequestBody WalletPinRequest request, Authentication authentication) {
        User user = authenticatedUserService.requireOwnedUser(authentication, request.getUserId());

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
    public String getWalletPinStatus(@PathVariable Long userId, Authentication authentication) {
        User user = authenticatedUserService.requireOwnedUser(authentication, userId);

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            return "PIN_NOT_SET";
        }

        return "PIN_SET";
    }

}
