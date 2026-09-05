package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.NotNull;

// ezClass Tuition admin console's direct "Promote Class" action (AdminTuitionAdsController).
// Only the plan is client-supplied - price, duration, campaign, channel, and owner are all
// resolved server-side from the class and the selected plan, never accepted from the client.
public record PromoteTuitionClassRequest(@NotNull Long promotionPlanId) {
}
