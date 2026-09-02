package com.slmanju.ceylonads.ad.controller;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/moderation/** is restricted to ROLE_MODERATOR and ROLE_ADMIN centrally in SecurityConfig -
// this is the shared ad-moderation boundary, kept deliberately separate from /api/admin/** (which
// stays ADMIN-only for promotion/payment/category/location administration).
//
// A MODERATOR here is scoped to the MAIN CeylonAds storefront only - it must never see/approve/
// reject a TUITION or BOARDING listing (those get their own moderation surface later). ADMIN
// stays cross-channel through this same endpoint, matching its cross-channel access via
// /api/admin/ads/**, so restrictToChannel below is resolved from the caller's own role rather
// than hardcoded per-route.
@RestController
@RequestMapping("/api/moderation/ads")
@SecurityRequirement(name = "bearerAuth")
public class ModerationController {

    private final AdService adService;

    public ModerationController(AdService adService) {
        this.adService = adService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List ads waiting for moderation", description =
            "A MODERATOR only sees MAIN CeylonAds listings; ADMIN sees every channel.")
    List<AdResponse> pendingAds(Authentication authentication) {
        return adService.pendingReview(restrictToChannel(authentication));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve an ad", description =
            "A moderator may approve an ad they created themselves; self-approval is an intentional MVP allowance. "
                    + "A MODERATOR cannot approve a TUITION/BOARDING listing.")
    AdResponse approve(Authentication authentication, @PathVariable Long id) {
        return adService.approve(id, authentication.getName(), restrictToChannel(authentication));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject an ad", description = "A MODERATOR cannot reject a TUITION/BOARDING listing.")
    AdResponse reject(Authentication authentication, @PathVariable Long id) {
        return adService.reject(id, authentication.getName(), restrictToChannel(authentication));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an ad", description = "A MODERATOR cannot deactivate a TUITION/BOARDING listing.")
    AdResponse deactivate(Authentication authentication, @PathVariable Long id) {
        return adService.adminDeactivate(id, restrictToChannel(authentication));
    }

    // null (no restriction) for ADMIN, MAIN_SITE for a MODERATOR-only caller.
    private SourceChannel restrictToChannel(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? null : SourceChannel.MAIN_SITE;
    }
}
