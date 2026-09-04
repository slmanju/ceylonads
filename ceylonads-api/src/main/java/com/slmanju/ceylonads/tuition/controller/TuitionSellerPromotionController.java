package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.dto.CompatiblePromotionPlanResponse;
import com.slmanju.ceylonads.promotion.dto.CreatePromotionRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.mapper.PromotionCampaignMapper;
import com.slmanju.ceylonads.promotion.service.PromotionCampaignService;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import com.slmanju.ceylonads.tuition.dto.TuitionCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// The Tuition UI's dedicated, self-contained promotion API - the only promotion namespace the
// public Tuition UI may depend on for its catalog or purchase flow (see tuition CLAUDE.md
// "Promotions"). Every endpoint here is channel-scoped to SourceChannel.TUITION at the
// PromotionService layer, never by category or by trusting a client-supplied value, so this
// namespace can never leak or accept a MAIN_SITE/BOARDING plan, ad, or promotion. Reuses the same
// promotions/promotion_plans/promotion_slots tables and creation/pricing rules as the generic
// /api/promotions and /api/promotion-plans APIs (see PromotionService/PromotionPricingService) -
// this controller only narrows what's visible/eligible, it doesn't duplicate any business logic.
// Admins keep using the separate cross-channel /api/admin/promotions APIs.
@RestController
@RequestMapping("/api/tuition/promotions")
public class TuitionSellerPromotionController {

    private final PromotionService promotionService;
    private final PromotionCampaignService promotionCampaignService;
    private final PromotionCampaignMapper campaignMapper;

    public TuitionSellerPromotionController(
            PromotionService promotionService,
            PromotionCampaignService promotionCampaignService,
            PromotionCampaignMapper campaignMapper) {
        this.promotionService = promotionService;
        this.promotionCampaignService = promotionCampaignService;
        this.campaignMapper = campaignMapper;
    }

    @GetMapping("/campaign")
    @Operation(summary = "Get the currently active, customer-visible Tuition campaign for storefront banner/modal presentation",
            description = "204 No Content when no Tuition campaign is currently active, in-date, and customer-visible. "
                    + "Presentation only (name/headline/message/ctaLabel/showBanner/showModal) - never authoritative for "
                    + "checkout price, see GET /plans for current effective pricing.")
    ResponseEntity<TuitionCampaignResponse> campaign() {
        return promotionCampaignService.findActiveCustomerCampaign(SourceChannel.TUITION, Instant.now())
                .map(campaignMapper::toTuitionResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/plans")
    @Operation(summary = "List active Tuition promotion plans, with backend-resolved current pricing and live slot availability",
            description = "TUITION-channel plans only - never MAIN_SITE, BOARDING, or inactive/legacy plans. Each plan "
                    + "already carries its resolved currentPrice/discount/active-campaign fields and live slot "
                    + "availability for its own duration starting now; the frontend never computes pricing or decides "
                    + "which campaign is active itself.")
    List<CompatiblePromotionPlanResponse> plans() {
        return promotionService.tuitionPlans();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Promote one of my active TUITION classes; starts out PENDING_PAYMENT or PENDING_APPROVAL",
            description = "The ad must belong to the authenticated caller and be a TUITION-channel listing, and the "
                    + "selected plan must be an active TUITION-channel plan - both enforced server-side regardless of "
                    + "what ids are supplied. The charged price is always the plan's current backend-resolved price; "
                    + "the client cannot supply or influence it.")
    PromotionResponse create(Authentication authentication, @Valid @RequestBody CreatePromotionRequest request) {
        return promotionService.createForTuitionAd(authentication.getName(), request.adId(), request.promotionPlanId());
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List my promotions on my TUITION listings",
            description = "Never includes promotions on the same account's MAIN_SITE or BOARDING listings.")
    List<PromotionResponse> mine(Authentication authentication) {
        return promotionService.mineForTuition(authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View one of my TUITION promotions",
            description = "404s if the promotion doesn't belong to the caller or isn't on a TUITION-channel ad.")
    PromotionResponse get(Authentication authentication, @PathVariable Long id) {
        return promotionService.getOwnedForTuition(id, authentication.getName());
    }
}
