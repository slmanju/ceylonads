package com.slmanju.ceylonads.search.service;

import com.slmanju.ceylonads.ad.dto.AdAttributeResponse;
import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.mapper.AdMapper;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.service.AdAttributeService;
import com.slmanju.ceylonads.ad.specification.AdAttributeSpecifications;
import com.slmanju.ceylonads.ad.specification.AdSpecifications;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.category.service.CategoryHierarchyService;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.common.web.PageResponse;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import com.slmanju.ceylonads.location.service.LocationHierarchyService;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.service.MediaService;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import com.slmanju.ceylonads.promotion.specification.AdPromotionSpecifications;
import com.slmanju.ceylonads.search.dto.AdSortOption;
import com.slmanju.ceylonads.search.dto.AttributeFilterCriterion;
import com.slmanju.ceylonads.search.specification.AdKeywordSpecifications;
import com.slmanju.ceylonads.search.specification.AdRelevanceOrdering;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdRepository ads;
    private final AdMapper adMapper;
    private final PromotionService promotionService;
    private final EntityManager entityManager;
    private final MediaService mediaService;
    private final AdAttributeService adAttributeService;
    private final AdLocationService adLocationService;
    private final CategoryRepository categories;
    private final CategoryHierarchyService categoryHierarchy;
    private final LocationRepository locations;
    private final LocationHierarchyService locationHierarchy;
    private final AttributeFilterValidator attributeFilterValidator;

    public AdSearchService(
            AdRepository ads,
            AdMapper adMapper,
            PromotionService promotionService,
            EntityManager entityManager,
            MediaService mediaService,
            AdAttributeService adAttributeService,
            AdLocationService adLocationService,
            CategoryRepository categories,
            CategoryHierarchyService categoryHierarchy,
            LocationRepository locations,
            LocationHierarchyService locationHierarchy,
            AttributeFilterValidator attributeFilterValidator) {
        this.ads = ads;
        this.adMapper = adMapper;
        this.promotionService = promotionService;
        this.entityManager = entityManager;
        this.mediaService = mediaService;
        this.adAttributeService = adAttributeService;
        this.adLocationService = adLocationService;
        this.categories = categories;
        this.categoryHierarchy = categoryHierarchy;
        this.locations = locations;
        this.locationHierarchy = locationHierarchy;
        this.attributeFilterValidator = attributeFilterValidator;
    }

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
            List<AttributeFilterCriterion> attributeFilters,
            SourceChannel channel) {
        return search(q, category, location, minPrice, maxPrice, page, size, sort, attributeFilters, channel, true);
    }

    // applyPromotionBoost=false skips the CATEGORY_FEATURED/TOP_SEARCH ranking boost below entirely
    // (content is always exactly `size` purely organic ads, never fewer, and totalElements reflects
    // only baseSpec's match count). Not used by TuitionClassService.search any more - Tuition's own
    // Search Boost product (TUITION_SEARCH_BOOST) ranks inside these same results via the
    // slot-code overload below, rather than opting out of ranking boost entirely.
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
            List<AttributeFilterCriterion> attributeFilters,
            SourceChannel channel,
            boolean applyPromotionBoost) {

        Specification<Ad> baseSpec = buildBaseSpecification(q, category, location, minPrice, maxPrice, attributeFilters, channel);

        // The promoted pool is expected to stay small (it's a paid, limited placement), so it's
        // fetched eagerly and sorted in Java; the potentially large "everything else" pool is
        // never loaded in full - it's always queried with a proper offset/limit at the DB level.
        List<Ad> allPromoted;
        int visibleCount;
        if (applyPromotionBoost) {
            // A category filter means this is effectively a category page: boost CATEGORY_FEATURED.
            // Otherwise this is a general browse/search: boost TOP_SEARCH. Either way the promoted
            // ad must already satisfy baseSpec, so promotion changes ranking, never relevance.
            PlacementType boostType = (category != null && !category.isBlank())
                    ? PlacementType.CATEGORY_FEATURED
                    : PlacementType.TOP_SEARCH;

            allPromoted = ads.findAll(baseSpec.and(AdPromotionSpecifications.hasActivePromotion(boostType)));
            Map<Long, Instant> startsAtByAdId = promotionService.activeStartsAtForAds(
                    allPromoted.stream().map(Ad::getId).toList(), boostType);
            allPromoted.sort(Comparator.comparing(
                    (Ad ad) -> startsAtByAdId.getOrDefault(ad.getId(), Instant.EPOCH)).reversed());

            // A placement's capacity (how many campaigns can be sold) can exceed its visibleCount
            // (how many should actually be boosted to the top at once). Only TOP_SEARCH is capped
            // here - general browse/search is the only case this ticket's "SEARCH_TOP" behavior
            // covers; ads beyond the cap still appear, just ranked normally rather than boosted.
            visibleCount = boostType == PlacementType.TOP_SEARCH ? promotionService.topSearchVisibleCount() : Integer.MAX_VALUE;
        } else {
            allPromoted = List.of();
            visibleCount = 0;
        }

        return assemblePage(baseSpec, allPromoted, visibleCount, page, size, sort, q);
    }

    // Tuition's Search Boost (TUITION_SEARCH_BOOST): ranks matching ads with a currently active
    // promotion in this EXACT slot code first within the same organic result set - boosted ads
    // occupy one of the normal `size` results, they are never additional to it, and every filter/
    // sort/pagination rule below is identical to the placement-type overload above. Slot-code
    // (rather than PlacementType) based because TUITION_SEARCH_BOOST's placement type can be shared
    // with other, unrelated slots - see AdPromotionSpecifications.hasActivePromotionSlotCode.
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
            List<AttributeFilterCriterion> attributeFilters,
            SourceChannel channel,
            String boostSlotCode) {

        Specification<Ad> baseSpec = buildBaseSpecification(q, category, location, minPrice, maxPrice, attributeFilters, channel);

        List<Ad> allPromoted = List.of();
        int visibleCount = 0;
        if (boostSlotCode != null && !boostSlotCode.isBlank()) {
            allPromoted = new ArrayList<>(ads.findAll(baseSpec.and(AdPromotionSpecifications.hasActivePromotionSlotCode(boostSlotCode))));
            Map<Long, Instant> startsAtByAdId = promotionService.activeStartsAtForAdsBySlotCode(
                    allPromoted.stream().map(Ad::getId).toList(), boostSlotCode);
            allPromoted.sort(Comparator.comparing(
                    (Ad ad) -> startsAtByAdId.getOrDefault(ad.getId(), Instant.EPOCH)).reversed());
            visibleCount = promotionService.visibleCountForSlotCode(boostSlotCode);
        }

        return assemblePage(baseSpec, allPromoted, visibleCount, page, size, sort, q);
    }

    // Shared by both search() overloads above: given a resolved, already-priority-sorted promoted
    // pool (which may be empty), splits it against `visibleCount`, excludes the visible promoted
    // ads from the normal DB-paginated pool, and windows/maps both pools into one page - so ranking
    // boost by placement type and by exact slot code share identical pagination/totalElements/
    // promoted-flag semantics instead of two parallel implementations.
    private PageResponse<AdResponse> assemblePage(
            Specification<Ad> baseSpec, List<Ad> allPromoted, int visibleCount, Integer page, Integer size, String sort, String q) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size < 1) ? 20 : Math.min(size, MAX_PAGE_SIZE);
        // NEWEST covers both "no sort requested" and an explicit ?sort=newest, so it's the only
        // option relevance ranking layers onto (createdAt DESC as the tiebreaker/fallback when q
        // is blank keeps this identical to today's behavior). oldest/price_asc/price_desc are an
        // explicit ask for a specific ordering and stay exactly as before, untouched by relevance.
        AdSortOption sortOption = AdSortOption.fromParam(sort);

        List<Ad> promotedAds = allPromoted.size() > visibleCount ? allPromoted.subList(0, visibleCount) : allPromoted;

        Set<Long> boostedIds = promotedAds.stream().map(Ad::getId).collect(Collectors.toSet());
        Specification<Ad> excludeBoosted = AdSpecifications.excludingIds(boostedIds);
        Specification<Ad> normalSpec = excludeBoosted == null ? baseSpec : baseSpec.and(excludeBoosted);
        long normalTotal = ads.count(normalSpec);

        long totalElements = promotedAds.size() + normalTotal;
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements / (double) safeSize);

        long windowStart = (long) safePage * safeSize;
        long windowEnd = windowStart + safeSize;

        List<Ad> pagePromoted = List.of();
        if (windowStart < promotedAds.size()) {
            int from = (int) windowStart;
            int to = (int) Math.min(windowEnd, promotedAds.size());
            pagePromoted = promotedAds.subList(from, to);
        }

        int remaining = safeSize - pagePromoted.size();
        List<Ad> pageNormal = List.of();
        if (remaining > 0) {
            long normalOffset = Math.max(0, windowStart - promotedAds.size());
            pageNormal = fetchWithOffsetLimit(normalSpec, sortOption, q, normalOffset, remaining);
        }

        // Media/attributes are only fetched for the page actually being returned (not the whole
        // promoted pool or the whole matching set), and in two queries total regardless of page
        // size, instead of two queries per ad.
        List<Ad> pageAds = Stream.concat(pagePromoted.stream(), pageNormal.stream()).toList();
        List<Long> pageAdIds = pageAds.stream().map(Ad::getId).toList();
        Map<Long, List<Media>> mediaByAdId = mediaService.byAdIds(pageAdIds);
        Map<Long, List<AdAttributeResponse>> attributesByAdId = adAttributeService.toResponsesForAds(pageAdIds);
        Map<Long, List<LocationResponse>> locationsByAdId = adLocationService.toResponsesForAds(pageAdIds);

        List<AdResponse> content = new ArrayList<>();
        for (Ad ad : pagePromoted) {
            content.add(adMapper.toResponse(ad, true, mediaByAdId.getOrDefault(ad.getId(), List.of()),
                    attributesByAdId.getOrDefault(ad.getId(), List.of()), locationsByAdId.getOrDefault(ad.getId(), List.of())));
        }
        for (Ad ad : pageNormal) {
            content.add(adMapper.toResponse(ad, false, mediaByAdId.getOrDefault(ad.getId(), List.of()),
                    attributesByAdId.getOrDefault(ad.getId(), List.of()), locationsByAdId.getOrDefault(ad.getId(), List.of())));
        }

        boolean first = safePage == 0;
        boolean last = windowEnd >= totalElements;

        return new PageResponse<>(content, safePage, safeSize, totalElements, totalPages, first, last);
    }

    // The reusable "which ads match these filters" predicate behind both search() overloads above -
    // category/location slug resolution (self + descendants), price range, keyword, and attr.<key>
    // filters, always scoped to `channel` and to active/unexpired ads.
    public Specification<Ad> buildBaseSpecification(
            String q,
            String category,
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<AttributeFilterCriterion> attributeFilters,
            SourceChannel channel) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }

        attributeFilterValidator.validate(attributeFilters);

        // category=vehicles must also match Cars/Motorcycles/... ads, so the slug resolves to the
        // category itself plus every descendant id rather than an exact-slug join.
        Set<Long> categoryIds = null;
        if (category != null && !category.isBlank()) {
            Category resolvedCategory = categories.findBySlugAndActiveTrue(category)
                    .orElseThrow(() -> new NotFoundException("Category not found: " + category));
            categoryIds = categoryHierarchy.descendantIdsInclusive(resolvedCategory);
        }

        // Same reasoning as category: a parent location (province/district) must include every
        // descendant location's ads.
        Set<Long> locationIds = null;
        if (location != null && !location.isBlank()) {
            Location resolvedLocation = locations.findBySlugAndActiveTrue(location)
                    .orElseThrow(() -> new NotFoundException("Location not found: " + location));
            locationIds = locationHierarchy.descendantIdsInclusive(resolvedLocation);
        }

        // Several of these legitimately come back null (e.g. AdKeywordSpecifications.matches(q)
        // when q is blank) and get filtered out below - List.of(...) would reject those nulls
        // outright, so build the list by hand instead.
        List<Specification<Ad>> specs = new ArrayList<>();
        specs.add(AdSpecifications.active());
        // Channel boundary: every result/count query built from this spec inherits this, so
        // pagination can never drift between content and totalElements. Callers pick their own
        // channel - AdController's generic /api/ads always passes MAIN_SITE; TuitionClassController
        // passes TUITION - so this same filtering machinery serves both verticals without either
        // one leaking into the other's results.
        specs.add(AdSpecifications.sourceChannel(channel));
        // No-op for MAIN_SITE/BOARDING (expiresAt is always null there); this is what makes an
        // expired TUITION listing disappear from search/browse the instant expiresAt passes,
        // without waiting for the scheduled status flip to EXPIRED.
        specs.add(AdSpecifications.notExpired(Instant.now()));
        specs.add(AdKeywordSpecifications.matches(q));
        specs.add(AdSpecifications.categoryIdIn(categoryIds));
        specs.add(AdSpecifications.locationIdIn(locationIds));
        specs.add(AdSpecifications.minPrice(minPrice));
        specs.add(AdSpecifications.maxPrice(maxPrice));

        for (AttributeFilterCriterion criterion : attributeFilters) {
            specs.add(criterion.isRange()
                    ? AdAttributeSpecifications.hasAttributeNumberInRange(criterion.key(), criterion.min(), criterion.max())
                    : AdAttributeSpecifications.hasAttributeValue(criterion.key(), criterion.value()));
        }

        return specs.stream()
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElseThrow();
    }

    private List<Ad> fetchWithOffsetLimit(Specification<Ad> spec, AdSortOption sortOption, String q, long offset, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Ad> query = cb.createQuery(Ad.class);
        Root<Ad> root = query.from(Ad.class);
        // To-one associations only (never a collection), so this can't multiply rows and stays
        // safe to combine with setFirstResult/setMaxResults below. Locations are 0..N per ad, so
        // they're deliberately batch-loaded afterwards (see locationsByAdId below) instead of
        // fetched here, which would multiply/duplicate rows under this offset/limit query.
        root.fetch("category", JoinType.LEFT);
        root.fetch("seller", JoinType.LEFT);

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        List<Order> orders = sortOption == AdSortOption.NEWEST
                ? AdRelevanceOrdering.apply(root, query, cb, q)
                : QueryUtils.toOrders(sortOption.sort(), root, cb);
        query.orderBy(orders);

        return entityManager.createQuery(query)
                .setFirstResult((int) offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
