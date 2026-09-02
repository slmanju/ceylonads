package com.slmanju.ceylonads.tuition.dto;

// A single sponsored placement for the Tuition search page - either an ad-backed card
// (type = "AD", targetType = "AD", targetId/adSlug point at the promoted ad) or a plain image
// banner (type = "BANNER", targetType = "EXTERNAL", targetUrl is the click-through link). Only
// one of {targetId+adSlug} / {targetUrl} is ever populated, matching which fields the underlying
// Promotion actually stores for that kind (see Promotion.kind).
public record TuitionPromotionResponse(
        Long id,
        String slot,
        String type,
        String title,
        String subtitle,
        String imageUrl,
        String badge,
        String ctaLabel,
        String targetUrl,
        String targetType,
        Long targetId,
        String adSlug,
        int displayOrder) {
}
