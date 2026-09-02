package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionFeaturedCardResponse;
import com.slmanju.ceylonads.tuition.service.TuitionFeaturedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Parallel, isolated read API for the CeylonAds Tuition UI's homepage "Featured Tuition"
// carousel. Does not replace or call through /api/ads or /api/promotions - see
// TuitionFeaturedService for why the query here is shaped differently from the generic
// /api/ads/category-featured endpoint it otherwise mirrors.
@RestController
@RequestMapping("/api/tuition")
public class TuitionFeaturedController {

    private final TuitionFeaturedService tuitionFeaturedService;

    public TuitionFeaturedController(TuitionFeaturedService tuitionFeaturedService) {
        this.tuitionFeaturedService = tuitionFeaturedService;
    }

    @GetMapping("/featured")
    @Operation(summary = "List currently featured tuition classes for a fixed tuition promotion carousel", description =
            "By default resolves the CATEGORY_FEATURED promotion slot bound to the Education & Tuition category "
                    + "tree (the homepage/search carousels' shared TUITION_FEATURED inventory). Pass slot to read a "
                    + "different, independently-sellable slot by its exact code instead (e.g. "
                    + "TUITION_DETAIL_TOP_CAROUSEL for the Tuition detail page's top carousel). Returns active, "
                    + "unexpired promotions whose ad is still ACTIVE, newest-started first. size defaults to 10 and "
                    + "is capped at 20. excludeAdId drops one ad (e.g. the listing currently being viewed) from the "
                    + "result without shrinking the page below size. An empty list means no featured promotions are "
                    + "currently active for the resolved slot.")
    List<TuitionFeaturedCardResponse> featured(
            @Parameter(description = "Max classes to return, capped at 20") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Exact slot code to read instead of the default TUITION_FEATURED slot")
            @RequestParam(required = false) String slot,
            @Parameter(description = "Ad id to exclude from the result, e.g. the ad currently being viewed")
            @RequestParam(required = false) Long excludeAdId) {
        return tuitionFeaturedService.getFeatured(size, slot, excludeAdId);
    }
}
