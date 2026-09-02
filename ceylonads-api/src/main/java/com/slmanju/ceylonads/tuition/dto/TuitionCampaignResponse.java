package com.slmanju.ceylonads.tuition.dto;

import java.time.Instant;

// The Tuition UI's storefront campaign banner/modal - GET /api/tuition/promotions/campaign.
// Deliberately minimal (no internal id, no campaign-plan join rows, no pricing/discount amounts,
// no audit timestamps): this endpoint is for customer-facing marketing presentation only, never
// for checkout pricing - see PromotionCampaignService#findActiveCustomerCampaign and
// GET /api/tuition/promotions/plans, which remains the authoritative pricing source.
public record TuitionCampaignResponse(
        String code,
        String name,
        String headline,
        String message,
        String ctaLabel,
        Instant startsAt,
        Instant endsAt,
        boolean showBanner,
        boolean showModal) {
}
