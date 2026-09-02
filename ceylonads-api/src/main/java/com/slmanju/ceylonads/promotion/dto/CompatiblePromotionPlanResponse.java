package com.slmanju.ceylonads.promotion.dto;

// A plan the requesting customer's ad is eligible to purchase, paired with the slot's
// availability for that plan's own duration starting now - the frontend never has to
// compute capacity itself.
public record CompatiblePromotionPlanResponse(
        PromotionPlanResponse plan,
        boolean available,
        int remainingCapacity) {
}
