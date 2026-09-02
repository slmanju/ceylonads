package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.service.MediaService;
import com.slmanju.ceylonads.promotion.dto.AdminCreatePromotionRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/promotions")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final MediaService mediaService;

    public AdminPromotionController(PromotionService promotionService, MediaService mediaService) {
        this.promotionService = promotionService;
        this.mediaService = mediaService;
    }

    @GetMapping
    @Operation(summary = "List promotions, optionally filtered by status")
    List<PromotionResponse> list(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) PromotionStatus status) {
        return promotionService.adminList(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "View one promotion")
    PromotionResponse get(@PathVariable Long id) {
        return promotionService.adminGet(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Manually activate a pending promotion, subject to the same slot capacity check payment approval uses")
    PromotionResponse activate(@PathVariable Long id) {
        return promotionService.activate(id);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a promotion pending approval (free plan, or a paid plan with payment waived)",
            description = "Uses the same slot-capacity-checked activation every other activation path uses.")
    PromotionResponse approve(@PathVariable Long id) {
        return promotionService.adminApprove(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or active promotion")
    PromotionResponse cancel(@PathVariable Long id) {
        return promotionService.adminCancel(id);
    }

    @PostMapping("/banner-media")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a banner image for an admin-created banner promotion")
    MediaResponse uploadBannerMedia(@RequestPart("file") MultipartFile file) throws IOException {
        return mediaService.uploadPromotionBanner(file);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a promotion of any placement type on behalf of a customer",
            description = "One generic flow for every placement type: the selected plan's slot decides whether an "
                    + "ad or a banner image is required. Price, duration, payment requirement and approval "
                    + "requirement all come from the plan. An admin may optionally waive payment for this one "
                    + "promotion without changing the plan itself.")
    PromotionResponse create(@Valid @RequestBody AdminCreatePromotionRequest request) {
        return promotionService.adminCreate(request);
    }
}
