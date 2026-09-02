package com.slmanju.ceylonads.ad.service;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdLocation;
import com.slmanju.ceylonads.ad.repository.AdLocationRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.service.CategoryHierarchyService;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.mapper.LocationMapper;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates and persists an ad's 0..N locations. Mirrors AdAttributeService's shape (validate on
 * write, batch-read for lists) so the same query-count discipline applies to locations as it does
 * to attributes; kept as its own service because the cardinality rule (whether zero/one/many is
 * allowed) is a business rule about the category, not a generic attribute concern.
 */
@Service
public class AdLocationService {

    // Root category slugs, from LocalDataSeeder's master data. Services ads may legitimately cover
    // no fixed location (remote/islandwide); every other category preserves the pre-existing
    // "a location is required" behavior, except Tuition which depends on its own Class Mode value.
    private static final String SERVICES_ROOT_SLUG = "services";
    private static final String TUITION_ROOT_SLUG = "education-tuition";
    private static final String TUITION_CLASS_MODE_KEY = "classMode";
    private static final String TUITION_CLASS_MODE_ONLINE = "ONLINE";

    private final AdLocationRepository adLocations;
    private final LocationRepository locations;
    private final LocationMapper locationMapper;
    private final CategoryHierarchyService categoryHierarchy;

    public AdLocationService(
            AdLocationRepository adLocations,
            LocationRepository locations,
            LocationMapper locationMapper,
            CategoryHierarchyService categoryHierarchy) {
        this.adLocations = adLocations;
        this.locations = locations;
        this.locationMapper = locationMapper;
        this.categoryHierarchy = categoryHierarchy;
    }

    @Transactional
    public void replaceLocations(Ad ad, List<String> rawSlugs, Map<String, String> attributes) {
        List<Location> resolved = resolveLocations(rawSlugs == null ? List.of() : rawSlugs);
        validateCardinality(ad.getCategory(), attributes == null ? Map.of() : attributes, resolved);

        adLocations.deleteByAdId(ad.getId());
        List<AdLocation> built = resolved.stream().map(location -> new AdLocation(ad, location)).toList();
        adLocations.saveAll(built);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> toResponses(Long adId) {
        return adLocations.findByAdIdOrderByLocationNameAsc(adId).stream()
                .map(al -> locationMapper.toResponse(al.getLocation()))
                .toList();
    }

    // Batch path: one query total regardless of how many ads are passed in, same shape as
    // AdAttributeService.toResponsesForAds.
    @Transactional(readOnly = true)
    public Map<Long, List<LocationResponse>> toResponsesForAds(Collection<Long> adIds) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<LocationResponse>> result = new LinkedHashMap<>();
        for (AdLocation al : adLocations.findByAdIdInOrderByAdIdAsc(adIds)) {
            result.computeIfAbsent(al.getAd().getId(), k -> new ArrayList<>())
                    .add(locationMapper.toResponse(al.getLocation()));
        }
        return result;
    }

    private List<Location> resolveLocations(List<String> rawSlugs) {
        // Case-insensitive de-dupe (trim blanks) so the same city selected twice, or submitted
        // with different casing, doesn't produce two rows against the unique constraint.
        Set<String> seen = new LinkedHashSet<>();
        List<Location> resolved = new ArrayList<>();
        for (String rawSlug : rawSlugs) {
            if (rawSlug == null || rawSlug.isBlank()) {
                continue;
            }
            String slug = rawSlug.trim().toLowerCase();
            if (!seen.add(slug)) {
                continue;
            }
            Location location = locations.findBySlugAndActiveTrue(slug)
                    .orElseThrow(() -> new NotFoundException("Location not found: " + rawSlug));
            resolved.add(location);
        }
        return resolved;
    }

    private void validateCardinality(Category category, Map<String, String> attributes, List<Location> resolved) {
        String rootSlug = categoryHierarchy.ancestorChainInclusive(category).get(0).getSlug();

        if (SERVICES_ROOT_SLUG.equals(rootSlug)) {
            return;
        }

        if (TUITION_ROOT_SLUG.equals(rootSlug)) {
            boolean online = TUITION_CLASS_MODE_ONLINE.equalsIgnoreCase(attributes.get(TUITION_CLASS_MODE_KEY));
            if (online && !resolved.isEmpty()) {
                throw new BadRequestException("Online classes cannot have a physical location");
            }
            if (!online && resolved.isEmpty()) {
                throw new BadRequestException("At least one location is required unless Class Mode is Online");
            }
            return;
        }

        // Vehicles, Property, Mobile Phones, and anything else added later: preserve the
        // pre-existing "every ad has a location" behavior.
        if (resolved.isEmpty()) {
            throw new BadRequestException("At least one location is required");
        }
    }
}
