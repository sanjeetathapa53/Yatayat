package com.yatayat.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double balance = 0.0;

    // NEW
    private String walletPin;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Wallet() {
    }

    public Wallet(User user) {
        this.user = user;
        this.balance = 0.0;
    }

    public Long getId() {
        return id;
    }

    public Double getBalance() {
        return balance;
    }

    public String getWalletPin() {
        return walletPin;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setWalletPin(String walletPin) {
        this.walletPin = walletPin;
    }

    public void setUser(User user) {
        this.user = user;
    }
}