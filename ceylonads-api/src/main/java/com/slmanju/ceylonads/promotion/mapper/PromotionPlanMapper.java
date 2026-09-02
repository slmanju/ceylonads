package com.slmanju.ceylonads.promotion.mapper;

import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.service.PromotionPricingService;
import com.slmanju.ceylonads.promotion.service.PromotionPricingService.PromotionPricing;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PromotionPlanMapper {

    private final PromotionPricingService pricingService;

    public PromotionPlanMapper(PromotionPricingService pricingService) {
        this.pricingService = pricingService;
    }

    public PromotionPlanResponse toResponse(PromotionPlan plan) {
        PromotionSlot slot = plan.getSlot();
        Category category = slot.getCategory();
        PromotionPricing pricing = pricingService.resolve(plan, Instant.now());
        return new PromotionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                slot.getPlacementType(),
                slot.getId(),
                slot.getCode(),
                slot.getName(),
                category != null ? category.getId() : null,
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                slot.getCapacity(),
                slot.getVisibleCount(),
                plan.getDurationDays(),
                plan.getPrice(),
                pricing.effectivePrice(),
                pricing.discounted(),
                pricing.discountAmount(),
                pricing.discountPercent(),
                pricing.campaignName(),
                pricing.campaignEndsAt(),
                plan.isActive(),
                plan.isPaymentRequired(),
                plan.isApprovalRequired(),
                plan.getDisplayOrder(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }
}
