package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PlacementType;

import java.time.Instant;

public record PromotionSlotResponse(
        Long id,
        String code,
        String name,
        String description,
        PlacementType placementType,
        Long categoryId,
        String categorySlug,
        String categoryName,
        SourceChannel sourceChannel,
        int capacity,
        int visibleCount,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
