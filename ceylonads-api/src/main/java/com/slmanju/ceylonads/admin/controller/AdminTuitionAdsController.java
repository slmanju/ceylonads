package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.admin.dto.AdminTuitionDashboardResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import com.slmanju.ceylonads.tuition.TuitionPromotionCatalog;
import com.slmanju.ceylonads.tuition.entity.SuggestionStatus;
import com.slmanju.ceylonads.tuition.repository.TuitionSuggestionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

// /api/admin/** is ROLE_ADMIN-only centrally in SecurityConfig. Every method below always passes
// SourceChannel.TUITION explicitly (never null, never derived from the caller's role) to
// AdService's existing channel-guarded methods - the same mechanism ModerationController uses to
// scope a MODERATOR to MAIN_SITE - so a MAIN_SITE/BOARDING ad id can never be approved/rejected/
// viewed through this controller, structurally, not just by convention.
@RestController
@RequestMapping("/api/admin/tuition")
@SecurityRequirement(name = "bearerAuth")
public class AdminTuitionAdsController {

    private final AdService adService;
    private final AdRepository adRepository;
    private final TuitionSuggestionRepository suggestionRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionPlanRepository promotionPlanRepository;
    private final PromotionCampaignRepository promotionCampaignRepository;

    public AdminTuitionAdsController(
            AdService adService, AdRepository adRepository, TuitionSuggestionRepository suggestionRepository,
            PromotionRepository promotionRepository, PromotionPlanRepository promotionPlanRepository,
            PromotionCampaignRepository promotionCampaignRepository) {
        this.adService = adService;
        this.adRepository = adRepository;
        this.suggestionRepository = suggestionRepository;
        this.promotionRepository = promotionRepository;
        this.promotionPlanRepository = promotionPlanRepository;
        this.promotionCampaignRepository = promotionCampaignRepository;
    }

    @GetMapping("/pending")
    @Operation(summary = "List TUITION classes waiting for moderation")
    List<AdResponse> pending() {
        return adService.pendingReview(SourceChannel.TUITION);
    }

    @GetMapping("/ads")
    @Operation(summary = "List TUITION classes by status", description =
            "Backs the Classes page's Pending/Active/Rejected/Expired tabs - a generalized form of /pending above.")
    List<AdResponse> byStatus(@RequestParam AdStatus status) {
        return adService.listByStatus(status, SourceChannel.TUITION);
    }

    @GetMapping("/ads/{id}")
    @Operation(summary = "Get a TUITION class for admin review")
    AdResponse get(@PathVariable Long id) {
        return adService.getForAdmin(id, SourceChannel.TUITION);
    }

    @PatchMapping("/ads/{id}/approve")
    @Operation(summary = "Approve a TUITION class",
            description = "Applies the existing Tuition publish/expiry lifecycle (Ad.approve) - publishedAt/expiresAt are set on first publication only.")
    AdResponse approve(Authentication authentication, @PathVariable Long id) {
        return adService.approve(id, authentication.getName(), SourceChannel.TUITION);
    }

    @PatchMapping("/ads/{id}/reject")
    @Operation(summary = "Reject a TUITION class", description = "The listing is not deleted; the owner still sees it (as REJECTED) in My Classes.")
    AdResponse reject(Authentication authentication, @PathVariable Long id) {
        return adService.reject(id, authentication.getName(), SourceChannel.TUITION);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Tuition admin dashboard summary counts")
    AdminTuitionDashboardResponse dashboard() {
        // "Current" is narrower than a plain active=true count: a retired product (e.g. the old
        // TUITION_SEARCH_TOP_BANNER_7D plan) can still be active=true in the shared tables, and a
        // campaign can be active=true but not yet started or already past its own end date - see
        // TuitionPromotionCatalog. Both lists here are small (a handful of TUITION-channel rows),
        // so filtering in Java after one query is simpler than bespoke count queries per case.
        long currentPromotionPlans = promotionPlanRepository.findBySlot_SourceChannelOrderByDisplayOrderAscIdAsc(SourceChannel.TUITION)
                .stream()
                .filter(plan -> TuitionPromotionCatalog.isCurrentPlan(plan.getSlot().getCode(), plan.isActive()))
                .count();

        Instant now = Instant.now();
        long currentCampaigns = promotionCampaignRepository.findBySourceChannel(SourceChannel.TUITION).stream()
                .filter(PromotionCampaign::isActive)
                .filter(c -> !c.getStartsAt().isAfter(now) && c.getEndsAt().isAfter(now))
                .count();

        return new AdminTuitionDashboardResponse(
                adRepository.countByStatusAndSourceChannel(AdStatus.PENDING_REVIEW, SourceChannel.TUITION),
                adRepository.countByStatusAndSourceChannel(AdStatus.ACTIVE, SourceChannel.TUITION),
                adRepository.countByStatusAndSourceChannel(AdStatus.EXPIRED, SourceChannel.TUITION),
                suggestionRepository.countByStatus(SuggestionStatus.NEW),
                promotionRepository.countByStatusInAndPlan_Slot_SourceChannel(
                        EnumSet.of(PromotionStatus.PENDING_PAYMENT, PromotionStatus.PENDING_APPROVAL), SourceChannel.TUITION),
                promotionRepository.countByStatusAndPlan_Slot_SourceChannel(PromotionStatus.ACTIVE, SourceChannel.TUITION),
                currentPromotionPlans,
                currentCampaigns);
    }
}
