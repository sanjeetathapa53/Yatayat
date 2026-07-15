package com.yatayat.backend.dto;

public class WalletPaymentRequest {

    private Long userId;
    private Double amount;
    private String purpose;

    public Long getUserId() {
        return userId;
    }

    public Double getAmount() {
        return amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}