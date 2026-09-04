package com.slmanju.ceylonads.promotion.service;

import com.slmanju.ceylonads.ad.dto.AdAttributeResponse;
import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.mapper.AdMapper;
import com.slmanju.ceylonads.ad.service.AdAttributeService;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.service.CustomerService;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.service.MediaService;
import com.slmanju.ceylonads.promotion.dto.AdminCreatePromotionRequest;
import com.slmanju.ceylonads.promotion.dto.CompatiblePromotionPlanResponse;
import com.slmanju.ceylonads.promotion.dto.CreatePromotionRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotAvailabilityResponse;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionKind;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.event.PromotionCreatedEvent;
import com.slmanju.ceylonads.promotion.mapper.PromotionMapper;
import com.slmanju.ceylonads.promotion.mapper.PromotionPlanMapper;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central place for the promotion lifecycle (create -> pending payment -> activate -> expire /
 * cancel) and for the read-side queries that let ad listings reflect active promotions. Payment
 * approval calls {@link #activate(Long)} directly instead of duplicating this logic.
 */
@Service
public class PromotionService {

    private static final Set<PromotionStatus> BLOCKING_STATUSES = EnumSet.of(
            PromotionStatus.PENDING_PAYMENT, PromotionStatus.PENDING_APPROVAL, PromotionStatus.ACTIVE);

    private static final Set<PromotionStatus> ADMIN_CANCELLABLE_STATUSES = EnumSet.of(
            PromotionStatus.PENDING_PAYMENT, PromotionStatus.PENDING_APPROVAL, PromotionStatus.ACTIVE);

    private final PromotionRepository promotions;
    private final PromotionPlanRepository plans;
    private final PromotionPlanService planService;
    private final PromotionSlotService slotService;
    private final AdService adService;
    private final CustomerService customerService;
    private final MediaService mediaService;
    private final AdAttributeService adAttributeService;
    private final AdLocationService adLocationService;
    private final PromotionMapper mapper;
    private final PromotionPlanMapper planMapper;
    private final AdMapper adMapper;
    private final ApplicationEventPublisher events;
    private final PromotionPricingService pricingService;

    public PromotionService(
            PromotionRepository promotions,
            PromotionPlanRepository plans,
            PromotionPlanService planService,
            PromotionSlotService slotService,
            AdService adService,
            CustomerService customerService,
            MediaService mediaService,
            AdAttributeService adAttributeService,
            AdLocationService adLocationService,
            PromotionMapper mapper,
            PromotionPlanMapper planMapper,
            AdMapper adMapper,
            ApplicationEventPublisher events,
            PromotionPricingService pricingService) {
        this.promotions = promotions;
        this.plans = plans;
        this.planService = planService;
        this.slotService = slotService;
        this.adService = adService;
        this.customerService = customerService;
        this.mediaService = mediaService;
        this.adAttributeService = adAttributeService;
        this.adLocationService = adLocationService;
        this.mapper = mapper;
        this.planMapper = planMapper;
        this.adMapper = adMapper;
        this.events = events;
        this.pricingService = pricingService;
    }

    @Transactional
    public PromotionResponse create(String username, CreatePromotionRequest request) {
        Customer customer = customerService.requireByUsername(username);
        Ad ad = adService.requireOwned(request.adId(), username);
        return createForAd(customer, ad, request.promotionPlanId());
    }

    /**
     * Tuition My Classes' "Promote" action. Same creation rules as {@link #create}, but the ad
     * lookup is channel-scoped ({@link AdService#requireOwned(Long, String, SourceChannel)} with
     * {@link SourceChannel#TUITION}) so a tutor can never promote a MAIN_SITE/BOARDING ad - by
     * source_channel, not by category - through this entry point, even by supplying another ad's
     * id. 404s (not 403) on a channel mismatch, matching that method's existing behavior.
     */
    @Transactional
    public PromotionResponse createForTuitionAd(String username, Long adId, Long promotionPlanId) {
        Customer customer = customerService.requireByUsername(username);
        Ad ad = adService.requireOwned(adId, username, SourceChannel.TUITION);
        return createForAd(customer, ad, promotionPlanId);
    }

    private PromotionResponse createForAd(Customer customer, Ad ad, Long promotionPlanId) {
        if (ad.getStatus() != AdStatus.ACTIVE) {
            throw new BadRequestException("Only active ads can be promoted");
        }
        // A Tuition ad can still read ACTIVE in the brief window between its expiresAt passing and
        // the next scheduler sweep (see TuitionExpiryScheduler); it must not be sellable while
        // publicly invisible - the tutor has to Renew first (see TuitionClassService.renew).
        if (ad.getSourceChannel() == SourceChannel.TUITION && ad.getExpiresAt() != null && !ad.getExpiresAt().isAfter(Instant.now())) {
            throw new BadRequestException("This class has expired. Renew it before purchasing a promotion.");
        }

        PromotionPlan plan = planService.requirePlan(promotionPlanId);
        if (!plan.isActive()) {
            throw new BadRequestException("This promotion plan is no longer available");
        }

        PromotionSlot slot = plan.getSlot();
        if (!slot.isActive()) {
            throw new BadRequestException("This placement is no longer available");
        }
        if (slot.getPlacementType().isBanner()) {
            throw new BadRequestException("This plan is for a banner placement and cannot be purchased for an ad");
        }
        // A category-unbound slot is compatible with any ad by category alone (see
        // categoryCompatible), so the channel must be checked independently - otherwise an ad from
        // one channel could be purchased against another channel's plan just by knowing its id. This
        // also backs createForTuitionAd's "selected plan belongs to TUITION" requirement without
        // hardcoding TUITION here, since this path is shared with the generic create().
        if (slot.getSourceChannel() != ad.getSourceChannel()) {
            throw new BadRequestException("This promotion plan is not available for this ad");
        }
        requireCategoryCompatible(ad, slot);

        if (promotions.existsByAdIdAndPlan_SlotAndStatusIn(ad.getId(), slot, BLOCKING_STATUSES)) {
            throw new BadRequestException("This ad already has a pending or active promotion for this placement");
        }

        // The customer never supplies a price: whatever campaign (if any) is active right now for
        // this plan's channel is resolved and snapshotted here, server-side, as the charged price.
        // A later price/campaign change never retroactively alters this already-sold promotion.
        BigDecimal chargedPrice = pricingService.resolve(plan, Instant.now()).effectivePrice();
        CreationPlan creationPlan = resolveCreationPlan(plan, chargedPrice, false);
        Promotion promotion = promotions.save(new Promotion(
                ad, customer, plan, chargedPrice, plan.getDurationDays(), creationPlan.initialStatus(), false));
        finishCreation(promotion, creationPlan);
        return mapper.toResponse(promotion);
    }

    /**
     * Promotion plans the given ad is actually eligible to buy: active plans on active slots that
     * aren't banner-only, and whose slot category (if any) contains the ad's own category. Each
     * result carries the slot's live availability for that plan's own duration starting now, so
     * the frontend never has to compute capacity itself.
     */
    @Transactional(readOnly = true)
    public List<CompatiblePromotionPlanResponse> compatiblePlansForAd(Long adId, String username) {
        Ad ad = adService.requireOwned(adId, username);
        return compatiblePlansForAd(ad);
    }

    /**
     * Tuition My Classes' plan-selection step. Same eligibility rules as
     * {@link #compatiblePlansForAd(Long, String)}, but channel-scoped to TUITION - see
     * {@link #createForTuitionAd} for why.
     */
    @Transactional(readOnly = true)
    public List<CompatiblePromotionPlanResponse> compatiblePlansForTuitionAd(Long adId, String username) {
        Ad ad = adService.requireOwned(adId, username, SourceChannel.TUITION);
        return compatiblePlansForAd(ad);
    }

    /**
     * The Tuition UI's dedicated promotion catalog - GET /api/tuition/promotions/plans. Every
     * active, non-banner plan on a TUITION-channel slot, each paired with its live slot
     * availability for that plan's own duration starting now (same computation
     * {@link #compatiblePlansForAd} uses), but not narrowed to any one ad's category eligibility -
     * this is the general catalog a tutor browses before picking which class to promote, so the
     * Tuition UI never has to fall back to the generic /api/promotion-plans catalog to learn
     * pricing/availability.
     */
    @Transactional(readOnly = true)
    public List<CompatiblePromotionPlanResponse> tuitionPlans() {
        Instant now = Instant.now();
        return plans.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(plan -> plan.getSlot().isActive())
                .filter(plan -> !plan.getSlot().getPlacementType().isBanner())
                .filter(plan -> plan.getSlot().getSourceChannel() == SourceChannel.TUITION)
                .map(plan -> {
                    PromotionSlotAvailabilityResponse availability = slotService.availabilityFor(
                            plan.getSlot(), now, now.plus(Duration.ofDays(plan.getDurationDays())));
                    return new CompatiblePromotionPlanResponse(planMapper.toResponse(plan), availability.available(), availability.remainingCapacity());
                })
                .toList();
    }

    private List<CompatiblePromotionPlanResponse> compatiblePlansForAd(Ad ad) {
        Instant now = Instant.now();

        return plans.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(plan -> plan.getSlot().isActive())
                .filter(plan -> !plan.getSlot().getPlacementType().isBanner())
                .filter(plan -> plan.getSlot().getSourceChannel() == ad.getSourceChannel())
                .filter(plan -> categoryCompatible(ad.getCategory(), plan.getSlot().getCategory()))
                .map(plan -> {
                    PromotionSlotAvailabilityResponse availability = slotService.availabilityFor(
                            plan.getSlot(), now, now.plus(Duration.ofDays(plan.getDurationDays())));
                    return new CompatiblePromotionPlanResponse(
                            planMapper.toResponse(plan), availability.available(), availability.remainingCapacity());
                })
                .toList();
    }

    @Transactional
    public List<PromotionResponse> mine(String username) {
        Customer customer = customerService.requireByUsername(username);
        expireOverdue();
        return promotions.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse getOwned(Long id, String username) {
        Customer customer = customerService.requireByUsername(username);
        return mapper.toResponse(requireOwned(id, customer.getId()));
    }

    /**
     * Tuition My Classes' own promotion list - GET /api/tuition/promotions/my. Same as
     * {@link #mine(String)}, scoped so a tutor who also sells on MAIN_SITE/BOARDING under the same
     * account never sees those promotions mixed into their Tuition list.
     */
    @Transactional
    public List<PromotionResponse> mineForTuition(String username) {
        Customer customer = customerService.requireByUsername(username);
        expireOverdue();
        return promotions.findByCustomerIdAndAd_SourceChannelOrderByCreatedAtDesc(customer.getId(), SourceChannel.TUITION)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Tuition-scoped promotion detail - GET /api/tuition/promotions/{id}. Same ownership check as
     * {@link #getOwned(Long, String)}, plus a channel check so this can never be used to view a
     * MAIN_SITE/BOARDING promotion just because it belongs to the same customer - 404s (not 403) on
     * a channel mismatch, matching {@link #createForTuitionAd}'s existing behavior.
     */
    @Transactional(readOnly = true)
    public PromotionResponse getOwnedForTuition(Long id, String username) {
        Customer customer = customerService.requireByUsername(username);
        Promotion promotion = requireOwned(id, customer.getId());
        if (promotion.getKind() != PromotionKind.AD_PROMOTION || promotion.getAd().getSourceChannel() != SourceChannel.TUITION) {
            throw new NotFoundException("Promotion not found");
        }
        return mapper.toResponse(promotion);
    }

    @Transactional
    public PromotionResponse cancelOwned(Long id, String username) {
        Customer customer = customerService.requireByUsername(username);
        Promotion promotion = requireOwned(id, customer.getId());
        if (promotion.getStatus() != PromotionStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only promotions pending payment can be cancelled");
        }
        promotion.cancel();
        return mapper.toResponse(promotion);
    }

    @Transactional
    public List<PromotionResponse> adminList(PromotionStatus statusFilter) {
        expireOverdue();
        List<Promotion> results = statusFilter == null
                ? promotions.findAllByOrderByCreatedAtDesc()
                : promotions.findByStatusOrderByCreatedAtDesc(statusFilter);
        return results.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse adminGet(Long id) {
        return mapper.toResponse(requireAny(id));
    }

    /**
     * Manual activation exists as an admin testing/override aid, standing in for the
     * bank-transfer approval step that normally drives activation. It is deliberately an explicit
     * admin action, not something a customer can trigger.
     */
    @Transactional
    public PromotionResponse activate(Long id) {
        Promotion promotion = requireAny(id);
        if (promotion.getStatus() != PromotionStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only promotions pending payment can be activated");
        }
        if (promotion.getKind() == PromotionKind.AD_PROMOTION
                && promotion.getAd().getStatus() != AdStatus.ACTIVE) {
            throw new BadRequestException("The ad for this promotion is no longer active");
        }
        activateWithCapacityCheck(promotion);
        return mapper.toResponse(promotion);
    }

    /**
     * The single generic admin creation path for any placement type (ad-linked or banner). The
     * slot's placement type - not a client-supplied "kind" - decides whether an ad or a banner
     * image is required, so the frontend can offer one "New Promotion" flow instead of a form per
     * placement type. Price, duration, payment requirement and approval requirement all come from
     * the plan; the client never supplies them directly.
     */
    @Transactional
    public PromotionResponse adminCreate(AdminCreatePromotionRequest request) {
        Customer customer = customerService.requireById(request.customerId());
        PromotionPlan plan = planService.requirePlan(request.promotionPlanId());
        if (!plan.isActive()) {
            throw new BadRequestException("This promotion plan is no longer available");
        }
        PromotionSlot slot = plan.getSlot();
        if (!slot.isActive()) {
            throw new BadRequestException("This placement is no longer available");
        }

        CreationPlan creationPlan = resolveCreationPlan(plan, plan.getPrice(), request.paymentWaived());
        Promotion promotion;
        if (slot.getPlacementType().isBanner()) {
            if (request.bannerMediaId() == null) {
                throw new BadRequestException("A banner image is required for this placement");
            }
            Media bannerMedia = mediaService.requireBannerMedia(request.bannerMediaId());
            promotion = Promotion.forBanner(
                    customer, plan, plan.getPrice(), plan.getDurationDays(), bannerMedia, trimToNull(request.targetUrl()),
                    creationPlan.initialStatus(), request.paymentWaived());
        } else {
            if (request.adId() == null) {
                throw new BadRequestException("An ad is required for this placement");
            }
            Ad ad = adService.requireOwnedByCustomer(request.adId(), customer.getId());
            if (ad.getStatus() != AdStatus.ACTIVE) {
                throw new BadRequestException("Only active ads can be promoted");
            }
            requireCategoryCompatible(ad, slot);
            if (promotions.existsByAdIdAndPlan_SlotAndStatusIn(ad.getId(), slot, BLOCKING_STATUSES)) {
                throw new BadRequestException("This ad already has a pending or active promotion for this placement");
            }
            promotion = new Promotion(
                    ad, customer, plan, plan.getPrice(), plan.getDurationDays(), creationPlan.initialStatus(), request.paymentWaived());
        }

        promotion = promotions.save(promotion);
        finishCreation(promotion, creationPlan);
        return mapper.toResponse(promotion);
    }

    /**
     * Admin decision on a promotion awaiting approval (free plans, or a paid plan whose payment
     * was waived). Goes through the same capacity-checked activation every other path uses.
     */
    @Transactional
    public PromotionResponse adminApprove(Long id) {
        Promotion promotion = requireAny(id);
        if (promotion.getStatus() != PromotionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only promotions pending approval can be approved");
        }
        if (promotion.getKind() == PromotionKind.AD_PROMOTION
                && promotion.getAd().getStatus() != AdStatus.ACTIVE) {
            throw new BadRequestException("The ad for this promotion is no longer active");
        }
        activateWithCapacityCheck(promotion);
        return mapper.toResponse(promotion);
    }

    @Transactional
    public PromotionResponse adminCancel(Long id) {
        Promotion promotion = requireAny(id);
        if (!ADMIN_CANCELLABLE_STATUSES.contains(promotion.getStatus())) {
            throw new BadRequestException("This promotion cannot be cancelled");
        }
        promotion.cancel();
        return mapper.toResponse(promotion);
    }

    @Transactional
    public void expireOverdue() {
        promotions.expireOverdue(Instant.now());
    }

    /**
     * Ads with an active Homepage Featured promotion, for the homepage carousel. The requested
     * limit is always clamped to the HOME_FEATURED slot's own capacity, so this endpoint can never
     * return an unbounded dataset regardless of what a caller asks for.
     */
    @Transactional(readOnly = true)
    public List<AdResponse> homeFeaturedAds(int limit) {
        int safeLimit = slotService.resolveSlotByPlacementType(PlacementType.HOME_FEATURED)
                .map(slot -> Math.max(1, Math.min(limit, slot.getCapacity())))
                .orElse(limit);
        List<Promotion> featured = promotions.findByStatusAndPlan_Slot_PlacementTypeAndEndsAtAfterAndAd_SourceChannelOrderByStartsAtDescIdAsc(
                PromotionStatus.ACTIVE, PlacementType.HOME_FEATURED, Instant.now(), SourceChannel.MAIN_SITE, PageRequest.of(0, safeLimit));
        return adResponsesForPromotedAds(featured);
    }

    /**
     * Ads with an active promotion on the CATEGORY_FEATURED slot bound to the given category (or
     * one of its ancestors), for a category page's featured carousel. Bounded by that slot's own
     * capacity; returns empty when the category has no CATEGORY_FEATURED slot.
     */
    @Transactional(readOnly = true)
    public List<AdResponse> categoryFeaturedAds(String categorySlug, int limit) {
        return slotService.resolveCategoryFeaturedSlot(categorySlug)
                .map(slot -> {
                    int safeLimit = Math.max(1, Math.min(limit, slot.getCapacity()));
                    List<Promotion> featured = promotions.findByStatusAndPlan_SlotAndEndsAtAfterAndAd_SourceChannelOrderByStartsAtDescIdAsc(
                            PromotionStatus.ACTIVE, slot, Instant.now(), SourceChannel.MAIN_SITE, PageRequest.of(0, safeLimit));
                    return adResponsesForPromotedAds(featured);
                })
                .orElse(List.of());
    }

    // Shared by homeFeaturedAds/categoryFeaturedAds: batches media and attributes for the whole
    // carousel into two queries total (regardless of how many ads it holds), instead of two
    // queries per promoted ad.
    private List<AdResponse> adResponsesForPromotedAds(List<Promotion> featured) {
        if (featured.isEmpty()) {
            return List.of();
        }
        List<Ad> featuredAds = featured.stream().map(Promotion::getAd).toList();
        List<Long> adIds = featuredAds.stream().map(Ad::getId).toList();
        Map<Long, List<Media>> mediaByAdId = mediaService.byAdIds(adIds);
        Map<Long, List<AdAttributeResponse>> attributesByAdId = adAttributeService.toResponsesForAds(adIds);
        Map<Long, List<LocationResponse>> locationsByAdId = adLocationService.toResponsesForAds(adIds);
        return featuredAds.stream()
                .map(ad -> adMapper.toResponse(ad, true,
                        mediaByAdId.getOrDefault(ad.getId(), List.of()),
                        attributesByAdId.getOrDefault(ad.getId(), List.of()),
                        locationsByAdId.getOrDefault(ad.getId(), List.of())))
                .toList();
    }

    /**
     * How many TOP_SEARCH promoted results should be boosted to the top of a general search/browse
     * request at once (the slot's visibleCount), as opposed to how many TOP_SEARCH campaigns can be
     * sold (its capacity). Falls back to "no cap" if the slot can't be resolved, preserving prior
     * behavior rather than hiding results.
     */
    @Transactional(readOnly = true)
    public int topSearchVisibleCount() {
        return slotService.resolveSlotByPlacementType(PlacementType.TOP_SEARCH)
                .map(PromotionSlot::getVisibleCount)
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * Used by search ranking to order the (small) promoted subset by how recently each promotion
     * started, without needing a sortable association on {@code Ad} itself.
     */
    @Transactional(readOnly = true)
    public Map<Long, Instant> activeStartsAtForAds(Collection<Long> adIds, PlacementType placementType) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        return promotions.findByAdIdInAndStatusAndPlan_Slot_PlacementTypeAndEndsAtAfter(
                        adIds, PromotionStatus.ACTIVE, placementType, Instant.now())
                .stream()
                .collect(Collectors.toMap(p -> p.getAd().getId(), Promotion::getStartsAt));
    }

    /**
     * How many results a specific-slot-code ranking boost (e.g. Tuition's TUITION_SEARCH_BOOST)
     * should promote to the top of matching search results at once - mirrors
     * {@link #topSearchVisibleCount()} but for a placement resolved by exact slot code rather than
     * placement type. Falls back to "no cap" if the slot can't be resolved.
     */
    @Transactional(readOnly = true)
    public int visibleCountForSlotCode(String slotCode) {
        return slotService.resolveSlotByCode(slotCode)
                .map(PromotionSlot::getVisibleCount)
                .orElse(Integer.MAX_VALUE);
    }

    // Exact-slot-code sibling of activeStartsAtForAds(Collection, PlacementType) above.
    @Transactional(readOnly = true)
    public Map<Long, Instant> activeStartsAtForAdsBySlotCode(Collection<Long> adIds, String slotCode) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        return promotions.findByAdIdInAndStatusAndPlan_Slot_CodeAndEndsAtAfter(
                        adIds, PromotionStatus.ACTIVE, slotCode, Instant.now())
                .stream()
                .collect(Collectors.toMap(p -> p.getAd().getId(), Promotion::getStartsAt));
    }

    // What a plan (plus an optional admin payment waiver) implies about how a brand-new promotion
    // should start out: PENDING_PAYMENT with a Payment record, PENDING_APPROVAL with no Payment,
    // or activated immediately subject to slot capacity. paymentRequired=true with
    // approvalRequired=false is rejected at the plan level, so it's never seen here.
    private record CreationPlan(PromotionStatus initialStatus, boolean createPayment, boolean autoActivate) {
    }

    // chargedPrice is the amount actually resolved for this purchase (base price, or a campaign's
    // discounted price - possibly zero). A plan's payment_required/approval_required flags describe
    // its normal paid economics; they never apply to a promotion a live campaign has made free, so
    // that customer is never asked to pay, or wait on approval, for something that costs nothing -
    // see PromotionPricingService.
    private CreationPlan resolveCreationPlan(PromotionPlan plan, BigDecimal chargedPrice, boolean paymentWaived) {
        boolean effectivelyFree = chargedPrice.compareTo(BigDecimal.ZERO) == 0;
        boolean paymentRequired = plan.isPaymentRequired() && !paymentWaived && !effectivelyFree;
        if (paymentRequired) {
            return new CreationPlan(PromotionStatus.PENDING_PAYMENT, true, false);
        }
        if (plan.isApprovalRequired() && !effectivelyFree) {
            return new CreationPlan(PromotionStatus.PENDING_APPROVAL, false, false);
        }
        // Initial status is a placeholder here: finishCreation() immediately activates it.
        return new CreationPlan(PromotionStatus.PENDING_PAYMENT, false, true);
    }

    // Fires the payment-creation event or activates immediately, matching what resolveCreationPlan
    // decided. Must run after the promotion itself is saved and assigned an id.
    private void finishCreation(Promotion promotion, CreationPlan creationPlan) {
        if (creationPlan.createPayment()) {
            // Synchronous, same transaction: if the payment listener throws, promotion creation
            // rolls back with it, so a promotion can never exist without its payment.
            events.publishEvent(new PromotionCreatedEvent(promotion));
        } else if (creationPlan.autoActivate()) {
            activateWithCapacityCheck(promotion);
        }
    }

    // Shared by manual/admin activation, payment approval, admin approval of PENDING_APPROVAL
    // promotions, and free/waived promotions that auto-activate on creation: never activate into a
    // slot that's already at capacity for the window this promotion would occupy starting now.
    private void activateWithCapacityCheck(Promotion promotion) {
        PromotionSlot slot = promotion.getPlan().getSlot();
        if (!slot.isActive()) {
            throw new BadRequestException("This placement is no longer available");
        }
        Instant now = Instant.now();
        Instant end = now.plus(Duration.ofDays(promotion.getDurationDays()));
        long overlapping = promotions.countOverlapping(slot, now, end);
        if (overlapping >= slot.getCapacity()) {
            throw new BadRequestException("This placement is fully booked right now. Please choose a different slot.");
        }
        promotion.activate();

        // The paid-duration guarantee (see Ad#extendExpiryToAtLeast): a Tuition promotion must
        // never outlive the listing it was bought for. Runs in the same transaction as activation
        // itself, so a promotion can never end up active without this protection also applying.
        if (promotion.getKind() == PromotionKind.AD_PROMOTION && promotion.getAd() != null
                && promotion.getAd().getSourceChannel() == SourceChannel.TUITION) {
            promotion.getAd().extendExpiryToAtLeast(promotion.getEndsAt());
        }
    }

    /**
     * The latest-ending currently-active promotion on this ad, if any - backs Tuition's
     * deactivation guard (see TuitionClassService.deactivateOwned): a listing with a live paid
     * promotion can't be deactivated until that promotion ends.
     */
    @Transactional(readOnly = true)
    public Optional<Instant> activePromotionEndsAt(Long adId) {
        return promotions.findTopByAdIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(adId, PromotionStatus.ACTIVE, Instant.now())
                .map(Promotion::getEndsAt);
    }

    private void requireCategoryCompatible(Ad ad, PromotionSlot slot) {
        if (!categoryCompatible(ad.getCategory(), slot.getCategory())) {
            throw new BadRequestException(
                    "This promotion is only available for ads in the " + slot.getCategory().getName() + " category");
        }
    }

    // A slot with no bound category is compatible with any ad. A category-bound slot is
    // compatible with an ad in that exact category or any of its subcategories, since real ads
    // are typically posted to leaf categories (e.g. "Cars") while a slot is more naturally sold
    // against the parent grouping (e.g. "Vehicles").
    private boolean categoryCompatible(Category adCategory, Category slotCategory) {
        if (slotCategory == null) {
            return true;
        }
        Category current = adCategory;
        while (current != null) {
            if (current.getId().equals(slotCategory.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Promotion requireOwned(Long id, Long customerId) {
        Promotion promotion = requireAny(id);
        if (!promotion.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("Not your promotion");
        }
        return promotion;
    }

    private Promotion requireAny(Long id) {
        return promotions.findById(id).orElseThrow(() -> new NotFoundException("Promotion not found"));
    }
}
