package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.PromotionSlotAdminRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotUsageResponse;
import com.slmanju.ceylonads.promotion.service.PromotionSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/promotion-slots")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionSlotController {

    private final PromotionSlotService slotService;

    public AdminPromotionSlotController(PromotionSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    @Operation(summary = "List all promotion slots, including inactive ones")
    List<PromotionSlotResponse> list() {
        return slotService.allSlots();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a promotion slot", description =
            "categorySlug is required for CATEGORY_FEATURED / CATEGORY_BANNER placements and must be omitted otherwise.")
    PromotionSlotResponse create(@Valid @RequestBody PromotionSlotAdminRequest request) {
        return slotService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a slot's name, description, capacity, display order and active flag",
            description = "Placement type and category are fixed at creation and cannot be changed here.")
    PromotionSlotResponse update(@PathVariable Long id, @Valid @RequestBody PromotionSlotUpdateRequest request) {
        return slotService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a slot so its plans become purchasable")
    PromotionSlotResponse activate(@PathVariable Long id) {
        return slotService.setActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Retire a slot; promotions already sold against it are unaffected")
    PromotionSlotResponse deactivate(@PathVariable Long id) {
        return slotService.setActive(id, false);
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "View a slot's current usage: active/pending counts, remaining capacity, and the underlying promotions")
    PromotionSlotUsageResponse usage(@PathVariable Long id) {
        return slotService.usage(id);
    }
}
