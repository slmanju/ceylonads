package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionPromotionsResponse;
import com.slmanju.ceylonads.tuition.service.TuitionPromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Parallel, isolated read API for the CeylonAds Tuition UI's search-page promotions (top banner +
// sidebar top/middle/bottom). Does not replace or call through /api/promotions or /api/ads - see
// TuitionPromotionService for how it reuses the shared promotion tables without touching any
// existing placement's resolution path.
@RestController
@RequestMapping("/api/tuition")
public class TuitionPromotionController {

    private final TuitionPromotionService tuitionPromotionService;

    public TuitionPromotionController(TuitionPromotionService tuitionPromotionService) {
        this.tuitionPromotionService = tuitionPromotionService;
    }

    @GetMapping("/promotions")
    @Operation(summary = "List active promotions for the Tuition search page", description =
            "Returns active, unexpired promotions (ad still ACTIVE for ad-backed slots) grouped by "
                    + "slot: topBanner, sidebarTop, sidebarMiddle, sidebarBottom. slots optionally filters to "
                    + "a subset of TUITION_SEARCH_TOP_BANNER/TUITION_SEARCH_SIDEBAR_TOP/_MIDDLE/_BOTTOM; omit "
                    + "it to fetch all four in one call. A slot with nothing eligible comes back as an empty "
                    + "list.")
    TuitionPromotionsResponse promotions(
            @Parameter(description = "Slot codes to fetch, comma-separated; omit for all 4 tuition search slots")
            @RequestParam(required = false) List<String> slots) {
        return tuitionPromotionService.getSearchPromotions(slots);
    }
}
