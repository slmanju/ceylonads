package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.admin.dto.TuitionCatalogScope;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotResponse;
import com.slmanju.ceylonads.promotion.service.PromotionPlanService;
import com.slmanju.ceylonads.promotion.service.PromotionSlotService;
import com.slmanju.ceylonads.tuition.TuitionPromotionCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig. Every method always
// passes SourceChannel.TUITION to the channel-scoped service overloads - a MAIN_SITE/BOARDING
// plan/slot can never be reached or created through this controller.
@RestController
@RequestMapping("/api/admin/tuition/promotion-plans")
@SecurityRequirement(name = "bearerAuth")
public class AdminTuitionPromotionPlanController {

    private final PromotionPlanService promotionPlanService;
    private final PromotionSlotService promotionSlotService;

    public AdminTuitionPromotionPlanController(
            PromotionPlanService promotionPlanService, PromotionSlotService promotionSlotService) {
        this.promotionPlanService = promotionPlanService;
        this.promotionSlotService = promotionSlotService;
    }

    @GetMapping
    @Operation(summary = "List TUITION promotion plans", description =
            "scope=CURRENT (default) returns only plans on one of the seven live ezClass placements that are "
                    + "themselves still active (TuitionPromotionCatalog.isCurrentPlan) - the primary admin screen. "
                    + "scope=HISTORICAL returns everything else (retired 7-day/test products, or a current-slot "
                    + "plan an admin has since closed - kept for audit). scope=ALL returns every TUITION-channel "
                    + "plan regardless of catalog membership.")
    List<PromotionPlanResponse> list(@RequestParam(defaultValue = "CURRENT") TuitionCatalogScope scope) {
        List<PromotionPlanResponse> all = promotionPlanService.allPlans(SourceChannel.TUITION);
        return switch (scope) {
            case CURRENT -> all.stream().filter(p -> TuitionPromotionCatalog.isCurrentPlan(p.slotCode(), p.active())).toList();
            case HISTORICAL -> all.stream().filter(p -> !TuitionPromotionCatalog.isCurrentPlan(p.slotCode(), p.active())).toList();
            case ALL -> all;
        };
    }

    @GetMapping("/slots")
    @Operation(summary = "List the current supported TUITION placements (slots) available to pick when creating a plan", description =
            "Restricted to TuitionPromotionCatalog.CURRENT_SLOT_CODES - retired/test slots (e.g. sidebar-middle/"
                    + "bottom, the old search top banner) are never offered for a brand-new plan. Read-only - there "
                    + "is no separate Tuition slot management surface.")
    List<PromotionSlotResponse> slots() {
        return promotionSlotService.allSlots(SourceChannel.TUITION).stream()
                .filter(s -> TuitionPromotionCatalog.CURRENT_SLOT_CODES.contains(s.code()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a TUITION promotion plan", description =
            "The selected slot must belong to the TUITION channel and be one of the current supported placements "
                    + "(defense in depth - the /slots picker above already only offers these, but a hand-crafted "
                    + "request must not be able to spin up a new plan on a retired placement).")
    PromotionPlanResponse create(@Valid @RequestBody AdminPromotionPlanRequest request) {
        PromotionSlotResponse slot = promotionSlotService.allSlots(SourceChannel.TUITION).stream()
                .filter(s -> s.id().equals(request.slotId()))
                .findFirst()
                .orElse(null);
        if (slot == null || !TuitionPromotionCatalog.CURRENT_SLOT_CODES.contains(slot.code())) {
            throw new BadRequestException("Selected placement is not a current supported TUITION placement");
        }
        return promotionPlanService.create(request, SourceChannel.TUITION);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a TUITION promotion plan's name, description, price, duration, display order and active flag")
    PromotionPlanResponse update(@PathVariable Long id, @Valid @RequestBody AdminPromotionPlanUpdateRequest request) {
        return promotionPlanService.update(id, request, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a TUITION promotion plan so tutors can purchase it")
    PromotionPlanResponse activate(@PathVariable Long id) {
        return promotionPlanService.setActive(id, true, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Close a TUITION promotion plan", description =
            "Hides it from the public Tuition promotion catalog and new purchases; already-sold promotions "
                    + "under this plan are unaffected, and the plan remains visible here in admin/history.")
    PromotionPlanResponse deactivate(@PathVariable Long id) {
        return promotionPlanService.setActive(id, false, SourceChannel.TUITION);
    }
}
