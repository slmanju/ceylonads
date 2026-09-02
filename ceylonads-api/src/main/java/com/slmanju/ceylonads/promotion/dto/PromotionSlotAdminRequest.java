package com.slmanju.ceylonads.promotion.dto;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromotionSlotAdminRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotNull PlacementType placementType,
        // Required when placementType is category-scoped (CATEGORY_FEATURED / CATEGORY_BANNER),
        // must be omitted otherwise. Validated in the service since it depends on placementType.
        String categorySlug,
        @NotNull SourceChannel sourceChannel,
        @NotNull @Min(1) Integer capacity,
        // How many campaigns render to a visitor at once, e.g. a carousel page size. Optional:
        // defaults to 1 when omitted. Must be <= capacity; validated in the service since it
        // depends on capacity.
        @Min(1) Integer visibleCount,
        @Min(0) Integer displayOrder) {
}
