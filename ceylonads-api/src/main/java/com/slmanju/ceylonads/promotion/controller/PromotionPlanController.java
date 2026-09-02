package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.service.PromotionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promotion-plans")
public class PromotionPlanController {

    private final PromotionPlanService promotionPlanService;

    public PromotionPlanController(PromotionPlanService promotionPlanService) {
        this.promotionPlanService = promotionPlanService;
    }

    @GetMapping
    @Operation(summary = "List active promotion plans available for purchase")
    List<PromotionPlanResponse> list() {
        return promotionPlanService.activePlans();
    }
}
