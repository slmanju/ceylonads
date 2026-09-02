package com.slmanju.ceylonads.payment.config;

import com.slmanju.ceylonads.payment.dto.BankTransferDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Backs GET /api/payments/bank-transfer-details. Values come from app.payment.bank-transfer.*
// (see application.yml), overridable via env vars in production. Local/demo defaults are
// clearly fake so nothing resembling real banking details ships in this repository.
@Component
public class BankTransferProperties {

    private final String bankName;
    private final String accountName;
    private final String accountNumber;
    private final String branch;
    private final String instructions;

    public BankTransferProperties(
            @Value("${app.payment.bank-transfer.bank-name}") String bankName,
            @Value("${app.payment.bank-transfer.account-name}") String accountName,
            @Value("${app.payment.bank-transfer.account-number}") String accountNumber,
            @Value("${app.payment.bank-transfer.branch}") String branch,
            @Value("${app.payment.bank-transfer.instructions}") String instructions) {
        this.bankName = bankName;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.branch = branch;
        this.instructions = instructions;
    }

    public BankTransferDetailsResponse toResponse() {
        return new BankTransferDetailsResponse(bankName, accountName, accountNumber, branch, instructions);
    }
}
