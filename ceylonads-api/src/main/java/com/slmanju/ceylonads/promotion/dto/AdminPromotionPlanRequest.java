package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// paymentRequired/approvalRequired default to true (the original paid + admin-approved-by-payment
// behavior) when omitted, so existing callers that don't know about this flag still get a normal
// paid plan rather than an accidental free one.
public record AdminPromotionPlanRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotNull Long slotId,
        @NotNull @Min(1) Integer durationDays,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        Boolean paymentRequired,
        Boolean approvalRequired,
        @Min(0) Integer displayOrder) {
}
