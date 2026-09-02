package com.slmanju.ceylonads.promotion.mapper;

import com.slmanju.ceylonads.promotion.dto.PromotionCampaignResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.tuition.dto.TuitionCampaignResponse;
import org.springframework.stereotype.Component;

@Component
public class PromotionCampaignMapper {

    public PromotionCampaignResponse toResponse(PromotionCampaign campaign) {
        return new PromotionCampaignResponse(
                campaign.getId(),
                campaign.getCode(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getSourceChannel(),
                campaign.getPricingType(),
                campaign.getDiscountPercent(),
                campaign.getFixedPrice(),
                campaign.getMinimumPrice(),
                campaign.isActive(),
                campaign.getStartsAt(),
                campaign.getEndsAt(),
                campaign.getPlans().stream().map(PromotionPlan::getId).toList(),
                campaign.getHeadline(),
                campaign.getMessage(),
                campaign.getCtaLabel(),
                campaign.isCustomerVisible(),
                campaign.isShowBanner(),
                campaign.isShowModal(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }

    // The Tuition UI's minimal public storefront view - see TuitionCampaignResponse for why this
    // is a separate, narrower shape than the admin-facing toResponse above.
    public TuitionCampaignResponse toTuitionResponse(PromotionCampaign campaign) {
        return new TuitionCampaignResponse(
                campaign.getCode(),
                campaign.getName(),
                campaign.getHeadline(),
                campaign.getMessage(),
                campaign.getCtaLabel(),
                campaign.getStartsAt(),
                campaign.getEndsAt(),
                campaign.isShowBanner(),
                campaign.isShowModal());
    }
}
