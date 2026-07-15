package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletOrderByTransactionDateDesc(Wallet wallet);
}