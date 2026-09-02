package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.common.web.PageResponse;
import com.slmanju.ceylonads.search.dto.AttributeFilterParams;
import com.slmanju.ceylonads.tuition.dto.TuitionClassCardResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionClassCreateRequest;
import com.slmanju.ceylonads.tuition.dto.TuitionClassDetailResponse;
import com.slmanju.ceylonads.tuition.service.TuitionClassService;
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

// Parallel, isolated Tuition class lifecycle API for the CeylonAds Tuition UI. Read endpoints do
// not replace or call through /api/ads - see TuitionClassService for why the queries are shaped
// differently. Create/update/deactivate do not duplicate the generic Ad create/update
// implementation either - they delegate the actual persistence to the same shared
// AdService.createAd/updateAd/deactivateOwned that /api/ads/** uses, always passing TUITION
// (never a client-supplied sourceChannel - TuitionClassCreateRequest has no such field).
@RestController
@RequestMapping("/api/tuition/classes")
public class TuitionClassController {

    private final TuitionClassService tuitionClassService;

    public TuitionClassController(TuitionClassService tuitionClassService) {
        this.tuitionClassService = tuitionClassService;
    }

    @GetMapping
    @Operation(summary = "List latest tuition classes across the Education & Tuition category tree", description =
            "Zero-based, paginated newest-first listing scoped to the tuition category tree (root or a direct "
                    + "child category). size defaults to 6 and is capped at 50. Isolated from /api/ads and the "
                    + "generic search pipeline - see TuitionClassService.getLatest.")
    PageResponse<TuitionClassCardResponse> latest(
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Max classes per page, capped at 50") @RequestParam(defaultValue = "6") Integer size) {
        return tuitionClassService.getLatest(page, size);
    }

    @GetMapping("/search")
    @Operation(summary = "Search tuition classes with filters", description =
            "Full filtered/paginated search scoped to Tuition listings only, for the Classes/Tutors/Online "
                    + "Classes pages. Mirrors GET /api/ads (same category/location tree resolution, price range, "
                    + "sort, attr.<key> attribute filters) but is scoped to Tuition listings only and returns the "
                    + "same AdResponse shape - see AdSearchService. Unlike /api/ads, results here are always "
                    + "purely organic (every ad's promoted flag is false, content is always exactly size items): "
                    + "Tuition's Search Boost product renders as a separate, additive placement instead - see "
                    + "GET /api/tuition/featured?slot=TUITION_SEARCH_BOOST.")
    PageResponse<AdResponse> search(
            @RequestParam(required = false) String q,
            @Parameter(description = "Category slug; matches this category and all of its descendants") @RequestParam(required = false) String category,
            @Parameter(description = "Location slug; matches this location and all of its descendants") @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size; defaults to 9 (the Classes/Tutors/Online Classes 3x3 grid), capped at 100")
            @RequestParam(defaultValue = "9") Integer size,
            @Parameter(description = "One of: newest, oldest, price_asc, price_desc. Defaults to newest.")
            @RequestParam(defaultValue = "newest") String sort,
            @Parameter(description = "Tuition attribute filters: attr.subject, attr.grade, attr.curriculum, "
                    + "attr.medium, attr.classMode, attr.classType (exact/SELECT match).")
            @RequestParam(required = false) Map<String, String> allParams) {
        return tuitionClassService.search(q, category, location, minPrice, maxPrice, page, size, sort,
                AttributeFilterParams.parse(allParams));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a tuition class by id or SEO slug", description =
            "Accepts either the numeric ad id or a full slug of the form {normalized-title}-{id}; "
                    + "the trailing numeric id is authoritative. 404s when the ad doesn't exist, isn't "
                    + "currently active, or isn't in the Tuition category.")
    TuitionClassDetailResponse get(@PathVariable String slug) {
        return tuitionClassService.getDetailBySlug(slug);
    }

    @GetMapping("/{slug}/similar")
    @Operation(summary = "List a small number of tuition classes similar to the given class", description =
            "Ranked roughly by same subject, then level, then curriculum, then overlapping location; "
                    + "excludes the class itself. size defaults to 3 and is capped at 10.")
    List<TuitionClassCardResponse> similar(
            @PathVariable String slug,
            @Parameter(description = "Max classes to return, capped at 10") @RequestParam(defaultValue = "3") Integer size) {
        return tuitionClassService.getSimilarBySlug(slug, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a tuition class; new classes enter pending review", description =
            "categorySlug must be education-tuition or one of its direct child categories. Always creates a "
                    + "TUITION listing - there is no client-controlled channel field.")
    TuitionClassDetailResponse create(Authentication authentication, @Valid @RequestBody TuitionClassCreateRequest request) {
        return tuitionClassService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update one of my tuition classes; edited classes return to pending review", description =
            "404s if the ad isn't yours or isn't a TUITION listing. The class's TUITION channel is always "
                    + "preserved regardless of request content.")
    TuitionClassDetailResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TuitionClassCreateRequest request) {
        return tuitionClassService.update(id, authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate one of my tuition classes", description =
            "Same semantics as the generic ad deactivate: a status change to DEACTIVATED, not a hard delete. "
                    + "404s if the ad isn't yours or isn't a TUITION listing.")
    void deactivate(Authentication authentication, @PathVariable Long id) {
        tuitionClassService.deactivateOwned(id, authentication.getName());
    }
}
