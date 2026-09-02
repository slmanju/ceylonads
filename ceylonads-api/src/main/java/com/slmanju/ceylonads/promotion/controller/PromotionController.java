package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.CompatiblePromotionPlanResponse;
import com.slmanju.ceylonads.promotion.dto.CreatePromotionRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Promote one of my active ads; starts out PENDING_PAYMENT")
    PromotionResponse create(Authentication authentication, @Valid @RequestBody CreatePromotionRequest request) {
        return promotionService.create(authentication.getName(), request);
    }

    @GetMapping("/me")
    @Operation(summary = "List my promotions")
    List<PromotionResponse> mine(Authentication authentication) {
        return promotionService.mine(authentication.getName());
    }

    @GetMapping("/compatible-plans/{adId}")
    @Operation(summary = "List promotion plans one of my ads is eligible to buy, with live slot availability",
            description = "Excludes banner-only plans, plans on inactive slots, and category-bound plans "
                    + "whose category doesn't contain the ad's own category.")
    List<CompatiblePromotionPlanResponse> compatiblePlans(Authentication authentication, @PathVariable Long adId) {
        return promotionService.compatiblePlansForAd(adId, authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "View one of my promotions")
    PromotionResponse get(Authentication authentication, @PathVariable Long id) {
        return promotionService.getOwned(id, authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my promotions while it is still pending payment")
    PromotionResponse cancel(Authentication authentication, @PathVariable Long id) {
        return promotionService.cancelOwned(id, authentication.getName());
    }
}
