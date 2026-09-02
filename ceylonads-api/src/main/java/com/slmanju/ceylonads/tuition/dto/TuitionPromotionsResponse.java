package com.slmanju.ceylonads.tuition.dto;

import java.util.List;

// Grouped by slot so the Tuition search page's top banner + 3 sidebar positions can be fetched
// in a single request instead of one call per slot. A slot with no currently eligible promotion
// simply comes back as an empty list, never null.
public record TuitionPromotionsResponse(
        List<TuitionPromotionResponse> topBanner,
        List<TuitionPromotionResponse> sidebarTop,
        List<TuitionPromotionResponse> sidebarMiddle,
        List<TuitionPromotionResponse> sidebarBottom) {
}
