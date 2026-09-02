package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PricingType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionCampaignResponse(
        Long id,
        String code,
        String name,
        String description,
        SourceChannel sourceChannel,
        PricingType pricingType,
        BigDecimal discountPercent,
        BigDecimal fixedPrice,
        BigDecimal minimumPrice,
        boolean active,
        Instant startsAt,
        Instant endsAt,
        List<Long> planIds,
        String headline,
        String message,
        String ctaLabel,
        boolean customerVisible,
        boolean showBanner,
        boolean showModal,
        Instant createdAt,
        Instant updatedAt) {
}
