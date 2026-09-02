package com.slmanju.ceylonads.ad.dto;

import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.dto.MediaResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdResponse(
        Long id,
        String slug,
        String title,
        String description,
        BigDecimal price,
        String category,
        String categorySlug,
        // 0..N: e.g. empty for online tuition/remote services, one for a property, several for a
        // teacher/service covering multiple towns.
        List<LocationResponse> locations,
        AdSellerResponse seller,
        AdStatus status,
        Instant createdAt,
        Instant publishedAt,
        // When this ad was last approved/rejected; null until first reviewed. Mirrors
        // PaymentResponse.reviewedAt - the reviewer's account id itself is not exposed here either.
        Instant reviewedAt,
        List<MediaResponse> media,
        boolean promoted,
        List<AdAttributeResponse> attributes,
        // Resolved effective contact for buyers; only populated on the public detail response.
        AdContactResponse contact,
        // Raw ad-specific contact override; only populated on responses returned to the ad's
        // owner (create/update/mine), so Edit can distinguish "no override" from the fallback.
        AdContactOverrideResponse contactOverride) {
}
