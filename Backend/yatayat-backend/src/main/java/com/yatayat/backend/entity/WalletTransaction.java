package com.yatayat.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // TOPUP, TICKET_PAYMENT

    private Double amount;

    private String status; // SUCCESS, FAILED, PENDING

    private String paymentMethod; // ESEWA, KHALTI, WALLET

    private LocalDateTime transactionDate;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    public WalletTransaction() {
    }

    public WalletTransaction(Wallet wallet, String type, Double amount, String status, String paymentMethod) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.transactionDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public Double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public Wallet getWallet() { return wallet; }

    public void setId(Long id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
}