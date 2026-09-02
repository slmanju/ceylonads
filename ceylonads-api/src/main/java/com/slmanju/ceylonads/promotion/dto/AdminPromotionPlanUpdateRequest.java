package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// placementType is deliberately absent: it is immutable after a plan is created, since
// changing it would retroactively change the meaning of promotions already sold under this code.
// paymentRequired/approvalRequired are nullable: omitting them leaves the plan's current values
// untouched, so an editor that only changes price/duration can never accidentally flip a free
// plan into a paid one.
public record AdminPromotionPlanUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(1) Integer durationDays,
        @NotNull Boolean active,
        Boolean paymentRequired,
        Boolean approvalRequired,
        @Min(0) Integer displayOrder) {
}
