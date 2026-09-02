package com.slmanju.ceylonads.payment.dto;

public record BankTransferDetailsResponse(
        String bankName,
        String accountName,
        String accountNumber,
        String branch,
        String instructions) {
}
