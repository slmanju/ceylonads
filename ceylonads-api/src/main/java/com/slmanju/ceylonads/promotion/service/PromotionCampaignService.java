package com.slmanju.ceylonads.promotion.service;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionCampaignUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionCampaignResponse;
import com.slmanju.ceylonads.promotion.entity.PricingType;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.mapper.PromotionCampaignMapper;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Admin CRUD for {@link PromotionCampaign}. Lets an admin move a channel from launch pricing to a
 * seasonal discount to normal pricing purely by editing campaign rows (dates/active flag) - no
 * deploy required, per PromotionPricingService resolving pricing live from this data. Also the
 * single source of truth for resolving which campaign (if any) is currently live on a channel's
 * public storefront - see {@link #findActiveCustomerCampaign}.
 */
@Service
public class PromotionCampaignService {

    private static final Logger log = LoggerFactory.getLogger(PromotionCampaignService.class);

    private final PromotionCampaignRepository campaigns;
    private final PromotionPlanRepository plans;
    private final PromotionCampaignMapper mapper;

    public PromotionCampaignService(
            PromotionCampaignRepository campaigns, PromotionPlanRepository plans, PromotionCampaignMapper mapper) {
        this.campaigns = campaigns;
        this.plans = plans;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PromotionCampaignResponse> list() {
        return campaigns.findAllByOrderByIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public PromotionCampaignResponse create(AdminPromotionCampaignRequest request) {
        if (campaigns.findByCode(request.code().trim()).isPresent()) {
            throw new BadRequestException("A promotion campaign with this code already exists");
        }
        requireSupportedPricingFields(request.pricingType(), request.discountPercent(), request.fixedPrice());
        requireValidDateRange(request.startsAt(), request.endsAt());
        requireValidStorefrontFields(
                request.name(), request.headline(), request.message(), request.ctaLabel(),
                request.customerVisible(), request.showBanner(), request.showModal());
        // A brand-new campaign is always active=true at construction (see PromotionCampaign's
        // `active` field default) until setActive(false) is called separately, so that's the state
        // to check overlap against here.
        requireNoOverlappingStorefrontCampaign(
                request.sourceChannel(), request.customerVisible(), true, request.startsAt(), request.endsAt(), null);
        Set<PromotionPlan> plansForCampaign = requirePlans(request.planIds());

        PromotionCampaign campaign = campaigns.save(new PromotionCampaign(
                request.code().trim(),
                request.name().trim(),
                request.description().trim(),
                request.sourceChannel(),
                request.pricingType(),
                request.discountPercent(),
                request.fixedPrice(),
                request.minimumPrice(),
                request.startsAt(),
                request.endsAt(),
                plansForCampaign,
                trimToNull(request.headline()),
                trimToNull(request.message()),
                trimToNull(request.ctaLabel()),
                request.customerVisible(),
                request.showBanner(),
                request.showModal()));
        return mapper.toResponse(campaign);
    }

    @Transactional
    public PromotionCampaignResponse update(Long id, AdminPromotionCampaignUpdateRequest request) {
        PromotionCampaign campaign = requireCampaign(id);
        requireSupportedPricingFields(campaign.getPricingType(), request.discountPercent(), request.fixedPrice());
        requireValidDateRange(request.startsAt(), request.endsAt());
        requireValidStorefrontFields(
                request.name(), request.headline(), request.message(), request.ctaLabel(),
                request.customerVisible(), request.showBanner(), request.showModal());
        requireNoOverlappingStorefrontCampaign(
                campaign.getSourceChannel(), request.customerVisible(), request.active(),
                request.startsAt(), request.endsAt(), campaign.getId());
        Set<PromotionPlan> plansForCampaign = requirePlans(request.planIds());

        campaign.update(
                request.name().trim(),
                request.description().trim(),
                request.discountPercent(),
                request.fixedPrice(),
                request.minimumPrice(),
                request.startsAt(),
                request.endsAt(),
                plansForCampaign,
                trimToNull(request.headline()),
                trimToNull(request.message()),
                trimToNull(request.ctaLabel()),
                request.customerVisible(),
                request.showBanner(),
                request.showModal());
        campaign.setActive(request.active());
        return mapper.toResponse(campaign);
    }

    @Transactional
    public PromotionCampaignResponse setActive(Long id, boolean active) {
        PromotionCampaign campaign = requireCampaign(id);
        if (active) {
            requireNoOverlappingStorefrontCampaign(
                    campaign.getSourceChannel(), campaign.isCustomerVisible(), true,
                    campaign.getStartsAt(), campaign.getEndsAt(), campaign.getId());
        }
        campaign.setActive(active);
        return mapper.toResponse(campaign);
    }

    /**
     * The Tuition (or other channel) storefront's single currently-live campaign, for the
     * banner/modal presentation endpoint - GET /api/tuition/promotions/campaign. Configuration-time
     * validation ({@link #requireNoOverlappingStorefrontCampaign}) is meant to make more than one
     * match impossible; if it ever happens anyway (e.g. data edited outside this service), this
     * logs a warning and deterministically picks the most recently started campaign rather than
     * either crashing the storefront or silently relying on row order.
     */
    @Transactional(readOnly = true)
    public Optional<PromotionCampaign> findActiveCustomerCampaign(SourceChannel channel, Instant now) {
        List<PromotionCampaign> matches = campaigns.findBySourceChannel(channel).stream()
                .filter(PromotionCampaign::isActive)
                .filter(PromotionCampaign::isCustomerVisible)
                .filter(c -> !c.getStartsAt().isAfter(now) && c.getEndsAt().isAfter(now))
                .toList();
        if (matches.size() > 1) {
            log.warn("Ambiguous storefront campaigns for channel {}: {} active customer-visible campaigns overlap at {} ({})",
                    channel, matches.size(), now, matches.stream().map(PromotionCampaign::getCode).toList());
        }
        return matches.stream().max(Comparator.comparing(PromotionCampaign::getStartsAt));
    }

    private void requireValidStorefrontFields(
            String name, String headline, String message, String ctaLabel,
            boolean customerVisible, boolean showBanner, boolean showModal) {
        if ((showBanner || showModal) && !customerVisible) {
            throw new BadRequestException("showBanner/showModal require customerVisible to be true");
        }
        if (customerVisible) {
            requireNonBlank("name", name);
            requireNonBlank("headline", headline);
            requireNonBlank("message", message);
            requireNonBlank("ctaLabel", ctaLabel);
        }
    }

    private void requireNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("A customer-visible campaign requires a non-blank " + field);
        }
    }

    // "Reject overlapping active storefront campaigns during configuration" (as opposed to
    // resolving ambiguity arbitrarily at serve time) - checked on create, update, and activate,
    // since setActive(true) can flip a campaign live independently of update(). Only
    // active+customerVisible campaigns can ever collide on the storefront, so anything else is
    // exempt regardless of date overlap.
    private void requireNoOverlappingStorefrontCampaign(
            SourceChannel channel, boolean customerVisible, boolean active,
            Instant startsAt, Instant endsAt, Long excludeId) {
        if (!customerVisible || !active) {
            return;
        }
        boolean overlaps = campaigns.findBySourceChannel(channel).stream()
                .filter(c -> excludeId == null || !c.getId().equals(excludeId))
                .filter(PromotionCampaign::isActive)
                .filter(PromotionCampaign::isCustomerVisible)
                .anyMatch(c -> c.getStartsAt().isBefore(endsAt) && startsAt.isBefore(c.getEndsAt()));
        if (overlaps) {
            throw new BadRequestException(
                    "Another active, customer-visible campaign already overlaps this date range for the " + channel + " channel");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireSupportedPricingFields(
            PricingType pricingType, BigDecimal discountPercent, BigDecimal fixedPrice) {
        if (pricingType == PricingType.FIXED_PRICE) {
            if (fixedPrice == null || discountPercent != null) {
                throw new BadRequestException("A FIXED_PRICE campaign requires fixedPrice and must not set discountPercent");
            }
        } else {
            if (discountPercent == null || fixedPrice != null) {
                throw new BadRequestException(
                        "A PERCENTAGE_DISCOUNT campaign requires discountPercent and must not set fixedPrice");
            }
        }
    }

    private void requireValidDateRange(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
    }

    private Set<PromotionPlan> requirePlans(List<Long> planIds) {
        List<PromotionPlan> found = plans.findAllById(planIds);
        if (found.size() != Set.copyOf(planIds).size()) {
            throw new NotFoundException("One or more promotion plans not found");
        }
        return new HashSet<>(found);
    }

    @Transactional(readOnly = true)
    public PromotionCampaign requireCampaign(Long id) {
        return campaigns.findById(id).orElseThrow(() -> new NotFoundException("Promotion campaign not found"));
    }

    // --- Channel-scoped admin surface (Tuition admin console) -------------------------------
    // Additive overloads only - every method above stays untouched and keeps serving the
    // cross-channel MAIN_SITE admin UI exactly as before. Overlap/pricing/date validation is
    // entirely inherited by delegating to create/update/setActive above - never reimplemented.

    @Transactional(readOnly = true)
    public List<PromotionCampaignResponse> list(SourceChannel restrictToChannel) {
        List<PromotionCampaign> results = restrictToChannel == null
                ? campaigns.findAllByOrderByIdAsc()
                : campaigns.findBySourceChannel(restrictToChannel);
        return results.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public PromotionCampaignResponse create(AdminPromotionCampaignRequest request, SourceChannel restrictToChannel) {
        if (restrictToChannel != null) {
            if (request.sourceChannel() != restrictToChannel) {
                throw new BadRequestException("This admin console can only manage " + restrictToChannel + " campaigns");
            }
            requirePlansChannelMatch(request.planIds(), restrictToChannel);
        }
        return create(request);
    }

    @Transactional
    public PromotionCampaignResponse update(Long id, AdminPromotionCampaignUpdateRequest request, SourceChannel restrictToChannel) {
        requireChannelMatch(id, restrictToChannel);
        if (restrictToChannel != null) {
            requirePlansChannelMatch(request.planIds(), restrictToChannel);
        }
        return update(id, request);
    }

    @Transactional
    public PromotionCampaignResponse setActive(Long id, boolean active, SourceChannel restrictToChannel) {
        requireChannelMatch(id, restrictToChannel);
        return setActive(id, active);
    }

    // 404s (never leaks that the campaign exists in another channel) - same shape as
    // AdService.requireAny/requireOwned.
    private void requireChannelMatch(Long id, SourceChannel restrictToChannel) {
        if (restrictToChannel == null) {
            return;
        }
        if (requireCampaign(id).getSourceChannel() != restrictToChannel) {
            throw new NotFoundException("Promotion campaign not found");
        }
    }

    private void requirePlansChannelMatch(List<Long> planIds, SourceChannel restrictToChannel) {
        List<PromotionPlan> found = plans.findAllById(planIds);
        boolean mismatch = found.stream().anyMatch(plan -> plan.getSlot().getSourceChannel() != restrictToChannel);
        if (mismatch) {
            throw new BadRequestException("All selected promotion plans must belong to the " + restrictToChannel + " channel");
        }
    }
}
