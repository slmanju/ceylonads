package com.slmanju.ceylonads.tuition.dto;

import jakarta.validation.constraints.NotNull;

// adId deliberately isn't a field here - it comes from the {adId} path variable on
// TuitionMyClassPromotionController so a tutor can only ever request a promotion for the ad the
// URL is already scoped to, not one supplied in the body.
public record TuitionPromotionRequest(
        @NotNull Long promotionPlanId) {
}
