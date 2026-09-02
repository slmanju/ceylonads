package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.location.dto.LocationResponse;

import java.math.BigDecimal;
import java.util.List;

// Lightweight card shape for the Tuition homepage "Featured Tuition" carousel - GET
// /api/tuition/featured. Same leanness as TuitionClassCardResponse (no description, no full
// media/seller object) plus the couple of extra fields the featured carousel design needs
// (deliveryMode, providerName). Every card returned by that endpoint is, by construction, an
// active featured placement - no separate "featured" flag is needed on the DTO itself.
public record TuitionFeaturedCardResponse(
        Long id,
        String slug,
        String title,
        BigDecimal price,
        String primaryImageUrl,
        LocationResponse primaryLocation,
        String subject,
        String level,
        AttributeValueLabel curriculum,
        List<AttributeValueLabel> medium,
        AttributeValueLabel deliveryMode,
        String providerName) {
}
