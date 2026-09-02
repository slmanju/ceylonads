package com.slmanju.ceylonads.promotion.controller;

import com.slmanju.ceylonads.promotion.dto.PromotionBannerResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotAvailabilityResponse;
import com.slmanju.ceylonads.promotion.service.PromotionSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/promotion-slots")
public class PromotionSlotController {

    private final PromotionSlotService slotService;

    public PromotionSlotController(PromotionSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check a slot's remaining capacity for a date window",
            description = "Defaults to a 1-day window starting now when startDate/durationDays are omitted.")
    PromotionSlotAvailabilityResponse availability(
            @PathVariable Long id,
            @Parameter(description = "Defaults to today") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Defaults to 1 day") @RequestParam(required = false) Integer durationDays) {
        return slotService.availability(id, startDate, durationDays);
    }

    @GetMapping("/code/{code}/active-banners")
    @Operation(summary = "List currently active banner promotions for a banner slot, by slot code",
            description = "Returns an empty list when no banner promotion is currently active; callers should "
                    + "render nothing rather than an empty placeholder.")
    List<PromotionBannerResponse> activeBanners(@PathVariable String code) {
        return slotService.activeBannersByCode(code);
    }
}
