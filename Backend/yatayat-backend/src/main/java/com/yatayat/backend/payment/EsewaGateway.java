package com.yatayat.backend.payment;

import java.math.BigDecimal;

public interface EsewaGateway {
    StatusResult lookup(String productCode, String totalAmount, String transactionUuid);

    record StatusResult(
            String productCode,
            String transactionUuid,
            BigDecimal totalAmount,
            String status,
            String referenceId
    ) {}
}
