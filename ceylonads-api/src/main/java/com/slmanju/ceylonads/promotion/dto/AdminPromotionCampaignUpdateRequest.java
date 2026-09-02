package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// code, sourceChannel and pricingType are deliberately absent: immutable after creation, same
// reasoning as PromotionPlan's admin update DTO. See AdminPromotionCampaignRequest for the
// headline/message/ctaLabel/customerVisible/showBanner/showModal invariants.
public record AdminPromotionCampaignUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @DecimalMin("0.00") BigDecimal discountPercent,
        @DecimalMin("0.00") BigDecimal fixedPrice,
        @DecimalMin("0.00") BigDecimal minimumPrice,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull Boolean active,
        @NotEmpty List<Long> planIds,
        @Size(max = 180) String headline,
        @Size(max = 500) String message,
        @Size(max = 80) String ctaLabel,
        boolean customerVisible,
        boolean showBanner,
        boolean showModal) {
}
