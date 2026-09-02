package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PricingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// discountPercent is required and fixedPrice must be omitted when pricingType is
// PERCENTAGE_DISCOUNT, and vice-versa for FIXED_PRICE - validated in the service since it depends
// on pricingType. minimumPrice only ever applies to PERCENTAGE_DISCOUNT and is always optional.
//
// headline/message/ctaLabel are optional here (a campaign can be a pure pricing override with no
// storefront presence) but required non-blank when customerVisible=true - see
// PromotionCampaignService#requireValidStorefrontFields. Plain text only, no HTML.
public record AdminPromotionCampaignRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotNull SourceChannel sourceChannel,
        @NotNull PricingType pricingType,
        @DecimalMin("0.00") BigDecimal discountPercent,
        @DecimalMin("0.00") BigDecimal fixedPrice,
        @DecimalMin("0.00") BigDecimal minimumPrice,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotEmpty List<Long> planIds,
        @Size(max = 180) String headline,
        @Size(max = 500) String message,
        @Size(max = 80) String ctaLabel,
        boolean customerVisible,
        boolean showBanner,
        boolean showModal) {
}
