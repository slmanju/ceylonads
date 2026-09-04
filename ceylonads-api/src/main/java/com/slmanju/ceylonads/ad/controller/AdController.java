package com.slmanju.ceylonads.ad.controller;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.dto.CreateAdRequest;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.common.web.PageResponse;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import com.slmanju.ceylonads.search.dto.AttributeFilterParams;
import com.slmanju.ceylonads.search.service.AdSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdService adService;
    private final AdSearchService searchService;
    private final PromotionService promotionService;

    public AdController(AdService adService, AdSearchService searchService, PromotionService promotionService) {
        this.adService = adService;
        this.searchService = searchService;
        this.promotionService = promotionService;
    }

    @GetMapping("/featured")
    @Operation(summary = "List ads with an active Homepage Featured promotion", description =
            "Returns ads ready to display as-is; an empty list means no HOME_FEATURED promotions are currently active.")
    List<AdResponse> featured(
            @Parameter(description = "Max ads to return, capped at 50") @RequestParam(defaultValue = "8") Integer limit) {
        int safeLimit = (limit == null || limit < 1) ? 8 : Math.min(limit, 50);
        return promotionService.homeFeaturedAds(safeLimit);
    }

    @GetMapping("/category-featured")
    @Operation(summary = "List ads with an active Category Featured promotion for a category", description =
            "Resolves the CATEGORY_FEATURED slot bound to the given category or one of its ancestors; returns "
                    + "an empty list when that category has no such slot or no active promotions right now.")
    List<AdResponse> categoryFeatured(
            @RequestParam String categorySlug,
            @Parameter(description = "Max ads to return, capped at 50") @RequestParam(defaultValue = "20") Integer limit) {
        int safeLimit = (limit == null || limit < 1) ? 20 : Math.min(limit, 50);
        return promotionService.categoryFeaturedAds(categorySlug, safeLimit);
    }

    @GetMapping
    @Operation(summary = "Search active ads", description = "Zero-based, paginated and sortable listing of active ads. "
            + "Unknown sort values fall back to 'newest'; size is capped at 100. category/location accept a leaf "
            + "slug (e.g. 'cars', 'colombo') or a parent slug (e.g. 'vehicles', 'western-province'), in which case "
            + "matching ads from every descendant category/location are included too; an unknown slug is a 404, "
            + "and minPrice > maxPrice is a 400. Category-specific filterable attributes can be queried via "
            + "attr.<key>=<value> for an exact/SELECT match, or attr.<key>.min= / attr.<key>.max= for a numeric "
            + "range, e.g. attr.make=Toyota&attr.year.min=2018&attr.year.max=2024; see GET /api/categories/"
            + "{slug}/filters for the available keys per category.")
    PageResponse<AdResponse> search(
            @RequestParam(required = false) String q,
            @Parameter(description = "Category slug; matches this category and all of its descendants") @RequestParam(required = false) String category,
            @Parameter(description = "Location slug; matches this location and all of its descendants") @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size, capped at 100") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "One of: newest, oldest, price_asc, price_desc. Defaults to newest.")
            @RequestParam(defaultValue = "newest") String sort,
            @Parameter(description = "Category attribute filters: attr.<key>=value for exact/SELECT match, "
                    + "attr.<key>.min / attr.<key>.max for numeric range. Not individually listed here since the "
                    + "set of keys is category-defined.")
            @RequestParam(required = false) Map<String, String> allParams) {
        return searchService.search(q, category, location, minPrice, maxPrice, page, size, sort,
                AttributeFilterParams.parse(allParams), SourceChannel.MAIN_SITE);
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Get an active ad by id or SEO slug", description =
            "Accepts either the numeric id or a full slug of the form {normalized-title}-{id} "
                    + "(e.g. toyota-aqua-2019-12345); the trailing numeric id is authoritative.")
    AdResponse get(@PathVariable String idOrSlug) {
        return adService.getPublic(idOrSlug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create an ad; new ads enter pending review")
    AdResponse create(Authentication authentication, @Valid @RequestBody CreateAdRequest request) {
        return adService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update one of my ads; edited ads return to pending review")
    AdResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateAdRequest request) {
        return adService.updateOwned(id, authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate one of my ads")
    void deactivate(Authentication authentication, @PathVariable Long id) {
        adService.deactivateOwned(id, authentication.getName());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List my ads including non-public statuses")
    List<AdResponse> mine(Authentication authentication) {
        return adService.mine(authentication.getName());
    }
}
