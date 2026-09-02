package com.slmanju.ceylonads.tuition.service;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.mapper.MediaMapper;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionSlotRepository;
import com.slmanju.ceylonads.tuition.dto.TuitionFeaturedCardResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionPromotionResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionPromotionsResponse;
import com.slmanju.ceylonads.tuition.mapper.TuitionClassMapper;
import com.slmanju.ceylonads.tuition.repository.TuitionAdAttributeValueRepository;
import com.slmanju.ceylonads.tuition.repository.TuitionPromotionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Isolated read path for the CeylonAds Tuition UI's search-page promotions (top banner + 3
 * sidebar positions) - {@code GET /api/tuition/promotions}. Reuses the shared promotion_slots/
 * promotion_plans/promotions tables exactly as {@link TuitionFeaturedService} does for the
 * homepage carousel, resolving each of the 4 tuition search slots by its unique code rather than
 * placement type, so this stays additive and never touches the resolution paths any other
 * placement (HOME_FEATURED, CATEGORY_FEATURED, TOP_SEARCH, the existing AD_DETAIL_SIDEBAR slot)
 * already depends on.
 */
@Service
public class TuitionPromotionService {

    static final String TOP_BANNER_CODE = "TUITION_SEARCH_TOP_BANNER";
    static final String SIDEBAR_TOP_CODE = "TUITION_SEARCH_SIDEBAR_TOP";
    static final String SIDEBAR_MIDDLE_CODE = "TUITION_SEARCH_SIDEBAR_MIDDLE";
    static final String SIDEBAR_BOTTOM_CODE = "TUITION_SEARCH_SIDEBAR_BOTTOM";

    private static final List<String> ALL_SLOT_CODES =
            List.of(TOP_BANNER_CODE, SIDEBAR_TOP_CODE, SIDEBAR_MIDDLE_CODE, SIDEBAR_BOTTOM_CODE);
    private static final List<String> SIDEBAR_CODES = List.of(SIDEBAR_TOP_CODE, SIDEBAR_MIDDLE_CODE, SIDEBAR_BOTTOM_CODE);

    private static final List<String> CARD_ATTRIBUTE_KEYS = List.of("subject", "grade", "curriculum", "medium", "classMode");

    private static final String BADGE = "SPONSORED";
    private static final String AD_TYPE = "AD";
    private static final String BANNER_TYPE = "BANNER";
    private static final String AD_CTA_LABEL = "View Class";

    private final PromotionSlotRepository promotionSlots;
    private final PromotionRepository promotions;
    private final TuitionPromotionRepository tuitionPromotions;
    private final TuitionAdAttributeValueRepository tuitionAttributeValues;
    private final AttributeOptionRepository attributeOptions;
    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;
    private final AdLocationService adLocationService;
    private final TuitionClassMapper tuitionClassMapper;

    public TuitionPromotionService(
            PromotionSlotRepository promotionSlots,
            PromotionRepository promotions,
            TuitionPromotionRepository tuitionPromotions,
            TuitionAdAttributeValueRepository tuitionAttributeValues,
            AttributeOptionRepository attributeOptions,
            MediaRepository mediaRepository,
            MediaMapper mediaMapper,
            AdLocationService adLocationService,
            TuitionClassMapper tuitionClassMapper) {
        this.promotionSlots = promotionSlots;
        this.promotions = promotions;
        this.tuitionPromotions = tuitionPromotions;
        this.tuitionAttributeValues = tuitionAttributeValues;
        this.attributeOptions = attributeOptions;
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.adLocationService = adLocationService;
        this.tuitionClassMapper = tuitionClassMapper;
    }

    // Target shape per request: 1 query to resolve the requested slots, 1 query for the banner
    // slot's active promotions, 1 query across all 3 sidebar slots for their active promotions,
    // then (only if any sidebar promotion exists) the same small batched lookups
    // TuitionFeaturedService uses - attribute values (1), options for selects present (0-1),
    // locations (1), primary media (1). No generic search, no COUNT, no full category tree load.
    @Transactional(readOnly = true)
    public TuitionPromotionsResponse getSearchPromotions(List<String> requestedSlots) {
        List<String> codes = (requestedSlots == null || requestedSlots.isEmpty()) ? ALL_SLOT_CODES : requestedSlots;
        Map<String, PromotionSlot> slotsByCode = promotionSlots.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(PromotionSlot::getCode, slot -> slot));

        Instant now = Instant.now();

        PromotionSlot topBannerSlot = slotsByCode.get(TOP_BANNER_CODE);
        List<TuitionPromotionResponse> topBanner = topBannerSlot == null ? List.of() : bannerPromotions(topBannerSlot, now);

        List<PromotionSlot> sidebarSlots = SIDEBAR_CODES.stream()
                .map(slotsByCode::get)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<TuitionPromotionResponse>> sidebarBySlotId = sidebarSlots.isEmpty()
                ? Map.of()
                : adPromotionsBySlot(sidebarSlots, now);

