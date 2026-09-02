package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionCampaignResponse;
import com.slmanju.ceylonads.promotion.service.PromotionCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/promotion-campaigns")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionCampaignController {

    private final PromotionCampaignService campaignService;

    public AdminPromotionCampaignController(PromotionCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "List all promotion campaigns, including inactive ones")
    List<PromotionCampaignResponse> list() {
        return campaignService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a promotion campaign (temporary price override for one or more plans)")
    PromotionCampaignResponse create(@Valid @RequestBody AdminPromotionCampaignRequest request) {
        return campaignService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a promotion campaign's name, description, pricing amounts, dates, active flag and plans")
    PromotionCampaignResponse update(@PathVariable Long id, @Valid @RequestBody AdminPromotionCampaignUpdateRequest request) {
        return campaignService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a promotion campaign so its pricing takes effect immediately")
    PromotionCampaignResponse activate(@PathVariable Long id) {
        return campaignService.setActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a promotion campaign; affected plans revert to their base price")
    PromotionCampaignResponse deactivate(@PathVariable Long id) {
        return campaignService.setActive(id, false);
    }
}
