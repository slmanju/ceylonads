package com.slmanju.ceylonads.promotion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// placementType and categorySlug are deliberately absent: both are immutable after a slot is
// created, since changing either would retroactively change the meaning of promotions already
// sold against this slot's inventory.
public record PromotionSlotUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotNull @Min(1) Integer capacity,
        // How many campaigns render to a visitor at once. Optional: omitting it keeps the slot's
        // current value. Must be <= capacity; validated in the service since it depends on capacity.
        @Min(1) Integer visibleCount,
        @Min(0) Integer displayOrder,
        @NotNull Boolean active) {
}
