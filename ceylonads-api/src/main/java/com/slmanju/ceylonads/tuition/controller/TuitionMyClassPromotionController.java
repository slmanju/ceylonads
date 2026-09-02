package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.promotion.dto.CompatiblePromotionPlanResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import com.slmanju.ceylonads.tuition.dto.TuitionPromotionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// My Classes' "Promote" action. Thin wrapper over the shared /api/promotions backend (same
// promotions/promotion_plans/promotion_slots tables, same PromotionService creation/eligibility
// rules) that only adds one thing: the ad lookup is scoped to SourceChannel.TUITION, so this path
// can never be used to promote a MAIN_SITE/BOARDING ad regardless of what id is supplied - see
// PromotionService#createForTuitionAd/#compatiblePlansForTuitionAd.
@RestController
@RequestMapping("/api/tuition/my-classes/{adId}")
@PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
@SecurityRequirement(name = "bearerAuth")
public class TuitionMyClassPromotionController {

    private final PromotionService promotionService;

    public TuitionMyClassPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/promotion-plans")
    @Operation(summary = "List promotion plans one of my TUITION classes is eligible to buy, with live slot availability",
            description = "Same eligibility rules as GET /api/promotions/compatible-plans/{adId}, scoped so adId "
                    + "must be one of the authenticated tutor's own TUITION-channel listings.")
    List<CompatiblePromotionPlanResponse> compatiblePlans(Authentication authentication, @PathVariable Long adId) {
        return promotionService.compatiblePlansForTuitionAd(adId, authentication.getName());
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Promote one of my active TUITION classes; starts out PENDING_PAYMENT or PENDING_APPROVAL",
            description = "Same creation rules as POST /api/promotions, scoped so adId must be one of the "
                    + "authenticated tutor's own TUITION-channel listings.")
    PromotionResponse promote(
            Authentication authentication, @PathVariable Long adId, @Valid @RequestBody TuitionPromotionRequest request) {
        return promotionService.createForTuitionAd(authentication.getName(), adId, request.promotionPlanId());
    }
}
