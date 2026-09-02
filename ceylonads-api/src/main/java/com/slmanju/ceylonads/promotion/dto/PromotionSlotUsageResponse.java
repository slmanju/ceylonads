package com.slmanju.ceylonads.promotion.dto;

import java.util.List;

public record PromotionSlotUsageResponse(
        PromotionSlotResponse slot,
        int activeCount,
        int pendingPaymentCount,
        int remainingCapacity,
        List<PromotionResponse> activePromotions,
        List<PromotionResponse> pendingPromotions) {
}
