package com.slmanju.ceylonads.promotion.mapper;

import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    private final MediaStorage storage;

    public PromotionMapper(MediaStorage storage) {
        this.storage = storage;
    }

    public PromotionResponse toResponse(Promotion promotion) {
        PromotionPlan plan = promotion.getPlan();
        PromotionSlot slot = plan.getSlot();
        Media bannerMedia = promotion.getBannerMedia();
        return new PromotionResponse(
                promotion.getId(),
                promotion.getKind(),
                promotion.getAd() != null ? promotion.getAd().getId() : null,
                promotion.getAd() != null ? promotion.getAd().getTitle() : null,
                promotion.getCustomer().getId(),
                promotion.getCustomer().getDisplayName(),
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                slot.getId(),
                slot.getCode(),
                slot.getPlacementType(),
                bannerMedia != null ? storage.publicUrl(bannerMedia.getStorageKey()) : null,
                promotion.getTargetUrl(),
                promotion.getPriceAmount(),
                promotion.getDurationDays(),
                plan.isPaymentRequired(),
                promotion.isPaymentWaived(),
                promotion.getCreatedByAdminUsername(),
                promotion.getStatus(),
                promotion.getCreatedAt(),
                promotion.getStartsAt(),
                promotion.getEndsAt());
    }
}
