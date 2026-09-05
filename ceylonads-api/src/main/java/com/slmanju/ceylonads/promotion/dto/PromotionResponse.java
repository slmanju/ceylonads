package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.PromotionKind;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(
        Long id,
        PromotionKind kind,
        // Populated for AD_PROMOTION, null for BANNER_PROMOTION.
        Long adId,
        String adTitle,
        Long customerId,
        String customerDisplayName,
        Long promotionPlanId,
        String promotionPlanCode,
        String promotionPlanName,
        Long slotId,
        String slotCode,
        PlacementType placementType,
        // Populated for BANNER_PROMOTION, null for AD_PROMOTION.
        String bannerMediaUrl,
        String targetUrl,
        BigDecimal price,
        int durationDays,
        // Reflects the plan's current configuration (not a snapshot), since it's only used to
        // decide how to label payment status in the UI, not to reconstruct billing history.
        boolean paymentRequired,
        boolean paymentWaived,
        // Null for a customer/tutor's own request. Set only for a promotion created directly via
        // the Tuition admin console's "Promote Class" action - the one reliable signal
        // distinguishing that lifecycle from a normal pending-review customer request (see
        // Promotion#markAdminCreated).
        String createdByAdminUsername,
        PromotionStatus status,
        Instant createdAt,
        Instant startsAt,
        Instant endsAt) {
}
