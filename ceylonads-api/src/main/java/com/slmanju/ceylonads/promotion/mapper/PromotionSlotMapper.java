package com.slmanju.ceylonads.promotion.mapper;

import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import org.springframework.stereotype.Component;

@Component
public class PromotionSlotMapper {

    public PromotionSlotResponse toResponse(PromotionSlot slot) {
        Category category = slot.getCategory();
        return new PromotionSlotResponse(
                slot.getId(),
                slot.getCode(),
                slot.getName(),
                slot.getDescription(),
                slot.getPlacementType(),
                category != null ? category.getId() : null,
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                slot.getSourceChannel(),
                slot.getCapacity(),
                slot.getVisibleCount(),
                slot.getDisplayOrder(),
                slot.isActive(),
                slot.getCreatedAt(),
                slot.getUpdatedAt());
    }
}
