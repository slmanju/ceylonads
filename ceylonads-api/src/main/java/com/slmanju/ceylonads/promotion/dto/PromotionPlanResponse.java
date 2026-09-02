package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.promotion.entity.PlacementType;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionPlanResponse(
        Long id,
        String code,
        String name,
        String description,
        PlacementType placementType,
        Long slotId,
        String slotCode,
        String slotName,
        Long categoryId,
        String categorySlug,
        String categoryName,
        int slotCapacity,
        int slotVisibleCount,
        int durationDays,
        BigDecimal price,
        // price above is always the plan's permanent base price. currentPrice is what a customer
        // pays right now - equal to price unless a PromotionCampaign is active for this plan (see
        // PromotionPricingService), in which case discounted/discountAmount/discountPercent and
        // the campaign fields below describe the difference.
        BigDecimal currentPrice,
        boolean discounted,
        BigDecimal discountAmount,
        Integer discountPercent,
        String campaignName,
        Instant campaignEndsAt,
        boolean active,
        boolean paymentRequired,
        boolean approvalRequired,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt) {
}
