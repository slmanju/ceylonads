package com.slmanju.ceylonads.promotion.dto;

import java.time.Instant;

public record PromotionBannerResponse(
        Long promotionId,
        String bannerMediaUrl,
        String targetUrl,
        Instant startsAt,
        Instant endsAt) {
}
