package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig. Every method here always
// passes SourceChannel.TUITION explicitly to PromotionService's channel-scoped overloads (never
// null, never role-derived), so a MAIN_SITE/BOARDING promotion can never be reached through this
// controller - same structural guarantee as AdminTuitionAdsController for classes.
@RestController
@RequestMapping("/api/admin/tuition/promotions")
@SecurityRequirement(name = "bearerAuth")
public class AdminTuitionPromotionController {

    private final PromotionService promotionService;

    public AdminTuitionPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    @Operation(summary = "List TUITION promotions, optionally filtered by status")
    List<PromotionResponse> list(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) PromotionStatus status) {
        return promotionService.adminList(status, SourceChannel.TUITION);
    }

    @GetMapping("/{id}")
    @Operation(summary = "View one TUITION promotion")
    PromotionResponse get(@PathVariable Long id) {
        return promotionService.adminGet(id, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a TUITION promotion pending approval", description =
            "Uses the existing slot-capacity-checked activation logic (PromotionService.adminApprove) - "
                    + "sets startsAt/endsAt from the plan's duration and extends the underlying ad's expiresAt "
                    + "to cover the promotion, same as every other activation path.")
    PromotionResponse approve(@PathVariable Long id) {
        return promotionService.adminApprove(id, SourceChannel.TUITION);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a pending TUITION promotion", description =
            "The promotion record is kept (status CANCELLED), never deleted, and never renders publicly again.")
    PromotionResponse reject(@PathVariable Long id) {
        return promotionService.adminCancel(id, SourceChannel.TUITION);
    }
}
