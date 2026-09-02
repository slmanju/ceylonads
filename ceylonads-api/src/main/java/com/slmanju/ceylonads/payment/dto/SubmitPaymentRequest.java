package com.slmanju.ceylonads.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitPaymentRequest(
        @NotBlank @Size(max = 100) String bankReference,
        @Size(max = 500) String customerNote) {
}
