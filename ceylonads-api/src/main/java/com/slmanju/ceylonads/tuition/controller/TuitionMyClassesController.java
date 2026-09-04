package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionClassDetailResponse;
import com.slmanju.ceylonads.tuition.service.TuitionClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Deliberately its own controller rather than nested under TuitionClassController's
// /api/tuition/classes base path: the route is /api/tuition/my-classes (mirrors how
// TuitionFeaturedController/TuitionPromotionController each own a single top-level /api/tuition/*
// route rather than sharing one controller).
@RestController
@RequestMapping("/api/tuition/my-classes")
@SecurityRequirement(name = "bearerAuth")
public class TuitionMyClassesController {

    private final TuitionClassService tuitionClassService;

    public TuitionMyClassesController(TuitionClassService tuitionClassService) {
        this.tuitionClassService = tuitionClassService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "List my tuition classes, including non-public statuses", description =
            "The authenticated seller's own TUITION listings only - never their MAIN_SITE/BOARDING ads. "
                    + "Generic /api/ads/mine is unaffected and continues to return a seller's ads across every channel.")
    List<TuitionClassDetailResponse> myClasses(Authentication authentication) {
        return tuitionClassService.myClasses(authentication.getName());
    }
}
