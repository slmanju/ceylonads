package com.slmanju.ceylonads.tuition.service;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.dto.CreateAdRequest;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.search.dto.AttributeFilterCriterion;
import com.slmanju.ceylonads.search.service.AdSearchService;
import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.common.web.PageResponse;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.service.CustomerService;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.tuition.dto.TuitionClassCardResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionClassCreateRequest;
import com.slmanju.ceylonads.tuition.dto.TuitionClassDetailResponse;
import com.slmanju.ceylonads.tuition.mapper.TuitionClassMapper;
import com.slmanju.ceylonads.tuition.repository.TuitionAdAttributeValueRepository;
import com.slmanju.ceylonads.tuition.repository.TuitionAdRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application/service layer for the CeylonAds Tuition vertical: an isolated read path (see class
 * doc on the read methods below) plus the Tuition class lifecycle (create/update/deactivate/My
 * Classes). Every write here delegates the actual Ad persistence - seller resolution, category
 * validation, attribute/location persistence, transaction boundary - to the shared
 * {@link AdService#createAd}/{@link AdService#updateAd}/{@link AdService#deactivateOwned}, always
 * passing {@link SourceChannel#TUITION}; this class only adds Tuition-specific category validation
 * and maps between {@link TuitionClassCreateRequest}/{@link TuitionClassDetailResponse} and the
 * shared {@link CreateAdRequest} command. {@link TuitionAdRepository} stays read-only (it extends
 * the bare {@code Repository} marker, not {@code JpaRepository}), so no write to {@code ads} is
 * even possible from this package outside that shared core.
 */
@Service
public class TuitionClassService {

    // Master data root, from LocalDataSeeder - see AdLocationService.TUITION_ROOT_SLUG for the
    // matching write-path constant.
    private static final String TUITION_ROOT_SLUG = "education-tuition";

    private static final List<String> DETAIL_ATTRIBUTE_KEYS =
            List.of("subject", "grade", "curriculum", "medium", "classMode", "classType");
    private static final List<String> CARD_ATTRIBUTE_KEYS = List.of("subject", "grade", "curriculum", "medium");

    private static final int DEFAULT_SIMILAR_SIZE = 3;
    private static final int MAX_SIMILAR_SIZE = 10;

    private static final int DEFAULT_LATEST_SIZE = 6;
    private static final int MAX_LATEST_SIZE = 50;

    private final TuitionAdRepository tuitionAds;
    private final TuitionAdAttributeValueRepository tuitionAttributeValues;
    private final AttributeOptionRepository attributeOptions;
    private final MediaRepository mediaRepository;
    private final AdLocationService adLocationService;
    private final TuitionClassMapper tuitionClassMapper;
    private final AdService adService;
    private final CategoryRepository categories;
    private final CustomerService customerService;
    private final AdSearchService adSearchService;

    public TuitionClassService(
            TuitionAdRepository tuitionAds,
            TuitionAdAttributeValueRepository tuitionAttributeValues,
            AttributeOptionRepository attributeOptions,
            MediaRepository mediaRepository,
            AdLocationService adLocationService,
            TuitionClassMapper tuitionClassMapper,
            AdService adService,
            CategoryRepository categories,
            CustomerService customerService,
            AdSearchService adSearchService) {
        this.tuitionAds = tuitionAds;
        this.tuitionAttributeValues = tuitionAttributeValues;
        this.attributeOptions = attributeOptions;
        this.mediaRepository = mediaRepository;
        this.adLocationService = adLocationService;
        this.tuitionClassMapper = tuitionClassMapper;
        this.adService = adService;
        this.categories = categories;
        this.customerService = customerService;
        this.adSearchService = adSearchService;
    }

    // --- Lifecycle (create / update / deactivate / My Classes) --------------------------------

    @Transactional
    public TuitionClassDetailResponse create(String username, TuitionClassCreateRequest request) {
        requireTuitionCategory(request.categorySlug());
        Ad ad = adService.createAd(username, toCommand(request), SourceChannel.TUITION);
        return toDetailResponses(List.of(ad)).get(0);
    }

    // AdService.updateAd verifies the ad is owned by username AND already TUITION (expectedChannel)
    // before this ever runs - the ad can't have been switched to another channel here, only its
    // category could try to leave the Tuition tree, which requireTuitionCategory below blocks.
    @Transactional
    public TuitionClassDetailResponse update(Long id, String username, TuitionClassCreateRequest request) {
        requireTuitionCategory(request.categorySlug());
        Ad ad = adService.updateAd(id, username, toCommand(request), SourceChannel.TUITION);
        return toDetailResponses(List.of(ad)).get(0);
    }

    // Same semantics as the generic AdController#deactivate: a status change (DEACTIVATED), not a
    // hard delete - see Ad#deactivate.
    @Transactional
    public void deactivateOwned(Long id, String username) {
        adService.deactivateOwned(id, username, SourceChannel.TUITION);
    }

    @Transactional(readOnly = true)
    public List<TuitionClassDetailResponse> myClasses(String username) {
        Customer customer = customerService.requireByUsername(username);
        List<Ad> myAds = tuitionAds.findBySellerIdAndSourceChannelOrderByCreatedAtDesc(customer.getId(), SourceChannel.TUITION);
        return toDetailResponses(myAds);
    }

    private CreateAdRequest toCommand(TuitionClassCreateRequest request) {
        return new CreateAdRequest(
                request.title(),
                request.description(),
                request.price(),
                request.categorySlug(),
                null,
                request.locationSlugs(),
                toAttributeMap(request),
                request.contactName(),
                request.phoneNumber(),
                request.whatsappNumber());
    }

    // Maps the typed Tuition fields onto the existing subject/grade/curriculum/medium/classMode/
    // classType attribute keys; only non-blank fields are included; AdAttributeService (called
    // transitively by AdService.createAd/updateAd) is what actually validates required-ness and
    // option values against the ad's real category master data.
    private Map<String, String> toAttributeMap(TuitionClassCreateRequest request) {
        Map<String, String> attrs = new LinkedHashMap<>();
        putIfPresent(attrs, "subject", request.subject());
        putIfPresent(attrs, "grade", request.level());
        putIfPresent(attrs, "curriculum", request.curriculum());
        if (request.medium() != null && !request.medium().isEmpty()) {
            attrs.put("medium", String.join(",", request.medium()));
        }
        putIfPresent(attrs, "classMode", request.deliveryMode());
        putIfPresent(attrs, "classType", request.classFormat());
        return attrs;
    }

    private void putIfPresent(Map<String, String> attrs, String key, String value) {
        if (value != null && !value.isBlank()) {
            attrs.put(key, value.trim());
        }
    }

    // Batched detail-response builder shared by create/update (a single-element list) and
    // myClasses (a full list): media/attributes/locations are fetched in three queries total
    // regardless of how many ads are being mapped, instead of three queries per ad.
    private List<TuitionClassDetailResponse> toDetailResponses(List<Ad> adsToMap) {
        if (adsToMap.isEmpty()) {
            return List.of();
        }
        List<Long> adIds = adsToMap.stream().map(Ad::getId).toList();

        Map<Long, List<Media>> mediaByAd = new LinkedHashMap<>();
        for (Media media : mediaRepository.findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(adIds)) {
            mediaByAd.computeIfAbsent(media.getAd().getId(), k -> new ArrayList<>()).add(media);
        }

        List<AdAttributeValue> attributeRows = tuitionAttributeValues.findByAdIdInAndKeyIn(adIds, DETAIL_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        Map<Long, List<AdAttributeValue>> attributeRowsByAd = attributeRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAd().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<LocationResponse>> locationsByAd = adLocationService.toResponsesForAds(adIds);

        return adsToMap.stream()
                .map(ad -> tuitionClassMapper.toDetailResponse(
                        ad,
                        mediaByAd.getOrDefault(ad.getId(), List.of()),
                        attributeRowsByAd.getOrDefault(ad.getId(), List.of()),
                        optionsByDefinition,
                        locationsByAd.getOrDefault(ad.getId(), List.of())))
                .toList();
    }

    // Validates a category slug is within the Tuition tree before creating/updating an ad through
    // this vertical - the generic /api/ads/** create/update flow never calls this, and a Tuition
    // request can never slip a "vehicles"/"property" categorySlug through.
    private void requireTuitionCategory(String categorySlug) {
        Category category = categories.findBySlugAndActiveTrue(categorySlug)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (!isTuitionCategory(category)) {
            throw new BadRequestException("categorySlug must be within the Education & Tuition category tree");
        }
    }

    // Target shape: ad+category+parent+seller (1), media (1), attribute values+definitions (1),
    // attribute options for the small set of select-type values actually present (0-1), locations
    // (1).
    @Transactional(readOnly = true)
    public TuitionClassDetailResponse getDetailBySlug(String slug) {
        Ad ad = loadTuitionAd(slug);

        List<Media> media = mediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc(ad.getId());
        List<AdAttributeValue> attributeRows =
                tuitionAttributeValues.findByAdIdInAndKeyIn(List.of(ad.getId()), DETAIL_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        List<LocationResponse> locations = adLocationService.toResponses(ad.getId());

        return tuitionClassMapper.toDetailResponse(ad, media, attributeRows, optionsByDefinition, locations);
    }

    @Transactional(readOnly = true)
    public List<TuitionClassCardResponse> getSimilarBySlug(String slug, Integer requestedSize) {
        int size = clampSize(requestedSize);
        Ad current = loadTuitionAd(slug);

        List<Ad> candidates = tuitionAds.findTop20ByCategoryIdAndStatusAndSourceChannelAndIdNotOrderByCreatedAtDesc(
                current.getCategory().getId(), AdStatus.ACTIVE, SourceChannel.TUITION, current.getId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> allIds = new ArrayList<>(candidates.size() + 1);
        allIds.add(current.getId());
        candidates.forEach(candidate -> allIds.add(candidate.getId()));

        List<AdAttributeValue> attributeRows = tuitionAttributeValues.findByAdIdInAndKeyIn(allIds, CARD_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        Map<Long, List<AdAttributeValue>> attributeRowsByAd = attributeRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAd().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<LocationResponse>> locationsByAd = adLocationService.toResponsesForAds(allIds);
        Map<Long, Media> primaryMediaByAd = primaryMediaByAd(allIds);

        List<Ad> ranked = rankBySimilarity(current, candidates, attributeRowsByAd, locationsByAd);

        return ranked.stream()
                .limit(size)
                .map(candidate -> tuitionClassMapper.toCardResponse(
                        candidate,
                        primaryMediaByAd.get(candidate.getId()),
                        attributeRowsByAd.getOrDefault(candidate.getId(), List.of()),
                        optionsByDefinition,
                        locationsByAd.getOrDefault(candidate.getId(), List.of()).stream().findFirst().orElse(null)))
                .toList();
    }

    // Homepage/list "Latest Classes" feed: paginated, newest-first, scoped to the whole tuition
    // category tree (not a single leaf category like getSimilarBySlug). A real COUNT query is
    // unavoidable and intentional here - unlike the featured carousel, this is genuine page-by-page
    // browsing that needs an accurate totalPages for the frontend's Pagination control.
    @Transactional(readOnly = true)
    public PageResponse<TuitionClassCardResponse> getLatest(Integer requestedPage, Integer requestedSize) {
        int page = (requestedPage == null || requestedPage < 0) ? 0 : requestedPage;
        int size = clampLatestSize(requestedSize);

        Page<Ad> adsPage = tuitionAds.findByStatusAndSourceChannelOrderByCreatedAtDesc(
                AdStatus.ACTIVE, SourceChannel.TUITION, PageRequest.of(page, size));
        List<Ad> ads = adsPage.getContent();
        if (ads.isEmpty()) {
            return PageResponse.from(adsPage.map(ad -> (TuitionClassCardResponse) null));
        }

        List<Long> adIds = ads.stream().map(Ad::getId).toList();
        List<AdAttributeValue> attributeRows = tuitionAttributeValues.findByAdIdInAndKeyIn(adIds, CARD_ATTRIBUTE_KEYS);
        Map<Long, List<AttributeOption>> optionsByDefinition = optionsBySelectDefinition(attributeRows);
        Map<Long, List<AdAttributeValue>> attributeRowsByAd = attributeRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAd().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<LocationResponse>> locationsByAd = adLocationService.toResponsesForAds(adIds);
        Map<Long, Media> primaryMediaByAd = primaryMediaByAd(adIds);

        Map<Long, TuitionClassCardResponse> cardsById = ads.stream()
                .collect(Collectors.toMap(Ad::getId, ad -> tuitionClassMapper.toCardResponse(
                        ad,
                        primaryMediaByAd.get(ad.getId()),
                        attributeRowsByAd.getOrDefault(ad.getId(), List.of()),
                        optionsByDefinition,
                        locationsByAd.getOrDefault(ad.getId(), List.of()).stream().findFirst().orElse(null))));

        return PageResponse.from(adsPage.map(ad -> cardsById.get(ad.getId())));
    }

    // Full filtered/paginated tuition search (subject/grade/curriculum/medium/classMode via attr.*,
    // location, price range, free-text q, sort) for the Classes/Tutors/Online Classes pages -
    // reuses the exact same category-tree resolution, attribute filtering and pagination the
    // generic /api/ads search uses, scoped to SourceChannel.TUITION so it only ever returns Tuition
    // listings. Promotion ranking-boost is deliberately OFF (applyPromotionBoost=false): the
    // generic CATEGORY_FEATURED boost the main site uses here would also match TUITION_FEATURED
    // (the Homepage Featured slot, which happens to share that placement type), incorrectly
    // ranking/badging a Homepage Featured purchase as "boosted" in search too. Tuition's actual
    // Search Boost product (TUITION_SEARCH_BOOST) is a separate, additive placement instead - see
    // TuitionFeaturedService (GET /api/tuition/featured?slot=TUITION_SEARCH_BOOST) and
    // ClassSearchResults.tsx - so these results are always exactly `size` organic listings.
    @Transactional(readOnly = true)
    public PageResponse<AdResponse> search(
            String q,
            String category,
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer page,
            Integer size,
            String sort,
            List<AttributeFilterCriterion> attributeFilters) {
        return adSearchService.search(q, category, location, minPrice, maxPrice, page, size, sort,
                attributeFilters, SourceChannel.TUITION, false);
    }

    private int clampLatestSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize < 1) {
            return DEFAULT_LATEST_SIZE;
        }
        return Math.min(requestedSize, MAX_LATEST_SIZE);
    }

    // sourceChannel = TUITION is the sole gate here - not category. Category is validated once, at
    // create/update time (requireTuitionCategory below); every read trusts the channel instead of
    // re-walking the category tree on every request.
    private Ad loadTuitionAd(String slug) {
        Long id = Slugs.extractTrailingId(slug);
        return tuitionAds.findByIdAndStatusAndSourceChannel(id, AdStatus.ACTIVE, SourceChannel.TUITION)
                .orElseThrow(() -> new NotFoundException("Tuition class not found: " + slug));
    }

    // Current tuition category tree is exactly two levels deep (education-tuition -> its direct
    // children), so checking "is the root itself, or a direct child of the root" is sufficient
    // without walking the full category tree (CategoryHierarchyService) on every detail request.
    private boolean isTuitionCategory(Category category) {
        boolean isRoot = TUITION_ROOT_SLUG.equals(category.getSlug());
        boolean isDirectChild = category.getParent() != null && TUITION_ROOT_SLUG.equals(category.getParent().getSlug());
        return isRoot || isDirectChild;
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

    // Simple additive scoring, not a full ranking engine: same subject counts most, then level
    // (grade), then curriculum, then any overlapping location; ties keep the newest-first order the
    // candidate query already provided.
    private List<Ad> rankBySimilarity(
            Ad current,
            List<Ad> candidates,
            Map<Long, List<AdAttributeValue>> attributeRowsByAd,
            Map<Long, List<LocationResponse>> locationsByAd) {
        Map<String, String> currentValues = firstValueByKey(attributeRowsByAd.getOrDefault(current.getId(), List.of()));
        Set<String> currentLocationSlugs = locationsByAd.getOrDefault(current.getId(), List.of()).stream()
                .map(LocationResponse::slug)
                .collect(Collectors.toSet());

        return candidates.stream()
                .sorted(Comparator.comparingInt((Ad candidate) ->
                                similarityScore(candidate, currentValues, currentLocationSlugs, attributeRowsByAd, locationsByAd))
                        .reversed())
                .toList();
    }

    private int similarityScore(
            Ad candidate,
            Map<String, String> currentValues,
            Set<String> currentLocationSlugs,
            Map<Long, List<AdAttributeValue>> attributeRowsByAd,
            Map<Long, List<LocationResponse>> locationsByAd) {
        Map<String, String> candidateValues = firstValueByKey(attributeRowsByAd.getOrDefault(candidate.getId(), List.of()));

        int score = 0;
        if (sameValue(candidateValues, currentValues, "subject")) score += 3;
        if (sameValue(candidateValues, currentValues, "grade")) score += 2;
        if (sameValue(candidateValues, currentValues, "curriculum")) score += 1;

        boolean sameLocation = locationsByAd.getOrDefault(candidate.getId(), List.of()).stream()
                .map(LocationResponse::slug)
                .anyMatch(currentLocationSlugs::contains);
        if (sameLocation) score += 1;

        return score;
    }

    private boolean sameValue(Map<String, String> a, Map<String, String> b, String key) {
        String left = a.get(key);
        String right = b.get(key);
        return left != null && left.equalsIgnoreCase(right);
    }

    private Map<String, String> firstValueByKey(List<AdAttributeValue> rows) {
        Map<String, String> values = new LinkedHashMap<>();
        for (AdAttributeValue row : rows) {
            values.putIfAbsent(row.getAttributeDefinition().getKey(), row.getValueText());
        }
        return values;
    }

    private int clampSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize < 1) {
            return DEFAULT_SIMILAR_SIZE;
        }
        return Math.min(requestedSize, MAX_SIMILAR_SIZE);
    }
}
