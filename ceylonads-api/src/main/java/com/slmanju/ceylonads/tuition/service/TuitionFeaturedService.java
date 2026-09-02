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
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionSlotRepository;
import com.slmanju.ceylonads.promotion.service.PromotionSlotService;
import com.slmanju.ceylonads.tuition.dto.TuitionFeaturedCardResponse;
import com.slmanju.ceylonads.tuition.mapper.TuitionClassMapper;
import com.slmanju.ceylonads.tuition.repository.TuitionAdAttributeValueRepository;
import com.slmanju.ceylonads.tuition.repository.TuitionPromotionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Isolated read path for the CeylonAds Tuition UI's "Featured Tuition" homepage carousel. Reuses
 * the shared CATEGORY_FEATURED promotion slot bound to the Education & Tuition category (the same
 * slot the generic /api/ads/category-featured endpoint reads via
 * {@link com.slmanju.ceylonads.promotion.service.PromotionService#categoryFeaturedAds}) to find
 * which slot to read - a promotion-config lookup, not an ad filter - but maps into the lean
 * {@link TuitionFeaturedCardResponse} shape instead of the full generic AdResponse, and requires
 * the promoted ad to be ACTIVE and source_channel = TUITION (see TuitionPromotionRepository): the
 * slot's category binding says which carousel a promotion was sold into, source_channel is what
 * actually gates whether the ad belongs to this vertical. No generic search or COUNT query is ever
 * triggered here.
 *
 * <p>Also backs other fixed, independently-sellable TUITION_FEATURED-shaped carousels (e.g. the
 * Tuition detail page's top carousel, slot code TUITION_DETAIL_TOP_CAROUSEL - see
 * TuitionFeaturedController) by resolving an explicit slot {@code code} instead of the homepage/
 * search carousels' category-ancestor-walk lookup, so each such placement is its own distinct,
 * independently purchasable slot rather than sharing TUITION_FEATURED's inventory.
 */
@Service
public class TuitionFeaturedService {

    // Master data root, from LocalDataSeeder - see TuitionClassService.TUITION_ROOT_SLUG for the
    // matching read-path constant and AdLocationService.TUITION_ROOT_SLUG for the write-path one.
    private static final String TUITION_ROOT_SLUG = "education-tuition";

    private static final List<String> CARD_ATTRIBUTE_KEYS = List.of("subject", "grade", "curriculum", "medium", "classMode");

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 20;

    private final TuitionPromotionRepository tuitionPromotions;
    private final PromotionSlotService promotionSlotService;
    private final PromotionSlotRepository promotionSlotRepository;
    private final TuitionAdAttributeValueRepository tuitionAttributeValues;
    private final AttributeOptionRepository attributeOptions;
    private final MediaRepository mediaRepository;
    private final AdLocationService adLocationService;
    private final TuitionClassMapper tuitionClassMapper;

    public TuitionFeaturedService(
            TuitionPromotionRepository tuitionPromotions,
            PromotionSlotService promotionSlotService,
            PromotionSlotRepository promotionSlotRepository,
            TuitionAdAttributeValueRepository tuitionAttributeValues,
            AttributeOptionRepository attributeOptions,
            MediaRepository mediaRepository,
            AdLocationService adLocationService,
            TuitionClassMapper tuitionClassMapper) {
        this.tuitionPromotions = tuitionPromotions;
        this.promotionSlotService = promotionSlotService;
        this.promotionSlotRepository = promotionSlotRepository;
        this.tuitionAttributeValues = tuitionAttributeValues;
        this.attributeOptions = attributeOptions;
        this.mediaRepository = mediaRepository;
        this.adLocationService = adLocationService;
        this.tuitionClassMapper = tuitionClassMapper;
    }

    // Target shape: promotions in the slot (1), attribute values+definitions (1), attribute
    // options for the small set of select-type values actually present (0-1), locations (1),
    // primary media (1). No COUNT query - the carousel is a fixed-size Pageable slice of a small,
    // capacity-limited slot.
    //
    // slotCode, when present, resolves a specific slot by its unique code (e.g. the detail page's
    // TUITION_DETAIL_TOP_CAROUSEL) instead of the default category-ancestor-walk TUITION_FEATURED
    // lookup - see class javadoc. excludeAdId, when present, drops that one ad from the result (the
    // detail page excluding the listing currently being viewed) by over-fetching one extra row so a
    // match never shrinks the page below the requested size.
    @Transactional(readOnly = true)
    public List<TuitionFeaturedCardResponse> getFeatured(Integer requestedSize, String slotCode, Long excludeAdId) {
        Optional<PromotionSlot> slot = (slotCode != null && !slotCode.isBlank())
                ? promotionSlotRepository.findByCode(slotCode.trim())
                : promotionSlotService.resolveCategoryFeaturedSlot(TUITION_ROOT_SLUG);
        if (slot.isEmpty()) {
            return List.of();
        }

        int size = clampSize(requestedSize);
        int fetchSize = excludeAdId != null ? size + 1 : size;
        List<Promotion> featured = tuitionPromotions.findByStatusAndPlan_SlotAndAd_StatusAndAd_SourceChannelAndEndsAtAfterOrderByStartsAtDescIdAsc(
                PromotionStatus.ACTIVE, slot.get(), AdStatus.ACTIVE, SourceChannel.TUITION, Instant.now(), PageRequest.of(0, fetchSize));
        if (excludeAdId != null) {
            featured = featured.stream().filter(p -> !p.getAd().getId().equals(excludeAdId)).limit(size).toList();
        }
        if (featured.isEmpty()) {
            return List.of();
        }

        List<Ad> ads = featured.stream().map(Promotion::getAd).toList();
        List<Long> adIds = ads.stream().map(Ad::getId).toList();

        List<AdAttributeValue> attributeRows = tuitionAttributeValues.findByAdIdInAndKeyIn(adIds, CARD_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        Map<Long, List<AdAttributeValue>> attributeRowsByAd = attributeRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAd().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<LocationResponse>> locationsByAd = adLocationService.toResponsesForAds(adIds);
        Map<Long, Media> primaryMediaByAd = primaryMediaByAd(adIds);

        return ads.stream()
                .map(ad -> tuitionClassMapper.toFeaturedCardResponse(
                        ad,
                        primaryMediaByAd.get(ad.getId()),
                        attributeRowsByAd.getOrDefault(ad.getId(), List.of()),
                        optionsByDefinition,
                        locationsByAd.getOrDefault(ad.getId(), List.of()).stream().findFirst().orElse(null)))
                .toList();
    }

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

    private int clampSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(requestedSize, MAX_SIZE);
    }
}
