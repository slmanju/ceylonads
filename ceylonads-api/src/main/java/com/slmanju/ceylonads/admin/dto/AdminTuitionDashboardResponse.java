package com.slmanju.ceylonads.admin.dto;

// currentPromotionPlans/currentCampaigns are narrower than a plain "active" count: a plan/campaign
// can be active=true in the shared promotion tables while still being a retired product (e.g. the
// old TUITION_SEARCH_TOP_BANNER_7D plan) or a not-yet-started/already-ended campaign window - see
// AdminTuitionAdsController.dashboard() and TuitionPromotionCatalog for the exact definitions.
public record AdminTuitionDashboardResponse(
        long pendingClasses,
        long activeClasses,
        long expiredClasses,
        long newSuggestions,
        long pendingPromotions,
        long activePromotions,
        long currentPromotionPlans,
        long currentCampaigns) {
}
