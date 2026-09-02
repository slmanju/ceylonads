package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.service.PromotionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/promotion-plans")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionPlanController {

    private final PromotionPlanService promotionPlanService;

    public AdminPromotionPlanController(PromotionPlanService promotionPlanService) {
        this.promotionPlanService = promotionPlanService;
    }

    @GetMapping
    @Operation(summary = "List all promotion plans, including inactive ones")
    List<PromotionPlanResponse> list() {
        return promotionPlanService.allPlans();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a promotion plan")
    PromotionPlanResponse create(@Valid @RequestBody AdminPromotionPlanRequest request) {
        return promotionPlanService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a promotion plan's name, description, price, duration, display order and active flag")
    PromotionPlanResponse update(@PathVariable Long id, @Valid @RequestBody AdminPromotionPlanUpdateRequest request) {
        return promotionPlanService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a promotion plan so customers can purchase it")
    PromotionPlanResponse activate(@PathVariable Long id) {
        return promotionPlanService.setActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Retire a promotion plan; existing promotions already sold under it are unaffected")
    PromotionPlanResponse deactivate(@PathVariable Long id) {
        return promotionPlanService.setActive(id, false);
    }
}
