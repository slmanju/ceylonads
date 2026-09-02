package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionFilterMetadataResponse;
import com.slmanju.ceylonads.tuition.service.TuitionFilterMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Dedicated filter master-data read for the CeylonAds Tuition UI's search filter panel. Isolated
// from /api/categories/{slug}/filters and the generic search pipeline - see
// TuitionFilterMetadataService for why the query shape is different.
@RestController
@RequestMapping("/api/tuition/filters")
public class TuitionFilterController {

    private final TuitionFilterMetadataService tuitionFilterMetadataService;

    public TuitionFilterController(TuitionFilterMetadataService tuitionFilterMetadataService) {
        this.tuitionFilterMetadataService = tuitionFilterMetadataService;
    }

    @GetMapping
    @Operation(summary = "Get filter master data for the Tuition search UI", description =
            "Active subjects, levels, curricula, mediums, and delivery modes for the Tuition category, "
                    + "resolved from attribute_definitions/attribute_options rather than hardcoded.")
    TuitionFilterMetadataResponse filters() {
        return tuitionFilterMetadataService.getFilters();
    }
}
