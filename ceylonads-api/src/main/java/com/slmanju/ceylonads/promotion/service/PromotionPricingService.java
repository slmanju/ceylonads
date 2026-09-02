package com.slmanju.ceylonads.promotion.service;

import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * The single place that turns a {@link PromotionPlan}'s permanent base price into the price a
 * customer actually pays right now. Used identically for catalog display and for purchase, so a
 * discount is never calculated twice or drift between what's shown and what's charged.
 */
@Service
public class PromotionPricingService {

    private static final int MONEY_SCALE = 2;

    private final PromotionCampaignRepository campaigns;

    public PromotionPricingService(PromotionCampaignRepository campaigns) {
        this.campaigns = campaigns;
    }

    public record PromotionPricing(
            BigDecimal basePrice,
            BigDecimal effectivePrice,
            BigDecimal discountAmount,
            Integer discountPercent,
            String campaignCode,
            String campaignName,
            Instant campaignEndsAt,
            boolean discounted) {
    }

    @Transactional(readOnly = true)
    public PromotionPricing resolve(PromotionPlan plan, Instant now) {
        BigDecimal basePrice = plan.getPrice();
        PromotionCampaign campaign = findApplicableCampaign(plan, now);

        if (campaign == null) {
            return new PromotionPricing(basePrice, basePrice, BigDecimal.ZERO.setScale(MONEY_SCALE), null,
                    null, null, null, false);
        }

        BigDecimal effectivePrice = switch (campaign.getPricingType()) {
            case FIXED_PRICE -> campaign.getFixedPrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            case PERCENTAGE_DISCOUNT -> applyPercentageDiscount(basePrice, campaign);
        };
        // A campaign should only ever lower the price; never let a misconfigured fixed price
        // above base or a floor above the discounted amount charge the customer more.
        if (effectivePrice.compareTo(basePrice) > 0) {
            effectivePrice = basePrice;
        }

        BigDecimal discountAmount = basePrice.subtract(effectivePrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        boolean discounted = discountAmount.compareTo(BigDecimal.ZERO) > 0;
        Integer discountPercent = discounted
                ? discountAmount.multiply(BigDecimal.valueOf(100))
                        .divide(basePrice, 0, RoundingMode.HALF_UP)
                        .intValue()
                : null;

        return new PromotionPricing(
                basePrice, effectivePrice, discountAmount, discountPercent,
                campaign.getCode(), campaign.getName(), campaign.getEndsAt(), discounted);
    }

    private BigDecimal applyPercentageDiscount(BigDecimal basePrice, PromotionCampaign campaign) {
        BigDecimal fraction = BigDecimal.ONE.subtract(
                campaign.getDiscountPercent().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal discounted = basePrice.multiply(fraction).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (campaign.getMinimumPrice() != null && discounted.compareTo(campaign.getMinimumPrice()) < 0) {
            return campaign.getMinimumPrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return discounted;
    }

    private PromotionCampaign findApplicableCampaign(PromotionPlan plan, Instant now) {
        List<PromotionCampaign> matches = campaigns.findActiveFor(
                plan.getSlot().getSourceChannel(), plan.getId(), now);
        return matches.isEmpty() ? null : matches.get(0);
    }
}
