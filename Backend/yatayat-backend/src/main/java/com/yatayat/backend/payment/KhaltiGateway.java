package com.yatayat.backend.payment;

public interface KhaltiGateway {
    InitiationResult initiate(InitiationRequest request);
    LookupResult lookup(String pidx);
    record CustomerInfo(String name, String email, String phone) {}
    record InitiationRequest(String returnUrl, String websiteUrl, long amount,
                             String purchaseOrderId, String purchaseOrderName,
                             CustomerInfo customerInfo) {}
    record InitiationResult(String pidx, String paymentUrl, String expiresAt, Long expiresIn) {}
    record LookupResult(String pidx, Long totalAmount, String status,
                        String transactionId, Boolean refunded) {}
}
