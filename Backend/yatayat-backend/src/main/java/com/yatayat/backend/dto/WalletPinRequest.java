package com.yatayat.backend.dto;

public class WalletPinRequest {

    private Long userId;
    private String walletPin;

    public WalletPinRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getWalletPin() {
        return walletPin;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setWalletPin(String walletPin) {
        this.walletPin = walletPin;
    }
}