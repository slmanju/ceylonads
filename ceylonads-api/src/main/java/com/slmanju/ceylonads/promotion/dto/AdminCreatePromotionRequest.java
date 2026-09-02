package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.NotNull;

// One generic creation request for any placement type: adId is required for ad-linked slots
// (HOME_FEATURED, CATEGORY_FEATURED, TOP_SEARCH) and ignored for banner slots, bannerMediaId is
// required for banner slots (HOME_BANNER, CATEGORY_BANNER) and ignored for ad-linked slots. The
// service determines which fields are required from the selected plan's slot placement type.
public record AdminCreatePromotionRequest(
        @NotNull Long customerId,
        @NotNull Long promotionPlanId,
        Long adId,
        Long bannerMediaId,
        String targetUrl,
        // Admin-only override: skip the plan's normal payment step for this one promotion without
        // altering the plan itself. The price is still snapshotted for the audit trail.
        boolean paymentWaived) {
}
