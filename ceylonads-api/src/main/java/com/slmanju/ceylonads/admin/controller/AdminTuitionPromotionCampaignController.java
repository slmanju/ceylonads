package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionCampaignResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.service.PromotionCampaignService;
import com.slmanju.ceylonads.promotion.service.PromotionPlanService;
import com.slmanju.ceylonads.tuition.TuitionPromotionCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig. Every method always
// passes SourceChannel.TUITION to the channel-scoped service overloads - a MAIN_SITE/BOARDING
// campaign can never be reached, created, or have its plans include a non-TUITION plan through
// this controller. Overlap/pricing/date validation is entirely inherited from
// PromotionCampaignService's existing create/update/setActive - never reimplemented here.
//
// Additionally restricts which plans a Tuition campaign may map to: a brand-new selection must be
// one of the seven current ezClass products (TuitionPromotionCatalog), while a plan already
// mapped to this campaign before this edit is grandfathered through untouched - preserves
// historical campaign->plan mappings on retired products without letting an admin add a *new*
// mapping to one.
@RestController
@RequestMapping("/api/admin/tuition/campaigns")
@SecurityRequirement(name = "bearerAuth")
public class AdminTuitionPromotionCampaignController {

    private final PromotionCampaignService campaignService;
    private final PromotionPlanService promotionPlanService;

    public AdminTuitionPromotionCampaignController(
            PromotionCampaignService campaignService, PromotionPlanService promotionPlanService) {
        this.campaignService = campaignService;
        this.promotionPlanService = promotionPlanService;
    }

    @GetMapping
    @Operation(summary = "List all TUITION promotion campaigns, including inactive/historical ones")
    List<PromotionCampaignResponse> list() {
        return campaignService.list(SourceChannel.TUITION);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a TUITION promotion campaign", description =
            "sourceChannel must be TUITION, and every planId must be one of the seven current Tuition products.")
    PromotionCampaignResponse create(@Valid @RequestBody AdminPromotionCampaignRequest request) {
        requireCurrentCatalogPlans(request.planIds(), Set.of());
        return campaignService.create(request, SourceChannel.TUITION);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a TUITION promotion campaign's name, description, pricing amounts, dates, active flag and plans",
            description = "Never rewrites the charged-price snapshot of promotions already sold under this campaign. "
                    + "A newly-added plan must be a current Tuition product; plans already mapped before this edit "
                    + "are preserved even if retired.")
    PromotionCampaignResponse update(@PathVariable Long id, @Valid @RequestBody AdminPromotionCampaignUpdateRequest request) {
        requireCurrentCatalogPlans(request.planIds(), existingPlanIds(id));
        return campaignService.update(id, request, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a TUITION promotion campaign", description =
            "Rejected if another active, customer-visible TUITION campaign already overlaps this campaign's date range.")
    PromotionCampaignResponse activate(@PathVariable Long id) {
        return campaignService.setActive(id, true, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Close a TUITION promotion campaign", description =
            "Plans revert to their base price for new purchases; already-sold promotions keep their charged-price "
                    + "snapshot, and the campaign remains visible here in admin/history.")
    PromotionCampaignResponse deactivate(@PathVariable Long id) {
        return campaignService.setActive(id, false, SourceChannel.TUITION);
    }

    private Set<Long> existingPlanIds(Long campaignId) {
        return campaignService.list(SourceChannel.TUITION).stream()
                .filter(c -> c.id().equals(campaignId))
                .findFirst()
                .map(c -> Set.copyOf(c.planIds()))
                .orElse(Set.of());
    }

    private void requireCurrentCatalogPlans(List<Long> planIds, Set<Long> alreadyMappedPlanIds) {
        Map<Long, PromotionPlanResponse> byId = promotionPlanService.allPlans(SourceChannel.TUITION).stream()
                .collect(Collectors.toMap(PromotionPlanResponse::id, plan -> plan));
        for (Long planId : planIds) {
            if (alreadyMappedPlanIds.contains(planId)) {
                continue;
            }
            PromotionPlanResponse plan = byId.get(planId);
            if (plan == null || !TuitionPromotionCatalog.isCurrentPlan(plan.slotCode(), plan.active())) {
                throw new BadRequestException(
                        "Promotion plan " + planId + " is not part of the current Tuition promotion catalog");
            }
        }
    }
}
