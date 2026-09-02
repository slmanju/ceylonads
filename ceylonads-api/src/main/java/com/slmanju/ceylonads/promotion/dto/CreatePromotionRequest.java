package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePromotionRequest(
        @NotNull Long adId,
        @NotNull Long promotionPlanId) {
}