        return new TuitionPromotionsResponse(
                topBanner,
                forSlot(slotsByCode, sidebarBySlotId, SIDEBAR_TOP_CODE),
                forSlot(slotsByCode, sidebarBySlotId, SIDEBAR_MIDDLE_CODE),
                forSlot(slotsByCode, sidebarBySlotId, SIDEBAR_BOTTOM_CODE));
    }

    private List<TuitionPromotionResponse> forSlot(
            Map<String, PromotionSlot> slotsByCode, Map<Long, List<TuitionPromotionResponse>> bySlotId, String code) {
        PromotionSlot slot = slotsByCode.get(code);
        return slot == null ? List.of() : bySlotId.getOrDefault(slot.getId(), List.of());
    }

    // Banner-kind promotions carry no ad, so this deliberately reuses the generic
    // PromotionRepository query (no Ad_Status join) rather than TuitionPromotionRepository's
    // ad-joined ones, which would silently exclude every banner via an inner join on a null ad.
    private List<TuitionPromotionResponse> bannerPromotions(PromotionSlot slot, Instant now) {
        List<Promotion> active = promotions.findByStatusAndPlan_SlotAndEndsAtAfterOrderByStartsAtDescIdAsc(
                PromotionStatus.ACTIVE, slot, now, PageRequest.of(0, Math.max(1, slot.getVisibleCount())));

        List<TuitionPromotionResponse> result = new ArrayList<>();
        int order = 1;
        for (Promotion promotion : active) {
            Media bannerMedia = promotion.getBannerMedia();
            result.add(new TuitionPromotionResponse(
                    promotion.getId(),
                    slot.getCode(),
                    BANNER_TYPE,
                    null,
                    null,
                    bannerMedia == null ? null : mediaMapper.toResponse(bannerMedia).url(),
                    BADGE,
                    null,
                    promotion.getTargetUrl(),
                    "EXTERNAL",
                    null,
                    null,
                    order++));
        }
        return result;
    }

    // sourceChannel = TUITION gates which ad a sidebar promotion may surface - the slots
    // themselves are category-bound (a promotion-config concern), not ad-filtered.
    private Map<Long, List<TuitionPromotionResponse>> adPromotionsBySlot(List<PromotionSlot> slots, Instant now) {
        List<Promotion> active = tuitionPromotions.findByStatusAndPlan_SlotInAndAd_StatusAndAd_SourceChannelAndEndsAtAfterOrderByStartsAtDescIdAsc(
                PromotionStatus.ACTIVE, slots, AdStatus.ACTIVE, SourceChannel.TUITION, now);
        if (active.isEmpty()) {
            return Map.of();
        }

        List<Ad> ads = active.stream().map(Promotion::getAd).toList();
        List<Long> adIds = ads.stream().map(Ad::getId).toList();

        List<AdAttributeValue> attributeRows = tuitionAttributeValues.findByAdIdInAndKeyIn(adIds, CARD_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        Map<Long, List<AdAttributeValue>> attributeRowsByAd = attributeRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAd().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<LocationResponse>> locationsByAd = adLocationService.toResponsesForAds(adIds);
        Map<Long, Media> primaryMediaByAd = primaryMediaByAd(adIds);

        Map<Long, List<TuitionPromotionResponse>> bySlotId = new LinkedHashMap<>();
        int order = 1;
        for (Promotion promotion : active) {
            Ad ad = promotion.getAd();
            TuitionFeaturedCardResponse card = tuitionClassMapper.toFeaturedCardResponse(
                    ad,
                    primaryMediaByAd.get(ad.getId()),
                    attributeRowsByAd.getOrDefault(ad.getId(), List.of()),
                    optionsByDefinition,
                    locationsByAd.getOrDefault(ad.getId(), List.of()).stream().findFirst().orElse(null));

            TuitionPromotionResponse response = new TuitionPromotionResponse(
                    promotion.getId(),
                    promotion.getPlan().getSlot().getCode(),
                    AD_TYPE,
                    card.title(),
                    subtitle(card),
                    card.primaryImageUrl(),
                    BADGE,
                    AD_CTA_LABEL,
                    null,
                    AD_TYPE,
                    card.id(),
                    card.slug(),
                    order++);

            bySlotId.computeIfAbsent(promotion.getPlan().getSlot().getId(), k -> new ArrayList<>()).add(response);
        }
        return bySlotId;
    }

    private String subtitle(TuitionFeaturedCardResponse card) {
        List<String> parts = Stream.of(card.subject(), card.level(), card.primaryLocation() == null ? null : card.primaryLocation().name())
                .filter(Objects::nonNull)
                .toList();
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    // Duplicated from TuitionFeaturedService/TuitionClassService, which independently duplicate
    // this same helper for the same reason - each tuition read path resolves labels for only the
    // small fixed set of attribute keys it fetched, not the whole attribute_options table.
    private Map<Long, List<AttributeOption>> optionsBySelectDefinition(List<AdAttributeValue> rows) {
        List<Long> selectDefinitionIds = rows.stream()
                .map(AdAttributeValue::getAttributeDefinition)
                .filter(def -> def.getDataType() == AttributeDataType.SELECT || def.getDataType() == AttributeDataType.MULTI_SELECT)
                .map(AttributeDefinition::getId)
                .distinct()
                .toList();
        if (selectDefinitionIds.isEmpty()) {
            return Map.of();
        }
        return attributeOptions.findByAttributeDefinitionIdIn(selectDefinitionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getAttributeDefinition().getId()));
    }

    private Map<Long, Media> primaryMediaByAd(List<Long> adIds) {
        Map<Long, Media> primary = new LinkedHashMap<>();
        for (Media media : mediaRepository.findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(adIds)) {
            primary.putIfAbsent(media.getAd().getId(), media);
        }
        return primary;
    }
}
